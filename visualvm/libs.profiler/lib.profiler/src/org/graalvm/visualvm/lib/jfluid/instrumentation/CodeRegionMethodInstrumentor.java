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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import java.io.IOException;
import java.util.ArrayList;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassRepository;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassRepository.CodeRegionBCI;
import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils.SourceCodeSelection;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.utils.MiscUtils;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.RootClassLoadedCommand;


/**
 * High-level access to functionality that instruments a (so far single) code region in a (single again) TA method.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class CodeRegionMethodInstrumentor extends ClassManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ClientUtils.SourceCodeSelection sourceCodeSelection;
    private ArrayList instrClasses;
    private String className;
    private int nInstrClasses;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CodeRegionMethodInstrumentor(ClassRepository repo, ProfilingSessionStatus status, SourceCodeSelection codeSelection) {
        super(repo, status);
        sourceCodeSelection = codeSelection;
        className = sourceCodeSelection.getClassName().replace('.', '/').intern(); // NOI18N
        instrClasses = new ArrayList();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object[] getFollowUpInstrumentCodeRegionResponse(int classLoaderId) {
        DynamicClassInfo clazz = javaClassForName(className, classLoaderId);

        instrClasses.clear();
        instrClasses.add(clazz);
        nInstrClasses = 1;

        return createInstrumentedMethodPack();
    }

    protected Object[] createInstrumentedMethodPack() {
        if (nInstrClasses == 0) {
            return null;
        }

        return createInstrumentedMethodPack15();
    }

    Object[] getInitialInstrumentCodeRegionResponse(RootClassLoadedCommand rootLoaded) {
        String[] loadedClasses = rootLoaded.getAllLoadedClassNames();
        int[] loadedClassLoaderIds = rootLoaded.getAllLoadedClassLoaderIds();
        DynamicClassInfo clazz = null;

        storeClassFileBytesForCustomLoaderClasses(rootLoaded);
         // We may have more than one version of the class with the given name, hence this search and instrClasses array
        for (int i = 0; i < loadedClasses.length; i++) {
            String loadedClassName = loadedClasses[i];

            if (className == loadedClassName) {
                clazz = javaClassForName(loadedClasses[i], loadedClassLoaderIds[i]);

                if (clazz != null) {
                    CodeRegionBCI instrLocation = computeCodeRegionFromSourceCodeSelection(clazz);

                    if (instrLocation != null) {
                        int mIdx = clazz.getMethodIndex(instrLocation.methodName, instrLocation.methodSignature);

                        if (mIdx != -1) { // Not all class versions may have this method
                            clazz.setLoaded(true);
                            instrClasses.add(clazz);
                        }
                    }
                }
            }
        }

        nInstrClasses = instrClasses.size();

        return createInstrumentedMethodPack();
    }

    private CodeRegionBCI computeCodeRegionFromSourceCodeSelection(ClassInfo clazz) {
        try {
            if (sourceCodeSelection.definedViaSourceLines()) {
                int startLine = sourceCodeSelection.getStartLine();
                int endLine = sourceCodeSelection.getEndLine();
                CodeRegionBCI loc = classRepo.getMethodForSourceRegion(clazz, startLine, endLine);

                status.beginTrans(true);

                try {
                    status.setInstrMethodNames(new String[] { loc.methodName });
                    status.setInstrMethodSignatures(new String[] { loc.methodSignature });
                } finally {
                    status.endTrans();
                }

                return loc;
            } else if (sourceCodeSelection.definedViaMethodName()) {
                String methodName = sourceCodeSelection.getMethodName();
                String methodSignature = sourceCodeSelection.getMethodSignature();

                return ClassRepository.getMethodMinAndMaxBCI(clazz, methodName, methodSignature);
            }
        } catch (IOException ex) {
            MiscUtils.printErrorMessage(ex.getMessage());
        } catch (BadLocationException ex) {
            MiscUtils.printErrorMessage(ex.getMessage());
        } catch (ClassNotFoundException ex) {
            MiscUtils.printErrorMessage(ex.getMessage());
        }

        return null;
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

            CodeRegionBCI instrLocation = computeCodeRegionFromSourceCodeSelection(clazz);
            int mIdx = clazz.getMethodIndex(instrLocation.methodName, instrLocation.methodSignature); // TODO CHECK: local variable hides member variable
            clazz.setMethodInstrumented(mIdx);

            DynamicConstantPoolExtension ecp = DynamicConstantPoolExtension.getCPFragment(clazz, INJ_CODE_REGION);
            byte[] newMethodInfo = InstrumentationFactory.instrumentCodeRegion(clazz, mIdx, instrLocation.bci0, instrLocation.bci1);

            int nMethods = clazz.getMethodNames().length;
            byte[][] replacementMethodInfos = new byte[nMethods][];

            for (int i = 0; i < nMethods; i++) {
                replacementMethodInfos[i] = clazz.getMethodInfo(i);
            }

            replacementMethodInfos[mIdx] = newMethodInfo;

            int nAddedCPEntries = ecp.getNEntries();
            byte[] addedCPContents = ecp.getContents();
            replacementClassFileBytes[j] = ClassRewriter.rewriteClassFile(clazz, replacementMethodInfos, nAddedCPEntries,
                                                                          addedCPContents);
        }

        return new Object[] { instrMethodClasses, instrClassLoaderIds, replacementClassFileBytes };
    }
}
