/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.server;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.server.system.Classes;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.WeakHashMap;


/**
 * Functionality that ultimately allows us to obtain a class given its name and class loader.
 * One reason for this class to exist, is to enable access to non-public methods of class java.lang.ClassLoader,
 * that allow one to obtain a class loaded by the given loader, or make sure that this class hasn't been loaded by
 * the given loader. Also this class provides accounting for parent loader for each registered loader, which is
 * needed at the client side to correctly perform class instrumentation. Finally, we keep track of class (actually
 * class loader) unloading events, which is necessary e.g. during memory profiling, to prevent
 * getMethodNamesForJMethodIds from crashing or returning "unknown method" results.
 *
 * Manages:
 *  - class unloading
 *  - knowing what is loader for each class in CPU profiling
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
class ClassLoaderManager implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // TODO [release]: change value to TRUE to remove the print code below entirely by compiler
    private static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.server.ClassLoaderManager") != null; // NOI18N
    private static ProfilerServer profilerServer;
    private static WeakHashMap /*<ClassLoader, ClassLoaderManager>*/ manMap;
    private static Vector /*<ClassLoaderManager>*/ manVec;
    private static ReferenceQueue rq;
    private static boolean notifyToolAboutUnloadedClasses;
    private static Method findLoadedClassMethod;
    private static Method findBootstrapClassMethod;

    /*
       public static void reset() {
         manMap = null;
         manVec = null;
         rq = null;
       }
     */
    private static boolean notifyThreadIsRunning;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private PhantomReference targetLdrPhantomRef; // This is used to keep track of the moment when the loader is about
                                                  // to be GCed
    private WeakReference targetLdrWeakRef; // We use WeakReferences to prevent memory leaks due to direct
                                            // references to unused loaders
    private int indexIntoManVec; // Index into the vector of ClassLoaderManagers "manVec" below.
    private int parentLoaderId; // Index of the targetLoader's parent loader into "managers" below

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Instance methods ------------------------------------------------------------------------------------------------
    private ClassLoaderManager(ClassLoader targetLoader, int indexIntoManVec) {
        this.targetLdrWeakRef = new WeakReference(targetLoader);
        this.targetLdrPhantomRef = new PhantomReference(targetLoader, rq);
        this.indexIntoManVec = indexIntoManVec;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Debugging support */
    public String toString() {
        return ("CLManager: indexIntoManVec = " + indexIntoManVec + ", parentLoaderId = " + parentLoaderId); // NOI18N
    }

    static int getDefiningLoaderForClass(String className, int initiatingLoaderId) {
        if (initiatingLoaderId >= manVec.size()) {
            return -1;
        }

        ClassLoaderManager man = (ClassLoaderManager) manVec.get(initiatingLoaderId);

        if ((man == null) || (man.targetLdrWeakRef.get() == null)) {
            return -1;
        }

        Class clazz = man.getLoadedClassInThisLoaderOnly(className);

        if (clazz != null) {
            return registerLoader(clazz);
        } else {
            return -1;
        }
    }

    static Class getLoadedClass(String name, int loaderIdx) {
        if (loaderIdx == -1) {
            loaderIdx = 0;
        }

        Class res = ((ClassLoaderManager) manVec.get(loaderIdx)).getLoadedClass(name);

        if (res != null) {
            return res;
        } else {
            System.err.println(ENGINE_WARNING + "class " + name + " that should be instrumented is not loaded by target VM"); // NOI18N

            ClassLoader errLoader = (ClassLoader) (((ClassLoaderManager) manVec.get(loaderIdx)).targetLdrWeakRef.get());
            System.err.print("*** Requested classloader: " + errLoader); // NOI18N

            if (errLoader != null) {
                System.err.println(", its class = " + errLoader.getClass() + ", index = " // NOI18N
                                   + loaderIdx + ", hashcode = " + errLoader.hashCode()); // NOI18N
            } else {
                System.err.println(", its index = " + loaderIdx); // NOI18N
            }

            return null;
        }
    }

    static void setNotifyToolAboutUnloadedClasses(boolean v) {
        notifyToolAboutUnloadedClasses = v;
    }

    /**
     * Creates a table that maps loader id to its parent class loader.
     *
     * @return An array that maps class loader id (idx) to its parent class loader it ([idx])
     */
    static int[] getParentLoaderIdTable() {
        int size = manVec.size();
        int[] ret = new int[size];

        for (int i = 0; i < size; i++) {
            ret[i] = ((ClassLoaderManager) manVec.get(i)).parentLoaderId;
        }

        return ret;
    }

    /* Not used
       public static int getParentLoaderId(int thisLoaderId) {
         if (thisLoaderId == -1 || thisLoaderId == 0) return 0;
         else return ((ClassLoaderManager) manVec.get(thisLoaderId)).parentLoaderId;
       }
     */

    /**
     * This whole method exists, in addition to simple getParentLoaderId() above, to make possible passing to the tool
     * information about "chains of loaders" that may occasionally be discovered when a class is loaded. I.e. it may
     * happen that a new loader A is created, then A creates a child loader B, and finally a class is loaded by B. At
     * this time we happen to register *both* loaders B and A in registerLoader() above, and also parent loader A gets
     * a loaderId with a *greater* value than that for B.
     *
     * So for the tool to adequately reflect the class loader structure of the application, we need to pass info about
     * more than 2 loaders with a single class. Fortunately, when there are x>2 loaders involved, the first x-1 get
     * sequentially growing IDs. So what is returned here in the int[3] array, is the ID for the first and the last
     * loader in such a chain, plus the chain length (which is 0 in the case of a simple child-parent pair).
     *
     * @param thisLoaderId Class loader Id whose parent we are looking for
     * @return a 3 item array [0]=first loader in chain, [1]=last loader in chain, [2]=chain length, can be 0 in
     *         simple case
     */
    static int[] getThisAndParentLoaderData(int thisLoaderId) {
        if ((thisLoaderId == -1) || (thisLoaderId == 0)) {
            return new int[] { 0, 0, 0 };
        } else {
            int parentLoaderId = ((ClassLoaderManager) manVec.get(thisLoaderId)).parentLoaderId;

            if (parentLoaderId == -1) {
                parentLoaderId = 0;
            }

            if (parentLoaderId <= thisLoaderId) {
                return new int[] { thisLoaderId, parentLoaderId, 0 };
            } else {
                int ofs = 0;
                int curLoaderId = thisLoaderId;

                while (parentLoaderId > curLoaderId) {
                    ofs++;
                    curLoaderId = parentLoaderId;
                    parentLoaderId = ((ClassLoaderManager) manVec.get(curLoaderId)).parentLoaderId;
                }

                if (parentLoaderId < 0) {
                    parentLoaderId = 0;
                }

                return new int[] { thisLoaderId, parentLoaderId, ofs };
            }
        }
    }

    static void addLoader(ClassLoader loader) {
        if (DEBUG) {
            System.out.println("Add loader for: " + loader); // NOI18N
        }

        ClassLoaderManager ldrMan = (ClassLoaderManager) manMap.get(loader);

        if (ldrMan != null) {
            // a manager for this class loader already exists
            return;
        }

        // create new ClassLoaderManager, with the id being next available int
        int newId = manVec.size();
        ldrMan = new ClassLoaderManager(loader, newId);

        if (DEBUG) {
            System.out.println("ClassLoaderManager.DEBUG: Add loader for: " + loader + ", new Id: " + newId); // NOI18N
        }

        manMap.put(loader, ldrMan);
        manVec.add(ldrMan); // will be placed at the correct index: newId
    }

    /**
     * This method SHOULD be called frequently enough to allow unloaded classes go away.
     * On the other hand, since currently it's called from monitoring code, which itself has to execute at regular
     * enough intervals, it has to return quickly - that's why we are using a separate thread in it, that does the
     * potentially long-executing work.
     * In addition to just clearing a PhantomReference, the code could have removed the relevant ClassLoaderManager
     * from Vector/Hashtable that contain these managers. We can implement that later.
     */
    static void checkForUnloadedClasses() {
        if (rq == null) {
            return;
        }

        PhantomReference clRef = null;

        if ((clRef = (PhantomReference) rq.poll()) != null) {
            if (notifyToolAboutUnloadedClasses) {
                class NotifyThread extends Thread {
                    private PhantomReference clRef;

                    NotifyThread(PhantomReference clRef) {
                        this.clRef = clRef;
                        ThreadInfo.addProfilerServerThread(this);
                    }

                    public void run() {
                        notifyThreadIsRunning = true;

                        do {
                            // Note that there is a small chance that this call will not really dump all information, if some
                            // thread isfor some reason preempted while it's in traceObjAlloc() and not let finish it. This may
                            // result in some jmethodIDs not sent to client in time and thus not resolved in the call below. But
                            // the probability of such an event seems very low, and we also have exception handlers in native
                            // code now, to protect us from such mishaps.

                            // [ian]: this seems like a reason for EXCEPTION_ACCESS_VIOLATION when doing getMethodNamesForJMethodIds
                            ProfilerInterface.dumpExistingResults(false);

//                            synchronized (ProfilerServer.execInSeparateThreadLock) {
                                ProfilerInterface.serialClientOperationsLock.beginTrans(true);

                                try {
                                    // This will send the command to the client and will wait for a response from it, which will
                                    // come only after the client in turn asks the server for method names for all jmethodIDs it
                                    // currently has
                                    profilerServer.sendClassLoaderUnloadingCommand();
                                } finally {
                                    ProfilerInterface.serialClientOperationsLock.endTrans();
                                }
//                            }

                            clRef.clear();
                        } while ((clRef = (PhantomReference) rq.poll()) != null);

                        // Notify the native code that may cache class file bytes in a separate data structure,
                        // that some classes have gone
                        Classes.notifyAboutClassLoaderUnloading();
                        ThreadInfo.removeProfilerServerThread(this);
                        notifyThreadIsRunning = false;
                    }
                }

                if (!notifyThreadIsRunning) {
                    new NotifyThread(clRef).start(); // Otherwise will do the same thing next time we get here
                }
            } else {
                do {
                    clRef.clear();
                } while ((clRef = (PhantomReference) rq.poll()) != null);

                Classes.notifyAboutClassLoaderUnloading();
            }
        }
    }

    static void initialize(ProfilerServer inProfilerServer) {
        try {
            Class classLoaderClass = Class.forName("java.lang.ClassLoader"); // NOI18N
            Class[] stringArg = new Class[] { Class.forName("java.lang.String") }; // NOI18N
            findLoadedClassMethod = classLoaderClass.getDeclaredMethod("findLoadedClass", stringArg); // NOI18N
            findLoadedClassMethod.setAccessible(true); // REQUIRED to suppress
            findBootstrapClassMethod = classLoaderClass.getDeclaredMethod("findBootstrapClass", stringArg); // NOI18N
            findBootstrapClassMethod.setAccessible(true); // access checks
        } catch (Exception ex) {
            System.err.println("Profiler Agent Error: Internal error initializing ClassLoaderManager"); // NOI18N
            ex.printStackTrace(System.err);
        }

        // This is done to just initialize some reflection classes, which may otherwise be initialized only when
        // this class is used for the first time, and thus may cause endless class load recursion
        ClassLoaderManager clm = new ClassLoaderManager(ClassLoader.getSystemClassLoader(), 0);
        clm.getLoadedClass("java.lang.String"); // NOI18N

        profilerServer = inProfilerServer;

        manMap = new WeakHashMap();
        manVec = new Vector();
        rq = new ReferenceQueue();
    }

    static int registerLoader(Class clazz) {
        ClassLoader loader = clazz.getClassLoader();

        if (loader == null) {
            return -1;
        }

        int ret = registerLoader(loader);

        if (DEBUG) {
            System.out.println("ClassLoaderManager.DEBUG: Register loader for: " + clazz.getName() + ", ldr: " + loader
                               + ", id: " + ret); // NOI18N
        }

        return ret;
    }

    private static synchronized int registerLoader(ClassLoader loader) {
        ClassLoaderManager ldrMan = (ClassLoaderManager) manMap.get(loader);

        if (ldrMan != null) {
            if (ldrMan.targetLdrWeakRef.get() == loader) {
                return ldrMan.indexIntoManVec;
            } else {
                // This probably was a really bad (impossible?) clash - check if this loader is actually
                // registered somewhere
                int size = manVec.size();

                for (int i = 0; i < size; i++) {
                    ldrMan = (ClassLoaderManager) manVec.get(i);

                    if (ldrMan.targetLdrWeakRef.get() == loader) {
                        return ldrMan.indexIntoManVec;
                    }
                }
            }
        }

        int ldrIdx = manVec.size();
        ldrMan = new ClassLoaderManager(loader, ldrIdx);
        manMap.put(loader, ldrMan);
        manVec.add(ldrMan);

        ClassLoader parentLoader = loader.getParent();
        ldrMan.parentLoaderId = (parentLoader != null) ? registerLoader(parentLoader) : (-1);

        return ldrIdx;
    }

    /** Get a class with the given name, if it is loaded by the given class loader or one of its parent loaders */
    private Class getLoadedClass(String className) {
        try {
            Object[] args = new Object[] { className };
            ClassLoader loader = (ClassLoader) targetLdrWeakRef.get();

            while (loader != null) {
                Class res = (Class) findLoadedClassMethod.invoke(loader, args);

                // Class res = targetLoader.findLoadedClass(className);
                if (res != null) {
                    return res;
                } else {
                    loader = loader.getParent();
                }
            }

            try {
                return (Class) findBootstrapClassMethod.invoke(ClassLoader.getSystemClassLoader(), args);

                // targetLoader.findBootstrapClass(className);
            } catch (Exception ex) { // ClassNotFoundException may be thrown

                return null;
            }
        } catch (Exception ex) {
            System.err.println("Profiler Agent Error: internal error in ClassLoaderManager 1"); // NOI18N
            ex.printStackTrace(System.err);
        }

        return null;
    }

    private Class getLoadedClassInThisLoaderOnly(String className) {
        try {
            Object[] args = new Object[] { className };
            ClassLoader loader = (ClassLoader) targetLdrWeakRef.get();

            if (loader != null) {
                return (Class) findLoadedClassMethod.invoke(loader, args);

                // Class res = targetLoader.findLoadedClass(className);
            }
        } catch (Exception ex) {
            System.err.println("Profiler Agent Error: internal error in ClassLoaderManager 2"); // NOI18N
            ex.printStackTrace(System.err);
        }

        return null;
    }
}
