/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.ui.panels;

import org.netbeans.lib.profiler.jps.JpsProxy;
import org.netbeans.lib.profiler.jps.RunningVM;
import org.openide.DialogDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import javax.swing.*;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;

/**
 * A panel that allows to select a process PID from a combo box of all running processes
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "PIDSelectPanel_RefreshButtonName=&Refresh",
    "PIDSelectPanel_PidLabelText=PID:",
    "PIDSelectPanel_MainClassLabelText=Main Class:",
    "PIDSelectPanel_ArgumentsLabelText=Arguments:",
    "PIDSelectPanel_VmArgumentsLabelText=VM Arguments:",
    "PIDSelectPanel_VmFlagsLabelText=VM Flags:",
    "PIDSelectPanel_VmComboItemText={0} (pid: {1})",
    "PIDSelectPanel_ProcessesListItemText=Getting list of running processes...",
    "PIDSelectPanel_ErrorGettingProcessesItemText=<Error Getting Running Processes>",
    "PIDSelectPanel_NoProcessesItemText=<No Processes Running>",
    "PIDSelectPanel_SelectProcessItemText=<Select Process>",
    "PIDSelectPanel_OkButtonName=OK",
    "PIDSelectPanel_SelectProcessDialogCaption=Select Process To Attach",
    "PIDSelectPanel_ComboAccessName=List of processes available for Profiler connection.",
    "PIDSelectPanel_ComboAccessDescr=Select process which you want to attach the Profiler to.",
    "PIDSelectPanel_SelectedProcessAccessName=Selected process details",
    "PIDSelectPanel_ButtonAccessDescr=Refreshes list of processes available for Profiler connection."
})
public final class PIDSelectPanel extends JPanel implements ActionListener, HelpCtx.Provider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final HelpCtx HELP_CTX = new HelpCtx("PIDSelectPanel.HelpCtx");
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class PIDComboRenderer extends DefaultListCellRenderer {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            if (value instanceof RunningVM) {
                RunningVM vm = (RunningVM) value;
                String args = vm.getMainArgs();

                if (args == null) {
                    args = ""; //NOI18N
                } else {
                    args = " " + args; //NOI18N
                }

                String text = Bundle.PIDSelectPanel_VmComboItemText(vm.getMainClass(), "" + vm.getPid()); // NOI18N

                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final int MAX_WIDTH = 500;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JButton button;
    private JButton okButton;
    private JComboBox combo;
    private HTMLTextArea detailsArea;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public PIDSelectPanel(JButton okButton) {
        this.okButton = okButton;

        combo = new JComboBox();
        button = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(button, Bundle.PIDSelectPanel_RefreshButtonName());

        JPanel infoPanel = new JPanel(new BorderLayout());
        detailsArea = new HTMLTextArea();
        detailsArea.getAccessibleContext().setAccessibleName(Bundle.PIDSelectPanel_SelectedProcessAccessName());
        detailsArea.setEditable(false);
        detailsArea.setOpaque(true);
        detailsArea.setBackground(UIManager.getDefaults().getColor("Panel.background")); //NOI18N
        detailsArea.setPreferredSize(new Dimension(1, 1));
        JScrollPane detailsAreaScroll = new JScrollPane(detailsArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        detailsAreaScroll.setBorder(BorderFactory.createEmptyBorder());
        detailsAreaScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        detailsAreaScroll.setPreferredSize(new Dimension(250, 200));
        infoPanel.add(detailsAreaScroll, BorderLayout.CENTER);

        combo.setRenderer(new PIDComboRenderer());
        combo.getAccessibleContext().setAccessibleName(Bundle.PIDSelectPanel_ComboAccessName());
        combo.getAccessibleContext().setAccessibleDescription(Bundle.PIDSelectPanel_ComboAccessDescr());

        button.getAccessibleContext().setAccessibleDescription(Bundle.PIDSelectPanel_ButtonAccessDescr());

        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new BorderLayout(0, 10));

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BorderLayout(5, 0));

        northPanel.add(combo, BorderLayout.CENTER);
        northPanel.add(button, BorderLayout.EAST);

        add(northPanel, BorderLayout.NORTH);
        add(infoPanel, BorderLayout.CENTER);

        okButton.setEnabled(false);

        refreshCombo();

        button.addActionListener(this);
        combo.addActionListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public int getPID() {
        Object sel = combo.getSelectedItem();

        if ((sel != null) && sel instanceof RunningVM) {
            return ((RunningVM) sel).getPid();
        }

        return -1;
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();

        return new Dimension(Math.max(d.width, MAX_WIDTH), d.height);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == button) {
            refreshCombo();
        } else if (e.getSource() == combo) {
            okButton.setEnabled(combo.getSelectedItem() instanceof RunningVM);
            updateInfo();
        }
    }

    public static int selectPID() {
        JButton okButton = new JButton(Bundle.PIDSelectPanel_OkButtonName());
        PIDSelectPanel pidSelect = new PIDSelectPanel(okButton);

        DialogDescriptor dd = new DialogDescriptor(pidSelect, Bundle.PIDSelectPanel_SelectProcessDialogCaption(), true,
                                                   new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                   DialogDescriptor.BOTTOM_ALIGN, null, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            return pidSelect.getPID();
        } else {
            return -1;
        }
    }

    private void refreshCombo() {
        okButton.setEnabled(false);
        combo.setEnabled(false);
        combo.setModel(new DefaultComboBoxModel(new Object[] { Bundle.PIDSelectPanel_ProcessesListItemText() }));
        
        new SwingWorker<Object[],Object>() {
            protected Object[] doInBackground() throws Exception {
                RunningVM[] vms = JpsProxy.getRunningVMs();
                Object[] ar = new Object[((vms == null) ? 0 : vms.length) + 1];
                if (vms == null) {
                    ar[0] = Bundle.PIDSelectPanel_ErrorGettingProcessesItemText();
                } else if (vms.length == 0) {
                    ar[0] = Bundle.PIDSelectPanel_NoProcessesItemText();
                } else {
                    ar[0] = Bundle.PIDSelectPanel_SelectProcessItemText();
                    System.arraycopy(vms, 0, ar, 1, vms.length);
                }
                return ar;
            }
            
            protected void done() {
                try {
                    combo.setEnabled(true);
                    combo.setModel(new DefaultComboBoxModel(get()));
                    updateInfo();
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }.execute();       
    }

    private void updateInfo() {
        String pid = ""; //NOI18N
        String mainClass = ""; //NOI18N
        String arguments = ""; //NOI18N
        String vmArguments = ""; //NOI18N
        String vmFlags = ""; //NOI18N
        
        Object sel = combo.getSelectedItem();
        
        if ((sel != null) && sel instanceof RunningVM) {
            RunningVM vm = (RunningVM) sel;
            pid = "" + vm.getPid(); //NOI18N
            mainClass = vm.getMainClass();
            arguments = vm.getMainArgs();
            vmArguments = vm.getVMArgs();
            vmFlags = vm.getVMFlags();
        }
        
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("<table cellspacing=\"3\" cellpadding=\"0\">"); //NOI18N
        
        // --- PID -------------------------------------------------------------
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(Bundle.PIDSelectPanel_PidLabelText());
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(pid);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- Main Class ------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(Bundle.PIDSelectPanel_MainClassLabelText());
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(mainClass);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- Arguments -------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(Bundle.PIDSelectPanel_ArgumentsLabelText());
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(arguments);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- VM Arguments ----------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(Bundle.PIDSelectPanel_VmArgumentsLabelText());
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(vmArguments);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- VM Flags --------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(Bundle.PIDSelectPanel_VmFlagsLabelText());
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(vmFlags);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        buffer.append("</table>"); //NOI18N
        
        detailsArea.setText(buffer.toString());
        detailsArea.setCaretPosition(0);
    }
}
