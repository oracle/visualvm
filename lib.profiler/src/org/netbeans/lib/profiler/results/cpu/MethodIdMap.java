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

package org.netbeans.lib.profiler.results.cpu;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ResourceBundle;


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
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.cpu.Bundle"); // NOI18N
    private static final String ANONYMOUS_PACKAGE_STRING = messages.getString("MethodIdMap_AnonymousPackageString"); // NOI18N
                                                                                                                     // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList classOrPackageNames;
    private Hashtable classIdCache; // Maps a class (package) name to its integer id
    private int[] classIds;
    private int curClassId;
    private int newView;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * @param methodLevelInstrClassNames names of classes for instrumented methods. The total number of entries is
     *                                   equal to the number of instrumented methods, but some entries may be the same of
     *                                   course.
     * @param nInstrMethods              number of entries in this array that are actually used
     * @param newView                    the new view for which we are creeating ids - class-level or package-level
     */
    public MethodIdMap(String[] methodLevelInstrClassNames, int nInstrMethods, int newView) {
        this.newView = newView;
        classIds = new int[nInstrMethods];
        classIdCache = new Hashtable();
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
        String[] ret = (String[]) classOrPackageNames.toArray(new String[classOrPackageNames.size()]);
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
            classIdCache.put(name, new Integer(curClassId));

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
