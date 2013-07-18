/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.Collection;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
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
    "AttachDialog_Target=Target", // NOI18N
    "AttachDialog_Local=&Local", // NOI18N
    "AttachDialog_LocalHint=Profile a Java process running on this computer.", // NOI18N
    "AttachDialog_Remote=&Remote", // NOI18N
    "AttachDialog_RemoteHint=Connect to a Java process running on a remote system.", // NOI18N
    "AttachDialog_Dynamic=&Dynamic", // NOI18N
    "AttachDialog_DynamicHint=Connect to an already running Java 6+ process.", // NOI18N
    "AttachDialog_Direct=D&irect", // NOI18N
    "AttachDialog_DirectHint=Start a new Java 5+ process, configured for profiling.", // NOI18N
    "AttachDialog_Hostname=Host&name:", // NOI18N
    "AttachDialog_OsJvm=&OS & JVM:", // NOI18N
    "AttachDialog_Connection=Connection", // NOI18N
    "AttachDialog_Steps=&Before profiling you must perform the following steps:" // NOI18N
})
@ServiceProvider(service = AttachWizard.class)
public class AttachDialog extends AttachWizard {
    
    private AttachStepsProvider currentProvider;
    private Panel panel;
    

    @Override
    public AttachSettings configure(AttachSettings settings) {
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
        
        private JRadioButton local;
        private JRadioButton remote;
        private JRadioButton dynamic;
        private JRadioButton direct;
        private JTextField hostname;
        private JComboBox os;
        private HTMLTextArea steps;
        
        private AttachSettings as;
        
        private boolean updatingUI = false;
        
        
        Panel() {
            initComponents();
        }
        
        void setup(AttachSettings as) {
            this.as = as;
            updatingUI = true;
            local.setSelected(!as.isRemote());
            remote.setSelected(as.isRemote());
            dynamic.setSelected(!as.isDirect());
            direct.setSelected(as.isDirect());
            if (as.isRemote()) {
                hostname.setText(as.getHost());
                os.setSelectedItem(as.getHostOS());
            } else {
                hostname.setText(""); // NOI18N
                os.setSelectedIndex(0);
            }
            updatingUI = false;
            updateSteps();
        }
        
        AttachSettings getSettings() {
            as.setRemote(remote.isSelected());
            if (as.isRemote()) {
                as.setDirect(true);
                as.setDynamic16(false);
                as.setHost(hostname.getText().trim());
                as.setHostOS(os.getSelectedItem().toString());
            } else {
                as.setDirect(direct.isSelected());
                as.setDynamic16(dynamic.isSelected());
                as.setHostOS(IntegrationUtils.getLocalPlatform(-1));
            }
            return as;
        }
        
        private void initComponents() {
            setLayout(new GridBagLayout());
            
            final JPanel connection = new JPanel(new CardLayout());
            JPanel localConn = new JPanel(new GridBagLayout());
            JPanel remoteConn = new JPanel(new GridBagLayout());
            
            GridBagConstraints c;
            ButtonGroup bg1 = new ButtonGroup();
            ButtonGroup bg2 = new ButtonGroup();
            
            JPanel target = new JPanel(new GridBagLayout());
            target.setBorder(new TitledBorder(Bundle.AttachDialog_Target()));
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 0, 0);
            local = new JRadioButton() {
                protected void fireItemStateChanged(ItemEvent event) {
                    super.fireItemStateChanged(event);
                    if (event.getStateChange() != ItemEvent.SELECTED) return;
                    CardLayout layout = (CardLayout)connection.getLayout();
                    layout.show(connection, "LOCAL"); // NOI18N
                    updateSteps();
                }
            };
            Mnemonics.setLocalizedText(local, Bundle.AttachDialog_Local());
            
            bg1.add(local);
            target.add(local, c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 30, 0, 10);
            JLabel localHint = new JLabel(Bundle.AttachDialog_LocalHint());
            localHint.setEnabled(false);
            target.add(localHint, c);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 10, 0);
            remote = new JRadioButton() {
                protected void fireItemStateChanged(ItemEvent event) {
                    super.fireItemStateChanged(event);
                    if (event.getStateChange() != ItemEvent.SELECTED) return;
                    CardLayout layout = (CardLayout)connection.getLayout();
                    layout.show(connection, "REMOTE"); // NOI18N
                    updateSteps();
                }
            };
            Mnemonics.setLocalizedText(remote, Bundle.AttachDialog_Remote());
            bg1.add(remote);
            target.add(remote, c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 30, 10, 10);
            JLabel remoteHint = new JLabel(Bundle.AttachDialog_RemoteHint());
            remoteHint.setEnabled(false);
            target.add(remoteHint, c);
            

            c = new GridBagConstraints();
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 10, 0, 10);
            add(target, c);
            
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 0, 0);
            dynamic = new JRadioButton() {
                protected void fireItemStateChanged(ItemEvent event) {
                    super.fireItemStateChanged(event);
                    if (event.getStateChange() != ItemEvent.SELECTED) return;
                    updateSteps();
                }
            };
            Mnemonics.setLocalizedText(dynamic, Bundle.AttachDialog_Dynamic());
            bg2.add(dynamic);
            localConn.add(dynamic, c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 30, 0, 10);
            JLabel dynamicHint = new JLabel(Bundle.AttachDialog_DynamicHint());
            dynamicHint.setEnabled(false);
            localConn.add(dynamicHint, c);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 10, 0);
            direct = new JRadioButton() {
                protected void fireItemStateChanged(ItemEvent event) {
                    super.fireItemStateChanged(event);
                    if (event.getStateChange() != ItemEvent.SELECTED) return;
                    updateSteps();
                }
            };
            Mnemonics.setLocalizedText(direct, Bundle.AttachDialog_Direct());
            bg2.add(direct);
            localConn.add(direct, c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 30, 10, 10);
            JLabel directHint = new JLabel(Bundle.AttachDialog_DirectHint());
            directHint.setEnabled(false);
            localConn.add(directHint, c);
            
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 0, 0);
            JLabel hostnameLabel = new JLabel();
            Mnemonics.setLocalizedText(hostnameLabel, Bundle.AttachDialog_Hostname());
            remoteConn.add(hostnameLabel, c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 0, 10);
            hostname = new JTextField();
            hostnameLabel.setLabelFor(hostname);
            remoteConn.add(hostname, c);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 10, 0);
            JLabel osLabel = new JLabel();
            Mnemonics.setLocalizedText(osLabel, Bundle.AttachDialog_OsJvm());
            remoteConn.add(osLabel, c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(6, 10, 10, 10);
            os = new JComboBox(new Object[] {
                IntegrationUtils.PLATFORM_WINDOWS_OS,
                IntegrationUtils.PLATFORM_WINDOWS_AMD64_OS,
                IntegrationUtils.PLATFORM_WINDOWS_CVM,
                IntegrationUtils.PLATFORM_LINUX_OS,
                IntegrationUtils.PLATFORM_LINUX_AMD64_OS,
                IntegrationUtils.PLATFORM_LINUX_ARM_OS,
                IntegrationUtils.PLATFORM_LINUX_CVM,
                IntegrationUtils.PLATFORM_SOLARIS_SPARC_OS,
                IntegrationUtils.PLATFORM_SOLARIS_SPARC64_OS,
                IntegrationUtils.PLATFORM_SOLARIS_INTEL_OS,
                IntegrationUtils.PLATFORM_SOLARIS_AMD64_OS,
                IntegrationUtils.PLATFORM_MAC_OS,
            }) {
                protected void fireItemStateChanged(ItemEvent event) {
                    super.fireItemStateChanged(event);
                    if (event.getStateChange() != ItemEvent.SELECTED) return;
                    updateSteps();
                }
            };
            osLabel.setLabelFor(os);
            remoteConn.add(os, c);
            
            
            connection.setBorder(new TitledBorder(Bundle.AttachDialog_Connection()));
            connection.add(localConn, "LOCAL"); // NOI18N
            connection.add(remoteConn, "REMOTE"); // NOI18N
            
            
            c = new GridBagConstraints();
            c.gridy = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 10, 0, 10);
            add(connection, c);
            
            c = new GridBagConstraints();
            c.gridy = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 12, 0, 12);
            JLabel hint = new JLabel();
            Mnemonics.setLocalizedText(hint, Bundle.AttachDialog_Steps());
            hint.setIcon(Icons.getIcon(GeneralIcons.INFO));
            add(hint, c);
            
            c = new GridBagConstraints();
            c.gridy = 3;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(2, 12, 0, 12);
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
            hint.setLabelFor(steps);
            steps.setEditable(false);
            
            JScrollPane stepsScroll = new JScrollPane(steps);
            stepsScroll.setPreferredSize(new Dimension(505, 130));
            add(stepsScroll, c);
        }
        
        private void updateSteps() {
            if (updatingUI) return;
            steps.setText(steps(getSettings()));
            steps.setCaretPosition(0);
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
        
    }
    
}
