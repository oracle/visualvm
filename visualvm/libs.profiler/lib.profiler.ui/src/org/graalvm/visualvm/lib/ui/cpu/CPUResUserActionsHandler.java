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

package org.graalvm.visualvm.lib.ui.cpu;

import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;


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
