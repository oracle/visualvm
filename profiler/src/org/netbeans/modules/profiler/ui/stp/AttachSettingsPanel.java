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

import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ui.HyperlinkLabel;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.NbBundle;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URL;
import java.text.MessageFormat;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 *
 * @author Jiri Sedlacek
 */
public class AttachSettingsPanel extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String PROJECT_PENDING_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                             "AttachSettingsPanel_ProjectPendingString"); // NOI18N
    private static final String DEFINE_SETTINGS_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                             "AttachSettingsPanel_DefineSettingsString"); // NOI18N
    private static final String DIRECT_ATTACH_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                           "AttachSettingsPanel_DirectAttachString"); // NOI18N
    private static final String DYNAMIC_ATTACH_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                            "AttachSettingsPanel_DynamicAttachString"); // NOI18N
    private static final String REMOTE_ATTACH_HINT_TEXT = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                              "AttachSettingsPanel_RemoteAttachHintText"); // NOI18N
    private static final String LOCAL_ATTACH_HINT_TEXT = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                             "AttachSettingsPanel_LocalAttachHintText"); // NOI18N
    private static final String ATTACH_MODE_LABEL_TEXT = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                             "AttachSettingsPanel_AttachModeLabelText"); // NOI18N
                                                                                                                         // -----

    // --- Constants declaration -------------------------------------------------
    private static final int PREFERRED_HINT_HEIGHT = new HyperlinkLabel("ABC<a href='#'>ABC</a>", "ABC<a href='#'>ABC</a>", null)
                                                     .getPreferredSize().height; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AttachSettings settings;
    private HyperlinkLabel attachModeHintLabel;

    // --- UI components declaration ---------------------------------------------
    private JLabel attachModeLabel;

    // --- Instance variables declaration ----------------------------------------
    private Project project;
    private boolean settingsValid; // set for null settings when <Select project to attach to> is selected

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public AttachSettingsPanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        attachModeLabel.setEnabled(enabled);
        attachModeHintLabel.setEnabled(enabled);
    }

    public void setSettings(Project project, boolean settingsValid) {
        this.project = project;
        this.settingsValid = settingsValid;
        settings = Utils.getAttachSettings(project);
        updateSettingsHint();
    }

    public AttachSettings getSettings() {
        return settingsValid ? settings : null;
    }

    public void resetSettings() {
        setSettings(null, false);
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // attachModeLabel
        attachModeLabel = new JLabel();
        org.openide.awt.Mnemonics.setLocalizedText(attachModeLabel, ATTACH_MODE_LABEL_TEXT);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(6, 15, 6, 4);
        add(attachModeLabel, constraints);

        // attachModeHintLabel
        attachModeHintLabel = new HyperlinkLabel("ABC<a href='#'>ABC</a>", "ABC<a href='#'>ABC</a>",
                                                 new Runnable() { // NOI18N
                public void run() {
                    final AttachSettings attachSettings = Utils.selectAttachSettings(project);

                    if (attachSettings != null) {
                        settings = attachSettings;
                        updateSettingsHint();
                        IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                                public void run() {
                                    NetBeansProfiler.saveAttachSettings(project, attachSettings);
                                }
                            });
                    }
                }
            }) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, PREFERRED_HINT_HEIGHT);
                } // Needs to be overridden to prevent layout problems
            };
        attachModeLabel.setLabelFor(attachModeHintLabel);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(6, 0, 6, 5);
        add(attachModeHintLabel, constraints);
    }

    // --- Private implementation ------------------------------------------------
    private void updateSettingsHint() {
        Color linkColor = Color.RED;
        String colorText = "rgb(" + linkColor.getRed() + "," + linkColor.getGreen() + "," + linkColor.getBlue() + ")"; //NOI18N
        String labelText = ""; //NOI18N
        String labelFocusedText = ""; //NOI18N

        if (!settingsValid) {
            attachModeHintLabel.setFocusable(false);
            labelText = "<nobr>" + PROJECT_PENDING_STRING + "</nobr>"; //NOI18N
            labelFocusedText = labelText;
        } else if (settings == null) {
            attachModeHintLabel.setFocusable(true);
            labelText = "<nobr>" + MessageFormat.format(DEFINE_SETTINGS_STRING, new Object[] { "" }) + "</nobr>"; //NOI18N
            labelFocusedText = "<nobr>"
                               + MessageFormat.format(DEFINE_SETTINGS_STRING, new Object[] { "color=\"" + colorText + "\"" })
                               + "</nobr>"; //NOI18N
        } else {
            attachModeHintLabel.setFocusable(true);

            String attachMethodString = settings.isDirect() ? DIRECT_ATTACH_STRING : DYNAMIC_ATTACH_STRING;
            String targetType = settings.getTargetType();
            String serverType = settings.getServerType();
            String targetString = "".equals(serverType) ? targetType : serverType; //NOI18N
            String remoteString = settings.getHost();

            if (settings.isRemote()) {
                labelText = "<nobr>"
                            + MessageFormat.format(REMOTE_ATTACH_HINT_TEXT,
                                                   new Object[] { attachMethodString, targetString, remoteString, "" })
                            + "</nobr>"; //NOI18N
                labelFocusedText = "<nobr>"
                                   + MessageFormat.format(REMOTE_ATTACH_HINT_TEXT,
                                                          new Object[] {
                                                              attachMethodString, targetString, remoteString,
                                                              "color=\"" + colorText + "\""
                                                          }) + "</nobr>"; //NOI18N
                attachModeHintLabel.setText("<nobr>"
                                            + MessageFormat.format(REMOTE_ATTACH_HINT_TEXT,
                                                                   new Object[] { attachMethodString, targetString, remoteString })
                                            + "</nobr>"); //NOI18N
            } else {
                labelText = "<nobr>"
                            + MessageFormat.format(LOCAL_ATTACH_HINT_TEXT, new Object[] { attachMethodString, targetString, "" })
                            + "</nobr>"; //NOI18N
                labelFocusedText = "<nobr>"
                                   + MessageFormat.format(LOCAL_ATTACH_HINT_TEXT,
                                                          new Object[] {
                                                              attachMethodString, targetString, "color=\"" + colorText + "\""
                                                          }) + "</nobr>"; //NOI18N
            }
        }

        attachModeHintLabel.setText(labelText, labelFocusedText); //NOI18N
    }
}
