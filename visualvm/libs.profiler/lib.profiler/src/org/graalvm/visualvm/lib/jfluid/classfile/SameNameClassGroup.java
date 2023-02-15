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

package org.graalvm.visualvm.lib.jfluid.classfile;

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

    SameNameClassGroup() {
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
    public static BaseClassInfo checkForCompatibility(ClassRepository repo, BaseClassInfo clazz, int classLoaderId) {
        ClassLoaderTable table = repo.getClassPath().getClassLoaderTable();
        int entryLoader = clazz.getLoaderId();

        if (entryLoader == classLoaderId) {
            return clazz;
        } else {
            if (isParentLoaderTo(table, entryLoader, classLoaderId)) {
                return clazz;
            } else if (clazz instanceof PlaceholderClassInfo && isParentLoaderTo(table, classLoaderId, entryLoader)) { // This can happen at least with placeholders
                clazz.setLoaderId(classLoaderId);

                return clazz;
            } else if (classLoaderId > 0) {
                // In some cases, the class loader graph for the app may be a non-tree structure, i.e. one class loader may delegate
                // not just to its parent loader, but to some other loader(s) as well. In that case, our last resort is to ask for
                // its defining loader.          
                int loader = repo.getDefiningClassLoaderId(clazz.getName(), classLoaderId);

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
    public BaseClassInfo findCompatibleClass(ClassRepository repo, int classLoaderId) {
        int size = classes.size();
        for (int i = 0; i < size; i++) {
            BaseClassInfo clazz = (BaseClassInfo) classes.get(i);
            if (clazz.getLoaderId() == classLoaderId) {
                return clazz;
            }
        }
        for (int i = 0; i < size; i++) {
            BaseClassInfo clazz = (BaseClassInfo) classes.get(i);
            clazz = checkForCompatibility(repo, clazz, classLoaderId);

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

    private static boolean isParentLoaderTo(ClassLoaderTable table, int testParentLoader, int testChildLoader) {
        int parent = table.getParentLoader(testChildLoader);

        while (parent != testParentLoader) {
            if (parent == 0) {
                return false;
            } else {
                parent = table.getParentLoader(parent);
            }
        }

        return true;
    }
}
