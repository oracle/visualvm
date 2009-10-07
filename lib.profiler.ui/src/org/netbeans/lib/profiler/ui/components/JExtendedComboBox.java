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

import java.awt.Component;
import java.awt.event.ItemEvent;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 * JComboBox which supports JSeparator inside its popup menu.
 *
 * @author Jiri Sedlacek
 */
public class JExtendedComboBox extends JComboBox {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class ExtendedComboListRenderer extends DefaultListCellRenderer {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            if ((value != null) && value instanceof JSeparator) {
                return (JSeparator) value;
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DefaultComboBoxModel model = new DefaultComboBoxModel();
    private boolean closingWithSeparator = false;
    private int lastSelectedIndex = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JExtendedComboBox() {
        setModel(model);
        setRenderer(new ExtendedComboListRenderer());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setModel(ComboBoxModel aModel) {
        if (!(aModel instanceof DefaultComboBoxModel)) {
            throw new RuntimeException("Only DefaultComboBoxModel is supported for this component"); //NOI18N
        }

        model = (DefaultComboBoxModel) aModel;
        super.setModel(model);
    }

    public void firePopupMenuWillBecomeInvisible() {
        if (getSelectedItem() instanceof JSeparator) {
            closingWithSeparator = true;
        }

        super.firePopupMenuWillBecomeInvisible();
    }

    protected void fireItemStateChanged(ItemEvent e) {
        switch (e.getStateChange()) {
            case ItemEvent.SELECTED:

                if (e.getItem() instanceof JSeparator) {
                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                selectNextItem();
                            }
                        });

                }

                break;
            case ItemEvent.DESELECTED:

                if (!(e.getItem() instanceof JSeparator)) {
                    lastSelectedIndex = model.getIndexOf(e.getItem());
                }

                break;
        }

        super.fireItemStateChanged(e);
    }

    private void selectNextItem() {
        int currentSelectedIndex = getSelectedIndex();

        if (closingWithSeparator) {
            setSelectedIndex(lastSelectedIndex);
            closingWithSeparator = false;
        } else if (currentSelectedIndex > lastSelectedIndex) {
            setSelectedIndex(currentSelectedIndex + 1);
        } else {
            setSelectedIndex(currentSelectedIndex - 1);
        }
    }
}
