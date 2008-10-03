/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.components.table.JExtendedTablePanel;
import org.netbeans.modules.profiler.ppoints.ProfilingPointFactory;
import org.netbeans.modules.profiler.ppoints.Utils;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;


/**
 *
 * @author Jiri Sedlacek
 */
public class WizardPanel1UI extends ValidityAwarePanel implements HelpCtx.Provider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class PPointTypeTableModel extends DefaultTableModel {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Integer.class;
            } else {
                return String.class;
            }
        } // TODO: enable once Scope is implemented
          //    public Class getColumnClass(int columnIndex) { return String.class; }

        public int getColumnCount() {
            return 2;
        } // TODO: enable once Scope is implemented
          //    public int getColumnCount() { return 1; }

        public int getRowCount() {
            return ppFactories.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            return ppFactories[rowIndex];
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SELECT_PROJECT_STRING = NbBundle.getMessage(WizardPanel1UI.class,
                                                                            "WizardPanel1UI_SelectProjectString"); // NOI18N
    private static final String PP_TYPE_STRING = NbBundle.getMessage(WizardPanel1UI.class, "WizardPanel1UI_PpTypeString"); // NOI18N
    private static final String PP_PROJECT_STRING = NbBundle.getMessage(WizardPanel1UI.class, "WizardPanel1UI_PpProjectString"); // NOI18N
    private static final String DESCRIPTION_LABEL_TEXT = NbBundle.getMessage(WizardPanel1UI.class,
                                                                             "WizardPanel1UI_DescriptionLabelText"); // NOI18N
    private static final String SUPPORTED_MODES_LABEL_TEXT = NbBundle.getMessage(WizardPanel1UI.class,
                                                                                 "WizardPanel1UI_SupportedModesLabelText"); // NOI18N
    private static final String MONITOR_MODE_STRING = NbBundle.getMessage(WizardPanel1UI.class, "WizardPanel1UI_MonitorModeString"); // NOI18N
    private static final String CPU_MODE_STRING = NbBundle.getMessage(WizardPanel1UI.class, "WizardPanel1UI_CpuModeString"); // NOI18N
    private static final String MEMORY_MODE_STRING = NbBundle.getMessage(WizardPanel1UI.class, "WizardPanel1UI_MemoryModeString"); // NOI18N
    private static final String PP_LIST_ACCESS_NAME = NbBundle.getMessage(WizardPanel1UI.class, "WizardPanel1UI_PpListAccessName"); // NOI18N
    private static final String PROJECTS_LIST_ACCESS_NAME = NbBundle.getMessage(WizardPanel1UI.class,
                                                                                "WizardPanel1UI_ProjectsListAccessName"); // NOI18N
                                                                                                                          // -----
    private static final String HELP_CTX_KEY = "PPointsWizardPanel1UI.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static final Icon MONITOR_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/telemetryWindow.png")); // NOI18N
    private static final Icon CPU_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/cpu.png")); // NOI18N
    private static final Icon MEMORY_ICON = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/memory.png")); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DefaultTableModel ppointTypeTableModel;
    private Dimension initialMinSize;
    private JComboBox ppointProjectCombo;
    private JExtendedTable ppointTypeTable;
    private JLabel ppointDescriptionCaptionLabel;
    private JLabel ppointEffectiveCPULabel;
    private JLabel ppointEffectiveCaptionLabel;
    private JLabel ppointEffectiveMemoryLabel;
    private JLabel ppointEffectiveMonitorLabel;
    private JLabel ppointProjectLabel;
    private JLabel ppointTypeCaptionLabel;
    private JTextArea ppointDescriptionArea;
    private ProfilingPointFactory[] ppFactories = new ProfilingPointFactory[0];

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public WizardPanel1UI() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public Dimension getMinSize() {
        return initialMinSize;
    }

    public void setSelectedIndex(int index) {
        if (index == -1) {
            ppointTypeTable.clearSelection();
        } else {
            ppointTypeTable.setRowSelectionInterval(index, index);
        }
    }

    public int getSelectedIndex() {
        return ppointTypeTable.getSelectedRow();
    }

    public void setSelectedProject(Project project) {
        if (project != null) {
            ppointProjectCombo.setSelectedItem(project);
        }

        if ((ppointProjectCombo.getSelectedItem() != project) && (ppointProjectCombo.getItemAt(0) != SELECT_PROJECT_STRING)) {
            ppointProjectCombo.insertItemAt(SELECT_PROJECT_STRING, 0);
            ppointProjectCombo.setSelectedItem(SELECT_PROJECT_STRING);
        }
    }

    public Project getSelectedProject() {
        if (ppointProjectCombo.getSelectedItem() instanceof Project) {
            return (Project) ppointProjectCombo.getSelectedItem();
        } else {
            return null;
        }
    }

    public void init(final ProfilingPointFactory[] ppFactories) {
        this.ppFactories = ppFactories;
        initProjectsCombo();
        ppointTypeTableModel.fireTableDataChanged();
        ppointTypeTable.getColumnModel().getColumn(0)
                       .setMaxWidth(Math.max(ProfilingPointFactory.SCOPE_CODE_ICON.getIconWidth(),
                                             ProfilingPointFactory.SCOPE_GLOBAL_ICON.getIconWidth()) + 25); // TODO: enable once Scope is implemented

        refresh();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        ppointTypeCaptionLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(ppointTypeCaptionLabel, PP_TYPE_STRING);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 10);
        add(ppointTypeCaptionLabel, constraints);

        ppointTypeTableModel = new PPointTypeTableModel();
        ppointTypeTable = new JExtendedTable(ppointTypeTableModel);
        ppointTypeTable.getAccessibleContext().setAccessibleName(PP_LIST_ACCESS_NAME);
        ppointTypeCaptionLabel.setLabelFor(ppointTypeTable);
        ppointTypeTable.setTableHeader(null);
        ppointTypeTable.setRowSelectionAllowed(true);
        ppointTypeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ppointTypeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    refresh();
                }
            });
        ppointTypeTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        ppointTypeTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        ppointTypeTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        ppointTypeTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        ppointTypeTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        ppointTypeTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        ppointTypeTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        ppointTypeTable.setDefaultRenderer(Integer.class, Utils.getScopeRenderer()); // TODO: enable once Scope is implemented
        ppointTypeTable.setDefaultRenderer(String.class, Utils.getPresenterRenderer());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 15, 12, 10);
        add(new JExtendedTablePanel(ppointTypeTable), constraints);

        ppointProjectLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(ppointProjectLabel, PP_PROJECT_STRING);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 10);
        add(ppointProjectLabel, constraints);

        ppointProjectCombo = new JComboBox(new Object[] { SELECT_PROJECT_STRING }) {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }

                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                ;
            };
        ppointProjectLabel.getAccessibleContext().setAccessibleName(PROJECTS_LIST_ACCESS_NAME);
        ppointProjectLabel.setLabelFor(ppointProjectCombo);
        ppointProjectCombo.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    refresh();
                }
            });
        ppointProjectCombo.setRenderer(Utils.getProjectListRenderer());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 15, 12, 10);
        add(ppointProjectCombo, constraints);

        ppointDescriptionCaptionLabel = new JLabel(DESCRIPTION_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 5, 10);
        add(ppointDescriptionCaptionLabel, constraints);

        ppointDescriptionArea = new JTextArea();
        ppointDescriptionArea.setOpaque(false);
        ppointDescriptionArea.setWrapStyleWord(true);
        ppointDescriptionArea.setLineWrap(true);
        ppointDescriptionArea.setEnabled(false);
        ppointDescriptionArea.setFont(UIManager.getFont("Label.font")); //NOI18N
        ppointDescriptionArea.setDisabledTextColor(UIManager.getColor("Label.foreground")); //NOI18N

        int rows = ppointDescriptionArea.getRows();
        ppointDescriptionArea.setRows(4);

        final int height = ppointDescriptionArea.getPreferredSize().height;
        ppointDescriptionArea.setRows(rows);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 15, 12, 10);

        JScrollPane ppointDescriptionAreaScroll = new JScrollPane(ppointDescriptionArea,
                                                                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, height);
            }

            public Dimension getMinimumSize() {
                return new Dimension(super.getMinimumSize().width, height);
            }
        };

        ppointDescriptionAreaScroll.setBorder(BorderFactory.createEmptyBorder());
        ppointDescriptionAreaScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        ppointDescriptionAreaScroll.setOpaque(false);
        ppointDescriptionAreaScroll.getViewport().setOpaque(false);
        add(ppointDescriptionAreaScroll, constraints);

        int maxHeight = ppointDescriptionCaptionLabel.getPreferredSize().height;
        maxHeight = Math.max(maxHeight, MONITOR_ICON.getIconHeight());
        maxHeight = Math.max(maxHeight, CPU_ICON.getIconHeight());
        maxHeight = Math.max(maxHeight, MEMORY_ICON.getIconHeight());

        final int mheight = maxHeight;

        JPanel effectiveModesContainer = new JPanel(new GridBagLayout());

        ppointEffectiveCaptionLabel = new JLabel(SUPPORTED_MODES_LABEL_TEXT) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, mheight);
                }

                public Dimension getMinimumSize() {
                    return new Dimension(super.getMinimumSize().width, mheight);
                }
            };
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveCaptionLabel, constraints);

        ppointEffectiveMonitorLabel = new JLabel(MONITOR_MODE_STRING, MONITOR_ICON, SwingConstants.LEFT);
        ppointEffectiveMonitorLabel.setVisible(false); // TODO: remove once Monitor mode will support Profiling Points
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveMonitorLabel, constraints);

        ppointEffectiveCPULabel = new JLabel(CPU_MODE_STRING, CPU_ICON, SwingConstants.LEFT);
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveCPULabel, constraints);

        ppointEffectiveMemoryLabel = new JLabel(MEMORY_MODE_STRING, MEMORY_ICON, SwingConstants.LEFT);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 10);
        effectiveModesContainer.add(ppointEffectiveMemoryLabel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(effectiveModesContainer, constraints);

        initialMinSize = getMinimumSize();
    }

    private void initProjectsCombo() {
        Project[] projects = ProjectUtilities.getSortedProjects(ProjectUtilities.getOpenedProjects());
        ppointProjectCombo.removeAllItems();

        for (Project project : projects) {
            ppointProjectCombo.addItem(project);
        }

        setSelectedProject(Utils.getCurrentProject());
    }

    private void refresh() {
        if (ppointProjectCombo.getSelectedItem() instanceof Project && (ppointProjectCombo.getItemAt(0) == SELECT_PROJECT_STRING)) {
            ppointProjectCombo.removeItem(SELECT_PROJECT_STRING);
        }

        int selectedIndex = ppointTypeTable.getSelectedRow();

        if (selectedIndex != -1) {
            ProfilingPointFactory ppFactory = ppFactories[selectedIndex];
            ppointDescriptionArea.setText(ppFactory.getDescription());
            ppointEffectiveMonitorLabel.setVisible(ppFactory.supportsMonitor());
            ppointEffectiveCPULabel.setVisible(ppFactory.supportsCPU());
            ppointEffectiveMemoryLabel.setVisible(ppFactory.supportsMemory());
        } else {
            ppointDescriptionArea.setText(""); // NOI18N
            ppointEffectiveMonitorLabel.setVisible(false);
            ppointEffectiveCPULabel.setVisible(false);
            ppointEffectiveMemoryLabel.setVisible(false);
        }

        boolean ppointTypeSelected = selectedIndex != -1;
        boolean ppointProjectSelected = (ppointProjectCombo.getSelectedItem() != null)
                                        && ppointProjectCombo.getSelectedItem() instanceof Project;
        boolean isValid = ppointTypeSelected && ppointProjectSelected;

        if (isValid) {
            if (!areSettingsValid()) {
                fireValidityChanged(true);
            }
        } else {
            if (areSettingsValid()) {
                fireValidityChanged(false);
            }
        }
    }
}
