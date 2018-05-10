/*
 *  Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.profiler.startup;

import org.graalvm.visualvm.core.ui.DesktopUtils;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.profiler.CPUSettingsSupport;
import org.graalvm.visualvm.profiler.JDBCSettingsSupport;
import org.graalvm.visualvm.profiler.MemorySettingsSupport;
import org.graalvm.visualvm.profiler.ProfilerSettingsSupport;
import org.graalvm.visualvm.profiler.ProfilerSupport;
import org.graalvm.visualvm.profiling.presets.PresetSelector;
import org.graalvm.visualvm.profiling.presets.ProfilerPreset;
import org.graalvm.visualvm.profiling.presets.ProfilerPresets;
import org.graalvm.visualvm.uisupport.HorizontalLayout;
import org.graalvm.visualvm.uisupport.SeparatorLine;
import org.graalvm.visualvm.uisupport.UISupport;
import org.graalvm.visualvm.uisupport.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HINT_ProfileStartup=Profile manually started local application from its startup. Follow these steps to start the profiling session.",
    "STEP_1=1. Specify application configuration:",
    "STEP_2=2. Define profiler settings:",
    "STEP_3=3. Setup profiled application:",
    "CAP_InvalidSettings=Invalid Profiler Settings",
    "MSG_InvalidCPUSettings=Provided CPU settings are invalid.",
    "MSG_InvalidMemorySettings=Provided memory settings are invalid.",
    "LBL_Continue=Continue >>>",
    "LBL_Platform=Platform:",
    "LBL_Architecture=Architecture:",
    "LBL_Port=Port:",
    "BTN_Profile=Profile",
    "LBL_Profile=Profile:",
    "LBL_CPU=CPU",
    "LBL_Memory=Memory",
    "LBL_JDBC=JDBC",
    "BTN_Clipboard=Copy to clipboard",
    "CAP_Clipboard=Copy to Clipboard",
    "MSG_Clipboard=Profiler parameter copied to clipboard",
    "HINT_ConfigureApp=Configure the application to run using {0} {1} and add the following parameter to its JVM arguments:",
    "STR_User=user {0}",
    "STR_CurrentUser=the current user",
    "HINT_StartApp=Click the Profile button to submit this dialog and then start the application as {0}.",
    "CAP_OnlineHelp=Online Help",
    "LBL_OnlineHelp=Online help",
    "MSG_OnlineHelp=Please open the following address in your browser:"
})
final class StartupConfigurator {
    
    private static final String HELP = "https://visualvm.github.io/startupprofiler.html"; // NOI18N
    
    private static final String CPU_ICON_PATH = "org/graalvm/visualvm/profiler/startup/resources/cpu.png";  // NOI18N
    private static final String MEM_ICON_PATH = "org/graalvm/visualvm/profiler/startup/resources/memory.png";  // NOI18N
    private static final String JDBC_ICON_PATH = "org/graalvm/visualvm/profiler/startup/resources/jdbc.png";  // NOI18N
    private static final String HELP_ICON_PATH = "org/graalvm/visualvm/profiler/startup/resources/help.png";  // NOI18N
    private static final Icon CPU_ICON = ImageUtilities.loadImageIcon(CPU_ICON_PATH, false);
    private static final Icon MEM_ICON = ImageUtilities.loadImageIcon(MEM_ICON_PATH, false);
    private static final Icon JDBC_ICON = ImageUtilities.loadImageIcon(JDBC_ICON_PATH, false);
    private static final Icon HELP_ICON = ImageUtilities.loadImageIcon(HELP_ICON_PATH, false);
    
    private CPUSettingsSupport cpuSettings;
    private MemorySettingsSupport memorySettings;
    private JDBCSettingsSupport jdbcSettings;
    
    private DefaultComboBoxModel selectorModel;
    private List<PresetSelector> allSelectors;
    
    private JComponent ui;
    private boolean accepted;
    
    private JButton submit;
    private JTextArea start1;
    private JTextArea start2;
    private JTextArea param;
    
    private JPanel panel;
    private Dimension cpuSize;
    private Dimension memorySize;
    private Dimension jdbcSize;
    
    private JRadioButton cpuSelector;
    private JRadioButton memorySelector;
    private JRadioButton jdbcSelector;
    private JComboBox java;
    private JComboBox arch;
    private JSpinner port;
    
    private String[] javaPlatforms;
    private String[] architectures;

    
    StartupConfigurator() {
        cpuSettings = new CPUSettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid() &&
                       jdbcSettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return StartupConfigurator.this.createSelector(presetSynchronizer);
            }
        };
        memorySettings = new MemorySettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid() &&
                       jdbcSettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return StartupConfigurator.this.createSelector(presetSynchronizer);
            }
        };
        jdbcSettings = new JDBCSettingsSupport() {
            public boolean presetValid() {
                return cpuSettings.settingsValid() &&
                       memorySettings.settingsValid() &&
                       jdbcSettings.settingsValid();
            }
            public PresetSelector createSelector(Runnable presetSynchronizer) {
                return StartupConfigurator.this.createSelector(presetSynchronizer);
            }
        };
        
        // Warmup, the implementation expects both panels to be created
        cpuSettings.getComponent();
        memorySettings.getComponent();
        jdbcSettings.getComponent();
    }
    
    private PresetSelector createSelector(Runnable presetSynchronizer) {
        if (selectorModel == null) selectorModel = new DefaultComboBoxModel();
        if (allSelectors == null) allSelectors = new ArrayList();
        PresetSelector selector = ProfilerPresets.getInstance().createSelector(
                                  selectorModel, allSelectors, presetSynchronizer);
        allSelectors.add(selector);
        return selector;
    }
    
    JComponent getUI() {
        accepted = false;
        if (ui == null) ui = createUI();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { if (submit.isShowing()) submit.requestFocusInWindow(); }
        });
        return ui;
    }
    
    boolean accepted() {
        return accepted;
    }
    
    ProfilerSettingsSupport getSettings() {
        if (cpuSelector.isSelected()) return cpuSettings;
        else if (memorySelector.isSelected()) return memorySettings;
        else if (jdbcSelector.isSelected()) return jdbcSettings;
        return null;
    }
    
    ProfilerPreset getPreset() {
        return (ProfilerPreset)selectorModel.getSelectedItem();
    }
    
    String getJavaPlatform() {
        return javaPlatforms[java.getSelectedIndex()];
    }
    
    int getArchitecture() {
        return Integer.parseInt(architectures[arch.getSelectedIndex()]);
    }
    
    int getPort() {
        return (Integer)port.getValue();
    }
    
            
    private JComponent createUI() {
        JPanel header = new JPanel(new VerticalLayout(false));
        header.setOpaque(true);
        JTextArea hint = new JTextArea(Bundle.HINT_ProfileStartup());
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setOpaque(false);
        if (UISupport.isNimbusLookAndFeel()) hint.setBackground(new Color(0, 0, 0, 0));
        hint.setCaret(new NullCaret());
        hint.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        header.add(hint);
        header.add(new SeparatorLine());
        
        JPanel content = new JPanel(new VerticalLayout(false));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.setOpaque(false);
        
        content.add(new SectionSeparator(Bundle.STEP_1()));
        content.add(createAttachPanel());
        
        final JPanel show2 = new JPanel(new BorderLayout());
        show2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
        show2.setOpaque(false);
        show2.setVisible(false);
        content.add(show2);
        
        final JComponent separator2 = new SectionSeparator(Bundle.STEP_2());
        final JComponent profilerP = createProfilePanel();
        content.add(separator2);
        content.add(profilerP);
        
        final JPanel show3 = new JPanel(new BorderLayout());
        show3.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
        show3.setOpaque(false);
        show3.setVisible(false);
        content.add(show3);
        
        final JComponent separator3 = new SectionSeparator(Bundle.STEP_3());
        final JComponent stepsP = createStepsPanel();
        content.add(separator3);
        content.add(stepsP);
        
        final JPanel footer = new JPanel(new VerticalLayout(false));
        footer.setOpaque(true);
        footer.add(new SeparatorLine(), BorderLayout.NORTH);
        footer.add(createVerticalSpace(10));
        JPanel buttons = new JPanel(new BorderLayout(0, 0));
        JPanel buttonsL = new JPanel(new HorizontalLayout(false));
        JButton help = new JButton(HELP_ICON) {
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { showHelp(null); } // NOI18N
                });
            }
            public Dimension getPreferredSize() {
                Dimension d = submit.getPreferredSize();
                d.width = d.height;
                return d;
            }
        };
        help.setToolTipText(Bundle.LBL_OnlineHelp());
        buttonsL.add(createHorizontalSpace(10));
        buttonsL.add(help);
        JPanel buttonsR = new JPanel(new HorizontalLayout(false));
        submit = new JButton(Bundle.BTN_Profile(), new ImageIcon(StartupProfilerAction.ICON)) {
            protected void fireActionPerformed(ActionEvent e) {
                String err = null;
                if (cpuSelector.isSelected()) {
                    if (!cpuSettings.settingsValid())
                        err = Bundle.MSG_InvalidCPUSettings();
                } else if (memorySelector.isSelected()) {
                    if (!memorySettings.settingsValid())
                        err = Bundle.MSG_InvalidMemorySettings();
                }
                
                if (err != null) {
                    Dialogs.show(Dialogs.error(Bundle.CAP_InvalidSettings(), err));
                } else {
                    accepted = true;
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w != null) w.setVisible(false);
                }
            }
        };
        buttonsR.add(submit);
//        buttonsC.add(createHorizontalSpace(5));
//        buttonsC.add(new JButton("Cancel") {
//            protected void fireActionPerformed(ActionEvent e) {
//                Window w = SwingUtilities.getWindowAncestor(this);
//                if (w != null) w.setVisible(false);
//            }
//        });
        buttonsR.add(createHorizontalSpace(10));
        buttons.add(buttonsL, BorderLayout.WEST);
        buttons.add(buttonsR, BorderLayout.EAST);
        footer.add(buttons);
        footer.add(createVerticalSpace(10));
        
        // ---
        panel = new JPanel(new VerticalLayout(false));
        panel.setBackground(UISupport.getDefaultBackground());
        panel.setOpaque(true);
        panel.add(header);
        panel.add(content);
        panel.add(footer);
        // ---
        
        // Read the preferred dialog width to initialize textareas
        int width = panel.getPreferredSize().width;
        // Correctly layout multiline textareas
        hint.setSize(width - widthInsetsInContainer(hint, panel), Integer.MAX_VALUE);
        start1.setSize(width - widthInsetsInContainer(start1, panel), Integer.MAX_VALUE);
        start2.setSize(width - widthInsetsInContainer(start2, panel), Integer.MAX_VALUE);
        // Setup dialog size
        cpuSize = panel.getPreferredSize();
        memorySize = new Dimension(cpuSize);
        memorySize.height -= cpuSettings.getComponent().getPreferredSize().height - memorySettings.getComponent().getPreferredSize().height;
        jdbcSize = new Dimension(cpuSize);
        jdbcSize.height -= cpuSettings.getComponent().getPreferredSize().height - jdbcSettings.getComponent().getPreferredSize().height;
        panel.setPreferredSize(cpuSize);
        
        separator2.setVisible(false);
        profilerP.setVisible(false);
        separator3.setVisible(false);
        stepsP.setVisible(false);
        footer.setVisible(false);
        
        show2.setVisible(true);
        
        JButton show2A = new JButton(HELP_ICON) {
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { showHelp("appconfig"); } // NOI18N
                });
            }
        };
        show2A.setToolTipText(Bundle.LBL_OnlineHelp());
        show2A.setContentAreaFilled(false);
        show2A.setOpaque(false);
        show2A.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        show2A.setBorder(BorderFactory.createEmptyBorder(2, 3, 1, 3));
        
        JButton show2B = new JButton("<html><a href='#'>" + Bundle.LBL_Continue() + "</a></html>") { // NOI18N
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        separator2.setVisible(true);
                        profilerP.setVisible(true);
                        show3.setVisible(true);
                        show2.setVisible(false);
                    }
                });
            }
        };
        show2B.setContentAreaFilled(false);
        show2B.setOpaque(false);
        show2B.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        show2B.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        JPanel show2P = new JPanel(new HorizontalLayout(false));
        show2P.setOpaque(false);
        show2P.add(show2A);
        show2P.add(createHorizontalSpace(1));
        show2P.add(show2B);
        
        show2.add(show2P, BorderLayout.EAST);
        
        JButton show3A = new JButton(HELP_ICON) {
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { showHelp("profsettings"); } // NOI18N
                });
            }
        };
        show3A.setToolTipText(Bundle.LBL_OnlineHelp());
        show3A.setContentAreaFilled(false);
        show3A.setOpaque(false);
        show3A.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        show3A.setBorder(BorderFactory.createEmptyBorder(2, 3, 1, 3));
        
        JButton show3B = new JButton("<html><a href='#'>" + Bundle.LBL_Continue() + "</a></html>") { // NOI18N
            protected void fireActionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        separator3.setVisible(true);
                        stepsP.setVisible(true);
                        footer.setVisible(true);
                        show3.setVisible(false);
                    }
                });
            }
        };
        show3B.setContentAreaFilled(false);
        show3B.setOpaque(false);
        show3B.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        show3B.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        
        JPanel show3P = new JPanel(new HorizontalLayout(false));
        show3P.setOpaque(false);
        show3P.add(show3A);
        show3P.add(createHorizontalSpace(1));
        show3P.add(show3B);
        
        show3.add(show3P, BorderLayout.EAST);
        
        return panel;
    }
    
    private JPanel createAttachPanel() {
        JPanel attach = new JPanel(new HorizontalLayout(false, 5));
        attach.setBorder(BorderFactory.createEmptyBorder(5, 13, 15, 5));
        attach.setOpaque(false);
        
        attach.add(new JLabel(Bundle.LBL_Platform()));
        String[][] platforms = ProfilerSupport.getInstance().getSupportedJavaPlatforms();
        javaPlatforms = platforms[1];
        java = new JComboBox(platforms[0]) {
            public Dimension getPreferredSize() { // Workaround for Nimbus LaF
                Dimension d = super.getPreferredSize();
                if (UISupport.isNimbusLookAndFeel()) d.width += 5;
                return d;
            }
            protected void selectedItemChanged() {
                super.selectedItemChanged();
                if (arch != null) {
                    String[][] archs = ProfilerSupport.getInstance().getSupportedArchitectures(getJavaPlatform());
                    architectures = archs[1];
                    Object sel = arch.getSelectedItem();
                    arch.setModel(new DefaultComboBoxModel(archs[0]));
                    if (sel == null) sel = archs[2][0];
                    if (sel != null) arch.setSelectedItem(sel);
                    arch.setEnabled(arch.getItemCount() > 1);
                }
                
                updateParam();
            }
        };
        java.setEnabled(java.getItemCount() > 1);
        attach.add(java);
        attach.add(createHorizontalSpace(2));
        attach.add(new JLabel(Bundle.LBL_Architecture()));
        arch = new JComboBox() {
            public Dimension getPreferredSize() { // Workaround for Nimbus LaF
                Dimension d = super.getPreferredSize();
                if (UISupport.isNimbusLookAndFeel()) d.width += 5;
                return d;
            }
            protected void selectedItemChanged() {
                super.selectedItemChanged();
                updateParam();
            }
        };
        java.setSelectedItem(platforms[2][0]);
        attach.add(arch);
        attach.add(createHorizontalSpace(2));
        attach.add(new JLabel(Bundle.LBL_Port()));
        int portv = ProfilerSupport.getInstance().getDefaultPort();
        port = new JSpinner(new SpinnerNumberModel(portv, 1, 65535, 1));
        port.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) { updateParam(); }
        });
        attach.add(port);
        
        return attach;
    }
    
    private JPanel createProfilePanel() {
        final JComponent cpu = cpuSettings.getComponent();
        ((JComponent)cpu.getComponent(0)).setBorder(BorderFactory.createEmptyBorder(-3, -10, 0, -10));
        ((JComponent)cpu.getComponent(1)).setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        
        final JComponent memory = memorySettings.getComponent();
        ((JComponent)memory.getComponent(0)).setBorder(BorderFactory.createEmptyBorder(-3, -10, 0, -10));
        ((JComponent)memory.getComponent(1)).setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        memory.setVisible(false);
        
        final JComponent jdbc = jdbcSettings.getComponent();
        ((JComponent)jdbc.getComponent(0)).setBorder(BorderFactory.createEmptyBorder(-3, -10, 0, -10));
        ((JComponent)jdbc.getComponent(1)).setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        jdbc.setVisible(false);
        
        final JPanel profile = new JPanel(new VerticalLayout(false)); 
        profile.setBorder(BorderFactory.createEmptyBorder(5, 13, 15, 5));
        profile.setOpaque(false);
        
        JPanel mode = new JPanel(new HorizontalLayout(false, 5));
        mode.setOpaque(false);
        mode.add(new JLabel(Bundle.LBL_Profile()));
        final ButtonGroup bg = new ButtonGroup();
        cpuSelector = new IconRadioButton(Bundle.LBL_CPU(), CPU_ICON, true) {
            boolean firstEvent = true;
            { bg.add(this); }
            protected void fireItemStateChanged(ItemEvent e) {
                super.fireItemStateChanged(e);
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    cpu.setVisible(true);
                    memory.setVisible(false);
                    jdbc.setVisible(false);
                    if (panel != null) {
                        panel.setPreferredSize(cpuSize);
                        SwingUtilities.getWindowAncestor(profile).pack();
                    }
                }
            }
        };
        mode.add(cpuSelector);
        memorySelector = new IconRadioButton(Bundle.LBL_Memory(), MEM_ICON, false) {
            { bg.add(this); }
            protected void fireItemStateChanged(ItemEvent e) {
                super.fireItemStateChanged(e);
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    cpu.setVisible(false);
                    memory.setVisible(true);
                    jdbc.setVisible(false);
                    if (panel != null) {
                        panel.setPreferredSize(memorySize);
                        SwingUtilities.getWindowAncestor(profile).pack();
                    }
                }
            }
        };
        mode.add(memorySelector);
        jdbcSelector = new IconRadioButton(Bundle.LBL_JDBC(), JDBC_ICON, false) {
            { bg.add(this); }
            protected void fireItemStateChanged(ItemEvent e) {
                super.fireItemStateChanged(e);
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    cpu.setVisible(false);
                    memory.setVisible(false);
                    jdbc.setVisible(true);
                    if (panel != null) {
                        panel.setPreferredSize(jdbcSize);
                        SwingUtilities.getWindowAncestor(profile).pack();
                    }
                }
            }
        };
        mode.add(jdbcSelector);
        profile.add(mode);
        
        profile.add(cpu);
        profile.add(memory);
        profile.add(jdbc);
        
        return profile;
    }
    
    private JPanel createStepsPanel() {
        JPanel steps = new JPanel(new VerticalLayout(false)); 
        steps.setBorder(BorderFactory.createEmptyBorder(5, 13, 15, 5));
        steps.setOpaque(false);
        
        start1 = new JTextArea();
        start1.setLineWrap(true);
        start1.setWrapStyleWord(true);
        start1.setEditable(false);
        start1.setFocusable(false);
        start1.setOpaque(false);
        if (UISupport.isNimbusLookAndFeel()) start1.setBackground(new Color(0, 0, 0, 0));
        start1.setCaret(new NullCaret());
        start1.setBorder(BorderFactory.createEmptyBorder());
        steps.add(start1);
        
        final JPanel arg = new JPanel(new BorderLayout(5, 0));
        arg.setOpaque(false);
        TextAreaComponent paramA = createTextArea(1);
        param = paramA.getTextArea();
        updateParam();
        arg.add(paramA, BorderLayout.CENTER);
        JButton link = new JButton(Bundle.BTN_Clipboard()) {
            protected void fireActionPerformed(ActionEvent e) {
                RequestProcessor.getDefault().post(new Runnable() {
                    public void run() {
                        StringSelection s = new StringSelection(param.getText());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, s);
                        Dialogs.show(Dialogs.info(Bundle.CAP_Clipboard(), Bundle.MSG_Clipboard()));
                    }
                });
            }
        };
        arg.add(link, BorderLayout.EAST);
        
        steps.add(createVerticalSpace(8));
        steps.add(arg);
        steps.add(createVerticalSpace(8));
        
        String user = System.getProperty("user.name"); // NOI18N
        if (user != null) user = Bundle.STR_User(user);
        else user = Bundle.STR_CurrentUser();
        start2 = new JTextArea(Bundle.HINT_StartApp(user));
        start2.setLineWrap(true);
        start2.setWrapStyleWord(true);
        start2.setEditable(false);
        start2.setFocusable(false);
        start2.setOpaque(false);
        if (UISupport.isNimbusLookAndFeel()) start2.setBackground(new Color(0, 0, 0, 0));
        start2.setCaret(new NullCaret());
        start2.setBorder(BorderFactory.createEmptyBorder());
        steps.add(start2);
        
        return steps;
    }
    
    private void updateParam() {
        if (param == null) return;
        
        start1.setText(Bundle.HINT_ConfigureApp(java.getSelectedItem().toString(),
                                                arch.getSelectedItem().toString()));
        
        int caret = param.getCaretPosition();
        param.setText(ProfilerSupport.getInstance().getStartupParameter(
                      getJavaPlatform(), getArchitecture(), getPort()));
        try {
            param.setCaretPosition(caret);
        } catch (IllegalArgumentException e) {
            param.setCaretPosition(0);
        }
        
        if (param.isShowing()) {
            final JComponent c = (JComponent)param.getParent().getParent();
            c.setBorder(BorderFactory.createLineBorder(Color.RED));

            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            JComponent c = (JComponent)param.getParent().getParent();
                            c.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        }
                    });
                }
            }, 180);
        }
    }
    
    
    private static void showHelp(String section) {
        final String addr = (section == null) ? HELP : HELP + "#" + section; // NOI18N
        if (DesktopUtils.isBrowseAvailable()) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    try {
                        URI uri = new URI(addr);
                        DesktopUtils.browse(uri);
                    } catch (Exception e) { showHelpDialog(addr); }
                }
            });
        } else {
            showHelpDialog(addr);
        }
    }
    
    private static void showHelpDialog(final String addr) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Dialogs.show(Dialogs.info(Bundle.CAP_OnlineHelp(),
                             Bundle.MSG_OnlineHelp() + "\n" + addr)); // NOI18N
            }
        });
    }
    
    private static int widthInsetsInContainer(Container c, Container p) {
        int w = 0;
        
        while (c != null && p != null && c != p) {
            c = c.getParent();
            Insets i = c.getInsets();
            w += i.left + i.right;
        }
        
        return w;
    }
    
    private static JComponent createHorizontalSpace(final int width) {
        JPanel space = new JPanel(null) {
            public Dimension getPreferredSize() { return new Dimension(width, 0); }
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        space.setOpaque(false);
        return space;
    }
    
    private static JComponent createVerticalSpace(final int height) {
        JPanel space = new JPanel(null) {
            public Dimension getPreferredSize() { return new Dimension(0, height); }
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        space.setOpaque(false);
        return space;
    }
    
    private static TextAreaComponent createTextArea(int rows) {
        final JTextArea rootsArea = new JTextArea();
        rootsArea.setEditable(false);
        rootsArea.setFont(new Font("Monospaced", Font.PLAIN, UIManager.getFont("Label.font").getSize())); // NOI18N
        TextAreaComponent rootsAreaScrollPane = new TextAreaComponent(rootsArea,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                rootsArea.setEnabled(enabled);
            }
        };
        rootsAreaScrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JTextArea referenceArea = new JTextArea("X"); // NOI18N
        referenceArea.setFont(rootsArea.getFont());
        referenceArea.setRows(rows);
        Insets insets = rootsAreaScrollPane.getInsets();
        rootsAreaScrollPane.setPreferredSize(new Dimension(1, referenceArea.getPreferredSize().height + 
                (insets != null ? insets.top + insets.bottom : 0)));
        return rootsAreaScrollPane;
    }
    
    private static class TextAreaComponent extends JScrollPane {
        public TextAreaComponent(JTextArea textArea, int vPolicy, int hPolicy) { super(textArea, vPolicy, hPolicy); }
        public JTextArea getTextArea() { return (JTextArea)getViewport().getView(); }
    }
    
    private static class IconRadioButton extends JRadioButton {

        private static final int CHECKBOX_OFFSET = getCheckBoxOffset();

        private final JRadioButton renderer;

        public IconRadioButton(String text, Icon icon, boolean selected) {
            renderer = new JRadioButton(text, icon) {
                public boolean hasFocus() {
                    return IconRadioButton.this.hasFocus();
                }
            };
            renderer.setOpaque(false);
            renderer.setBorderPainted(false);
            setSelected(selected);
            setBorderPainted(false);
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.translate(renderer.getX(), renderer.getY());
            renderer.paint(g);
            g.translate(-renderer.getX(), -renderer.getY());
        }


        public void setBounds(int x, int y, int width, int height) {
            Dimension d = super.getPreferredSize();
            renderer.setBounds(d.width - CHECKBOX_OFFSET, 0,
                               width - d.width + CHECKBOX_OFFSET, height);
            super.setBounds(x, y, width, height);
        }

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width += renderer.getPreferredSize().width - CHECKBOX_OFFSET;
            return d;
        }


        private static int getCheckBoxOffset() {
            if (UISupport.isWindowsLookAndFeel()) return 3;
            else if (UISupport.isNimbusLookAndFeel()) return -3;
            else if (UISupport.isMetalLookAndFeel()) return 3;
            else if (UISupport.isAquaLookAndFeel()) return 6;
            else return 0;
        }

    }
    
    private static final class NullCaret implements Caret {
        public void install(javax.swing.text.JTextComponent c) {}
        public void deinstall(javax.swing.text.JTextComponent c) {}
        public void paint(Graphics g) {}
        public void addChangeListener(ChangeListener l) {}
        public void removeChangeListener(ChangeListener l) {}
        public boolean isVisible() { return false; }
        public void setVisible(boolean v) {}
        public boolean isSelectionVisible() { return false; }
        public void setSelectionVisible(boolean v) {}
        public void setMagicCaretPosition(Point p) {}
        public Point getMagicCaretPosition() { return new Point(0, 0); }
        public void setBlinkRate(int rate) {}
        public int getBlinkRate() { return 0; }
        public int getDot() { return 0; }
        public int getMark() { return 0; }
        public void setDot(int dot) {}
        public void moveDot(int dot) {}
    }
    
}
