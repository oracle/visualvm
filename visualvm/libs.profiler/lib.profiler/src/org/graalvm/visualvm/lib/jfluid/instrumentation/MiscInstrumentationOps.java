/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.classfile.ClassRepository;
import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import org.graalvm.visualvm.lib.jfluid.utils.MiscUtils;
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

    public MiscInstrumentationOps(ClassRepository repo, ProfilingSessionStatus status) {
        super(repo, status);
        instrClasses = new ArrayList();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Object[] getOrigCodeForAllInstrumentedMethods() {
        nInstrClasses = nInstrMethods = 0;

        for (Enumeration e = classRepo.getClassEnumerationWithAllVersions(); e.hasMoreElements();) {
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

        List classes = classRepo.getAllClassVersions(className);

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
