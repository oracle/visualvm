/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.classfile;

import java.util.ArrayList;
import java.util.List;


/**
 * A container for a group of classes/placeholders with the same name and different classloaders,
 * plus the functionality to browse this group and check for compatible classes.
 *
 * @author Tomas Hurka
 * @author Misha Dmitirev
 */
public class SameNameClassGroup {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private List classes;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SameNameClassGroup() {
        classes = new ArrayList(4); // Hope we are not going to have too many class versions...
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public List getAll() {
        return classes;
    }

    public void add(BaseClassInfo clazz) {
        classes.add(clazz);
    }

    /**
     * Check if class clazz with its existing loader is compatible with loader classLoaderId - that is,
     * loader classLoaderId will return this class if asked for the class with this name.
     */
    public static BaseClassInfo checkForCompatibility(BaseClassInfo clazz, int classLoaderId) {
        int entryLoader = clazz.getLoaderId();

        if (entryLoader == classLoaderId) {
            return clazz;
        } else {
            if (isParentLoaderTo(entryLoader, classLoaderId)) {
                return clazz;
            } else if (isParentLoaderTo(classLoaderId, entryLoader)) { // This can happen at least with placeholders
                clazz.setLoaderId(classLoaderId);

                return clazz;
            } else if (classLoaderId > 0) {
                // In some cases, the class loader graph for the app may be a non-tree structure, i.e. one class loader may delegate
                // not just to its parent loader, but to some other loader(s) as well. In that case, our last resort is to ask for
                // its defining loader.          
                int loader = ClassRepository.getDefiningClassLoaderId(clazz.getName(), classLoaderId);

                if (loader == -1) {
                    return null;
                }

                if (loader == entryLoader) {
                    return clazz;
                }
            }

            return null;
        }
    }

    /** Find a class compatible with the given loader (see definition in checkFroCompatibility()) in this group. */
    public BaseClassInfo findCompatibleClass(int classLoaderId) {
        int size = classes.size();
        for (int i = 0; i < size; i++) {
            BaseClassInfo clazz = (BaseClassInfo) classes.get(i);
            if (clazz.getLoaderId() == classLoaderId) {
                return clazz;
            }
        }
        for (int i = 0; i < size; i++) {
            BaseClassInfo clazz = (BaseClassInfo) classes.get(i);
            clazz = checkForCompatibility(clazz, classLoaderId);

            if (clazz != null) {
                return clazz;
            }
        }

        return null;
    }

    public void replace(BaseClassInfo clazz1, BaseClassInfo clazz2) {
        classes.remove(clazz1);
        classes.add(clazz2);
    }

    private static boolean isParentLoaderTo(int testParentLoader, int testChildLoader) {
        int parent = ClassLoaderTable.getParentLoader(testChildLoader);

        while (parent != testParentLoader) {
            if (parent == 0) {
                return false;
            } else {
                parent = ClassLoaderTable.getParentLoader(parent);
            }
        }

        return true;
    }
}
