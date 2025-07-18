/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 * @author Jiri Sedlacek
 */
public final class DropdownButton extends JToggleButton {

    public static final String KEY_CLASS = "KEY_CLASS"; // NOI18N
    public static final String KEY_BOOLVALUE = "KEY_BOOLVALUE"; // NOI18N

    private final List<Action> actions = new ArrayList<>();


    public DropdownButton(Icon icon) {
        super(icon);
    }


    public void addAction(Action action) {
        actions.add(action);
    }

    public void addSeparator() {
        actions.add(null);
    }


    protected void fireActionPerformed(ActionEvent event) {
        JPopupMenu popup = new JPopupMenu();
        for (Action action : actions) addAction(popup, action);
        popup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                setSelected(true);
            }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                setSelected(false);
            }
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });
        popup.show(this, getWidth() - popup.getPreferredSize().width, getHeight());
    }

    private void addAction(JPopupMenu popup, Action action) {
        if (action == null) {
            popup.addSeparator();
        } else {
            Object cls = action.getValue(KEY_CLASS);
            if (Boolean.class.equals(cls)) {
                Boolean boolvalue = (Boolean)action.getValue(KEY_BOOLVALUE);
                JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
                item.setSelected(boolvalue);
                popup.add(item);
            } else {
                popup.add(action);
            }
        }
    }

}
