/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreePath;
import org.netbeans.modules.profiler.heapwalk.OQLSupport;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class OQLQueryCustomizer {

    // -----
    // I18N String constants
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_OkButtonText"); // NOI18N
    private static final String CLOSE_BUTTON_TEXT = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_CloseButtonText"); // NOI18N
    private static final String SAVE_QUERY_CAPTION = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_SaveQueryCaption"); // NOI18N
    private static final String QUERY_PROPERTIES_CAPTION = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_QueryPropertiesCaption"); // NOI18N
    private static final String UP_BUTTON_TOOLTIP = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_UpButtonToolTip"); // NOI18N
    private static final String DOWN_BUTTON_TOOLTIP = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_DownButtonToolTip"); // NOI18N
    private static final String UP_BUTTON_ACCESS_NAME = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_UpButtonAccessName"); // NOI18N
    private static final String DOWN_BUTTON_ACCESS_NAME = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_DownButtonAccessName"); // NOI18N
    private static final String NEW_QUERY_RADIO_TEXT = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_NewQueryRadioText"); // NOI18N
    private static final String EXISTING_QUERY_RADIO_TEXT = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_ExistingQueryRadioText"); // NOI18N
    private static final String NAME_LABEL_TEXT = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_NameLabelText"); // NOI18N
    private static final String DEFAULT_QUERY_NAME = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_DefaultQueryName"); // NOI18N
    private static final String DESCRIPTION_LABEL_TEXT = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_DescriptionLabelText"); // NOI18N
    private static final String UPDATE_QUERY_LABEL_TEXT = NbBundle.getMessage(
            OQLQueryCustomizer.class, "OQLQueryCustomizer_UpdateQueryLabelText"); // NOI18N
    // -----

    private static ImageIcon ICON_UP = ImageUtilities.loadImageIcon(
            "org/netbeans/modules/profiler/heapwalk/ui/resources/up.png", false); // NOI18N
    private static ImageIcon ICON_DOWN = ImageUtilities.loadImageIcon(
            "org/netbeans/modules/profiler/heapwalk/ui/resources/down.png", false); // NOI18N


    public static boolean saveQuery(final String query,
                                    final OQLSupport.OQLTreeModel treeModel,
                                    final JTree tree) {
        JButton okButton = new JButton();
        Mnemonics.setLocalizedText(okButton, OK_BUTTON_TEXT);

        CustomizerPanel customizer = new CustomizerPanel(okButton,  treeModel);
        final DialogDescriptor dd = new DialogDescriptor(customizer,
                                            SAVE_QUERY_CAPTION, true,
                                            new Object[] { okButton,
                                            DialogDescriptor.CANCEL_OPTION },
                                            okButton, 0, null, null);
        final Dialog d = ProfilerDialogs.createDialog(dd);
        d.pack();
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            OQLSupport.OQLQueryNode node;
            if (customizer.isNewQuery()) {
                OQLSupport.Query q = new OQLSupport.Query(query,
                                        customizer.getQueryName(),
                                        customizer.getQueryDescription());
                node = new OQLSupport.OQLQueryNode(q);
                treeModel.customCategory().add(node);
                treeModel.nodeStructureChanged(treeModel.customCategory());
            } else {
                node = (OQLSupport.OQLQueryNode)customizer.getSelectedValue();
                node.getUserObject().setScript(query);
                treeModel.nodeChanged(node);
            }
            tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(node)));
            return true;
        } else {
            return false;
        }
    }

    public static boolean editNode(final OQLSupport.OQLNode node,
                                   final OQLSupport.OQLTreeModel treeModel,
                                   final JTree tree) {

        boolean readOnly = node.isReadOnly();
        final OQLSupport.OQLNode parent = (OQLSupport.OQLNode)node.getParent();
        int originalIndex = parent.getIndex(node);

        JButton okButton = new JButton();
        Mnemonics.setLocalizedText(okButton, OK_BUTTON_TEXT);

        final JButton[] upDownButtons = new JButton[2];
        upDownButtons[0] = new JButton(ICON_UP) {
            protected void fireActionPerformed(ActionEvent e) {
                int index = parent.getIndex(node) - 1;
                treeModel.removeNodeFromParent(node);
                treeModel.insertNodeInto(node, parent, index);
                tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(node)));
                updateButtons(upDownButtons, node);
            }
        };
        upDownButtons[0].setToolTipText(UP_BUTTON_TOOLTIP);
        upDownButtons[0].getAccessibleContext().
                                        setAccessibleName(UP_BUTTON_ACCESS_NAME);
        upDownButtons[1] = new JButton(ICON_DOWN) {
            protected void fireActionPerformed(ActionEvent e) {
                int index = parent.getIndex(node) + 1;
                treeModel.removeNodeFromParent(node);
                treeModel.insertNodeInto(node, parent, index);
                tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(node)));
                updateButtons(upDownButtons, node);
            }
        };
        upDownButtons[1].setToolTipText(DOWN_BUTTON_TOOLTIP);
        upDownButtons[1].getAccessibleContext().
                                      setAccessibleName(DOWN_BUTTON_ACCESS_NAME);

        final CustomizerPanel customizer = new CustomizerPanel(okButton,
                                                node.toString(),
                                                node.getDescription(),
                                                readOnly);

        customizer.getInputMap(CustomizerPanel.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_MASK), "MOVE_UP"); // NOI18N
        customizer.getActionMap().put("MOVE_UP", new AbstractAction() {// NOI18N
            public void actionPerformed(ActionEvent e) {
                if (upDownButtons[0].isEnabled()) upDownButtons[0].doClick();
            }
        });
        customizer.getInputMap(CustomizerPanel.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_MASK), "MOVE_DOWN"); // NOI18N
        customizer.getActionMap().put("MOVE_DOWN", new AbstractAction() {// NOI18N
            public void actionPerformed(ActionEvent e) {
                if (upDownButtons[1].isEnabled()) upDownButtons[1].doClick();
            }
        });

        JButton closeButton = readOnly ? new JButton() : null;
        if (closeButton != null) Mnemonics.setLocalizedText(closeButton, CLOSE_BUTTON_TEXT);
        Object[] options = readOnly ? new Object[] { closeButton } :
                                      new Object[] { okButton,
                                            DialogDescriptor.CANCEL_OPTION };
        JButton defaultButton = readOnly ? closeButton : okButton;

        final DialogDescriptor dd = new DialogDescriptor(customizer,
                                            MessageFormat.format(QUERY_PROPERTIES_CAPTION,
                                            new Object[] { node.getCaption() }),
                                            true, options, defaultButton, 0, null, null);
        dd.setAdditionalOptions(new Object[] { upDownButtons[0],
                                               upDownButtons[1] });
        updateButtons(upDownButtons, node);

        final Dialog d = ProfilerDialogs.createDialog(dd);

        d.pack();
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            OQLSupport.Query query = (OQLSupport.Query)node.getUserObject();
            query.setName(customizer.getQueryName());
            query.setDescription(customizer.getQueryDescription());
            treeModel.nodeChanged(node); // Updates UI
            return true;
        } else {
            int index = parent.getIndex(node);
            if (index != originalIndex) {
                treeModel.removeNodeFromParent(node);
                treeModel.insertNodeInto(node, parent, originalIndex);
                tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(node)));
            }
            return false;
        }
    }

    private static void updateButtons(JButton[] upDownButtons,
                                      OQLSupport.OQLNode node) {
        if (node.isReadOnly()) {
            upDownButtons[0].setEnabled(false);
            upDownButtons[1].setEnabled(false);
        } else {
            upDownButtons[0].setEnabled(node.getPreviousSibling() != null);
            upDownButtons[1].setEnabled(node.getNextSibling() != null);
        }
    }

    private static class CustomizerPanel extends JPanel {

        private JComponent submitComponent;
        private Object lastSelectedValue;


        public CustomizerPanel(JComponent submitComponent,
                               OQLSupport.OQLTreeModel treeModel) {
            this.submitComponent = submitComponent;

            initComponents(treeModel, false);
            updateComponents();
        }

        public CustomizerPanel(JComponent submitComponent, String name,
                               String description, boolean readOnly) {
            this.submitComponent = submitComponent;

            initComponents(null, readOnly);

            nameField.setText(name);
            descriptionArea.setText(description == null ? "" : description); // NOI18N
            try { descriptionArea.setCaretPosition(0); } catch (Exception e) {}

            updateComponents();
        }


        public boolean isNewQuery() {
            return newRadio == null || newRadio.isSelected();
        }

        public String getQueryName() {
            return nameField.getText().trim();
        }

        public String getQueryDescription() {
            String description = descriptionArea.getText().trim();
            return description.length() > 0 ? description : null;
        }

        public Object getSelectedValue() {
            return existingList.getSelectedValue();
        }


        private void updateComponents() {
            if (newRadio != null) {
                boolean createNew = newRadio.isSelected();

                nameLabel.setEnabled(createNew);
                nameField.setEnabled(createNew);
                descriptionLabel.setEnabled(createNew);
                descriptionArea.setEnabled(createNew);

                existingLabel.setEnabled(!createNew);
                if (createNew && existingList.isEnabled()) {
                    lastSelectedValue = existingList.getSelectedValue();
                    existingList.setEnabled(false);
                    existingList.clearSelection();
                } else if (!createNew && !existingList.isEnabled()) {
                    existingList.setEnabled(true);
                    if (lastSelectedValue == null)
                        lastSelectedValue = existingList.getModel().getElementAt(0);
                    existingList.setSelectedValue(lastSelectedValue, false);
                }
            }

            if (existingRadio != null && existingRadio.isSelected()) {
                submitComponent.setEnabled(existingList.getSelectedValue() != null);
            } else {
                submitComponent.setEnabled(nameField.getText().trim().length() > 0);
            }
        }


        private void initComponents(OQLSupport.OQLTreeModel treeModel, boolean readOnly) {
            final boolean allowExisting = treeModel != null && treeModel.hasCustomCategories();

            setLayout(new GridBagLayout());
            GridBagConstraints c;


            if (allowExisting) {
                JPanel headerContainer1 = new JPanel(new GridBagLayout());

                newRadio = new JRadioButton() {
                    protected void fireActionPerformed(ActionEvent e) { updateComponents(); }
                };
                Mnemonics.setLocalizedText(newRadio, NEW_QUERY_RADIO_TEXT);
                newRadio.setSelected(true);
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.NONE;
                c.insets = new Insets(0, 0, 0, 0);
                headerContainer1.add(newRadio, c);

                newSeparator = new JSeparator(JSeparator.HORIZONTAL) {
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }
                };
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 0;
                c.weightx = 1;
                c.weighty = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(0, 0, 0, 0);
                headerContainer1.add(newSeparator, c);

                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(8, 8, 0, 8);
                add(headerContainer1, c);
            }

            nameLabel = new JLabel();
            Mnemonics.setLocalizedText(nameLabel, NAME_LABEL_TEXT);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(allowExisting ? 8 : 16, allowExisting ? 40 : 16, 8, 8);
            add(nameLabel, c);

            nameField = new JTextField();
            nameLabel.setLabelFor(nameField);
            nameField.setText(DEFAULT_QUERY_NAME);
            nameField.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {  updateComponents(); }
                public void removeUpdate(DocumentEvent e) {  updateComponents(); }
                public void changedUpdate(DocumentEvent e) {  updateComponents(); }
            });
            nameField.setEditable(!readOnly);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(allowExisting ? 8 : 16, 0, 8, 16);
            add(nameField, c);

            descriptionLabel = new JLabel();
            Mnemonics.setLocalizedText(descriptionLabel, DESCRIPTION_LABEL_TEXT);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(8, allowExisting ? 40 : 16, 8, 8);
            add(descriptionLabel, c);

            descriptionArea = new JTextArea();
            descriptionLabel.setLabelFor(descriptionArea);
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setFont(descriptionLabel.getFont());
            descriptionArea.setRows(3);
            JScrollPane descriptionAreaScroll = new JScrollPane(descriptionArea,
                                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                public Dimension getPreferredSize() {
                    return new Dimension(250, super.getPreferredSize().height);
                }
                public Dimension getMinimumSize() {
                    return allowExisting ? getPreferredSize() : super.getMinimumSize();
                }
            };
            descriptionArea.setEditable(!readOnly);
            if (readOnly) descriptionArea.setBackground(nameField.getBackground());
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 3;
            if (!allowExisting) c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, 0, allowExisting ? 8 : 16, 16);
            add(descriptionAreaScroll, c);

            if (allowExisting) {
                JPanel headerContainer2 = new JPanel(new GridBagLayout());

                existingRadio = new JRadioButton() {
                    protected void fireActionPerformed(ActionEvent e) { updateComponents(); }
                };
                Mnemonics.setLocalizedText(existingRadio, EXISTING_QUERY_RADIO_TEXT);
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 4;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.NONE;
                c.insets = new Insets(0, 0, 0, 0);
                headerContainer2.add(existingRadio, c);

                existingSeparator = new JSeparator(JSeparator.HORIZONTAL) {
                    public Dimension getMinimumSize() {
                        return getPreferredSize();
                    }
                };
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 4;
                c.weightx = 1;
                c.weighty = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(0, 0, 0, 0);
                headerContainer2.add(existingSeparator, c);

                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 4;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(8, 8, 0, 8);
                add(headerContainer2, c);

                existingLabel = new JLabel();
                Mnemonics.setLocalizedText(existingLabel, UPDATE_QUERY_LABEL_TEXT);
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 5;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.NONE;
                c.insets = new Insets(8, 40, 8, 8);
                add(existingLabel, c);

                Vector v = new Vector();
                Enumeration e = treeModel.customCategory().children();
                while (e.hasMoreElements()) v.add(e.nextElement());
                existingList = new JList(v);
                existingLabel.setLabelFor(existingList);
                existingList.setVisibleRowCount(3);
                existingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                existingList.addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        updateComponents();
                    }
                });
                JScrollPane existingListScroll = new JScrollPane(existingList,
                                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                c = new GridBagConstraints();
                c.gridx = 1;
                c.gridy = 6;
                c.weighty = 1;
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.fill = GridBagConstraints.BOTH;
                c.insets = new Insets(0, 0, 16, 16);
                add(existingListScroll, c);

                ButtonGroup radios = new ButtonGroup();
                radios.add(newRadio);
                radios.add(existingRadio);
            }

            addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        if (isShowing()) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    nameField.requestFocus();
                                    nameField.selectAll();
                                }
                            });
                        }
                    }
                }
            });

        }


        private JRadioButton newRadio;
        private JSeparator newSeparator;
        private JLabel nameLabel;
        private JTextField nameField;
        private JLabel descriptionLabel;
        private JTextArea descriptionArea;
        private JRadioButton existingRadio;
        private JSeparator existingSeparator;
        private JLabel existingLabel;
        private JList existingList;

    }

}
