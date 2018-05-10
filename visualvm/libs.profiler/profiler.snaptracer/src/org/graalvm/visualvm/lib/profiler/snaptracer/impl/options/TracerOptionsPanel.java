/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl.options;

import org.netbeans.modules.profiler.snaptracer.impl.swing.SectionSeparator;
import org.netbeans.modules.profiler.snaptracer.impl.swing.Spacer;
import org.netbeans.modules.profiler.snaptracer.impl.swing.CustomComboRenderer;
import org.netbeans.modules.profiler.snaptracer.impl.swing.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.ui.UIUtils;

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


    void setProbesApp(int probesApp) {
        internalChange = true;
        probesDefaultsCombo.setSelectedIndex(probesApp);
        internalChange = false;
    }

    int getProbesApp() {
        return probesDefaultsCombo.getSelectedIndex();
    }

    void setRefresh(int refresh) {
        internalChange = true;
        refreshCombo.setSelectedItem(refresh);
        internalChange = false;
    }

    int getRefresh() {
        return (Integer)refreshCombo.getSelectedItem();
    }

    void setRefreshCustomizable(boolean customizable) {
        refreshCheckBox.setSelected(customizable);
    }

    boolean isRefreshCustomizable() {
        return refreshCheckBox.isSelected();
    }

    void setShowValuesEnabled(boolean showValuesEnabled) {
        internalChange = true;
        minMaxValsCombo.setSelectedItem(showValuesEnabled);
        internalChange = false;
    }

    boolean isShowValuesEnabled() {
        return (Boolean)minMaxValsCombo.getSelectedItem();
    }

    void setShowLegendEnabled(boolean showLegendEnabled) {
        internalChange = true;
        rowLegendCombo.setSelectedItem(showLegendEnabled);
        internalChange = false;
    }

    boolean isShowLegendEnabled() {
        return (Boolean)rowLegendCombo.getSelectedItem();
    }

    void setRowsDecorationEnabled(boolean rowsDecorationEnabled) {
        internalChange = true;
        rowsDecorationCombo.setSelectedItem(rowsDecorationEnabled);
        internalChange = false;
    }

    boolean isRowsDecorationEnabled() {
        return (Boolean)rowsDecorationCombo.getSelectedItem();
    }

    void setRowsSelectionEnabled(boolean rowsSelectionEnabled) {
        internalChange = true;
        rowsSelectionCombo.setSelectedIndex(rowsSelectionEnabled ? 0 : 1);
        internalChange = false;
    }

    boolean isRowsSelectionEnabled() {
        return rowsSelectionCombo.getSelectedIndex() == 0;
    }

    void setTimelineToolbar(int visible) {
        internalChange = true;
        timelineToolbarCombo.setSelectedIndex(visible);
        internalChange = false;
    }

    int getTimelineToolbar() {
        return timelineToolbarCombo.getSelectedIndex();
    }

    void setSelectionToolbar(int visible) {
        internalChange = true;
        selectionToolbarCombo.setSelectedIndex(visible);
        internalChange = false;
    }

    int getSelectionToolbar() {
        return selectionToolbarCombo.getSelectedIndex();
    }

    void setExtraToolbar(int visible) {
        internalChange = true;
        extraToolbarCombo.setSelectedIndex(visible);
        internalChange = false;
    }

    int getExtraToolbar() {
        return extraToolbarCombo.getSelectedIndex();
    }

    void setInitiallyOpened(String opened) {
        internalChange = true;
        initialProbesCheckBox.setSelected(
                opened.contains(TracerOptions.VIEW_PROBES));
        initialTimelineCheckBox.setSelected(
                opened.contains(TracerOptions.VIEW_TIMELINE));
        initialDetailsCheckBox.setSelected(
                opened.contains(TracerOptions.VIEW_DETAILS));
        internalChange = false;
    }

    String getInitiallyOpened() {
        String result = TracerOptions.VIEWS_UNCHANGED;
        result = append(result, TracerOptions.VIEW_PROBES,
               initialProbesCheckBox.isSelected());
        result = append(result, TracerOptions.VIEW_TIMELINE,
               initialTimelineCheckBox.isSelected());
        result = append(result, TracerOptions.VIEW_DETAILS,
               initialDetailsCheckBox.isSelected());
        return result;
    }

    void setOnProbeAdded(String opened) {
        internalChange = true;
        if (TracerOptions.VIEWS_UNCHANGED.equals(opened)) {
            onProbeAddedProbesCheckBox.setSelected(false);
            onProbeAddedTimelineCheckBox.setSelected(false);
            onProbeAddedDetailsCheckBox.setSelected(false);
            onProbeAddedNothingCheckBox.setSelected(true);
        } else {
            onProbeAddedProbesCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_PROBES));
            onProbeAddedTimelineCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_TIMELINE));
            onProbeAddedDetailsCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_DETAILS));
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
        result = append(result, TracerOptions.VIEW_DETAILS,
               onProbeAddedDetailsCheckBox.isSelected());
        return result;
    }

    void setOnProbeAdded2(String opened) {
        internalChange = true;
        if (TracerOptions.VIEWS_UNCHANGED.equals(opened)) {
            onProbeAddedProbesCheckBox2.setSelected(false);
            onProbeAddedTimelineCheckBox2.setSelected(false);
            onProbeAddedDetailsCheckBox2.setSelected(false);
            onProbeAddedNothingCheckBox2.setSelected(true);
        } else {
            onProbeAddedProbesCheckBox2.setSelected(
                    opened.contains(TracerOptions.VIEW_PROBES));
            onProbeAddedTimelineCheckBox2.setSelected(
                    opened.contains(TracerOptions.VIEW_TIMELINE));
            onProbeAddedDetailsCheckBox2.setSelected(
                    opened.contains(TracerOptions.VIEW_DETAILS));
            onProbeAddedNothingCheckBox2.setSelected(false);
        }
        internalChange = false;
    }

    String getOnProbeAdded2() {
        String result = TracerOptions.VIEWS_UNCHANGED;
        result = append(result, TracerOptions.VIEW_PROBES,
               onProbeAddedProbesCheckBox2.isSelected());
        result = append(result, TracerOptions.VIEW_TIMELINE,
               onProbeAddedTimelineCheckBox2.isSelected());
        result = append(result, TracerOptions.VIEW_DETAILS,
               onProbeAddedDetailsCheckBox2.isSelected());
        return result;
    }

    void setOnSessionStart(String opened) {
        internalChange = true;
        if (TracerOptions.VIEWS_UNCHANGED.equals(opened)) {
            onStartProbesCheckBox.setSelected(false);
            onStartTimelineCheckBox.setSelected(false);
            onStartDetailsCheckBox.setSelected(false);
            onStartNothingCheckBox.setSelected(true);
        } else {
            onStartProbesCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_PROBES));
            onStartTimelineCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_TIMELINE));
            onStartDetailsCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_DETAILS));
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
        result = append(result, TracerOptions.VIEW_DETAILS,
               onStartDetailsCheckBox.isSelected());
        return result;
    }

    void setOnRowSelected(String opened) {
        internalChange = true;
        if (TracerOptions.VIEWS_UNCHANGED.equals(opened)) {
            onRowSelectedProbesCheckBox.setSelected(false);
            onRowSelectedTimelineCheckBox.setSelected(false);
            onRowSelectedDetailsCheckBox.setSelected(false);
            onRowSelectedNothingCheckBox.setSelected(true);
        } else {
            onRowSelectedProbesCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_PROBES));
            onRowSelectedTimelineCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_TIMELINE));
            onRowSelectedDetailsCheckBox.setSelected(
                    opened.contains(TracerOptions.VIEW_DETAILS));
            onRowSelectedNothingCheckBox.setSelected(false);
        }
        internalChange = false;
    }

    String getOnRowSelected() {
        String result = TracerOptions.VIEWS_UNCHANGED;
        result = append(result, TracerOptions.VIEW_PROBES,
               onRowSelectedProbesCheckBox.isSelected());
        result = append(result, TracerOptions.VIEW_TIMELINE,
               onRowSelectedTimelineCheckBox.isSelected());
        result = append(result, TracerOptions.VIEW_DETAILS,
               onRowSelectedDetailsCheckBox.isSelected());
        return result;
    }

    void setOnRowSelected2(String opened) {
        internalChange = true;
        if (TracerOptions.VIEWS_UNCHANGED.equals(opened)) {
            onRowSelectedProbesCheckBox2.setSelected(false);
            onRowSelectedTimelineCheckBox2.setSelected(false);
            onRowSelectedDetailsCheckBox2.setSelected(false);
            onRowSelectedNothingCheckBox2.setSelected(true);
        } else {
            onRowSelectedProbesCheckBox2.setSelected(
                    opened.contains(TracerOptions.VIEW_PROBES));
            onRowSelectedTimelineCheckBox2.setSelected(
                    opened.contains(TracerOptions.VIEW_TIMELINE));
            onRowSelectedDetailsCheckBox2.setSelected(
                    opened.contains(TracerOptions.VIEW_DETAILS));
            onRowSelectedNothingCheckBox2.setSelected(false);
        }
        internalChange = false;
    }

    String getOnRowSelected2() {
        String result = TracerOptions.VIEWS_UNCHANGED;
        result = append(result, TracerOptions.VIEW_PROBES,
               onRowSelectedProbesCheckBox2.isSelected());
        result = append(result, TracerOptions.VIEW_TIMELINE,
               onRowSelectedTimelineCheckBox2.isSelected());
        result = append(result, TracerOptions.VIEW_DETAILS,
               onRowSelectedDetailsCheckBox2.isSelected());
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
    
    void setClearSelection(boolean clear) {
        clearSelectionsCheckBox.setSelected(clear);
    }

    boolean isClearSelection() {
        return clearSelectionsCheckBox.isSelected();
    }


    private String append(String result, String item, boolean append) {
        if (!append) return result;
        if (result.length() == 0) return result += item;
        else return result += "," + item; // NOI18N
    }

    void update() {
        if (internalChange) return;

        // --- refreshInterval -------------------------------------------------
        if (refreshCombo.getSelectedIndex() == 0) {
            refreshCheckBox.setSelected(false);
            refreshCheckBox.setEnabled(false);
        } else {
            refreshCheckBox.setEnabled(true);
        }

        // --- rowsDecoration --------------------------------------------------
        if (Utils.forceSpeed()) {
            rowsDecorationCombo.setSelectedItem(Boolean.FALSE);
            rowsDecorationCombo.setEnabled(false);
        }

        // --- initiallyOpened -------------------------------------------------
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
            initialDetailsCheckBox.setEnabled(selected.size() > 1 ||
                    !selected.contains(initialDetailsCheckBox));
        }

        // --- onProbeAdded ----------------------------------------------------
        if (onProbeAddedNothingCheckBox.isSelected()) {
            onProbeAddedProbesCheckBox.setSelected(false);
            onProbeAddedProbesCheckBox.setEnabled(false);
            onProbeAddedTimelineCheckBox.setSelected(false);
            onProbeAddedTimelineCheckBox.setEnabled(false);
            onProbeAddedDetailsCheckBox.setSelected(false);
            onProbeAddedDetailsCheckBox.setEnabled(false);
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
                onProbeAddedDetailsCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onProbeAddedDetailsCheckBox));
            }
        }

        if (onProbeAddedNothingCheckBox2.isSelected()) {
            onProbeAddedProbesCheckBox2.setSelected(false);
            onProbeAddedProbesCheckBox2.setEnabled(false);
            onProbeAddedTimelineCheckBox2.setSelected(false);
            onProbeAddedTimelineCheckBox2.setEnabled(false);
            onProbeAddedDetailsCheckBox2.setSelected(false);
            onProbeAddedDetailsCheckBox2.setEnabled(false);
        } else {
            selected = getSelected(onProbeAddedPanel2);

            if (selected.isEmpty()) {
                // Fallback to defaults
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setOnProbeAdded2(TracerOptions.PROBE_ADDED_DEFAULT2);
                        update();
                    }
                });
            } else {
                onProbeAddedProbesCheckBox2.setEnabled(selected.size() > 1 ||
                        !selected.contains(onProbeAddedProbesCheckBox2));
                onProbeAddedTimelineCheckBox2.setEnabled(selected.size() > 1 ||
                        !selected.contains(onProbeAddedTimelineCheckBox2));
                onProbeAddedDetailsCheckBox2.setEnabled(selected.size() > 1 ||
                        !selected.contains(onProbeAddedDetailsCheckBox2));
            }
        }

        // --- onStart ---------------------------------------------------------
        if (onStartNothingCheckBox.isSelected()) {
            onStartProbesCheckBox.setSelected(false);
            onStartProbesCheckBox.setEnabled(false);
            onStartTimelineCheckBox.setSelected(false);
            onStartTimelineCheckBox.setEnabled(false);
            onStartDetailsCheckBox.setSelected(false);
            onStartDetailsCheckBox.setEnabled(false);
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
                onStartDetailsCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onStartDetailsCheckBox));
            }
        }

        // --- onRowSelected ---------------------------------------------------
        if (onRowSelectedNothingCheckBox.isSelected()) {
            onRowSelectedProbesCheckBox.setSelected(false);
            onRowSelectedProbesCheckBox.setEnabled(false);
            onRowSelectedTimelineCheckBox.setSelected(false);
            onRowSelectedTimelineCheckBox.setEnabled(false);
            onRowSelectedDetailsCheckBox.setSelected(false);
            onRowSelectedDetailsCheckBox.setEnabled(false);
        } else {
            selected = getSelected(onRowSelectedPanel);

            if (selected.isEmpty()) {
                // Fallback to defaults
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setOnRowSelected(TracerOptions.ROW_SELECTED_DEFAULT);
                        update();
                    }
                });
            } else {
                onRowSelectedProbesCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onRowSelectedProbesCheckBox));
                onRowSelectedTimelineCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onRowSelectedTimelineCheckBox));
                onRowSelectedDetailsCheckBox.setEnabled(selected.size() > 1 ||
                        !selected.contains(onRowSelectedDetailsCheckBox));
            }
        }

        if (onRowSelectedNothingCheckBox2.isSelected()) {
            onRowSelectedProbesCheckBox2.setSelected(false);
            onRowSelectedProbesCheckBox2.setEnabled(false);
            onRowSelectedTimelineCheckBox2.setSelected(false);
            onRowSelectedTimelineCheckBox2.setEnabled(false);
            onRowSelectedDetailsCheckBox2.setSelected(false);
            onRowSelectedDetailsCheckBox2.setEnabled(false);
        } else {
            selected = getSelected(onRowSelectedPanel2);

            if (selected.isEmpty()) {
                // Fallback to defaults
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        setOnRowSelected2(TracerOptions.ROW_SELECTED_DEFAULT2);
                        update();
                    }
                });
            } else {
                onRowSelectedProbesCheckBox2.setEnabled(selected.size() > 1 ||
                        !selected.contains(onRowSelectedProbesCheckBox2));
                onRowSelectedTimelineCheckBox2.setEnabled(selected.size() > 1 ||
                        !selected.contains(onRowSelectedTimelineCheckBox2));
                onRowSelectedDetailsCheckBox2.setEnabled(selected.size() > 1 ||
                        !selected.contains(onRowSelectedDetailsCheckBox2));
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

        // probesDefaultsSeparator
        SectionSeparator probesDefaultsSeparator =
                new SectionSeparator("Probes Settings"); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 5, 0);
        add(probesDefaultsSeparator, c);

        JLabel probesDefaultsLabel = new JLabel("Initial appearance:");
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 15, 3, 5);
        add(probesDefaultsLabel, c);

        // probesDefaultsCombo
        probesDefaultsCombo = new JComboBox(new String[] { "first expanded",
                                                           "all expanded",
                                                           "all collapsed" }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void selectedItemChanged() {
                TracerOptionsPanel.this.update();
                super.selectedItemChanged();
            }
        };
        probesDefaultsLabel.setLabelFor(probesDefaultsCombo);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(probesDefaultsCombo, c);

        // timelineDefaultsSeparator
        SectionSeparator timelineDefaultsSeparator =
                new SectionSeparator("Timeline Settings"); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 0, 5, 0);
        add(timelineDefaultsSeparator, c);

        JLabel refreshRateLabel = new JLabel("Sampling frequency:");
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 15, 3, 5);
        add(refreshRateLabel, c);

        Integer[] refreshRates = new Integer[] { -1, 100, 200, 500, 1000, 2000, 5000, 10000 };
        refreshCombo = new JComboBox(refreshRates)  {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void selectedItemChanged() {
                TracerOptionsPanel.this.update();
                super.selectedItemChanged();
            }
        };
        refreshRateLabel.setLabelFor(refreshCombo);
        refreshCombo.setRenderer(new CustomComboRenderer.Number(refreshCombo, "ms", true));
        refreshCombo.setEditable(false);
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 5, 3, 0);
        add(refreshCombo, c);

        refreshCheckBox = new JCheckBox("Customizable in Tracer tab") {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = Math.min(d.height, refreshCombo.getPreferredSize().height);
                return d;
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 5, 3, 0);
        add(refreshCheckBox, c);

        // zoomModeLabel
        JLabel zoomModeLabel = new JLabel("Zoom mode:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
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
        zoomModeCombo.setRenderer(new CustomComboRenderer.String(zoomModeCombo));
        zoomModeCombo.setEditable(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(zoomModeCombo, c);

        // customizableZoomModeLabel
        JLabel customizableZoomModeLabel = new JLabel("Customizable in Tracer tab");
        customizableZoomModeLabel.setEnabled(false);
        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 9, 3, 0);
        add(customizableZoomModeLabel, c);

        // mouseWheelLabel
        JLabel mouseWheelLabel = new JLabel("Mouse wheel action:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
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
        mouseWheelCombo.setRenderer(new CustomComboRenderer.String(mouseWheelCombo));
        mouseWheelCombo.setEditable(false);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(mouseWheelCombo, c);

        // customizableMouseWheelLabel
        JLabel customizableMouseWheelLabel = new JLabel("Customizable in Tracer tab");
        customizableMouseWheelLabel.setEnabled(false);
        c.gridx = 2;
        c.gridy = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 9, 3, 0);
        add(customizableMouseWheelLabel, c);


        // timelineAppearanceSeparator
        SectionSeparator timelineAppearanceSeparator =
                new SectionSeparator("Timeline Appearance"); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 6;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 0, 5, 0);
        add(timelineAppearanceSeparator, c);

        // minMaxValsLabel
        JLabel minMaxValsLabel = new JLabel("Show min/max values:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
        add(minMaxValsLabel, c);

        // minMaxValsCombo
        minMaxValsCombo = new JComboBox(new Boolean[] { Boolean.TRUE,
                                                        Boolean.FALSE }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        minMaxValsLabel.setLabelFor(minMaxValsCombo);
        minMaxValsCombo.setRenderer(new CustomComboRenderer.Boolean(minMaxValsCombo));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 7;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(minMaxValsCombo, c);

        // customizableMinMaxValsLabel
        JLabel customizableMinMaxValsLabel = new JLabel("Customizable in Tracer tab");
        customizableMinMaxValsLabel.setEnabled(false);
        c.gridx = 2;
        c.gridy = 7;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 9, 3, 0);
        add(customizableMinMaxValsLabel, c);

        // rowLegendLabel
        JLabel rowLegendLabel = new JLabel("Show row legend:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 8;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
        add(rowLegendLabel, c);

        // rowLegendCombo
        rowLegendCombo = new JComboBox(new Boolean[] { Boolean.TRUE,
                                                       Boolean.FALSE }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        rowLegendLabel.setLabelFor(rowLegendCombo);
        rowLegendCombo.setRenderer(new CustomComboRenderer.Boolean(rowLegendCombo));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 8;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(rowLegendCombo, c);

        // customizableRowLegendLabel
        JLabel customizableRowLegendLabel = new JLabel("Customizable in Tracer tab");
        customizableRowLegendLabel.setEnabled(false);
        c.gridx = 2;
        c.gridy = 8;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 9, 3, 0);
        add(customizableRowLegendLabel, c);

        // rowsDecorationLabel
        JLabel rowsDecorationLabel = new JLabel("Rows decoration:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 9;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
        add(rowsDecorationLabel, c);

        // rowsDecorationCombo
        rowsDecorationCombo = new JComboBox(new Boolean[] { Boolean.TRUE,
                                                            Boolean.FALSE }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        rowsDecorationLabel.setLabelFor(rowsDecorationCombo);
        rowsDecorationCombo.setRenderer(new CustomComboRenderer.Boolean(rowsDecorationCombo));
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 9;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(rowsDecorationCombo, c);

        // rowsSelectionLabel
        JLabel rowsSelectionLabel = new JLabel("Rows selection:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 10;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
        add(rowsSelectionLabel, c);

        // rowsSelectionCombo
        rowsSelectionCombo = new JComboBox(new String[] { "panel and chart",
                                                          "panel only" }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        rowsSelectionLabel.setLabelFor(rowsSelectionCombo);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 10;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(rowsSelectionCombo, c);


        // toolbarsBehaviorSeparator
        SectionSeparator toolbarsBehaviorSeparator =
                new SectionSeparator("Toolbars Behavior"); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 11;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 0, 5, 0);
        add(toolbarsBehaviorSeparator, c);

        // minMaxValsLabel
        JLabel timelineToolbarLabel = new JLabel("Timeline toolbar:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 12;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
        add(timelineToolbarLabel, c);

        // timelineToolbarCombo
        timelineToolbarCombo = new JComboBox(new String[] { "always visible",
                                                            "show with timeline" }) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void selectedItemChanged() {
                TracerOptionsPanel.this.update();
                super.selectedItemChanged();
            }
        };
        timelineToolbarLabel.setLabelFor(timelineToolbarCombo);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 12;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(timelineToolbarCombo, c);

        // selectionToolbarLabel
        JLabel selectionToolbarLabel = new JLabel("Selection toolbar:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 13;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
        add(selectionToolbarLabel, c);

        // rowLegendCombo
        selectionToolbarCombo = new JComboBox(new String[] { "always visible",
                                                             "show on selection",
                                                             "always hidden"}) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void selectedItemChanged() {
                TracerOptionsPanel.this.update();
                super.selectedItemChanged();
            }
        };
        selectionToolbarLabel.setLabelFor(selectionToolbarCombo);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 13;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(selectionToolbarCombo, c);

        // extraToolbarLabel
        JLabel extraToolbarLabel = new JLabel("Export toolbar:");
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 14;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 15, 3, 5);
        add(extraToolbarLabel, c);

        // extraToolbarCombo
        extraToolbarCombo = new JComboBox(new String[] { "always visible",
                                                          "show on data",
                                                          "always hidden"}) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            protected void selectedItemChanged() {
                TracerOptionsPanel.this.update();
                super.selectedItemChanged();
            }
        };
        extraToolbarLabel.setLabelFor(extraToolbarCombo);
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 14;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 5, 3, 0);
        add(extraToolbarCombo, c);


        // viewsBehaviorSeparator
        SectionSeparator viewsBehaviorSeparator =
                new SectionSeparator("Views Behavior"); // NOI18N
        c = new GridBagConstraints();
        c.gridy = 15;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(15, 0, 5, 0);
        add(viewsBehaviorSeparator, c);

        // viewsBehaviorPanel
        JPanel viewsBehaviorPanel = new JPanel(new BorderLayout(0, 0));
        viewsBehaviorPanel.setOpaque(false);
        c = new GridBagConstraints();
        c.gridy = 16;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 15, 0, 0);
        add(viewsBehaviorPanel, c);

        final CardLayout viewsSettingsLayout = new CardLayout();
        final JPanel viewsSettingsContainer = new JPanel(viewsSettingsLayout);
        final String INITIALLY_OPEN = "Initial appearance";
        final String OPEN_PROBE_ADDED = "Probe added or removed";
        final String OPEN_SESSION_STARTS = "Session started";
        final String OPEN_ROW_SELECTED = "Row selected or unselected";

        final DefaultListModel connectionTypeListModel = new DefaultListModel();
        connectionTypeListModel.addElement(INITIALLY_OPEN);
        connectionTypeListModel.addElement(OPEN_PROBE_ADDED);
        connectionTypeListModel.addElement(OPEN_SESSION_STARTS);
        connectionTypeListModel.addElement(OPEN_ROW_SELECTED);

        JList connectionTypeList = new JList(connectionTypeListModel);
        connectionTypeList.setVisibleRowCount(connectionTypeListModel.getSize());

        connectionTypeList.setSelectionModel(new DefaultListSelectionModel() {
            public void setSelectionInterval(int index0, int index1) {
                super.setSelectionInterval(index0, index1);
                viewsSettingsLayout.show(viewsSettingsContainer, connectionTypeListModel.get(getMinSelectionIndex()).toString());
            }
            public void removeSelectionInterval(int i1, int i2) {}
        });
        connectionTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        connectionTypeList.setSelectedIndex(0);
        final ListCellRenderer defaultRenderer = connectionTypeList.getCellRenderer();
        Component rc = defaultRenderer.getListCellRendererComponent(connectionTypeList, "X", 0, false, false); // NOI18N
        connectionTypeList.setFixedCellHeight(rc.getPreferredSize().height + 2);
        connectionTypeList.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return defaultRenderer.getListCellRendererComponent(list, " " + value + " ", index, isSelected, cellHasFocus); // NOI18N
            }
        });
        JScrollPane connectionTypeScroll = new JScrollPane(connectionTypeList,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            public Dimension getMinimumSize() { return getPreferredSize(); }
        };
        createBorder(connectionTypeScroll, BorderFactory.createEmptyBorder(0, 0, 0, 10));
        viewsBehaviorPanel.add(connectionTypeScroll, BorderLayout.WEST);
        viewsBehaviorPanel.add(viewsSettingsContainer, BorderLayout.CENTER);


        // initiallyOpenedPanel
        initiallyOpenedPanel = new JPanel(new VerticalLayout(false));
        initiallyOpenedPanel.setBorder(titledBorder("Select the views to open:"));
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
        // initialDetailsCheckBox
        initialDetailsCheckBox = new JCheckBox("Details") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        initiallyOpenedPanel.add(initialDetailsCheckBox);
        viewsSettingsContainer.add(initiallyOpenedPanel, INITIALLY_OPEN);

        // onProbeAddedPanel
        onProbeAddedPanel = new JPanel(new VerticalLayout(false));
        onProbeAddedPanel.setBorder(titledBorder("Open for selected probes:"));
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
        // initialDetailsCheckBox
        onProbeAddedDetailsCheckBox = new JCheckBox("Details") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel.add(onProbeAddedDetailsCheckBox);
        // onStartNothingCheckBox
        onProbeAddedNothingCheckBox = new JCheckBox("No change") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel.add(onProbeAddedNothingCheckBox);


        // onProbeAddedPanel2
        onProbeAddedPanel2 = new JPanel(new VerticalLayout(false));
        onProbeAddedPanel2.setBorder(titledBorder("Open for no selection:"));
        onProbeAddedPanel2.setOpaque(false);
        // initialProbesCheckBox2
        onProbeAddedProbesCheckBox2 = new JCheckBox("Probes") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel2.add(onProbeAddedProbesCheckBox2);
        // initialTimelineCheckBox2
        onProbeAddedTimelineCheckBox2 = new JCheckBox("Timeline") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel2.add(onProbeAddedTimelineCheckBox2);
        // onProbeAddedDetailsCheckBox2
        onProbeAddedDetailsCheckBox2 = new JCheckBox("Details") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel2.add(onProbeAddedDetailsCheckBox2);
        // onProbeAddedNothingCheckBox2
        onProbeAddedNothingCheckBox2 = new JCheckBox("No change") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onProbeAddedPanel2.add(onProbeAddedNothingCheckBox2);

        JPanel onProbeAddedContainer = new JPanel(new GridLayout(1, 2));
        onProbeAddedContainer.add(onProbeAddedPanel);
        onProbeAddedContainer.add(onProbeAddedPanel2);
        viewsSettingsContainer.add(onProbeAddedContainer, OPEN_PROBE_ADDED);

        // onStartOpenedPanel
        onStartOpenedPanel = new JPanel(new VerticalLayout(false));
        onStartOpenedPanel.setBorder(titledBorder("Select the views to open:"));
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
        // initialDetailsCheckBox
        onStartDetailsCheckBox = new JCheckBox("Details") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onStartOpenedPanel.add(onStartDetailsCheckBox);
        // onStartNothingCheckBox
        onStartNothingCheckBox = new JCheckBox("No change") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onStartOpenedPanel.add(onStartNothingCheckBox);
        viewsSettingsContainer.add(onStartOpenedPanel, OPEN_SESSION_STARTS);

        // onRowSelectedPanel
        onRowSelectedPanel = new JPanel(new VerticalLayout(false));
        onRowSelectedPanel.setBorder(titledBorder("Open for selected rows:"));
        onRowSelectedPanel.setOpaque(false);
        // initialProbesCheckBox
        onRowSelectedProbesCheckBox = new JCheckBox("Probes") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel.add(onRowSelectedProbesCheckBox);
        // initialTimelineCheckBox
        onRowSelectedTimelineCheckBox = new JCheckBox("Timeline") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel.add(onRowSelectedTimelineCheckBox);
        // initialDetailsCheckBox
        onRowSelectedDetailsCheckBox = new JCheckBox("Details") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel.add(onRowSelectedDetailsCheckBox);
        // onStartNothingCheckBox
        onRowSelectedNothingCheckBox = new JCheckBox("No change") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel.add(onRowSelectedNothingCheckBox);


        // onRowSelectedPanel2
        onRowSelectedPanel2 = new JPanel(new VerticalLayout(false));
        onRowSelectedPanel2.setBorder(titledBorder("Open for no selection:"));
        onRowSelectedPanel2.setOpaque(false);
        // initialProbesCheckBox2
        onRowSelectedProbesCheckBox2 = new JCheckBox("Probes") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel2.add(onRowSelectedProbesCheckBox2);
        // initialTimelineCheckBox2
        onRowSelectedTimelineCheckBox2 = new JCheckBox("Timeline") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel2.add(onRowSelectedTimelineCheckBox2);
        // onProbeAddedDetailsCheckBox2
        onRowSelectedDetailsCheckBox2 = new JCheckBox("Details") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel2.add(onRowSelectedDetailsCheckBox2);
        // onProbeAddedNothingCheckBox2
        onRowSelectedNothingCheckBox2 = new JCheckBox("No change") {
            protected void fireActionPerformed(ActionEvent e) {
                TracerOptionsPanel.this.update();
            }
        };
        onRowSelectedPanel2.add(onRowSelectedNothingCheckBox2);

        JPanel onRowSelectedContainer = new JPanel(new GridLayout(1, 2));
        onRowSelectedContainer.add(onRowSelectedPanel);
        onRowSelectedContainer.add(onRowSelectedPanel2);
        viewsSettingsContainer.add(onRowSelectedContainer, OPEN_ROW_SELECTED);

        // clearSelectionsCheckBox
        clearSelectionsCheckBox = new JCheckBox("Clear selected rows when closing Details view") {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 17;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(8, 15, 0, 0);
        add(clearSelectionsCheckBox, c);


        // bottomFiller
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 100;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(Spacer.create(), c);
    }


    private JComboBox probesDefaultsCombo;
    private JComboBox refreshCombo;
    private JCheckBox refreshCheckBox;
    private JComboBox minMaxValsCombo;
    private JComboBox rowLegendCombo;
    private JComboBox rowsDecorationCombo;
    private JComboBox rowsSelectionCombo;
    private JComboBox timelineToolbarCombo;
    private JComboBox selectionToolbarCombo;
    private JComboBox extraToolbarCombo;
    private JPanel initiallyOpenedPanel;
    private JCheckBox initialProbesCheckBox;
    private JCheckBox initialTimelineCheckBox;
    private JCheckBox initialDetailsCheckBox;
    private JPanel onProbeAddedPanel;
    private JCheckBox onProbeAddedProbesCheckBox;
    private JCheckBox onProbeAddedTimelineCheckBox;
    private JCheckBox onProbeAddedDetailsCheckBox;
    private JCheckBox onProbeAddedNothingCheckBox;
    private JPanel onProbeAddedPanel2;
    private JCheckBox onProbeAddedProbesCheckBox2;
    private JCheckBox onProbeAddedTimelineCheckBox2;
    private JCheckBox onProbeAddedDetailsCheckBox2;
    private JCheckBox onProbeAddedNothingCheckBox2;
    private JPanel onStartOpenedPanel;
    private JCheckBox onStartProbesCheckBox;
    private JCheckBox onStartTimelineCheckBox;
    private JCheckBox onStartDetailsCheckBox;
    private JCheckBox onStartNothingCheckBox;
    private JPanel onRowSelectedPanel;
    private JCheckBox onRowSelectedProbesCheckBox;
    private JCheckBox onRowSelectedTimelineCheckBox;
    private JCheckBox onRowSelectedDetailsCheckBox;
    private JCheckBox onRowSelectedNothingCheckBox;
    private JPanel onRowSelectedPanel2;
    private JCheckBox onRowSelectedProbesCheckBox2;
    private JCheckBox onRowSelectedTimelineCheckBox2;
    private JCheckBox onRowSelectedDetailsCheckBox2;
    private JCheckBox onRowSelectedNothingCheckBox2;
    private JCheckBox clearSelectionsCheckBox;

    private JComboBox zoomModeCombo;
    private JComboBox mouseWheelCombo;


    private static Border titledBorder(String title) {
        String titleBorder = UIUtils.isWindowsLookAndFeel() ? " " : ""; //NOI18N
        Border inner = BorderFactory.createEmptyBorder(0, 12, 3, 3);
        Border outer = BorderFactory.createTitledBorder(titleBorder + title);
        return BorderFactory.createCompoundBorder(outer, inner);
    }

    private static void createBorder(JComponent component, Border border) {
        Border cBorder = component.getBorder();
        if (cBorder == null) component.setBorder(border);
        else component.setBorder(BorderFactory.createCompoundBorder(border, cBorder));
    }

}
