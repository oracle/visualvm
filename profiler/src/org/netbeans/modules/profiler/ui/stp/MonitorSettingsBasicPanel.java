/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.ui.stp;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class MonitorSettingsBasicPanel extends DefaultSettingsPanel implements HelpCtx.Provider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ENABLE_THREADS_CHECKBOX_TEXT = NbBundle.getMessage(MonitorSettingsBasicPanel.class,
                                                                                   "MonitorSettingsBasicPanel_EnableThreadsCheckboxText"); // NOI18N
    private static final String STP_MONITOR_TOOLTIP = NbBundle.getMessage(MonitorSettingsBasicPanel.class, "StpMonitorTooltip"); // NOI18N
                                                                                                                                 // -----
    private static final String HELP_CTX_KEY = "MonitorSettings.Basic.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // --- UI components declaration ---------------------------------------------
    private JCheckBox threadsMonitoringCheckbox;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public MonitorSettingsBasicPanel() {
        super();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public void setThreadsMonitoring(boolean enabled) {
        threadsMonitoringCheckbox.setSelected(enabled);
    }

    public boolean getThreadsMonitoring() {
        return threadsMonitoringCheckbox.isSelected();
    }

    // --- Static tester frame ---------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("plaf.metal.MetalLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); //NOI18N
                                                                                            //      UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); //NOI18N
        } catch (Exception e) {
        }

        ;

        JFrame frame = new JFrame("Tester Frame"); //NOI18N
        JPanel contents = new MonitorSettingsBasicPanel();
        contents.setPreferredSize(new Dimension(375, 255));
        frame.getContentPane().add(contents);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    // --- Private implementation ------------------------------------------------

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // threadsMonitoringCheckbox
        threadsMonitoringCheckbox = new JCheckBox();
        org.openide.awt.Mnemonics.setLocalizedText(threadsMonitoringCheckbox, ENABLE_THREADS_CHECKBOX_TEXT);
        threadsMonitoringCheckbox.setToolTipText(STP_MONITOR_TOOLTIP);
        threadsMonitoringCheckbox.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(15 + threadsMonitoringCheckbox.getPreferredSize().height + 3, 30, 0, 0);
        add(threadsMonitoringCheckbox, constraints);
        threadsMonitoringCheckbox.addActionListener(getSettingsChangeListener());

        // fillerPanel
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(Utils.createFillerPanel(), constraints);
    }
}
