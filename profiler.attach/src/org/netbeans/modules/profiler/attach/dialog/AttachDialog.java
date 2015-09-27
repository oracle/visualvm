/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.attach.dialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.netbeans.lib.profiler.jps.JpsProxy;
import org.netbeans.lib.profiler.jps.RunningVM;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.swing.ActionPopupButton;
import org.netbeans.lib.profiler.ui.swing.FilteringToolbar;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.attach.AttachWizard;
import org.netbeans.modules.profiler.attach.spi.AttachStepsProvider;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "AttachDialog_JavaApplication=Java application", // NOI18N
    "AttachDialog_Caption=Attach Settings", // NOI18N
    "AttachDialog_NoSteps=No steps available.", // NOI18N
    "AttachDialog_SelectProcess=&Select the process for profiling:", // NOI18N
    "AttachDialog_SelectProcessReload=&Select the process for profiling (reloading...):", // NOI18N
    "AttachDialog_ReloadProcesses=Reload processes", // NOI18N
    "AttachDialog_ShowDetails=Show process details", // NOI18N
    "AttachDialog_ConnectToSelected=&Always connect to selected process", // NOI18N
    "AttachDialog_ConnectToProcess=&Always connect to \"{0}\"", // NOI18N
    "AttachDialog_FilterProcesses=Filter processes", // NOI18N
    "AttachDialog_Profile=&Profile:", // NOI18N
    "AttachDialog_Hostname=Host&name:", // NOI18N
    "AttachDialog_OsJvm=&OS & JVM:", // NOI18N
    "AttachDialog_LocalRunningProcess=Already running local Java process", // NOI18N
    "AttachDialog_LocalStartedProcess=Manually started local Java process", // NOI18N
    "AttachDialog_RemoteStartedProcess=Manually started remote Java process", // NOI18N
    "AttachDialog_UnknownProcess=Java process", // NOI18N
    "AttachDialog_ColumnName=Name", // NOI18N
    "AttachDialog_ColumnPid=PID", // NOI18N
    "AttachDialog_RowPid=PID:", // NOI18N
    "AttachDialog_RowMainClass=Main class:", // NOI18N
    "AttachDialog_RowArguments=Arguments:", // NOI18N
    "AttachDialog_RowJvmArguments=JVM Arguments:", // NOI18N
    "AttachDialog_RowJvmFlags=JVM Flags:", // NOI18N
    "AttachDialog_DetailsUnknown=unknown", // NOI18N
    "AttachDialog_DetailsNone=none", // NOI18N
    "AttachDialog_BtnClose=Close", // NOI18N
    "AttachDialog_DetailsCaption=Details of {0}", // NOI18N
    "AttachDialog_Steps=P&erform the following steps to start profiling:", // NOI18N
    "AttachDialog_ProcessNameTtp=Process name", // NOI18N
    "AttachDialog_ProcessIdTtp=Process identifier" // NOI18N
})
@ServiceProvider(service = AttachWizard.class)
public class AttachDialog extends AttachWizard {
    
    private AttachStepsProvider currentProvider;
    private Panel panel;
    
    
    @Override
    public boolean configured(AttachSettings settings) {
        if (settings == null) return false; // no settings provided
        
        if (settings.isRemote()) {
            String host = settings.getHost();
            return host != null && !host.trim().isEmpty(); // remote settings valid if host provided
        } else {
            if (settings.isDirect()) return true; // local direct settings always valid
            
            int pid = settings.getPid();
            String name = settings.getProcessName();
            
            if (pid == -1 || name == null) return false; // no preselected process for dynamic attach
            
            assert !SwingUtilities.isEventDispatchThread();
            
            RunningVM[] vms = JpsProxy.getRunningVMs();
            if (vms == null || vms.length == 0) return false; // no locally running processes for dynamic attach
            
            List<RunningVM> targets = new ArrayList();
            for (RunningVM vm : vms)
                if (getProcessName(vm.getMainClass()).equals(name))
                    targets.add(vm); // all processes with the preferred process name ready for profiling
            
            if (targets.isEmpty()) return false; // no locally running process with the preselected process name
            
            if (settings.isAutoSelectProcess() && targets.size() == 1) {
                settings.setPid(targets.get(0).getPid());
                return true; // exactly one preferred process found for dynamic attach
            }
            
            for (RunningVM vm : targets) if (vm.getPid() == pid) return true; // preselected process found
        }
        
        return false;
    }

    @Override
    public AttachSettings configure(AttachSettings settings, boolean partially) {
        if (settings == null)
            settings = new AttachSettings();
        
        // Configure default settings
        if (settings.getTargetType().isEmpty()) {
            settings.setDirect(false);
            settings.setDynamic16(true);
            settings.setRemote(false);
        }
        
        // Configure implicit settings
        settings.setTargetType(Bundle.AttachDialog_JavaApplication());
        settings.setServerType(Bundle.AttachDialog_JavaApplication());
                
        // Workaround for remote OS
        if (settings.isRemote()) settings.setHostOS(null);
        
        panel = new Panel();
        panel.setup(settings);
        DialogDescriptor dd = new DialogDescriptor(panel, Bundle.AttachDialog_Caption());
        if (!partially) {
            dd.setValid(false);
            panel.setDisplayer(dd);
        }
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);
        AttachSettings result = dd.getValue() == DialogDescriptor.OK_OPTION ?
                                panel.getSettings() : null;
        if (currentProvider != null) currentProvider.removeChangeListener(panel);
        currentProvider = null;
        panel = null;
        return result;
    }
    
    private String steps(AttachSettings settings) {
        Collection<? extends AttachStepsProvider> providers =
                Lookup.getDefault().lookupAll(AttachStepsProvider.class);
        
        if (currentProvider != null) currentProvider.removeChangeListener(panel);
        
        for (AttachStepsProvider provider : providers) {
            String steps = provider.getSteps(settings);
            if (steps != null) {
                currentProvider = provider;
                currentProvider.addChangeListener(panel);
                return steps;
            }
        }
        
        currentProvider = null;
        
        return Bundle.AttachDialog_NoSteps();
    }
    
    
    private static final String ATTACH_WIZARD_HELPCTX = "AttachDialog.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(ATTACH_WIZARD_HELPCTX);
    
    private class Panel extends JPanel implements HelpCtx.Provider, ChangeListener {
        
        private static final int VISIBLE_ROWS = 9;
        
        private ActionPopupButton modeButton;
        private JTextField hostname;
        private ActionPopupButton os;
        private HTMLTextArea steps;
        private JCheckBox autoSelect;
        
        private DialogDescriptor displayer;
        private AttachSettings as;
        
        private String selectedName;
        private int selectedPid = -1;
        
        private boolean updatingUI = false;
        
        
        Panel() {
            initComponents();
        }
        
        void setDisplayer(DialogDescriptor displayer) {
            this.displayer = displayer;
        }
        
        void setup(AttachSettings as) {
            this.as = as;
            updatingUI = true;
            if (as.isRemote()) modeButton.selectAction(3);
            else if (as.isDirect()) modeButton.selectAction(2);
            else modeButton.selectAction(0);
            modeButton.getSelectedAction().actionPerformed(null);
            if (as.isRemote()) {
                hostname.setText(as.getHost());
                String hostOS = as.getHostOS();
                for (Action action : os.getActions())
                    if (action != null && action.getValue(Action.NAME).equals(hostOS)) {
                        os.selectAction(action);
                        break;
                    }
            } else {
                hostname.setText(""); // NOI18N
                os.selectAction(0);
            }
            if (!as.isRemote() && as.isDynamic16()) {
                selectedName = as.getProcessName();
                selectedPid = as.getPid();
                autoSelect.setSelected(as.isAutoSelectProcess());
                updateAutoSelect();
            }
            updatingUI = false;
            updateSteps();
        }
        
        AttachSettings getSettings() {
            int mode = modeButton.getSelectedIndex();
            as.setRemote(mode == 3);
            if (as.isRemote()) {
                as.setDirect(true);
                as.setDynamic16(false);
                as.setHost(hostname.getText().trim());
                as.setHostOS(os.getSelectedAction().getValue(Action.NAME).toString());
            } else {
                as.setDirect(mode == 2);
                as.setDynamic16(mode == 0);
                as.setHostOS(IntegrationUtils.getLocalPlatform(-1));
                as.setProcessName(selectedName);
                as.setPid(selectedPid);
                as.setAutoSelectProcess(autoSelect.isSelected());
            }
            return as;
        }
        
        private void initComponents() {
            
            final JLabel processesHint = new JLabel("", JLabel.LEADING); // NOI18N
            Mnemonics.setLocalizedText(processesHint, Bundle.AttachDialog_SelectProcess());
            processesHint.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
            
            final ProcessesModel processesModel = new ProcessesModel();
            
            final ProfilerTable processes = new ProfilerTable(processesModel, true, true, null){
                public String getToolTipText(MouseEvent event) {
                    int row = rowAtPoint(event.getPoint());
                    if (row == -1) return null;
                    
                    row = convertRowIndexToModel(row);
                    return "<html>" + getDetails((RunningVM)processesModel.getValueAt(row, -1)) + "</html>"; // NOI18N
                }
                public Point getToolTipLocation(MouseEvent event) {
                    int row = rowAtPoint(event.getPoint());
                    if (row == -1) return null;
                    
                    Rectangle rect = getCellRect(row, 0, false);
                    return new Point(event.getX() + 15, rect.y + rect.height + 2);
                }
            };
            processesHint.setLabelFor(processes);
            processes.setMainColumn(0);
            processes.setFitWidthColumn(0);
            processes.setDefaultSortOrder(SortOrder.ASCENDING);
            processes.setSortColumn(0);
            processes.setColumnToolTips(new String[] { Bundle.AttachDialog_ProcessNameTtp(),
                                                       Bundle.AttachDialog_ProcessIdTtp() });
            LabelRenderer processRenderer = new LabelRenderer();
            processes.setColumnRenderer(0, processRenderer);
            LabelRenderer pidRenderer = new LabelRenderer();
            pidRenderer.setHorizontalAlignment(LabelRenderer.TRAILING);
            processes.setColumnRenderer(1, pidRenderer);
            pidRenderer.setValue(processes.getColumnName(1), -1);
            int w = pidRenderer.getPreferredSize().width;
            pidRenderer.setValue("9999999", -1); // NOI18N
            w = Math.max(w, pidRenderer.getPreferredSize().width);
            processes.setDefaultColumnWidth(1, w + 10);
            Dimension prefSize = processes.getPreferredSize();
            prefSize.height = processes.getRowHeight() * VISIBLE_ROWS;
            prefSize.height += processes.getTableHeader().getPreferredSize().height;
            processes.setPreferredScrollableViewportSize(prefSize);
            ProfilerTableContainer processesContainer = new ProfilerTableContainer(processes, true, null);
            
            final JButton refresh = new JButton(Icons.getIcon(GeneralIcons.UPDATE_NOW)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    processesModel.refresh();
                }
            };
            refresh.setToolTipText(Bundle.AttachDialog_ReloadProcesses());
            
            processesModel.addTableModelListener(new TableModelListener() {
                public void tableChanged(TableModelEvent e) {
                    boolean enabled = !processesModel.isRefreshing();
                    processes.setEnabled(enabled);
                    refresh.setEnabled(enabled);
                    Mnemonics.setLocalizedText(processesHint, enabled ?
                                Bundle.AttachDialog_SelectProcess() :
                                Bundle.AttachDialog_SelectProcessReload());
                    if (processesModel.isFirstRefresh() && selectedName != null) {
                        SwingUtilities.invokeLater(new Runnable() { // wait for the table to refresh its model
                            public void run() {
                                int _nameC = processes.convertColumnIndexToView(0);
                                int _pidC = processes.convertColumnIndexToView(1);
                                for (int row = 0; row < processes.getRowCount(); row++) {
                                    if (selectedName.equals(processes.getValueAt(row, _nameC)) &&
                                        selectedPid == (Integer)processes.getValueAt(row, _pidC)) {
                                        processes.selectRow(row, true);
                                        return;
                                    }
                                }
                                if (!autoSelect.isSelected()) selectedName = null;
                                selectedPid = -1;
                                updateAutoSelect();
                            }
                        });
                    }
                }
            });
            
            final JButton details = new JButton(Icons.getIcon(GeneralIcons.INFO)) {
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    showDetails((RunningVM)processes.getSelectedValue(-1));
                }
            };
            details.setToolTipText(Bundle.AttachDialog_ShowDetails());
            
            autoSelect = new JCheckBox() {
                private final RowFilter rowFilter = new RowFilter() {
                    public boolean include(RowFilter.Entry entry) {
                        return selectedName == null || selectedName.equals(entry.getValue(0));
                    }
                };
                protected void fireItemStateChanged(ItemEvent event) {
                    super.fireItemStateChanged(event);
                    if (isSelected()) processes.addRowFilter(rowFilter);
                    else processes.removeRowFilter(rowFilter);
                    updateAutoSelect();
                }
            };
            Mnemonics.setLocalizedText(autoSelect, Bundle.AttachDialog_ConnectToSelected());
            autoSelect.setEnabled(false);
            
            processes.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (processes.getSelectedRow() == -1) {
                        selectedPid = -1;
                    } else {
                        selectedPid = (Integer)processes.getSelectedValue(1);
                        selectedName = getProcessName(processes.getSelectedValue(0).toString());
                    }
                    updateDetails(details);
                    updateAutoSelect();
                    updateDisplayer();
                }
            });
            
            FilteringToolbar toolbar = new FilteringToolbar(Bundle.AttachDialog_FilterProcesses()) {
                private String filter;
                private final RowFilter rowFilter = new RowFilter() {
                    public boolean include(RowFilter.Entry entry) {
                        return filter == null || entry.getValue(0).toString().toLowerCase().contains(filter);
                    }
                };
                protected void filterChanged(String filter) {
                    this.filter = filter == null ? null : filter.toLowerCase();
                    processes.addRowFilter(rowFilter);
                }
            };
            toolbar.add(Box.createHorizontalStrut(2));
            toolbar.addSeparator();
            toolbar.add(Box.createHorizontalStrut(1));
            toolbar.add(refresh);
            toolbar.add(Box.createHorizontalStrut(2));
            toolbar.add(details);
            toolbar.add(Box.createHorizontalStrut(3));
            toolbar.add(autoSelect);
            
            final JPanel dynamicContent = new JPanel(new BorderLayout());
            dynamicContent.add(processesHint, BorderLayout.NORTH);
            dynamicContent.add(processesContainer, BorderLayout.CENTER);
            dynamicContent.add(toolbar, BorderLayout.SOUTH);
            
            
            
            JLabel stepsHint = new JLabel("", JLabel.LEADING); // NOI18N
            Mnemonics.setLocalizedText(stepsHint, Bundle.AttachDialog_Steps());
            
            steps = new HTMLTextArea() {
                protected void showURL(URL url) {
                    if (currentProvider != null) {
                        final String action = url.toString();
                        final AttachSettings settings = new AttachSettings();
                        getSettings().copyInto(settings);
                        RequestProcessor.getDefault().post(new Runnable() {
                            public void run() {
                                currentProvider.handleAction(action, settings);
                            }
                        });
                    }
                }
            };
            stepsHint.setLabelFor(steps);
            JScrollPane stepsScroll = new JScrollPane(steps);
            
            final JPanel directContent = new JPanel(new BorderLayout(3, 3));
            directContent.add(stepsHint, BorderLayout.NORTH);
            directContent.add(stepsScroll, BorderLayout.CENTER);
            
            final JPanel remoteSettings = new JPanel(new GridBagLayout());
            GridBagConstraints c;
            
            final JPanel content = new JPanel(new BorderLayout());
            content.add(dynamicContent, BorderLayout.CENTER);
            
            setLayout(new GridBagLayout());
            
            JLabel profile = new JLabel();
            Mnemonics.setLocalizedText(profile, Bundle.AttachDialog_Profile());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(10, 10, 0, 0);
            add(profile, c);
            
            class ModeAction extends AbstractAction {
                private final boolean remote;
                private final boolean hasSteps;
                ModeAction(String name, boolean remote, boolean hasSteps) {
                    super(name);
                    this.remote = remote;
                    this.hasSteps = hasSteps;
                }
                public void actionPerformed(ActionEvent e) {
                    content.removeAll();
                    remoteSettings.setVisible(remote);
                    if (hasSteps) updateSteps();
                    content.add(hasSteps ? directContent : dynamicContent, BorderLayout.CENTER);
                    content.invalidate();
                    content.revalidate();
                    content.repaint();
                    updateDisplayer();
                }
            }
            modeButton = new ActionPopupButton(new ModeAction(Bundle.AttachDialog_LocalRunningProcess(), false, false),
                                               null,
                                               new ModeAction(Bundle.AttachDialog_LocalStartedProcess(), false, true),
                                               new ModeAction(Bundle.AttachDialog_RemoteStartedProcess(), true, true));
            modeButton.setPopupAlign(SwingConstants.RIGHT);
            profile.setLabelFor(modeButton);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(10, 5, 0, 10);
            add(modeButton, c);
            
            
            
            JLabel hostnameLabel = new JLabel();
            Mnemonics.setLocalizedText(hostnameLabel, Bundle.AttachDialog_Hostname());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 0, 0);
            remoteSettings.add(hostnameLabel, c);
            
            hostname = new JTextField();
            hostnameLabel.setLabelFor(hostname);
            hostname.setText("https://netbeans.org/features/index.html"); // NOI18N
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 5, 0, 10);
            remoteSettings.add(hostname, c);
            
            JLabel osJvmLabel = new JLabel();
            Mnemonics.setLocalizedText(osJvmLabel, Bundle.AttachDialog_OsJvm());
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 0, 0);
            remoteSettings.add(osJvmLabel, c);
            
            class OsAction extends AbstractAction {
                OsAction(String name) { super(name); }
                public void actionPerformed(ActionEvent e) { updateSteps(); }
            }
            os = new ActionPopupButton(new OsAction(IntegrationUtils.PLATFORM_WINDOWS_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_WINDOWS_AMD64_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_WINDOWS_CVM),
                                       null,
                                       new OsAction(IntegrationUtils.PLATFORM_LINUX_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_LINUX_AMD64_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_LINUX_ARM_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_LINUX_ARM_VFP_HFLT_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_LINUX_CVM),
                                       null,
                                       new OsAction(IntegrationUtils.PLATFORM_MAC_OS),
                                       null,
                                       new OsAction(IntegrationUtils.PLATFORM_SOLARIS_SPARC_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_SOLARIS_SPARC64_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_SOLARIS_INTEL_OS),
                                       new OsAction(IntegrationUtils.PLATFORM_SOLARIS_AMD64_OS));
            osJvmLabel.setLabelFor(os);
            os.setPopupAlign(SwingConstants.RIGHT);
            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 5, 0, 0);
            remoteSettings.add(os, c);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(5, 10, 0, 10);
            add(remoteSettings, c);
            
            
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 10;
            c.weighty = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.insets = new Insets(20, 10, 10, 10);
            add(content, c);
            
            setPreferredSize(getPreferredSize());
            hostname.setText(""); // NOI18N
            
            processesModel.refresh();
            updateDetails(details);
            updateAutoSelect();
        }
        
        private void updateSteps() {
            if (updatingUI) return;
            steps.setText(steps(getSettings()));
            // Make sure the first step is visible
            steps.setCaretPosition(0);
            // Really make sure the first step is visible
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { steps.setCaretPosition(0); }
            });
        }

        @Override
        public HelpCtx getHelpCtx() {
            return HELP_CTX;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { updateSteps(); }
            });
        }
        
        private void updateDetails(JButton info) {
            info.setEnabled(selectedPid != -1);
        }
        
        private void updateAutoSelect() {
            if (!autoSelect.isSelected() && selectedPid == -1) selectedName = null;
            autoSelect.setEnabled(autoSelect.isSelected() || selectedPid != -1);
            Mnemonics.setLocalizedText(autoSelect, selectedName == null ? Bundle.AttachDialog_ConnectToSelected() :
                                                   Bundle.AttachDialog_ConnectToProcess(selectedName));
        }
        
        private void updateDisplayer() {
            if (displayer == null) return;
            
            if (modeButton.getSelectedIndex() != 0) {
                displayer.setValid(true);
                return;
            }
            
            displayer.setValid(selectedPid != -1);
        }
        
    }
    
    
    private static String getProcessName(String name) {
        name = name == null ? null : name.trim();
        return name == null || name.isEmpty() ? Bundle.AttachDialog_UnknownProcess() : name;
    }
    
    private static String escapedText(String text, String replace) {
        if (text != null) text = text.trim();
        if (text == null || text.isEmpty()) return "&lt;" + replace + "&gt;"; // NOI18N
        return text;
    }
    
    private static void configureScrollBar(final JScrollBar s) {
        s.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) { s.setEnabled(s.getVisibleAmount() < s.getMaximum()); }
        });
    }
    
    private static void showDetails(RunningVM vm) {
        HTMLTextArea area = new HTMLTextArea();
        JScrollPane areaScroll = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                       JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        areaScroll.setBorder(BorderFactory.createEmptyBorder());
        areaScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        areaScroll.setPreferredSize(new Dimension(500, 260));
        configureScrollBar(areaScroll.getVerticalScrollBar());
        configureScrollBar(areaScroll.getHorizontalScrollBar());
        
        area.setText(getDetails(vm));
        area.setCaretPosition(0);
        
        HelpCtx helpCtx = new HelpCtx("ProcessDetails.HelpCtx"); //NOI18N
        JButton close = new JButton(Bundle.AttachDialog_BtnClose());
        close.setDefaultCapable(true);
        DialogDescriptor dd = new DialogDescriptor(areaScroll, Bundle.AttachDialog_DetailsCaption(getProcessName(vm.getMainClass())),
                              true, new Object[] { close }, close, DialogDescriptor.DEFAULT_ALIGN, helpCtx, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.pack();
        d.setVisible(true);
    }
    
    private static String getDetails(RunningVM vm) {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("<table cellspacing=\"3\" cellpadding=\"0\" width=\"400\">"); //NOI18N
        
        // --- PID -------------------------------------------------------------
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td valign='top'><nobr><b>"); //NOI18N
        buffer.append(Bundle.AttachDialog_RowPid());
        buffer.append("</b>&nbsp;&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td width=\"100%\">"); //NOI18N
        buffer.append(vm.getPid());
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- Main Class ------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td valign='top'><nobr><b>"); //NOI18N
        buffer.append(Bundle.AttachDialog_RowMainClass());
        buffer.append("</b>&nbsp;&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td width=\"100%\">"); //NOI18N
        buffer.append(escapedText(vm.getMainClass(), Bundle.AttachDialog_DetailsUnknown()));
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- Arguments -------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td valign='top'><nobr><b>"); //NOI18N
        buffer.append(Bundle.AttachDialog_RowArguments());
        buffer.append("</b>&nbsp;&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td width=\"100%\">"); //NOI18N
        buffer.append(escapedText(vm.getMainArgs(), Bundle.AttachDialog_DetailsNone()));
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- VM Arguments ----------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td valign='top'><nobr><b>"); //NOI18N
        buffer.append(Bundle.AttachDialog_RowJvmArguments());
        buffer.append("</b>&nbsp;&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td width=\"100%\">"); //NOI18N
        buffer.append(escapedText(vm.getVMArgs(), Bundle.AttachDialog_DetailsNone()));
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- VM Flags --------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td valign='top'><nobr><b>"); //NOI18N
        buffer.append(Bundle.AttachDialog_RowJvmFlags());
        buffer.append("</b>&nbsp;&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td width=\"100%\">"); //NOI18N
        buffer.append(escapedText(vm.getVMFlags(), Bundle.AttachDialog_DetailsNone()));
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        buffer.append("</table>"); //NOI18N
        
        return buffer.toString();
    }
    
    
    private class ProcessesModel extends AbstractTableModel {
        
        private RunningVM[] vms;
        private boolean refreshing;
        private boolean firstRefresh = true;
        
        void refresh() {
            refreshing = true;
            fireTableDataChanged();
            new RequestProcessor("Processes refresher").post(new Runnable() { // NOI18N
                public void run() {
                    final RunningVM[] _vms = JpsProxy.getRunningVMs();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            vms = _vms;
                            refreshing = false;
                            fireTableDataChanged();
                            firstRefresh = false;
                        }
                    });
                }
            }, 500);
        }
        
        boolean isRefreshing() {
            return refreshing;
        }
        
        boolean isFirstRefresh() {
            return !refreshing && firstRefresh;
        }
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return Bundle.AttachDialog_ColumnName();
            } else if (columnIndex == 1) {
                return Bundle.AttachDialog_ColumnPid();
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return String.class;
            } else if (columnIndex == 1) {
                return Integer.class;
            }
            return null;
        }

        public int getRowCount() {
            return vms == null ? 0 : vms.length;
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return getProcessName(vms[rowIndex].getMainClass());
            } else if (columnIndex == 1) {
                return vms[rowIndex].getPid();
            } else if (columnIndex == -1) {
                return vms[rowIndex];
            }
            return null;
        }
        
    }
    
}
