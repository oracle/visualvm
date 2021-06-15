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

package org.graalvm.visualvm.lib.profiler;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.ui.UIConstants;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerTopComponent extends TopComponent {

    public static final String RECENT_FILE_KEY = "nb.recent.file.path"; // NOI18N

    private Component lastFocusOwner;

    private final PropertyChangeListener focusListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            Component c = evt.getNewValue() instanceof Component ?
                    (Component)evt.getNewValue() : null;
            processFocusedComponent(c);
        }
        private void processFocusedComponent(Component c) {
            Component cc = c;
            while (c != null) {
                if (c == ProfilerTopComponent.this) {
                    lastFocusOwner = cc;
                    return;
                }
                c = c.getParent();
            }
        }
    };

    protected void componentActivated() {
        super.componentActivated();
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        } else {
            Component defaultFocusOwner = defaultFocusOwner();
            if (defaultFocusOwner != null) defaultFocusOwner.requestFocus();
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                addPropertyChangeListener("focusOwner", focusListener); // NOI18N
    }

    protected void componentDeactivated() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                removePropertyChangeListener("focusOwner", focusListener); // NOI18N
        super.componentDeactivated();
    }
    
    protected Component defaultFocusOwner() {
        return null;
    }
    
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }
    
    public void paintComponent(Graphics g) {
        Color background = UIManager.getColor(UIConstants.PROFILER_PANELS_BACKGROUND);
        if (background != null) {
            g.setColor(background);
            Insets i = getInsets();
            g.fillRect(i.left, i.top, getWidth() - i.left - i.right, getHeight() - i.top - i.bottom);
        } else {
            super.paintComponent(g);
        }
    }
    
}
