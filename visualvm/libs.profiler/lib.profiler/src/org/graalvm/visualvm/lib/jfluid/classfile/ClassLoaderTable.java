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


/**
 * A table that maps a class loader to its parent class loader.
 *
 * @author Misha Dmitirev
 * @author Ian Formanek
 */
public class ClassLoaderTable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // TODO [release]: change value to TRUE to remove the print code below entirely by compiler
    private static final boolean DEBUG = System.getProperty("org.graalvm.visualvm.lib.jfluid.classfile.ClassLoaderTable") != null; // NOI18N
    private static int[] parentLoaderIds;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Will provide Id of parent classloader for the specified classloader. The parent of system classloader (id=0)
     * is the same system classloader (id=0).
     *
     * @param loader id of ClassLoader whose parent we are looking for
     * @return The id of the parent classloader of the specified classloader
     */
    public static int getParentLoader(int loader) {
        if (DEBUG) {
            System.err.println("ClassLoaderTable.DEBUG: getParent loader: " + loader); // NOI18N
        }

        if (loader >= parentLoaderIds.length) {
            // very strange situation: loader not known on tool side, seems like this can happen when using
            // instrumentation for Method.invoke, where the MethodLoadedCommand comes for class sun.reflect.misc.Trampoline
            // although the ClassLoadedCommand never is issued for this class
            return -1;
        }

        return parentLoaderIds[loader];
    }

    /**
     * @param thisAndParentLoaderData An array
     */
    public static void addChildAndParent(int[] thisAndParentLoaderData) {
        int ofs = thisAndParentLoaderData[2];

        if (ofs == 0) {
            addChildAndParent(thisAndParentLoaderData[0], thisAndParentLoaderData[1]);
        } else {
            int loaderId = thisAndParentLoaderData[0];

            for (int i = 0; i < ofs; i++) {
                addChildAndParent(loaderId, loaderId + 1);
                loaderId++;
            }

            addChildAndParent(loaderId, thisAndParentLoaderData[1]);
        }
    }

    /**
     * Will perform initial initialization of the classloader table with data provided from the profiler VM.
     *
     * @param inParentLoaderIds table mapping id (idx) -> parent id ([idx])
     */
    public static void initTable(int[] inParentLoaderIds) {
        if (DEBUG) {
            System.err.println("ClassLoaderTable.DEBUG: init patent loader ids: " + inParentLoaderIds.length); // NOI18N

            for (int i = 0; i < inParentLoaderIds.length; i++) {
                System.err.println("ClassLoaderTable.DEBUG: inParentLoaderIds[" + i + "]=" + inParentLoaderIds[i]); // NOI18N
            }
        }

        parentLoaderIds = inParentLoaderIds;

        for (int i = 0; i < parentLoaderIds.length; i++) {
            // We don't distinguish between bootstrap (-1) and system (0) class loaders, as well as some
            // sun.reflect.DelegatingClassLoaders or whatever, that also have -1 as their parent.
            if (parentLoaderIds[i] == -1) {
                parentLoaderIds[i] = 0;
            }
        }
    }

    /**
     * Will add a new pair of classloader, its parent to the table.
     *
     * @param childLoader  Id of classloader
     * @param parentLoader Id of its parent classloader
     */
    private static void addChildAndParent(int childLoader, int parentLoader) {
        if (DEBUG) {
            System.err.println("ClassLoaderTable.DEBUG: add child and parent: child: " // NOI18N
                               + childLoader + ", parent: " // NOI18N
                               + parentLoader);
        }

        int maxLoader = (childLoader > parentLoader) ? childLoader : parentLoader;

        if (parentLoaderIds.length < (maxLoader + 1)) {
            // new loader, need to enlarge the array
            int[] oldTable = parentLoaderIds;
            parentLoaderIds = new int[(childLoader * 2) + 1];
            System.arraycopy(oldTable, 0, parentLoaderIds, 0, oldTable.length);
        }

        if (parentLoader == -1) {
            parentLoader = 0;
        }

        parentLoaderIds[childLoader] = parentLoader;
    }
}
