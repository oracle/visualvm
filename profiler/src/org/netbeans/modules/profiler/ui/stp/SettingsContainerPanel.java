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

import org.netbeans.lib.profiler.ui.components.DiscreteProgress;
import org.netbeans.modules.profiler.ui.HyperlinkTextArea;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 *
 * @author Jiri Sedlacek
 */
public class SettingsContainerPanel extends JPanel implements ChangeListener, HelpCtx.Provider {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    // --- Required interfaces definition ----------------------------------------
    public static interface Contents {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public JPanel getAdvancedSettingsPanel();

        public JPanel getBasicSettingsPanel();

        // estimated profiling overhead based on current profiling settings, <0, 1>
        public float getProfilingOverhead();

        // Used for updating Overhead visualization
        public void addChangeListener(ChangeListener listener);

        public void removeChangeListener(ChangeListener listener);

        public void synchronizeBasicAdvancedPanels();
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ADVANCED_CAPTION_TEXT = NbBundle.getMessage(SettingsContainerPanel.class,
                                                                            "SettingsContainerPanel_AdvancedCaptionText"); // NOI18N
    private static final String BASIC_SETTINGS_STRING = NbBundle.getMessage(SettingsContainerPanel.class,
                                                                            "SettingsContainerPanel_BasicSettingsString"); // NOI18N
    private static final String ADVANCED_SETTINGS_STRING = NbBundle.getMessage(SettingsContainerPanel.class,
                                                                               "SettingsContainerPanel_AdvancedSettingsString"); // NOI18N
    private static final String READONLY_SETTINGS_MSG = NbBundle.getMessage(SettingsContainerPanel.class,
                                                                            "SettingsContainerPanel_ReadOnlySettingsMsg"); // NOI18N
    private static final String OVERHEAD_LABEL_TEXT = NbBundle.getMessage(SettingsContainerPanel.class,
                                                                          "SettingsContainerPanel_OverheadLabelText"); // NOI18N
                                                                                                                       // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CardLayout basicAdvancedSettingsSwitchContainerLayout;
    private Contents contents;
    private DiscreteProgress progress;
    private HyperlinkTextArea basicAdvancedSettingsSwitchArea;

    // --- UI components declaration ---------------------------------------------
    private JLabel captionLabel;
    private JPanel basicAdvancedSettingsSwitchContainer;
    private JScrollPane contentsScroll;

    // --- Instance variables declaration ----------------------------------------
    private String caption;
    private boolean showingAdvancedSettings = true; // required to correctly display initial state
    private boolean showingPreset;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public SettingsContainerPanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setCaption(String caption) {
        this.caption = caption;
        captionLabel.setText(caption);
    }

    public void setContents(Contents contents) {
        setContents(contents, false);
    }

    public void setContents(Contents contents, boolean showAdvancedSettings) {
        if (this.contents != null) {
            contents.removeChangeListener(this);
        }

        this.contents = contents;

        if (this.contents != null) {
            contents.addChangeListener(this);
        }

        if (showAdvancedSettings) {
            switchToAdvancedSettings();
        } else {
            switchToBasicSettings();
        }

        //    stateChanged(null); // update overhead for new contents
    }

    public HelpCtx getHelpCtx() {
        if (contents == null) {
            return HelpCtx.DEFAULT_HELP;
        }

        JPanel currentSettings = showingAdvancedSettings ? contents.getAdvancedSettingsPanel() : contents.getBasicSettingsPanel();

        if ((currentSettings == null) || !(currentSettings instanceof HelpCtx.Provider)) {
            return HelpCtx.DEFAULT_HELP;
        }

        return ((HelpCtx.Provider) currentSettings).getHelpCtx();
    }

    public void setShowingPreset(boolean showingPreset) {
        this.showingPreset = showingPreset;
    }

    // Overhead value changed
    public void stateChanged(ChangeEvent e) {
        progress.setActiveUnits(Math.round(contents.getProfilingOverhead() * 10));
    }

    public void switchToAdvancedSettings() {
        showingAdvancedSettings = false;
        toggleBasicAdvancedSettingsView();
    }

    public void switchToBasicSettings() {
        showingAdvancedSettings = true;
        toggleBasicAdvancedSettingsView();
    }

    // --- Private implementation ------------------------------------------------
    void setPreferredContentsSize(Dimension preferredSize) {
        contentsScroll.setPreferredSize(preferredSize);
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(SelectProfilingTask.BACKGROUND_COLOR);

        GridBagConstraints constraints;

        captionLabel = new JLabel(" "); // NOI18N // Non-empty value required for correct preferred size detection
        captionLabel.setFont(captionLabel.getFont().deriveFont(Font.BOLD, captionLabel.getFont().getSize2D() + 3));
        captionLabel.setForeground(new Color(198, 129, 0));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(27, 30, 12, 20);
        add(captionLabel, constraints);

        JSeparator separator1 = Utils.createHorizontalSeparator();
        separator1.setBackground(getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 20, 0, 20);
        add(separator1, constraints);

        contentsScroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentsScroll.setBorder(BorderFactory.createEmptyBorder());
        contentsScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 20, 10, 20);
        add(contentsScroll, constraints);

        JSeparator separator2 = Utils.createHorizontalSeparator();
        separator2.setBackground(getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 20, 0, 20);
        add(separator2, constraints);

        JLabel overheadLabel = new JLabel(OVERHEAD_LABEL_TEXT);
        overheadLabel.setFont(overheadLabel.getFont().deriveFont(overheadLabel.getFont().getSize2D() - 1));
        overheadLabel.setForeground(SelectProfilingTask.DARKLINK_COLOR_INACTIVE);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 22, 4, 0);
        add(overheadLabel, constraints);

        progress = new DiscreteProgress();
        progress.setActiveUnits(4);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 5, 4, 0);
        add(progress, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 4;
        constraints.weightx = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 0, 4, 0);
        add(Utils.createFillerPanel(), constraints);

        // basicAdvancedSettingsSwitchArea
        basicAdvancedSettingsSwitchArea = new HyperlinkTextArea(ADVANCED_SETTINGS_STRING) {
                protected Color getHighlightColor() {
                    return SelectProfilingTask.DARKLINK_COLOR;
                }

                protected String getHighlightText(String originalText) {
                    return "<nobr><u>" + originalText + "</u></nobr>"; // NOI18N
                }

                protected Color getNormalColor() {
                    return SelectProfilingTask.DARKLINK_COLOR_INACTIVE;
                }

                protected String getNormalText(String originalText) {
                    return "<nobr><u>" + originalText + "</u></nobr>"; // NOI18N
                }
            };

        Font font = UIManager.getFont("Label.font"); // NOI18N
        basicAdvancedSettingsSwitchArea.setFont(font.deriveFont(font.getSize2D() - 1));

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 0, 4, 22);
        add(basicAdvancedSettingsSwitchArea, constraints);

        basicAdvancedSettingsSwitchArea.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        toggleBasicAdvancedSettingsView();
                    }
                }
            });

        basicAdvancedSettingsSwitchArea.addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        toggleBasicAdvancedSettingsView();
                    }
                }
            });
    }

    private void toggleBasicAdvancedSettingsView() {
        contents.synchronizeBasicAdvancedPanels(); // TODO: cleanup, calling only when switching basic -> advanced is enough

        showingAdvancedSettings = !showingAdvancedSettings;
        JPanel contentsPanel;

        if (showingAdvancedSettings) {
            captionLabel.setText(MessageFormat.format(ADVANCED_CAPTION_TEXT, new Object[] { caption }));
            basicAdvancedSettingsSwitchArea.setText(BASIC_SETTINGS_STRING);
            contentsPanel = contents.getAdvancedSettingsPanel();
        } else {
            captionLabel.setText(caption);
            basicAdvancedSettingsSwitchArea.setText(ADVANCED_SETTINGS_STRING);
            contentsPanel = contents.getBasicSettingsPanel();
        }
        
        basicAdvancedSettingsSwitchArea.updateAppearance();
        contentsPanel.setBackground(getBackground());
        contentsScroll.setViewportView(contentsPanel);

        // TODO: update cursor according to current mouse position
        SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JScrollBar contentsScrollBar = contentsScroll.getVerticalScrollBar();

                    if ((contentsScrollBar == null) || !contentsScrollBar.isVisible()) {
                        return;
                    }

                    contentsScrollBar.setValue(0);
                }
            });

        SelectProfilingTask.getDefault().updateHelpCtx();

        if (showingPreset && showingAdvancedSettings) {
            ProfilerDialogs.DNSAMessage dnsa = new ProfilerDialogs.DNSAMessage("SettingsContainerPanel.switchToAdvancedSettings.presetNotification", //NOI18N
                                                                               READONLY_SETTINGS_MSG,
                                                                               ProfilerDialogs.DNSAMessage.INFORMATION_MESSAGE);
            dnsa.setDNSADefault(false);
            ProfilerDialogs.notify(dnsa);
        }
    }
}
