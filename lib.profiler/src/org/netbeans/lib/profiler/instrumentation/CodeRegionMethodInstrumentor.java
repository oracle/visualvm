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

import org.netbeans.lib.profiler.classfile.ClassInfo;
import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.classfile.ClassRepository.CodeRegionBCI;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.client.ClientUtils.SourceCodeSelection;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.utils.MiscUtils;
import java.io.IOException;
import java.util.ArrayList;


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
    private int methodIdx;
    private int nInstrClasses;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CodeRegionMethodInstrumentor(ProfilingSessionStatus status, SourceCodeSelection codeSelection) {
        super(status);
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

        if (status.targetJDKVersionString.equals(JDK_15_STRING)) {
            return createInstrumentedMethodPack15();
        } else if (status.targetJDKVersionString.equals(JDK_16_STRING)) {
            // for both 1.5 and 1.6 we use 15-style instrumentation
            return createInstrumentedMethodPack15();
        } else if (status.targetJDKVersionString.equals(JDK_17_STRING)) {
            // for 1.7 we use 15-style instrumentation
            return createInstrumentedMethodPack15();
        } else {
            throw new InternalError("Unsupported JDK version"); // NOI18N
        }
    }

    Object[] getInitialInstrumentCodeRegionResponse(String[] loadedClasses, int[] loadedClassLoaderIds) {
        DynamicClassInfo clazz = null;

        // We may have more than one version of the class with the given name, hence this search and instrClasses array
        for (int i = 0; i < loadedClasses.length; i++) {
            String loadedClassName = loadedClasses[i].replace('.', '/').intern(); // NOI18N

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
                CodeRegionBCI loc = ClassRepository.getMethodForSourceRegion(clazz, startLine, endLine);

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
