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

package org.graalvm.visualvm.lib.ui.memory;

import java.util.ResourceBundle;


/**
 * This interface declares actions that the user may initiate when browsing memory profiling results.
 * For example, the user may move the cursor to some class and request the tool to show stack
 * traces for allocations of instances of this class.
 *
 * @author Misha Dmitriev
 */
public interface MemoryResUserActionsHandler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    public static final String CANNOT_SHOW_PRIMITIVE_SRC_MSG = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle") // NOI18N
                                                                             .getString("MemoryResUserActionsHandler_CannotShowPrimitiveSrcMsg"); // NOI18N
    public static final String CANNOT_SHOW_REFLECTION_SRC_MSG = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.memory.Bundle") // NOI18N
                                                                             .getString("MemoryResUserActionsHandler_CannotShowReflectionSrcMsg"); // NOI18N
    // -----
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void showSourceForMethod(String className, String methodName, String methodSig);

    // if sorting is not defined, use showStacksForClass(selectedClassId, CommonConstants.SORTING_COLUMN_DEFAULT, false);
    public void showStacksForClass(int selectedClassId, int sortingColumn, boolean sortingOrder);
}
