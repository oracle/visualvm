/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.modules.appui.toolbar;

import java.awt.*;
import javax.swing.*;
import com.sun.tools.visualvm.uisupport.TransparentToolBar;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class VisualVMToolbar extends ProfilerToolbar {

    private final JComponent component;
    private final TransparentToolBar toolbar;

    VisualVMToolbar(boolean showSeparator) {
        toolbar = new TransparentToolBar();

        if (showSeparator) {
            component = TransparentToolBar.withSeparator(toolbar);
        } else {
            component = toolbar;
        }
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public Component add(Action action) {
        return toolbar.addItem(action);
    }

    @Override
    public Component add(Component component) {
        return toolbar.addItem(component);
    }

    @Override
    public Component add(Component component, int index) {
        return toolbar.addItem(component, index);
    }

    @Override
    public void addSeparator() {
        toolbar.addSeparator();
    }

    @Override
    public void addSpace(int width) {
        toolbar.addSpace(width);
    }

    @Override
    public void addFiller() {
        toolbar.addFiller();
    }

    @Override
    public void remove(Component component) {
        toolbar.removeItem(component);
    }

    @Override
    public void remove(int index) {
        toolbar.removeItem(index);
    }

    @Override
    public int getComponentCount() {
        return toolbar.getItemsCount();
    }
}
