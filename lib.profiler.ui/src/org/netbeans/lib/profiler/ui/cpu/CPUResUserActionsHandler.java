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

package org.netbeans.lib.profiler.ui.cpu;

import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;


/**
 * This interface declares actions that the user may initiate when browsing profiling results.
 * For example, the user may move the cursor to some method and request the tool to show its source
 * code, etc.
 *
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
public interface CPUResUserActionsHandler {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Adapter implements CPUResUserActionsHandler {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void addMethodToRoots(String className, String methodName, String methodSig) {
            throw new UnsupportedOperationException();
        }

        public void find(Object source, String findString) {
            throw new UnsupportedOperationException();
        }

        public void showReverseCallGraph(CPUResultsSnapshot snapshot, int threadId, int methodId, int view, int sortingColumn,
                                         boolean sortingOrder) {
            throw new UnsupportedOperationException();
        }

        public void showSourceForMethod(String className, String methodName, String methodSig) {
            throw new UnsupportedOperationException();
        }

        public void showSubtreeCallGraph(CPUResultsSnapshot snapshot, CCTNode node, int view, int sortingColumn,
                                         boolean sortingOrder) {
            throw new UnsupportedOperationException();
        }

        public void viewChanged(int viewType) {
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void addMethodToRoots(String className, String methodName, String methodSig);

    public void find(Object source, String findString);

    public void showReverseCallGraph(CPUResultsSnapshot snapshot, int threadId, int methodId, int view, int sortingColumn,
                                     boolean sortingOrder);

    /**
     * Display the source for the given method. className should never be null, but methodName and methodSig
     * may be null (for example, if we are viewing results at class level). In that case, just the class
     * source code should be displayed.
     * @param className  The fully qualified class name in VM format ("org/profiler/Main");
     * @param methodName The method name
     * @param methodSig  The method signature in VM format
     */
    public void showSourceForMethod(String className, String methodName, String methodSig);

    public void showSubtreeCallGraph(CPUResultsSnapshot snapshot, CCTNode node, int view, int sortingColumn, boolean sortingOrder);

    /**
     * Called when a view type change has been initiated from within a results component and the change should perhaps
     * be reflected in other views / ui as well.
     *
     * @param viewType the new view type
     */
    public void viewChanged(int viewType);
}
