/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.jfluid.server.system;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;


/**
 * Provides methods for dynamic bytecode redefinition and other class-related operations.
 * A version for JDK 1.5
 *
 * @author Tomas Hurka
 * @author  Misha Dmitriev
 */
public class Classes {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /** We convert all error codes or exceptions thrown by redefine calls into a single error message in
     * this exception
     */
    public static class RedefineException extends Exception {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public RedefineException(String msg) {
            super(msg);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static ClassLoadingMXBean classLoadingBean;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns all currently loaded *and linked* classes.
     * On JDK 1.5, it returns just a Class[] array of all loaded classes
     */
    public static native Class[] getAllLoadedClasses();

    /**
     * Returns the cached class file bytes for the given class. Currently
     * agent (on JDK 1.5) caches these bytes only for classes loaded with custom classloaders (that is, not
     * bootstrap (null) and not system classloader). If this method is called for a class loaded using bootstrap or
     * system classloader, it just returns null.
     */
    public static native byte[] getCachedClassFileBytes(Class clazz);

    public static int getLoadedClassCount() {
        return classLoadingBean.getLoadedClassCount();
    }

    /** Object size for a given object */
    public static native long getObjectSize(Object obj);

    public static native boolean setSleepTrackingEnabled(boolean value);

    public static native boolean setVMObjectAllocEnabled(boolean value);

    public static native boolean setWaitTrackingEnabled(boolean value);

    public static native boolean setParkTrackingEnabled(boolean value);

    public static native boolean setLockContentionMonitoringEnabled(boolean value);

    public static native void cacheLoadedClasses(Class[] nonSystemClasses, int nonSystemIndex);

    /** Disables the above class load hook */
    public static native void disableClassLoadHook();

    /**
     * When the class load hook is enabled, it will call the ProfilerInterface.classLoadHook(Class) method every time
     * a class is load and prepared.
     */
    public static native void enableClassLoadHook();

    /** Should be called at earliest possible time */
    public static void initialize() {
        classLoadingBean = ManagementFactory.getClassLoadingMXBean();
    }

    /**
     * This notifies the underlying native code about the fact that some classloader (we don't know which one since
     * we use PhantomReferences for them) is unloaded. Thus the native code that could have cached the class file
     * bytes for classes loaded with this loader, may release these bytes now. Really needed only in 1.5.
     */
    public static native void notifyAboutClassLoaderUnloading();

    /** RedefineClasses() as provided starting from JDK 1.5 */
    public static void redefineClasses(Class[] classes, byte[][] newClassFileBytes)
                                throws RedefineException {
        if (classes.length > newClassFileBytes.length) {
            throw new RedefineException("Inconsistent input data: classes.length = " + classes.length // NOI18N
                                        + ", newClassFileBytes.length = " + newClassFileBytes.length); // NOI18N
        }

        for (int i = 0; i < classes.length; i++) {
            if (classes[i] == null) {
                throw new RedefineException("null input data: classes at " + i); // NOI18N
            }

            if (newClassFileBytes[i] == null) {
                throw new RedefineException("null input data: newClassFileBytes at " + i + " for class "+classes[i].getName()); // NOI18N
            }
        }

        int res = doRedefineClasses(classes, newClassFileBytes);

        if (res != 0) {
            throw new RedefineException("Redefinition failed with error " + res + "\n" // NOI18N
                                        + "Check JVMTI documentation for this error code."); // NOI18N
        }
    }

    // ======================================== JDK 1.5 specific code ===============================================
    private static native int doRedefineClasses(Class[] classes, byte[][] newClassFileBytes);

    private static RedefineException newRedefineException(String msg, Throwable origCause) {
        msg = "Class redefinition error: " + msg + "\nOriginal exception:\n" + origCause; // NOI18N

        return new RedefineException(msg);
    }
}
