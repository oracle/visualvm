/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.oql;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.lib.profiler.heapwalk.OQLSupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "OQLQueryCustomizer_OkButtonText=OK",
    "OQLQueryCustomizer_CloseButtonText=Close",
    "OQLQueryCustomizer_SaveQueryCaption=Save Custom OQL Script",
    "OQLQueryCustomizer_NameLabelText=&Name:",
    "OQLQueryCustomizer_DefaultQueryName=Custom Script",
    "OQLQueryCustomizer_DescriptionLabelText=&Description (optional):"
})
public class OQLQueryCustomizer {
//    private static HelpCtx HELP_CTX_SAVE_QUERY = new HelpCtx("OQLQueryCustomizer.SaveQuery.HelpCtx");  //NOI18N
//    private static HelpCtx HELP_CTX_QUERY_PROPS = new HelpCtx("OQLQueryCustomizer.QueryProps.HelpCtx");//NOI18N


    public static OQLSupport.Query saveCustomizer(OQLSupport.Query query, String script) {
        JButton okButton = new JButton();
        Mnemonics.setLocalizedText(okButton, Bundle.OQLQueryCustomizer_OkButtonText());
        
        String name = query == null ? Bundle.OQLQueryCustomizer_DefaultQueryName() : query.getName();
        String description = query == null ? null : query.getDescription();
        if (description != null && new File(description).isFile()) description = null;

        CustomizerPanel customizer = new CustomizerPanel(name, description, okButton);
        final DialogDescriptor dd = new DialogDescriptor(customizer,
                                            Bundle.OQLQueryCustomizer_SaveQueryCaption(), true,
                                            new Object[] { okButton,
                                            DialogDescriptor.CANCEL_OPTION },
                                            okButton, 0, null, null);
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.pack();
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            return new OQLSupport.Query(script, customizer.name(), customizer.description());
        } else {
            return null;
        }
    }
    

    private static class CustomizerPanel extends JPanel {

        private final JComponent submitComponent;


        CustomizerPanel(String name, String description, JComponent submitComponent) {
            this.submitComponent = submitComponent;

            initComponents(name, description, false);
            updateComponents();
        }

        public String name() {
            return nameField.getText().trim();
        }

        public String description() {
            String description = descriptionArea.getText().trim();
            return description.isEmpty() ? null : description;
        }


        private void updateComponents() {
            submitComponent.setEnabled(!nameField.getText().trim().isEmpty());
        }


        private void initComponents(String name, String description, boolean readOnly) {
            setLayout(new GridBagLayout());
            GridBagConstraints c;

            nameLabel = new JLabel();
            Mnemonics.setLocalizedText(nameLabel, Bundle.OQLQueryCustomizer_NameLabelText());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(16, 16, 8, 8);
            add(nameLabel, c);

            nameField = new JTextField();
            nameLabel.setLabelFor(nameField);
            nameField.setText(name);
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
            c.insets = new Insets(16, 0, 8, 16);
            add(nameField, c);

            descriptionLabel = new JLabel();
            Mnemonics.setLocalizedText(descriptionLabel, Bundle.OQLQueryCustomizer_DescriptionLabelText());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(8, 16, 8, 8);
            add(descriptionLabel, c);

            descriptionArea = new JTextArea(description);
            descriptionLabel.setLabelFor(descriptionArea);
            descriptionArea.setLineWrap(true);
            descriptionArea.setWrapStyleWord(true);
            descriptionArea.setFont(descriptionLabel.getFont());
            descriptionArea.setRows(5);
            final int prefWidth = new JLabel("A lengthy string serving as OQL script description sizer").getPreferredSize().width; // NOI18N
            JScrollPane descriptionAreaScroll = new JScrollPane(descriptionArea,
                                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                public Dimension getPreferredSize() {
                    return new Dimension(prefWidth, super.getPreferredSize().height);
                }
            };
            descriptionArea.setEditable(!readOnly);
            if (readOnly) descriptionArea.setBackground(nameField.getBackground());
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 3;
            c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, 0, 16, 16);
            add(descriptionAreaScroll, c);

            addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                        if (isShowing()) {
                            removeHierarchyListener(this);
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


        private JLabel nameLabel;
        private JTextField nameField;
        private JLabel descriptionLabel;
        private JTextArea descriptionArea;

    }

}
