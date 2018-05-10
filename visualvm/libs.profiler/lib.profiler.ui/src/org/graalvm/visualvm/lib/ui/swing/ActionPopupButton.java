/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
package org.netbeans.lib.profiler.ui.swing;

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
