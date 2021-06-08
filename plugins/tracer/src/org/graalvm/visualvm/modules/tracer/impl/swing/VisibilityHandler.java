/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer.impl.swing;

import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class VisibilityHandler {

    private Component component;
    private boolean wasVisible;

    private HierarchyListener listener;


    public VisibilityHandler() {}

    public abstract void shown();
    public abstract void hidden();


    public final void handle(Component component) {
        if (component == null)
            throw new NullPointerException("component cannot be null"); // NOI18N

        if (listener != null && component != null)
            component.removeHierarchyListener(listener);

        this.component = component;
        wasVisible = component.isVisible();

        if (listener == null) listener = createListener();
        component.addHierarchyListener(listener);
    }


    private HierarchyListener createListener() {
        return new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    boolean visible = component.isShowing();
                    if (wasVisible == visible) return;

                    wasVisible = visible;

                    if (visible) shown();
                    else hidden();
                }
            }
        };
    }


}
