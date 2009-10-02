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

package org.netbeans.lib.profiler.instrumentation;

import org.netbeans.lib.profiler.classfile.BaseClassInfo;
import org.netbeans.lib.profiler.classfile.ClassInfo;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import java.util.ArrayList;


/**
 * Base class providing common functionality for instrumenting TA methods to generate object allocation/liveness data.
 * The fact that there are two classes at this time, MemoryProfMethodInstrumentor and ObjLivenessMethodInstrumentor, is
 * explained by purely historical reasons - in the past we had instrumentation implemented differently for object allocation
 * and liveness profiling. Now the same kind of instrumentation is used for both profiling types (the exact method names
 * for injected calls are different, but the way of injecting these calls and their signatures are exactly the same).
 *
 * In principle, MemoryProfMethodInstrumentor and ObjLivenessMethodInstrumentor can be merged, but there is no compelling
 * need for that. Furthermore, if in future say some different memory instrumentation kind is introduced, this division
 * may help.
 *
 * @author Misha Dmitriev
 */
public abstract class MemoryProfMethodInstrumentor extends ClassManager {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    static class MethodScanerForNewOpcodes extends SingleMethodScaner {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        boolean hasNewArrayOpcodes(MemoryProfMethodInstrumentor minstr, boolean checkForOpcNew) {
            int loaderId = clazz.getLoaderId();
            boolean found = false;
            int bc;
            int bci = 0;

            while (bci < bytecodesLength) {
                bc = (bytecodes[bci] & 0xFF);

                if ((bc == opc_new) && checkForOpcNew) {
                    found = true;

                    int classCPIdx = getU2(bci + 1);
                    String refClassName = clazz.getRefClassName(classCPIdx);
                    BaseClassInfo refClazz = javaClassOrPlaceholderForName(refClassName, loaderId);

                    if (refClazz.getInstrClassId() == -1) {
                        refClazz.setInstrClassId(minstr.getNextClassId(refClazz.getName()));
                    }
                } else if ((bc == opc_anewarray) || (bc == opc_multianewarray)) {
                    found = true;

                    int classCPIdx = getU2(bci + 1);
                    String refClassName = clazz.getRefClassName(classCPIdx);
                    BaseClassInfo refClazz = (bc == opc_anewarray) ? javaClassForObjectArrayType(refClassName)
                                                                   : ClassRepository.lookupSpecialClass(refClassName);

                    if (refClazz != null) { // Warning already issued

                        if (refClazz.getInstrClassId() == -1) {
                            refClazz.setInstrClassId(minstr.getNextClassId(refClazz.getName()));
                        }
                    }
                } else if (bc == opc_newarray) {
                    found = true;

                    int arrayClassId = getByte(bci + 1);
                    BaseClassInfo refClazz = javaClassForPrimitiveArrayType(arrayClassId);

                    if (refClazz.getInstrClassId() == -1) {
                        refClazz.setInstrClassId(minstr.getNextClassId(refClazz.getName()));
                    }
                }

                bci += opcodeLength(bci);
            }

            return found;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected ArrayList instrClasses = new ArrayList();
    MethodScanerForNewOpcodes msfno;
    protected String[] instantiatableClasses;
    protected int injType;
    protected int instrClassId;
    protected int nInstantiatableClasses;
    protected int nInstrClasses;
    protected int nInstrMethods;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemoryProfMethodInstrumentor(ProfilingSessionStatus status, int injType) {
        super(status);
        this.status = status;
        instantiatableClasses = new String[100];
        this.injType = injType;
        msfno = new MethodScanerForNewOpcodes();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object[] getInitialMethodsToInstrument(String[] loadedClasses, int[] loadedClassLoaderIds,
                                                  byte[][] cachedClassFileBytes) {
        resetLoadedClassData();
        initInstrumentationPackData();
        instrClassId = 0;

        storeClassFileBytesForCustomLoaderClasses(loadedClasses, loadedClassLoaderIds, cachedClassFileBytes);

        for (int i = 0; i < loadedClasses.length; i++) {
            if (loadedClasses[i].equals("java.lang.Object")) {
                continue; // NOI18N
            }

            findAndMarkMethodsToInstrumentInClass(loadedClasses[i], loadedClassLoaderIds[i]);
        }

        return createInstrumentedMethodPack();
    }

    public String[] getInstantiatableClasses() {
        return instantiatableClasses;
    }

    public Object[] getMethodsToInstrumentUponClassLoad(String className, int classLoaderId) {
        initInstrumentationPackData();
        findAndMarkMethodsToInstrumentInClass(className, classLoaderId);

        return createInstrumentedMethodPack();
    }

    public int getNInstantiatableClasses() {
        return nInstantiatableClasses;
    }

    /** Checks if there are any methods in this class that need to be instrumented. */
    protected void findAndMarkMethodsToInstrumentInClass(String className, int classLoaderId) {
        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        if (clazz == null) {
            return; // Warning has already been reported
        }

        if (!clazz.isLoaded()) {
            clazz.setLoaded(true);

            // We assign an ID to class no matter whether or not this class is going to be instantiated anywhere in the program
            // As a result, we may have quite some classes that are in the appropriate table in 'status', but have zero objects
            // associated with them forever.
            // I am not sure why this is done here, but perhaps there was a good reason for this. Need to comment such things immediately...
            if ((clazz.getInstrClassId() == -1) && !clazz.isInterface()) {
                clazz.setInstrClassId(getNextClassId(clazz.getName()));
            }

            String[] methodNames = clazz.getMethodNames();
            boolean found = false;

            for (int i = 0; i < methodNames.length; i++) {
                if (clazz.isMethodNative(i) || clazz.isMethodAbstract(i)) {
                    clazz.setMethodUnscannable(i);

                    continue;
                }

                if (methodNeedsInstrumentation(clazz, i)) {
                    nInstrMethods++;
                    clazz.setMethodInstrumented(i);
                    found = true;
                }
            }

            if (found) {
                nInstrClasses++;
                instrClasses.add(clazz);
            }
        }
    }

    protected void initInstrumentationPackData() {
        instrClasses.clear();
        nInstrClasses = nInstrMethods = 0;
        nInstantiatableClasses = 0;
    }

    protected abstract boolean methodNeedsInstrumentation(ClassInfo clazz, int methodIdx);

    /** Creates a multi-class packet of instrumented methods or classes */
    protected Object[] createInstrumentedMethodPack() {
        if (nInstrMethods == 0) {
            return null;
        }

        return createInstrumentedMethodPack15();
    }

    protected boolean hasNewOpcodes(ClassInfo clazz, int methodIdx, boolean checkForOpcNew) {
        msfno.setClassAndMethod(clazz, methodIdx);

        return msfno.hasNewArrayOpcodes(this, checkForOpcNew);
    }

    protected abstract byte[] instrumentMethod(DynamicClassInfo clazz, int methodIdx);

    protected boolean methodNeedsRewriting(DynamicClassInfo clazz, int methodIdx) {
        return clazz.isMethodInstrumented(methodIdx);
    }

    int getNextClassId(String className) {
        if (nInstantiatableClasses == instantiatableClasses.length) {
            String[] oldInstantiatableClasses = instantiatableClasses;
            instantiatableClasses = new String[oldInstantiatableClasses.length + 100];
            System.arraycopy(oldInstantiatableClasses, 0, instantiatableClasses, 0, oldInstantiatableClasses.length);
        }

        instantiatableClasses[nInstantiatableClasses++] = className;
        status.updateAllocatedInstancesCountInfoInClient(className);

        return instrClassId++;
    }

    /** Creates the 1.5-style array of instrumented class files. */
    private Object[] createInstrumentedMethodPack15() {
        String[] instrMethodClasses = new String[nInstrClasses];
        int[] instrClassLoaderIds = new int[nInstrClasses];
        byte[][] replacementClassFileBytes = new byte[nInstrClasses][];

        for (int j = 0; j < nInstrClasses; j++) {
            DynamicClassInfo clazz = (DynamicClassInfo) instrClasses.get(j);
            instrMethodClasses[j] = clazz.getName().replace('/', '.'); // NOI18N
            instrClassLoaderIds[j] = clazz.getLoaderId();

            String[] methodNames = clazz.getMethodNames();
            int nMethods = methodNames.length;
            byte[][] replacementMethodInfos = new byte[nMethods][];

            DynamicConstantPoolExtension.getCPFragment(clazz, injType);

            for (int i = 0; i < nMethods; i++) {
                if (methodNeedsRewriting(clazz, i)) {
                    replacementMethodInfos[i] = instrumentMethod(clazz, i);
                } else {
                    replacementMethodInfos[i] = clazz.getMethodInfo(i);
                }
            }

            DynamicConstantPoolExtension wholeECP = DynamicConstantPoolExtension.getAllAddedCPFragments(clazz);
            int nAddedCPEntries = wholeECP.getNEntries();
            byte[] addedCPContents = wholeECP.getContents();
            replacementClassFileBytes[j] = ClassRewriter.rewriteClassFile(clazz, replacementMethodInfos, nAddedCPEntries,
                                                                          addedCPContents);
        }

        return new Object[] { instrMethodClasses, instrClassLoaderIds, replacementClassFileBytes };
    }
}
