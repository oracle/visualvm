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

package org.graalvm.visualvm.lib.ui.components;

import java.awt.Component;
import java.awt.event.ItemEvent;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;


/**
 * JComboBox which supports JSeparator inside its popup menu.
 *
 * @author Jiri Sedlacek
 */
public class JExtendedComboBox extends JComboBox {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class ExtendedComboListRenderer extends DefaultListCellRenderer {
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
