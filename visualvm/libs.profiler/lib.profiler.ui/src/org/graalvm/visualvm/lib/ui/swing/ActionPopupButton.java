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
package org.graalvm.visualvm.lib.ui.swing;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 *
 * @author Jiri Sedlacek
 */
public class ActionPopupButton extends PopupButton {

    private Action action;
    private final Action[] actions;

//    private Dimension minSize;
//    private Dimension prefSize;

    public ActionPopupButton(Action... _actions) {
        this(0, _actions);
    }

    public ActionPopupButton(int initial, Action... _actions) {
        actions = _actions;
        selectAction(initial);
    }

    public final Action[] getActions() {
        return actions;
    }

    public final void selectAction(Action _action) {
        action = _action;
        setText(action == null ? "" : action.getValue(Action.NAME).toString()); // NOI18N
        putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N
    }

    public final void selectAction(int index) {
        selectAction(actions[index]);
    }

    public final Action getSelectedAction() {
        return action;
    }

    public final int getSelectedIndex() {
        for (int i = 0; i < actions.length; i++)
            if (actions[i] == action) return i;
        return -1;
    }

    protected void populatePopup(JPopupMenu popup) {
        for (final Action _action : actions) {
            if (_action != null) {
                popup.add(new JRadioButtonMenuItem(_action.getValue(Action.NAME).toString(), _action == action) {
                    protected void fireActionPerformed(ActionEvent e) {
                        selectAction(_action);
                        _action.actionPerformed(e);
                    }
                });
            } else {
                popup.addSeparator();
            }
        }
    }
    
//    public Dimension getMinimumSize() {
//        if (minSize == null) {
//            Action orig = action;
//            for (Action _action : actions) if (_action != null) {
//                selectAction(_action);
//                Dimension min = super.getMinimumSize();
//                if (minSize == null) minSize = min;
//                minSize.width = Math.max(minSize.width, min.width);
//                minSize.height = Math.max(minSize.height, min.height);
//            }
//            selectAction(orig);
//        }
//        return minSize;
//    }
//    
//    public Dimension getPreferredSize() {
//        if (prefSize == null) {
//            Action orig = action;
//            for (Action _action : actions) if (_action != null) {
//                selectAction(_action);
//                Dimension pref = super.getPreferredSize();
//                if (prefSize == null) prefSize = pref;
//                prefSize.width = Math.max(prefSize.width, pref.width);
//                prefSize.height = Math.max(prefSize.height, pref.height);
//            }
//            selectAction(orig);
//        }
//        return prefSize;
//    }
    
}
