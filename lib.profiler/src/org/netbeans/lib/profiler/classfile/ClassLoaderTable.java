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

package org.netbeans.lib.profiler.classfile;


/**
 * A table that maps a class loader to its parent class loader.
 *
 * @author Misha Dmitirev
 * @author Ian Formanek
 */
public class ClassLoaderTable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // TODO [release]: change value to TRUE to remove the print code below entirely by compiler
    private static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.classfile.ClassLoaderTable") != null; // NOI18N
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
            // very stranmge situation: loader not known on tool side, seems like this can happen when using
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
