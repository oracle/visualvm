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

package org.netbeans.modules.profiler.ui.panels;

import org.netbeans.lib.profiler.jps.JpsProxy;
import org.netbeans.lib.profiler.jps.RunningVM;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import javax.swing.*;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;


/**
 * A panel that allows to select a process PID from a combo box of all running processes
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class PIDSelectPanel extends JPanel implements ActionListener {
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

                String text = MessageFormat.format(VM_COMBO_ITEM_TEXT, new Object[] { vm.getMainClass(), "" + vm.getPid() }); // NOI18N

                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String REFRESH_BUTTON_NAME = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_RefreshButtonName"); //NOI18N
    private static final String PID_LABEL_TEXT = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_PidLabelText"); //NOI18N
    private static final String MAIN_CLASS_LABEL_TEXT = NbBundle.getMessage(PIDSelectPanel.class,
                                                                            "PIDSelectPanel_MainClassLabelText"); //NOI18N
    private static final String ARGUMENTS_LABEL_TEXT = NbBundle.getMessage(PIDSelectPanel.class,
                                                                           "PIDSelectPanel_ArgumentsLabelText"); //NOI18N
    private static final String VM_ARGUMENTS_LABEL_TEXT = NbBundle.getMessage(PIDSelectPanel.class,
                                                                              "PIDSelectPanel_VmArgumentsLabelText"); //NOI18N
    private static final String VM_FLAGS_LABEL_TEXT = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_VmFlagsLabelText"); //NOI18N
    private static final String VM_COMBO_ITEM_TEXT = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_VmComboItemText"); //NOI18N
    private static final String PROCESSES_LIST_ITEM_TEXT = NbBundle.getMessage(PIDSelectPanel.class,
                                                                               "PIDSelectPanel_ProcessesListItemText"); //NOI18N
    private static final String ERROR_GETTING_PROCESSES_ITEM_TEXT = NbBundle.getMessage(PIDSelectPanel.class,
                                                                                        "PIDSelectPanel_ErrorGettingProcessesItemText"); //NOI18N
    private static final String NO_PROCESSES_ITEM_TEXT = NbBundle.getMessage(PIDSelectPanel.class,
                                                                             "PIDSelectPanel_NoProcessesItemText"); //NOI18N
    private static final String SELECT_PROCESS_ITEM_TEXT = NbBundle.getMessage(PIDSelectPanel.class,
                                                                               "PIDSelectPanel_SelectProcessItemText"); //NOI18N
    private static final String OK_BUTTON_NAME = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_OkButtonName"); //NOI18N
    private static final String SELECT_PROCESS_DIALOG_CAPTION = NbBundle.getMessage(PIDSelectPanel.class,
                                                                                    "PIDSelectPanel_SelectProcessDialogCaption"); //NOI18N
    private static final String COMBO_ACCESS_NAME = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_ComboAccessName"); //NOI18N
    private static final String COMBO_ACCESS_DESCR = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_ComboAccessDescr"); //NOI18N
    private static final String BUTTON_ACCESS_DESCR = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_ButtonAccessDescr"); //NOI18N
    private static final String PROCESS_DETAILS_ACCESS_NAME = NbBundle.getMessage(PIDSelectPanel.class, "PIDSelectPanel_SelectedProcessAccessName"); //NOI18N
                                                                                                                                     // -----
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
        org.openide.awt.Mnemonics.setLocalizedText(button, REFRESH_BUTTON_NAME);

        JPanel infoPanel = new JPanel(new BorderLayout());
        detailsArea = new HTMLTextArea();
        detailsArea.getAccessibleContext().setAccessibleName(PROCESS_DETAILS_ACCESS_NAME);
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
        combo.getAccessibleContext().setAccessibleName(COMBO_ACCESS_NAME);
        combo.getAccessibleContext().setAccessibleDescription(COMBO_ACCESS_DESCR);

        button.getAccessibleContext().setAccessibleDescription(BUTTON_ACCESS_DESCR);

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
        JButton okButton = new JButton(OK_BUTTON_NAME);
        PIDSelectPanel pidSelect = new PIDSelectPanel(okButton);

        DialogDescriptor dd = new DialogDescriptor(pidSelect, SELECT_PROCESS_DIALOG_CAPTION, true,
                                                   new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                   DialogDescriptor.BOTTOM_ALIGN, null, null);
        Dialog d = ProfilerDialogs.createDialog(dd);
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
        combo.setModel(new DefaultComboBoxModel(new Object[] { PROCESSES_LIST_ITEM_TEXT }));
        new NBSwingWorker() {
                private RunningVM[] vms = JpsProxy.getRunningVMs();
                private Object[] ar = new Object[((vms == null) ? 0 : vms.length) + 1];

                protected void doInBackground() {
                    if (vms == null) {
                        ar[0] = ERROR_GETTING_PROCESSES_ITEM_TEXT;
                    } else if (vms.length == 0) {
                        ar[0] = NO_PROCESSES_ITEM_TEXT;
                    } else {
                        ar[0] = SELECT_PROCESS_ITEM_TEXT;
                        System.arraycopy(vms, 0, ar, 1, vms.length);
                    }
                }

                protected void done() {
                    combo.setEnabled(true);
                    combo.setModel(new DefaultComboBoxModel(ar));
                    updateInfo();
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
        
        StringBuffer buffer = new StringBuffer();
        
        buffer.append("<table cellspacing=\"3\" cellpadding=\"0\">"); //NOI18N
        
        // --- PID -------------------------------------------------------------
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(PID_LABEL_TEXT);
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(pid);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- Main Class ------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(MAIN_CLASS_LABEL_TEXT);
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(mainClass);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- Arguments -------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(ARGUMENTS_LABEL_TEXT);
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(arguments);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- VM Arguments ----------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(VM_ARGUMENTS_LABEL_TEXT);
        buffer.append("</b>&nbsp;&nbsp;</nobr></td>"); //NOI18N
        
        buffer.append("<td>"); //NOI18N
        buffer.append(vmArguments);
        buffer.append("</td>"); //NOI18N
        
        buffer.append("</tr>"); //NOI18N
        
        // --- VM Flags --------------------------------------------------------
        
        buffer.append("<tr>"); //NOI18N
        
        buffer.append("<td><nobr><b>"); //NOI18N
        buffer.append(VM_FLAGS_LABEL_TEXT);
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
