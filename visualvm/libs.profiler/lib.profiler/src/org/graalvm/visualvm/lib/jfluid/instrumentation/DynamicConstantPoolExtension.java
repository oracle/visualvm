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

import org.graalvm.visualvm.lib.jfluid.classfile.DynamicClassInfo;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;


/**
 * Support for adding multiple different fragments to a class's constant pool, that also keeps track of whether
 * a particular fragment has already been added to this class or not.
 * The main API call for this class is getCPFragment, that returns a relocated (i.e. with indices adjusted for a concrete
 * given class) copy of added constant pool for particular injection type defined in JFluid. Once this operation is
 * performed, the information is registered in the corresponding ClassInfo (it is assumed that a real class in the JVM is
 * actually instrumented by adding this cpool fragment). Thus if subsequently for this class the constant pool fragment
 * for the same injection type is requested again, an empty result is returned.
 *
 * When performing actual method instrumentation (bytecode rewriting), use cpool indices of injected methods defined as
 * public static variables in CPExtensionsRepository. These variables need to be adjusted for the base cpool count value
 * for the given class/injection type.
 *
 * @author Misha Dmitriev
 */
public class DynamicConstantPoolExtension extends ConstantPoolExtension implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    //------------------------------------------ Private implementation -------------------------------------------------
    private static DynamicConstantPoolExtension emptyECP = new DynamicConstantPoolExtension();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected DynamicClassInfo clazz;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected DynamicConstantPoolExtension(DynamicClassInfo clazz, int injectionType, int baseCPCount, int secondaryCPCount) {
        super(CPExtensionsRepository.getStandardCPFragment(injectionType), baseCPCount, secondaryCPCount);
        this.clazz = clazz;
    }

    protected DynamicConstantPoolExtension() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Re-create the whole appended constant pool chunk out of (possibly several) fragments added for different
     * kinds of instrumentation at possibly different times.
     */
    public static DynamicConstantPoolExtension getAllAddedCPFragments(DynamicClassInfo clazz) {
        DynamicConstantPoolExtension res = new DynamicConstantPoolExtension(); // Equivalent of emptyECP
        int lowerBaseCPCountBound = 0;
        int baseCPCLen = clazz.getBaseCPoolCountLen();

        for (int j = 0; j < baseCPCLen; j++) {
            int minBaseCPCount = 0xFFFFFFF;
            int cpFragmentIdx = -1;

            // Find the CP fragment with the minimum base index, that hasn't been used yet
            for (int i = 0; i < baseCPCLen; i++) {
                int curCPCount = clazz.getBaseCPoolCount(i);

                if (curCPCount != -1) {
                    if ((curCPCount > lowerBaseCPCountBound) && (curCPCount < minBaseCPCount)) {
                        minBaseCPCount = curCPCount;
                        cpFragmentIdx = i;
                    }
                }
            }

            // If such a fragment exists, append it to the result. Otherwise return the final result.
            if (cpFragmentIdx != -1) {
                DynamicConstantPoolExtension res1 = newDynamicCPExtension(clazz, cpFragmentIdx,
                                                                          clazz.getBaseCPoolCount(cpFragmentIdx));
                res.addedCPContents = res.getConcatenatedContents(res1); // Don't change res contents, because initially it's emptyECP
                res.nAddedEntries += res1.nAddedEntries;
                lowerBaseCPCountBound = minBaseCPCount;
            } else {
                return res;
            }
        }

        return res;
    }

    public static DynamicConstantPoolExtension getCPFragment(DynamicClassInfo clazz, int injType) {
        if (clazz.getBaseCPoolCount(injType) != -1) {
            return emptyECP; // clazz's cpool already extended for this instrumentation type
        } else {
            int currentCPCount = clazz.getCurrentCPoolCount();
            DynamicConstantPoolExtension ecp = newDynamicCPExtension(clazz, injType, currentCPCount);
            clazz.setBaseCPoolCount(injType, currentCPCount);
            clazz.setCurrentCPoolCount(currentCPCount + ecp.nAddedEntries);

            return ecp;
        }
    }

    public static DynamicConstantPoolExtension getEmptyCPFragment() {
        return emptyECP;
    }

    //-------------------------------------------- Protected methods ---------------------------------------------------
    protected static DynamicConstantPoolExtension newDynamicCPExtension(DynamicClassInfo clazz, int injectionType, int baseCPCount) {
        int secondaryCPCount = 0;

        switch (injectionType) {
            case INJ_RECURSIVE_ROOT_METHOD:
            case INJ_RECURSIVE_MARKER_METHOD:
                secondaryCPCount = clazz.getBaseCPoolCount(INJ_RECURSIVE_NORMAL_METHOD);

                break;
            case INJ_RECURSIVE_SAMPLED_ROOT_METHOD:
            case INJ_RECURSIVE_SAMPLED_MARKER_METHOD:
                secondaryCPCount = clazz.getBaseCPoolCount(INJ_RECURSIVE_SAMPLED_NORMAL_METHOD);

                break;
        }

        return new DynamicConstantPoolExtension(clazz, injectionType, baseCPCount, secondaryCPCount);
    }
}
