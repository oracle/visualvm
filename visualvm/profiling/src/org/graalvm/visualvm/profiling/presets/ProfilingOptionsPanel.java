/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiling.presets;

import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.core.options.UISupport;
import org.graalvm.visualvm.profiling.presets.ProfilerPresets.PresetsModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class ProfilingOptionsPanel extends JPanel {

//    final private static Logger LOGGER =
//            Logger.getLogger("org.graalvm.visualvm.profiling.options"); // NOI18N
    private final ProfilingOptionsPanelController controller;

    private final SamplerCPUSettings samplerCpuSettings;
    private final SamplerMemorySettings samplerMemorySettings;
    private final ProfilerCPUSettings profilerCpuSettings;
    private final ProfilerMemorySettings profilerMemorySettings;
    private final ProfilerJDBCSettings profilerJdbcSettings;

    private PresetsModel listModel;
    private final ListDataListener listModelListener;

    private boolean internalChange;

    private boolean nameValid = true;


    ProfilingOptionsPanel(ProfilingOptionsPanelController controller) {
        this.controller = controller;

        Runnable validator = new Runnable() {
            public void run() {
                ProfilerPreset preset = list.getSelectedValue();
                if (preset == null) return;
                preset.setValid(samplerCpuSettings.valid() &&
                                profilerCpuSettings.valid() &&
                                samplerMemorySettings.valid() &&
                                profilerMemorySettings.valid() &&
                                profilerJdbcSettings.valid());
                ProfilingOptionsPanel.this.controller.changed();
            }
        };

        samplerCpuSettings = new SamplerCPUSettings(validator);
        samplerMemorySettings = new SamplerMemorySettings();
        profilerCpuSettings = new ProfilerCPUSettings(validator);
        profilerMemorySettings = new ProfilerMemorySettings(validator);
        profilerJdbcSettings = new ProfilerJDBCSettings();

        listModelListener = new ListDataListener() {
            public void intervalAdded(ListDataEvent e) {
                updateComponents();
            }
            public void intervalRemoved(ListDataEvent e) {
                updateComponents();
            }
            public void contentsChanged(ListDataEvent e) {}
        };

        initComponents();
    }


    private void updateComponents() {
        int selectedIndex = listModel.isEmpty() ? -1 : list.getSelectedIndex();
        if (selectedIndex == listModel.getSize()) return; // isAdjusting
        
        removeButton.setEnabled(selectedIndex != -1);
        upButton.setEnabled(selectedIndex > 0);
        downButton.setEnabled(selectedIndex < listModel.getSize() - 1);

        refreshPreset(selectedIndex);
    }

    private String createPresetName() {
        Set<String> names = new HashSet<>();
        Enumeration<ProfilerPreset> presetsE = listModel.elements();
        while (presetsE.hasMoreElements())
            names.add(presetsE.nextElement().toString());

        int presetIndex = 1;
        String name = NbBundle.getMessage(ProfilingOptionsPanel.class, "MSG_Preset") + " "; // NOI18N

        while (names.contains(name + presetIndex)) presetIndex++;

        return name + presetIndex;
    }

    private void createPreset() {
        ProfilerPreset preset = new ProfilerPreset(createPresetName(), ""); // NOI18N
        listModel.addPreset(preset);
        list.setSelectedIndex(listModel.getSize() - 1);
        preselectNameField();
    }

    private void preselectNameField() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nameField.requestFocusInWindow();
                nameField.selectAll();
            }
        });
    }

    private void deletePreset() {
        int selectedIndex = list.getSelectedIndex();
        listModel.removePreset(selectedIndex);
        if (listModel.getSize() > 0)
            list.setSelectedIndex(selectedIndex == listModel.getSize() ?
                                  selectedIndex - 1 : selectedIndex);
    }

    private void movePresetUp() {
        int selectedIndex = list.getSelectedIndex();
        listModel.movePresetUp(selectedIndex);
        list.setSelectedIndex(selectedIndex - 1);
    }

    private void movePresetDown() {
        int selectedIndex = list.getSelectedIndex();
        listModel.movePresetDown(selectedIndex);
        list.setSelectedIndex(selectedIndex + 1);
    }

    private void refreshPreset(int presetIndex) {
        ProfilerPreset preset = presetIndex == -1 ? new ProfilerPreset("", "") : // NOI18N
                                listModel.get(presetIndex);

        internalChange = true;
        nameField.setText(preset.getName());
        targetField.setText(preset.getSelector());
        internalChange = false;

        samplerCpuSettings.setPreset(preset);
        samplerMemorySettings.setPreset(preset);
        profilerCpuSettings.setPreset(preset);
        profilerMemorySettings.setPreset(preset);
        profilerJdbcSettings.setPreset(preset);

        presetsPanel.setEnabled(presetIndex != -1);
    }

    private void updatePreset() {
        if (internalChange) return;
        ProfilerPreset preset = listModel.get(list.getSelectedIndex());

        preset.setName(nameField.getText());
        preset.setSelector(targetField.getText());

        nameValid = !nameField.getText().isEmpty();

        controller.changed();
    }
    
    private void initComponents() {
        final boolean nimbusLaF =
                org.graalvm.visualvm.uisupport.UISupport.isNimbusLookAndFeel();

        GridBagConstraints c;

        setLayout(new GridBagLayout());

        // --- Presets ---------------------------------------------------------
        SectionSeparator presetsSection = UISupport.createSectionSeparator(
                NbBundle.getMessage(ProfilingOptionsPanel.class, "CAPTION_Presets")); // NOI18N
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

        list = new JList<>();
        list.setSelectionModel(new DefaultListSelectionModel() {
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(index0, index1);
                updateComponents();
            }
            public void removeSelectionInterval(int i1, int i2) {}
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final Dimension oneDim = new Dimension(1, 1);
        final JLabel noPresetsLabel = new JLabel(NbBundle.getMessage(
                ProfilingOptionsPanel.class, "MSG_No_presets"), JLabel.CENTER); // NOI18N
        noPresetsLabel.setEnabled(false);
        noPresetsLabel.setSize(noPresetsLabel.getPreferredSize());
        final JScrollPane listScroll = new JScrollPane(list) {
            public Dimension getPreferredSize() {
                return oneDim;
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                if (listModel == null || listModel.getSize() == 0) {
                    int x = (getWidth() - noPresetsLabel.getWidth()) / 2;
                    int y = (getHeight() - noPresetsLabel.getHeight()) / 2;
                    g.translate(x, y);
                    noPresetsLabel.paint(g);
                    g.translate(-x, -y);
                }
            }
        };
        listPanel.add(listScroll, BorderLayout.CENTER);
        
        addButton = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                createPreset();
            }
        };
        addButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/graalvm/visualvm/profiler/resources/add.png", true)));   // NOI18N
        addButton.setToolTipText(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "TOOLTIP_Create_preset")); // NOI18N
        Insets margin = addButton.getMargin();
        int mar = nimbusLaF ? 0 : 8;
        margin.left = mar;
        margin.right = mar;
        addButton.setMargin(margin);
        removeButton = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                deletePreset();
                listScroll.repaint();
            }
        };
        removeButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/graalvm/visualvm/profiler/resources/remove.png", true)));   // NOI18N
        removeButton.setToolTipText(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "TOOLTIP_Delete_preset")); // NOI18N
        removeButton.setMargin(margin);
        upButton = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                movePresetUp();
            }
        };
        upButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/graalvm/visualvm/profiler/resources/up.png", true)));   // NOI18N
        upButton.setToolTipText(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "TOOLTIP_preset_up")); // NOI18N
        upButton.setMargin(margin);
        downButton = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                movePresetDown();
            }
        };
        downButton.setIcon(new ImageIcon(ImageUtilities.loadImage(
                "org/graalvm/visualvm/profiler/resources/down.png", true)));   // NOI18N
        downButton.setToolTipText(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "TOOLTIP_preset_down")); // NOI18N
        downButton.setMargin(margin);

        JPanel controlsPanel = new JPanel(new GridLayout(1, 4, 5, 0)) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        controlsPanel.add(addButton);
        controlsPanel.add(removeButton);
        controlsPanel.add(upButton);
        controlsPanel.add(downButton);
        listPanel.add(controlsPanel, BorderLayout.SOUTH);

        JPanel headerPanel = new JPanel(new GridBagLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };

        JLabel nameLabel = new JLabel();
        Mnemonics.setLocalizedText(nameLabel, NbBundle.getMessage(
                ProfilingOptionsPanel.class, "LBL_Preset_name")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 3, 3, 0);
        headerPanel.add(nameLabel, c);

        nameField = new JTextField();
        nameLabel.setLabelFor(nameField);
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreset(); listModel.fireItemChanged(list.getSelectedIndex()); }
            public void removeUpdate(DocumentEvent e) { updatePreset(); listModel.fireItemChanged(list.getSelectedIndex()); }
            public void changedUpdate(DocumentEvent e) { updatePreset(); listModel.fireItemChanged(list.getSelectedIndex()); }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 0);
        headerPanel.add(nameField, c);

        JLabel targetLabel = new JLabel();
        Mnemonics.setLocalizedText(targetLabel, NbBundle.getMessage(
                ProfilingOptionsPanel.class, "LBL_Preselect_for")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 3, 13, 0);
        headerPanel.add(targetLabel, c);

        final JLabel noTargetLabel = new JLabel(NbBundle.getMessage(
                ProfilingOptionsPanel.class, "LBL_Optional_class"), JLabel.CENTER); // NOI18N
        noTargetLabel.setEnabled(false);
        noTargetLabel.setSize(noTargetLabel.getPreferredSize());
        targetField = new JTextField() {
            protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                String text = getText();
                if (!isFocusOwner() && (text == null || text.isEmpty())) {
                    int x = nimbusLaF ? 6 : 2;
                    int y = (getHeight() - noTargetLabel.getHeight()) / 2;
                    g.translate(x, y);
                    noTargetLabel.paint(g);
                    g.translate(-x, -y);
                }
            }
            protected void processFocusEvent(FocusEvent e) {
                super.processFocusEvent(e);
                repaint();
            }
        };
        targetLabel.setLabelFor(targetField);
        targetField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updatePreset(); }
            public void removeUpdate(DocumentEvent e) { updatePreset(); }
            public void changedUpdate(DocumentEvent e) { updatePreset(); }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 13, 0);
        headerPanel.add(targetField, c);

        JTabbedPane settingsPanel = new JTabbedPane() {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };
        settingsPanel.addTab(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "LBL_Sampler_cpu"), new ImageIcon(ImageUtilities.loadImage( // NOI18N
                "org/graalvm/visualvm/profiling/resources/sampler.png", true)), // NOI18N
                samplerCpuSettings);
        settingsPanel.addTab(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "LBL_Sampler_memory"), new ImageIcon(ImageUtilities.loadImage( // NOI18N
                "org/graalvm/visualvm/profiling/resources/sampler.png", true)), // NOI18N
                samplerMemorySettings);
        settingsPanel.addTab(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "LBL_Profiler_cpu"), new ImageIcon(ImageUtilities.loadImage( // NOI18N
                "org/graalvm/visualvm/profiling/resources/profiler.png", true)), // NOI18N
                profilerCpuSettings);
        settingsPanel.addTab(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "LBL_Profiler_memory"), new ImageIcon(ImageUtilities.loadImage( // NOI18N
                "org/graalvm/visualvm/profiling/resources/profiler.png", true)), // NOI18N
                profilerMemorySettings);
        settingsPanel.addTab(NbBundle.getMessage(ProfilingOptionsPanel.class,
                "LBL_Profiler_jdbc"), new ImageIcon(ImageUtilities.loadImage( // NOI18N
                "org/graalvm/visualvm/profiling/resources/profiler.png", true)), // NOI18N
                profilerJdbcSettings);

        presetsPanel = new JPanel(new BorderLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents())
                    c.setEnabled(enabled);
            }
        };
        presetsPanel.add(headerPanel, BorderLayout.NORTH);
        presetsPanel.add(settingsPanel, BorderLayout.CENTER);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(3, 8, 3, 0);
        add(presetsPanel, c);


        // --- Miscellaneous ----------------------------------------------------
        int gridy = 50;
        for (ProfilingOptionsSectionProvider provider : Lookup.getDefault().lookupAll(
                                                        ProfilingOptionsSectionProvider.class)) {
            SectionSeparator section = UISupport.createSectionSeparator(provider.getSectionName());
            c = new GridBagConstraints();
            c.gridy = gridy++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(15, 0, 5, 0);
            add(section, c);
            
            c = new GridBagConstraints();
            c.gridy = gridy++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(2, 15, 3, 0);
            add(provider.getSection(), c);
        }
    }

    void load() {
        listModel = ProfilerPresets.getInstance().getPresets();
        listModel.addListDataListener(listModelListener);
        list.setModel(listModel);
        int items = listModel.getSize();

        ProfilerPreset toCreate = ProfilerPresets.getInstance().presetToCreate();
        if (toCreate != null) {
            toCreate.setName(createPresetName());
            listModel.addElement(toCreate);
            list.setSelectedIndex(items);
        } else if (listModel.size() > 0) {
            ProfilerPreset select = ProfilerPresets.getInstance().presetToSelect();
            String toSelect = select == null ? null : select.getName();
            int indexToSelect = 0;
            if (toSelect != null) {
                for (int i = 0; i < items; i++) {
                    ProfilerPreset preset = listModel.get(i);
                    if (preset.getName().equals(toSelect)) {
                        indexToSelect = i;
                        break;
                    }
                }
            }  
            list.setSelectedIndex(indexToSelect);
        }

        updateComponents();

        if (toCreate != null) preselectNameField();
        
        repaint();
    }

    void store() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                normalizeNames(listModel); // prevents duplicate preset names

                ProfilerPresets.getInstance().savePresets(listModel);
                ProfilerPreset selected = list.getSelectedValue();
                ProfilerPresets.getInstance().optionsSubmitted(selected);
            }
        });
    }

    void closed() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (listModel != null) listModel.removeListDataListener(listModelListener);
                list.setModel(new DefaultListModel<>());
            }
        });
    }

    boolean valid() {
        return nameValid /*&& presetsValid()*/;
    }
    
//    private boolean presetsValid() {
//        Enumeration presets = listModel.elements();
//        while (presets.hasMoreElements()) {
//            ProfilerPreset preset = (ProfilerPreset)presets.nextElement();
//            if (!preset.isValid()) return false;
//        }
//        return true;
//    }
    
    private static void normalizeNames(PresetsModel model) {
        Map<String, Integer> names = new HashMap<>();
        
        for (int i = 0; i < model.getSize(); i++) {
            ProfilerPreset preset = model.getElementAt(i);
            names.put(preset.getName(), 0);
        }
        
        for (int i = 0; i < model.getSize(); i++) {
            ProfilerPreset preset = model.getElementAt(i);
            String presetName = preset.getName();
            Integer nameCounts = names.get(presetName);
            if (nameCounts != null) {
                if (nameCounts == 0) {
                    names.put(presetName, ++nameCounts);
                } else {
                    String newName = normalizeName(presetName, nameCounts);
                    while (names.containsKey(newName)) newName = normalizeName(presetName, ++nameCounts);
                    preset.setName(newName);
                    names.put(newName, 1);
                    names.put(presetName, nameCounts);
                    model.fireItemChanged(i);
                }
            }
        }
    }
    
    private static String normalizeName(String name, int modifier) {
        return name + " (" + modifier + ")"; // NOI18N
    }


    private JPanel presetsPanel;
    private JList<ProfilerPreset> list;
    private JButton addButton;
    private JButton removeButton;
    private JButton upButton;
    private JButton downButton;
    private JTextField nameField;
    private JTextField targetField;
    
}
