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

package org.netbeans.lib.profiler.ui.components;

import java.awt.Font;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;


/**
 * JSpinner with some bugfixes
 *
 * - setModel doesn't break font
 * - setModel doesn't break accessible name and description
 * - can propagate accessible name and description to its editor (JFormattedTextField)
 * - doesn't consume ESC key
 *
 * @author Jiri Sedlacek
 */
public class JExtendedSpinner extends JSpinner {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JExtendedSpinner() {
        super();
        ((JSpinner.DefaultEditor) getEditor()).getTextField().setFont(UIManager.getFont("Label.font")); // NOI18N
        ((JSpinner.DefaultEditor) getEditor()).getTextField().addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(final java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        processKeyEvent(e);
                    }
                }
            });
    }

    public JExtendedSpinner(SpinnerModel model) {
        super(model);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().setFont(UIManager.getFont("Label.font")); // NOI18N
        ((JSpinner.DefaultEditor) getEditor()).getTextField().addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(final java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        processKeyEvent(e);
                    }
                }
            });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setModel(SpinnerModel model) {
        Font font = ((JSpinner.DefaultEditor) getEditor()).getTextField().getFont();
        String accessibleName = ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext().getAccessibleName();
        String accessibleDescription = ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
                                        .getAccessibleDescription();
        super.setModel(model);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().setFont(font);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext().setAccessibleName(accessibleName);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
         .setAccessibleDescription(accessibleDescription);
    }

    public void fixAccessibility() {
        if (getAccessibleContext() != null) {
            ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
             .setAccessibleName(getAccessibleContext().getAccessibleName());
            ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
             .setAccessibleDescription(getAccessibleContext().getAccessibleDescription());
        }
    }
}
