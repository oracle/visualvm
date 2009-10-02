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

package org.netbeans.modules.profiler.attach.panels.components;

import java.util.Iterator;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ButtonGroupEx extends ButtonGroup {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // the list of buttons participating in this group
    protected Vector buttons = new Vector();

    /**
     * The current selection.
     */
    ButtonModel selection = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new <code>ButtonGroup</code>.
     */
    public ButtonGroupEx() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the number of buttons in the group.
     * @return the button count
     * @since 1.3
     */
    public int getButtonCount() {
        if (buttons == null) {
            return 0;
        } else {
            return buttons.size();
        }
    }

    /**
     * Sets the selected value for the <code>ButtonModel</code>.
     * Only one button in the group may be selected at a time.
     * @param m the <code>ButtonModel</code>
     * @param b <code>true</code> if this button is to be
     *   selected, otherwise <code>false</code>
     */
    public void setSelected(ButtonModel m, boolean b) {
        if (b && (m != null) && (m != selection)) {
            ButtonModel oldSelection = selection;
            selection = m;

            if (oldSelection != null) {
                oldSelection.setSelected(false);
            }

            m.setSelected(true);
        }
    }

    /**
     * Returns whether a <code>ButtonModel</code> is selected.
     * @return <code>true</code> if the button is selected,
     *   otherwise returns <code>false</code>
     */
    public boolean isSelected(ButtonModel m) {
        return (m == selection);
    }

    /**
     * Returns the model of the selected button.
     * @return the selected button model
     */
    public ButtonModel getSelection() {
        return selection;
    }

    /**
     * Adds the button to the group.
     * @param b the button to be added
     */
    public void add(AbstractButton b) {
        if (b == null) {
            return;
        }

        buttons.addElement(b);

        if (b.isSelected()) {
            if (selection == null) {
                selection = b.getModel();
            } else {
                b.setSelected(false);
            }
        }

        b.getModel().setGroup(this);
    }

    public void clearSelection() {
        selection = null;

        for (Iterator it = buttons.iterator(); it.hasNext();) {
            AbstractButton btn = (AbstractButton) it.next();
            btn.setSelected(false);
        }
    }

    /**
     * Removes the button from the group.
     * @param b the button to be removed
     */
    public void remove(AbstractButton b) {
        if (b == null) {
            return;
        }

        buttons.removeElement(b);

        if (b.getModel() == selection) {
            selection = null;
        }

        b.getModel().setGroup(null);
    }
}
