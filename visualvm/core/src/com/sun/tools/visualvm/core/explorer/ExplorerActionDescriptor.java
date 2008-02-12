/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.explorer;

import javax.swing.Action;

/**
 * Descriptor for explorer context menu action.
 *
 * @author Jiri Sedlacek
 */
public final class ExplorerActionDescriptor implements Comparable {

    private final Action action;
    private final int preferredPosition;


    /**
     * Creates new ExplorerActionDescriptor.
     * 
     * @param action Action to be added to explorer context menu,
     * @param preferredPosition preferred position of the action among other actions in context menu.
     */
    public ExplorerActionDescriptor(Action action, int preferredPosition) {
        this.action = action;
        this.preferredPosition = preferredPosition;
    }


    /**
     * Returns action to be added to explorer context menu.
     * 
     * @return action to be added to explorer context menu.
     */
    public Action getAction() {
        return action;
    }

    /**
     * Returns preferred position of the action among other actions in context menu.
     * 
     * @return preferred position of the action among other actions in context menu.
     */
    public int getPreferredPosition() {
        return preferredPosition;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param   o the object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this object.
     */
    public int compareTo(Object o) {
        ExplorerActionDescriptor descriptor = (ExplorerActionDescriptor)o;
        int descriptorActionOrder = descriptor.preferredPosition;
        if (preferredPosition == descriptorActionOrder) return 0;
        if (preferredPosition > descriptorActionOrder) return 1;
        return -1;
    }

}
