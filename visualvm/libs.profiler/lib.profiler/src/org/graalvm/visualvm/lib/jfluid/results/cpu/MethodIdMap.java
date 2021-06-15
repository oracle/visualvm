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

package org.graalvm.visualvm.lib.jfluid.results.cpu;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;


/**
 * This class provides a map between method ids and class (package) ids, which is needed when
 * constructing an aggregated class- (package-) level view of CPU profiling results out of the
 * initial method-level view
 *
 * @author Misha Dmitriev
 */
public class MethodIdMap {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ANONYMOUS_PACKAGE_STRING = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.cpu.Bundle").getString("MethodIdMap_AnonymousPackageString"); // NOI18N
                                                                                                                     // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList classOrPackageNames;
    private Map classIdCache; // Maps a class (package) name to its integer id
    private int[] classIds;
    private int curClassId;
    private int newView;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * @param methodLevelInstrClassNames names of classes for instrumented methods. The total number of entries is
     *                                   equal to the number of instrumented methods, but some entries may be the same of
     *                                   course.
     * @param nInstrMethods              number of entries in this array that are actually used
     * @param newView                    the new view for which we are creating ids - class-level or package-level
     */
    public MethodIdMap(String[] methodLevelInstrClassNames, int nInstrMethods, int newView) {
        this.newView = newView;
        classIds = new int[nInstrMethods];
        classIdCache = new ConcurrentHashMap();
        classOrPackageNames = new ArrayList();
        curClassId = 0;
        classOrPackageNames.add(methodLevelInstrClassNames[0]);

        classIds[0] = 0; // The hidden "Thread" quazi-method transforms into "Thread" quazi-class

        for (int i = 1; i < nInstrMethods; i++) {
            classIds[i] = getClassId(methodLevelInstrClassNames[i]);
        }

        classIdCache = null; // Not needed anymore - free memory
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getClassOrPackageIdForMethodId(int methodId) {
        return classIds[methodId];
    }

    public String[] getInstrClassesOrPackages() {
        String[] ret = (String[]) classOrPackageNames.toArray(new String[0]);
        classOrPackageNames = null;

        return ret;
    }

    public int getNInstrClassesOrPackages() {
        return curClassId + 1;
    }

    private int getClassId(String className) {
        String name = (newView == CPUResultsSnapshot.CLASS_LEVEL_VIEW) ? className : getPackageName(className);
        Integer classId = (Integer) classIdCache.get(name);

        if (classId == null) {
            curClassId++;
            classOrPackageNames.add(name);
            classIdCache.put(name, Integer.valueOf(curClassId));

            return curClassId;
        } else {
            return classId.intValue();
        }
    }

    private String getPackageName(String className) {
        int lastDivPos = className.lastIndexOf('.'); // NOI18N

        if (lastDivPos == -1) {
            return ANONYMOUS_PACKAGE_STRING;
        } else {
            return className.substring(0, lastDivPos).intern();
        }
    }
}
