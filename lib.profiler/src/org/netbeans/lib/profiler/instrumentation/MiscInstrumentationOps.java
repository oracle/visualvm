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

import org.netbeans.lib.profiler.classfile.ClassRepository;
import org.netbeans.lib.profiler.classfile.DynamicClassInfo;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.utils.MiscUtils;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


/**
 * A number of miscellaneous, relatively high-level, instrumentation operations.
 *
 * @author Misha Dmitriev
 */
public class MiscInstrumentationOps extends ClassManager {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private List instrClasses;
    private int nInstrClasses;
    private int nInstrMethods;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MiscInstrumentationOps(ProfilingSessionStatus status) {
        super(status);
        instrClasses = new ArrayList();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object[] getOrigCodeForAllInstrumentedMethods() {
        nInstrClasses = nInstrMethods = 0;

        for (Enumeration e = ClassRepository.getClassEnumerationWithAllVersions(); e.hasMoreElements();) {
            Object ci = e.nextElement();

            if (!(ci instanceof DynamicClassInfo)) {
                continue; // It's a BaseClassInfo, created just for e.g. array classes, or a PlaceholderClassInfo
            }

            DynamicClassInfo clazz = (DynamicClassInfo) ci;

            if (!clazz.isLoaded()) {
                continue;
            }

            if (!clazz.hasInstrumentedMethods()) {
                continue;
            }

            instrClasses.add(clazz);

            int nMethods = clazz.getMethodNames().length;
            int nLocalInstrMethods = 0;

            for (int i = 0; i < nMethods; i++) {
                if (clazz.isMethodInstrumented(i) && !clazz.isMethodUnscannable(i)) {
                    nLocalInstrMethods++;
                }
            }

            nInstrClasses++;
            nInstrMethods += nLocalInstrMethods;
        }

        return createInstrumentedMethodPack();
    }

    Object[] getOrigCodeForSingleInstrumentedMethod(RootMethods rootMethods) {
        String className = rootMethods.classNames[ProfilingSessionStatus.CODE_REGION_CLASS_IDX];
        String methodName = rootMethods.methodNames[ProfilingSessionStatus.CODE_REGION_CLASS_IDX];
        String methodSignature = rootMethods.methodSignatures[ProfilingSessionStatus.CODE_REGION_CLASS_IDX];

        List classes = ClassRepository.getAllClassVersions(className);

        if (classes == null) {
            return null; // Can happen if actually nothing was instrumented, since class of intrest hasn't been loaded
        }

        methodName = methodName.intern();
        methodSignature = methodSignature.intern();

        nInstrClasses = nInstrMethods = 0;

        for (int i = 0; i < classes.size(); i++) {
            DynamicClassInfo clazz = (DynamicClassInfo) classes.get(i);
            int methodIdx = clazz.getMethodIndex(methodName, methodSignature);

            if (methodIdx != -1) { // Otherwise this method doesn't exist in this class version
                instrClasses.add(clazz);
                nInstrClasses++;
                nInstrMethods++;
            }
        }

        if (nInstrClasses == 0) {
            MiscUtils.printErrorMessage("got zero classes when attempting to deinstrument a single instrumented method"); // NOI18N

            return null; // Should not happen, but just in case...
        }

        return createInstrumentedMethodPack();
    }

    protected Object[] createInstrumentedMethodPack() {
        if (nInstrMethods == 0) {
            return null;
        }

        return createInstrumentedMethodPack15();
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

            // As an optimization, we now send just nulls for class file bytes to the server, who loads original class file bytes in place
            //try {
            //  replacementClassFileBytes[j] = clazz.getClassFileBytes();
            //} catch (IOException ex) {
            //  // Shouldn't happen, so a message just in case
            //  MiscUtils.internalError("MiscInstrumentationOps: can't get original class file bytes for class " + clazz.getName() + "\nIOException message = " + ex.getMessage());
            //}
        }

        return new Object[] { instrMethodClasses, instrClassLoaderIds, replacementClassFileBytes };
    }
}
