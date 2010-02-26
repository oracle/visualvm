/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.options;

import com.sun.tools.visualvm.core.options.UISupport;
import com.sun.tools.visualvm.core.ui.components.SectionSeparator;
import com.sun.tools.visualvm.modules.tracer.impl.swing.VerticalLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerOptionsPanel extends JPanel {

    private final TracerOptionsPanelController controller;
    private boolean internalChange;


    TracerOptionsPanel(TracerOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
    }


    boolean dataValid() {
        return true;
    }


    void setInitiallyOpened(String opened) {
        internalChange = true;
        initialProbesCheckBox.setSelected(
                opened.contains(TracerOptions.VIEW_PROBES));
        initialTimelineCheckBox.setSelected(
                opened.contains(TracerOptions.VIEW_TIMELINE));
        internalChange = false;
    }

    String getInitiallyOpened() {
        String result = TracerOptions.VIEWS_UNCHANGED;
        result = append(result, TracerOptions.VIEW_PROBES,
               initialProbesCheckBox.isSelected());
        result = append(result, TracerOptions.VIEW_TIMELINE,
               initialTimelineCheckBox.isSelected());
        return result;
    }

    void setOnProbeAdded(String opened) {
        internalChange = true;
        if (TracerOptions.VIEWS_UNCHANGED.equals(opened)) {
            onProbeAddedProbesCheckBox.setSelected(false);
            onProbeAddedTimelineCheckBox.setSelected(false);
            onProbeAddedNothingCheckBox.setSelected(true);
        } else {
            onProbeAddedProbesCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_PROBES));
            onProbeAddedTimelineCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_TIMELINE));
            onProbeAddedNothingCheckBox.setSelected(false);
        }
        internalChange = false;
    }

    String getOnProbeAdded() {
        String result = TracerOptions.VIEWS_UNCHANGED;
        result = append(result, TracerOptions.VIEW_PROBES,
               onProbeAddedProbesCheckBox.isSelected());
        result = append(result, TracerOptions.VIEW_TIMELINE,
               onProbeAddedTimelineCheckBox.isSelected());
        return result;
    }

    void setOnSessionStart(String opened) {
        internalChange = true;
        if (TracerOptions.VIEWS_UNCHANGED.equals(opened)) {
            onStartProbesCheckBox.setSelected(false);
            onStartTimelineCheckBox.setSelected(false);
            onStartNothingCheckBox.setSelected(true);
        } else {
            onStartProbesCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_PROBES));
            onStartTimelineCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_TIMELINE));
            onStartNothingCheckBox.setSelected(false);
        }
        internalChange = false;
    }

    String getOnSessionStart() {
        String result = TracerOptions.VIEWS_UNCHANGED;
        result = append(result, TracerOptions.VIEW_PROBES,
               onStartProbesCheckBox.isSelected());
        result = append(result, TracerOptions.VIEW_TIMELINE,
               onStartTimelineCheckBox.isSelected());
        return result;
    }

    void setZoomMode(String zoomMode) {
        internalChange = true;
        zoomModeCombo.setSelectedIndex(0); // fallback for invalid zoomMode
        zoomModeCombo.setSelectedItem(zoomMode);
        internalChange = false;
    }

    String getZoomMode() {
        return zoomModeCombo.getSelectedItem().toString();
    }

    void setMouseWheelAction(String action) {
        internalChange = true;
        mouseWheelCombo.setSelectedIndex(0); // fallback for invalid action
        mouseWheelCombo.setSelectedItem(action);
        internalChange = false;
    }

    String getMouseWheelAction() {
        return mouseWheelCombo.getSelectedItem().toString();
    }


    private String append(String result, String item, boolean append) {
        if (!append) return result;
        if (result.length() == 0) return result += item;
        else return result += "," + item; // NOI18N
    }

    void update() {
        if (internalChange) return;
        
        // --- initiallyOpened ----------------------------------------------------
        List selected = getSelected(initiallyOpenedPanel);

        if (selected.isEmpty()) {
            // Fallback to defaults
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setInitiallyOpened(TracerOptions.INITIALLY_OPEN_DEFAULT);
                    update();
                }
            });
        } else {
            initialProbesCheckBox.setEnabled(selected.size() > 1 ||
                    !selected.contains(initialProbesCheckBox));
            initialTimelineCheckBox.setEnabled(selected.size() > 1 ||
                    !selected.contains(initialTimelineCheckBox));
        }

        // --- onProbeAdded ----------------------------------------------------
        if (onProbeAddedNothingCheckBox.isSelected()) {
            onProbeAddedProbesCheckBox.setSelected(false);
            onProbeAddedProbesCheckBox.setEnabled(false);
            onProbeAddedTimelineCheckBox.setSelected(false);
            onProbeAddedTimelineCheckBox.setEnabled(false);
        } else {
            selected = getSelected(onProbeAddedPanel);

            if (selected.isEmpty()) {
                // Fallback to defaults
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setOnProbeAdded(TracerOptions.PROBE_ADDED_DEFAULT);
                        update();
                    }
                });
            } else {
                onProbeAddedProbesCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onProbeAddedProbesCheckBox));
                onProbeAddedTimelineCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onProbeAddedTimelineCheckBox));
            }
        }

        // --- onStart ---------------------------------------------------------
        if (onStartNothingCheckBox.isSelected()) {
            onStartProbesCheckBox.setSelected(false);
            onStartProbesCheckBox.setEnabled(false);
            onStartTimelineCheckBox.setSelected(false);
            onStartTimelineCheckBox.setEnabled(false);
        } else {
            selected = getSelected(onStartOpenedPanel);

            if (selected.isEmpty()) {
                // Fallback to defaults
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setOnSessionStart(TracerOptions.SESSION_STARTED_DEFAULT);
                        update();
                    }
                });
            } else {
                onStartProbesCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onStartProbesCheckBox));
                onStartTimelineCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onStartTimelineCheckBox));
            }
        }

        controller.changed();
    }

    private List<AbstractButton> getSelected(JPanel container) {
        List<AbstractButton> selected = new ArrayList();
        for (Component c : container.getComponents())
            if (c instanceof AbstractButton && ((AbstractButton)c).isSelected())
                selected.add((AbstractButton)c);
        return selected;
    }


    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints c;

        // timelineDefaultsSeparator
        SectionSeparator timelineDefaultsSeparator =
                UISupport.createSectionSeparator("Timeline Defaults"); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        add(timelineDefaultsSeparator, c);

        // zoomModeLabel
        JLabel zoomModeLabel = new JLabel("Zoom mode: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(zoomModeLabel, c);

        // zoomModeCombo
        zoomModeCombo = new JComboBox(new String[] {
                                          TracerOptions.FIXED_SCALE,
                                          TracerOptions.SCALE_TO_FIT }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void selectedItemChanged() {
                TracerOptionsPanel.this.update();
                super.selectedItemChanged();
            }
        };
        zoomModeLabel.setLabelFor(zoomModeCombo);
        zoomModeCombo.setEditable(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(zoomModeCombo, c);

        // mouseWheelLabel
        JLabel mouseWheelLabel = new JLabel("Mouse wheel: ");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 0);
        add(mouseWheelLabel, c);

        // mouseWheelCombo
        mouseWheelCombo = new JComboBox(new String[] {
                                          TracerOptions.MOUSE_WHEEL_ZOOMS,
                                          TracerOptions.MOUSE_WHEEL_HSCROLLS,
                                          TracerOptions.MOUSE_WHEEL_VSCROLLS }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void selectedItemChanged() {
                TracerOptionsPanel.this.update();
                super.selectedItemChanged();
            }
        };
        mouseWheelLabel.setLabelFor(mouseWheelCombo);
        mouseWheelCombo.setEditable(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(mouseWheelCombo, c);


        // viewsBehaviorSeparator
        SectionSeparator viewsBehaviorSeparator =
                UISupport.createSectionSeparator("Views Behavior"); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 0, 5, 0);
        add(viewsBehaviorSeparator, c);

        // viewsBehaviorPanel
        JPanel viewsBehaviorPanel = new JPanel(new GridLayout(1, 3));
        viewsBehaviorPanel.setOpaque(false);
        c = new GridBagConstraints();
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 15, 3, 0);
        add(viewsBehaviorPanel, c);

        // initiallyOpenedPanel
        initiallyOpenedPanel = new JPanel(new VerticalLayout(false));
        initiallyOpenedPanel.setBorder(titledBorder("Initially open:"));
        initiallyOpenedPanel.setOpaque(false);
        // initialProbesCheckBox
        initialProbesCheckBox = new JCheckBox("Probes") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        initiallyOpenedPanel.add(initialProbesCheckBox);
        // initialTimelineCheckBox
        initialTimelineCheckBox = new JCheckBox("Timeline") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        initiallyOpenedPanel.add(initialTimelineCheckBox);
//        // initialSettingsCheckBox
//        JCheckBox initialSettingsCheckBox = new JCheckBox("Settings");
//        initiallyOpenedPanel.add(initialSettingsCheckBox);
//        initialSettingsCheckBox.setEnabled(false);
//        // initialDetailsCheckBox
//        JCheckBox initialDetailsCheckBox = new JCheckBox("Details");
//        initiallyOpenedPanel.add(initialDetailsCheckBox);
//        initialDetailsCheckBox.setEnabled(false);
        viewsBehaviorPanel.add(initiallyOpenedPanel);

        // onProbeAddedPanel
        onProbeAddedPanel = new JPanel(new VerticalLayout(false));
        onProbeAddedPanel.setBorder(titledBorder("Open when probe added:"));
        onProbeAddedPanel.setOpaque(false);
        // initialProbesCheckBox
        onProbeAddedProbesCheckBox = new JCheckBox("Probes") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel.add(onProbeAddedProbesCheckBox);
        // initialTimelineCheckBox
        onProbeAddedTimelineCheckBox = new JCheckBox("Timeline") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel.add(onProbeAddedTimelineCheckBox);
//        // initialSettingsCheckBox
//        JCheckBox onProbeAddedSettingsCheckBox = new JCheckBox("Settings");
//        onProbeAddedPanel.add(onProbeAddedSettingsCheckBox);
//        onProbeAddedSettingsCheckBox.setEnabled(false);
//        // initialDetailsCheckBox
//        JCheckBox onProbeAddedDetailsCheckBox = new JCheckBox("Details");
//        onProbeAddedPanel.add(onProbeAddedDetailsCheckBox);
//        onProbeAddedDetailsCheckBox.setEnabled(false);
        // onStartNothingCheckBox
        onProbeAddedNothingCheckBox = new JCheckBox("No change") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel.add(onProbeAddedNothingCheckBox);
        viewsBehaviorPanel.add(onProbeAddedPanel);

        // onStartOpenedPanel
        onStartOpenedPanel = new JPanel(new VerticalLayout(false));
        onStartOpenedPanel.setBorder(titledBorder("Open when session starts:"));
        onStartOpenedPanel.setOpaque(false);
        // initialProbesCheckBox
        onStartProbesCheckBox = new JCheckBox("Probes") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onStartOpenedPanel.add(onStartProbesCheckBox);
        // initialTimelineCheckBox
        onStartTimelineCheckBox = new JCheckBox("Timeline") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onStartOpenedPanel.add(onStartTimelineCheckBox);
//        // initialSettingsCheckBox
//        JCheckBox onStartSettingsCheckBox = new JCheckBox("Settings");
//        onStartOpenedPanel.add(onStartSettingsCheckBox);
//        onStartSettingsCheckBox.setEnabled(false);
//        // initialDetailsCheckBox
//        JCheckBox onStartDetailsCheckBox = new JCheckBox("Details");
//        onStartOpenedPanel.add(onStartDetailsCheckBox);
//        onStartDetailsCheckBox.setEnabled(false);
        // onStartNothingCheckBox
        onStartNothingCheckBox = new JCheckBox("No change") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onStartOpenedPanel.add(onStartNothingCheckBox);
        viewsBehaviorPanel.add(onStartOpenedPanel);


        // bottomFiller
        JPanel bottomFiller = new JPanel(null);
        bottomFiller.setOpaque(false);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 100;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(bottomFiller, c);
    }


    private JPanel initiallyOpenedPanel;
    private JCheckBox initialProbesCheckBox;
    private JCheckBox initialTimelineCheckBox;
    private JPanel onProbeAddedPanel;
    private JCheckBox onProbeAddedProbesCheckBox;
    private JCheckBox onProbeAddedTimelineCheckBox;
    private JCheckBox onProbeAddedNothingCheckBox;
    private JPanel onStartOpenedPanel;
    private JCheckBox onStartProbesCheckBox;
    private JCheckBox onStartTimelineCheckBox;
    private JCheckBox onStartNothingCheckBox;

    private JComboBox zoomModeCombo;
    private JComboBox mouseWheelCombo;


    private static Border titledBorder(String title) {
        String titleBorder = isWindowsLookAndFeel() ? " " : ""; //NOI18N
        Border inner = BorderFactory.createEmptyBorder(0, 12, 3, 3);
        Border outer = BorderFactory.createTitledBorder(titleBorder + title);
        return BorderFactory.createCompoundBorder(outer, inner);
    }

    private static boolean isWindowsLookAndFeel() {
        return UIManager.getLookAndFeel().getID().equals("Windows"); //NOI18N
    }

}
