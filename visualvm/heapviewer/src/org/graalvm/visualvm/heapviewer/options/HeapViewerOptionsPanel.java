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

package org.graalvm.visualvm.heapviewer.options;

import org.graalvm.visualvm.core.options.UISupport;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.heapviewer.oql.OQLEditorComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.lib.profiler.heapwalk.OQLSupport;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapViewerOptionsPanel_CustomScript=Custom Script",
    "HeapViewerOptionsPanel_CustomScripts=Custom OQL Scripts",
    "HeapViewerOptionsPanel_LoadingScripts=<loading scripts>",
    "HeapViewerOptionsPanel_NoSavedScripts=<no saved scripts>",
    "HeapViewerOptionsPanel_DeleteScriptTooltip=Delete selected script",
    "HeapViewerOptionsPanel_MoveScriptUpTooltip=Move selected script up",
    "HeapViewerOptionsPanel_MoveScriptDownTooltip=Move selected script down",
    "HeapViewerOptionsPanel_NameLabel=&Name:",
    "HeapViewerOptionsPanel_DescriptionLabel=&Description (optional):",
    "HeapViewerOptionsPanel_PreviewLabel=&Preview:",
    "HeapViewerOptionsPanel_HintLabel=To add custom script, use the Save OQL Script action in Heap Viewer | OQL Console."
}) 
final class HeapViewerOptionsPanel extends JPanel {
    
    private boolean loaded;
    
    private final Model model;

    private boolean internalChange;


    HeapViewerOptionsPanel() {
        model = new Model();
        initComponents();
    }
    
    void setQueries(List<OQLSupport.Query> queries) {
        model.clear();
        for (OQLSupport.Query query : queries) model.addElement(query);
        loaded = true;
        model.fireChange();
        
        if (model.isEmpty()) updateSelection();
        else list.setSelectedIndex(0);
    }
    
    List<OQLSupport.Query> getQueries() {
        OQLSupport.Query[] queries = new OQLSupport.Query[model.size()];
        model.copyInto(queries);
        return Arrays.asList(queries);
    }


    private void updateSelection() {
        int selectedIndex = model.isEmpty() ? -1 : list.getSelectedIndex();
        if (selectedIndex == model.getSize()) return; // isAdjusting
        
        removeButton.setEnabled(selectedIndex != -1);
        upButton.setEnabled(selectedIndex > 0);
        downButton.setEnabled(selectedIndex < model.getSize() - 1);

        refreshPreset(selectedIndex);
    }

    private void deleteQuery() {
        int selectedIndex = list.getSelectedIndex();
        model.remove(selectedIndex);
        if (model.getSize() > 0) {
            list.setSelectedIndex(selectedIndex == model.getSize() ?
                                  selectedIndex - 1 : selectedIndex);
        } else {
            updateSelection();
        }
        model.fireChange();
    }

    private void moveQueryUp() {
        int selectedIndex = list.getSelectedIndex();
        OQLSupport.Query query = model.elementAt(selectedIndex);
        model.remove(selectedIndex);
        model.add(selectedIndex - 1, query);
        list.setSelectedIndex(selectedIndex - 1);
    }

    private void moveQueryDown() {
        int selectedIndex = list.getSelectedIndex();
        OQLSupport.Query query = model.elementAt(selectedIndex);
        model.remove(selectedIndex);
        model.add(selectedIndex + 1, query);
        list.setSelectedIndex(selectedIndex + 1);
    }

    private void refreshPreset(int index) {
        OQLSupport.Query query = index == -1 ? null : model.get(index);

        internalChange = true;
        
        nameField.setText(query == null ? "" : query.getName()); // NOI18N
        try { nameField.setCaretPosition(0); } catch (IllegalArgumentException e) {}
        descrArea.setText(query == null ? "" : query.getDescription()); // NOI18N
        try { descrArea.setCaretPosition(0); } catch (IllegalArgumentException e) {}
        previewArea.setScript(query == null ? "" : query.getScript()); // NOI18N
        internalChange = false;

        presetsPanel.setEnabled(index != -1);
    }

    private void updatePreset() {
        if (internalChange) return;
        
        int index = list.getSelectedIndex();
        OQLSupport.Query query = index == -1 ? null : model.get(index);
        
        if (query == null) return;

        query.setName(uniqueName(nameField.getText().trim(), index));
        query.setDescription(descrArea.getText().trim());
        
        model.fireChange();
    }
    
    private String uniqueName(String name, int index) {
        if (name.isEmpty()) name = Bundle.HeapViewerOptionsPanel_CustomScript();
        String baseName = name;
        
        int nameExt = 0;
        while (containsQuery(name, index)) name = baseName + " " + ++nameExt; // NOI18N
        
        return name;
    }
    
    private boolean containsQuery(String name, int index) {
        for (int i = 0; i < model.size(); i++)
            if (i != index && name.equals(model.get(i).getName()))
                return true;
        return false;
    }

    private void initComponents() {
        final boolean nimbusLaF = org.graalvm.visualvm.uisupport.UISupport.isNimbusLookAndFeel();

        GridBagConstraints c;

        setLayout(new GridBagLayout());

        SectionSeparator presetsSection = UISupport.createSectionSeparator(Bundle.HeapViewerOptionsPanel_CustomScripts());
        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        add(presetsSection, c);

        JPanel listPanel = new JPanel(new BorderLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.weighty = 0.5;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 15, 3, 0);
        add(listPanel, c);

        list = new JList(model);
        list.setSelectionModel(new DefaultListSelectionModel() {
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(index0, index1);
                updateSelection();
            }
            public void removeSelectionInterval(int i1, int i2) {}
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final Dimension oneDim = new Dimension(1, 1);
        final JLabel loadingScriptsLabel = new JLabel(Bundle.HeapViewerOptionsPanel_LoadingScripts(), JLabel.CENTER);
        loadingScriptsLabel.setEnabled(false);
        loadingScriptsLabel.setSize(loadingScriptsLabel.getPreferredSize());
        final JLabel noScriptsLabel = new JLabel(Bundle.HeapViewerOptionsPanel_NoSavedScripts(), JLabel.CENTER);
        noScriptsLabel.setEnabled(false);
        noScriptsLabel.setSize(noScriptsLabel.getPreferredSize());
        JScrollPane listScroll = new JScrollPane(list) {
            public Dimension getPreferredSize() {
                return oneDim;
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                if (model.isEmpty()) {
                    JLabel hint = loaded ? noScriptsLabel : loadingScriptsLabel;
                    int x = (getWidth() - hint.getWidth()) / 2;
                    int y = (getHeight() - hint.getHeight()) / 2;
                    g.translate(x, y);
                    hint.paint(g);
                    g.translate(-x, -y);
                }
            }
        };
        listPanel.add(listScroll, BorderLayout.CENTER);
        
        removeButton = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                deleteQuery();
            }
        };
        removeButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/graalvm/visualvm/profiler/resources/remove.png", true)));   // NOI18N
        Insets margin = removeButton.getMargin();
        int mar = nimbusLaF ? 0 : 8;
        margin.left = mar;
        margin.right = mar;
        removeButton.setToolTipText(Bundle.HeapViewerOptionsPanel_DeleteScriptTooltip());
        removeButton.setMargin(margin);
        upButton = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                moveQueryUp();
            }
        };
        upButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/graalvm/visualvm/profiler/resources/up.png", true)));   // NOI18N
        upButton.setToolTipText(Bundle.HeapViewerOptionsPanel_MoveScriptUpTooltip()); // NOI18N
        upButton.setMargin(margin);
        downButton = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                moveQueryDown();
            }
        };
        downButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/graalvm/visualvm/profiler/resources/down.png", true)));   // NOI18N
        downButton.setToolTipText(Bundle.HeapViewerOptionsPanel_MoveScriptDownTooltip());
        downButton.setMargin(margin);

        JPanel controlsPanel = new JPanel(new GridLayout(1, 4, 5, 0)) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        controlsPanel.add(removeButton);
        controlsPanel.add(new JPanel(null));
        controlsPanel.add(upButton);
        controlsPanel.add(downButton);
        listPanel.add(controlsPanel, BorderLayout.SOUTH);

        presetsPanel = new JPanel(new GridBagLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 8, 3, 0);
        add(presetsPanel, c);
        
        JLabel nameLabel = new JLabel();
        Mnemonics.setLocalizedText(nameLabel, Bundle.HeapViewerOptionsPanel_NameLabel());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 3, 3, 0);
        presetsPanel.add(nameLabel, c);

        nameField = new JTextField() {
            public void setBackground(Color bg) {
                super.setBackground(bg);
                if (descrArea != null) descrArea.setBackground(bg);
            }
        };
        nameLabel.setLabelFor(nameField);
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreset(); }
            public void removeUpdate(DocumentEvent e) { updatePreset(); }
            public void changedUpdate(DocumentEvent e) { updatePreset(); }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 0);
        presetsPanel.add(nameField, c);
        
        JLabel descrLabel = new JLabel();
        Mnemonics.setLocalizedText(descrLabel, Bundle.HeapViewerOptionsPanel_DescriptionLabel());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(10, 3, 3, 0);
        presetsPanel.add(descrLabel, c);
        
        descrArea = new JTextArea();
        descrLabel.setLabelFor(descrArea);
        descrArea.setLineWrap(true);
        descrArea.setWrapStyleWord(true);
        descrArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreset(); }
            public void removeUpdate(DocumentEvent e) { updatePreset(); }
            public void changedUpdate(DocumentEvent e) { updatePreset(); }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 0.3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 5, 3, 0);
        JScrollPane descrScroll = new JScrollPane(descrArea) {
            public Dimension getPreferredSize() {
                return getMinimumSize();
            }
            public void setEnabled(boolean b) {
                super.setEnabled(b);
                descrArea.setEnabled(b);
            }
        };
        presetsPanel.add(descrScroll, c);
        
        JLabel previewLabel = new JLabel();
        Mnemonics.setLocalizedText(previewLabel, Bundle.HeapViewerOptionsPanel_PreviewLabel());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(10, 3, 3, 0);
        presetsPanel.add(previewLabel, c);
        
        previewArea = new OQLEditorComponent(null);
        previewArea.setEditable(false);
        previewLabel.setLabelFor(previewArea);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 1;
        c.weighty = 0.7;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 5, 3, 0);
        presetsPanel.add(previewArea, c);
        
        
        JLabel hint = new JLabel(Bundle.HeapViewerOptionsPanel_HintLabel());
        hint.setEnabled(false);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 15, 3, 0);
        add(hint, c);
        
        updateSelection();
    }


    private JPanel presetsPanel;
    private JList list;
    private JButton removeButton;
    private JButton upButton;
    private JButton downButton;
    private JTextField nameField;
    private JTextArea descrArea;
    private OQLEditorComponent previewArea;
    
    
    private static class Model extends DefaultListModel<OQLSupport.Query> {
        
        void fireChange() {
            super.fireContentsChanged(this, 0, getSize() - 1);
        }
        
    }
    
}
