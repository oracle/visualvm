/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.lib.profiler.ui.swing.PopupButton;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.actions.HeapDumpAction;
import org.netbeans.modules.profiler.actions.RunGCAction;
import org.netbeans.modules.profiler.actions.TakeThreadDumpAction;
import org.netbeans.modules.profiler.api.ActionsSupport;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProfilerIDESettings;
import org.netbeans.modules.profiler.api.ProfilerStorage;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.attach.AttachWizard;
import org.netbeans.modules.profiler.v2.impl.FeaturesView;
import org.netbeans.modules.profiler.v2.impl.ProfilerStatus;
import org.netbeans.modules.profiler.v2.impl.WelcomePanel;
import org.netbeans.modules.profiler.v2.ui.DropdownButton;
import org.netbeans.modules.profiler.v2.ui.StayOpenPopupMenu;
import org.netbeans.modules.profiler.v2.ui.ToggleButtonMenuItem;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProfilerWindow_captionProject={0}",
    "ProfilerWindow_captionFile={0} | {1}",
    "ProfilerWindow_captionExternal=Profile External Process",
    "ProfilerWindow_configure=Configure Session",
    "ProfilerWindow_profile=Profile",
    "ProfilerWindow_attach=Attach",
    "ProfilerWindow_profileTooltip=Start profiling session",
    "ProfilerWindow_profileRunningTooltip=Start profiling session (already running)",
    "ProfilerWindow_configureTooltip=Configure profiling session",
    "ProfilerWindow_terminateCaption=Terminate Profiling Session",
    "ProfilerWindow_terminateMsg=Terminate profiling session?",
    "ProfilerWindow_loadingSession=Creating session...",
    "ProfilerWindow_settings=Settings",
    "ProfilerWindow_application=Process:",
    "ProfilerWindow_threadDump=Thread Dump",
    "ProfilerWindow_heapDump=Heap Dump",
    "ProfilerWindow_gc=GC",
    "ProfilerWindow_setupAttachProject=Setup Attach to Project...",
    "ProfilerWindow_setupAttachProcess=Setup Attach to Process...",
    "ProfilerWindow_multipleFeatures=Enable Multiple Modes",
    "ProfilerWindow_usePPoints=Use Defined Profiling Points",
    "ProfilerWindow_targetSection=Target:",
    "ProfilerWindow_profileSection=Profile:",
    "ProfilerWindow_settingsSection=Settings:",
    "ProfilerWindow_pluginsSection=Plugins:",
    "#NOI18N",
    "ProfilerWindow_mode=editor",
    "ProfilerWindow_noFeature=<html><b>No profiling feature selected.</b><br><br>Please select at least one profiling feature for the session.</html>"
})
class ProfilerWindow extends ProfilerTopComponent {    
    
    // --- Constructor ---------------------------------------------------------
    
    private final ProfilerSession session;
    
    ProfilerWindow(ProfilerSession session) {
        this.session = session;
        
        updateWindowName();
        updateWindowIcon();
        
        createUI();
    }
    
    // --- API -----------------------------------------------------------------
    
    void updateSession() {
        updateWindowName();
        
        start.setText(session.isAttach() ? Bundle.ProfilerWindow_attach() :
                                           Bundle.ProfilerWindow_profile());
    }
    
    void selectFeature(ProfilerFeature feature) {
        if (featuresView != null) featuresView.selectFeature(feature);
    }
    
    // --- Implementation ------------------------------------------------------
    
    private ProfilerFeatures features;
    private ProfilerPlugins plugins;
    
    private ProfilerToolbar toolbar;
    private ProfilerToolbar featureToolbar;
    private ProfilerToolbar applicationToolbar;
//    private ProfilerToolbar statusBar;
    private JPanel container;
    private FeaturesView featuresView;
    
    private PopupButton configure;
    private DropdownButton start;
    private JButton stop;
    private SettingsPresenter settingsButton;
    
    private AttachSettings attachSettings;
    
    private ProfilerStatus status;
//    private String preselectItem;
    
    
    private void createUI() {
        setLayout(new BorderLayout(0, 0));
        setFocusable(false);
        
        toolbar = ProfilerToolbar.create(true);
        add(toolbar.getComponent(), BorderLayout.NORTH);
        
        final JLabel loading = new JLabel(Bundle.ProfilerWindow_loadingSession());
        loading.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolbar.add(loading);
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                loadSessionSettings();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toolbar.remove(loading);
                        popupulateUI();
                    }
                });
            }
        });
    }
    
    private void loadSessionSettings() {
        features = session.getFeatures();
        plugins = session.getPlugins();
        
        Properties p = new Properties();
        try {
            ProfilerStorage.loadProjectProperties(p, session.getProject(), "attach"); // NOI18N
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (!p.isEmpty()) {
            attachSettings = new AttachSettings();
            attachSettings.load(p);
        }
    }
    
    private void popupulateUI() {  
        configure = new PopupButton(Bundle.ProfilerWindow_configure()) {
            protected void displayPopup() { displayPopupImpl(); }
        };
        configure.setToolTipText(Bundle.ProfilerWindow_configureTooltip());
        if (UIUtils.isAquaLookAndFeel())
            configure.putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N
        toolbar.add(configure);
        
        String command = session.isAttach() ? Bundle.ProfilerWindow_attach() :
                                            Bundle.ProfilerWindow_profile();
        start = new DropdownButton(command, Icons.getIcon(GeneralIcons.START), true) {
            public void displayPopup() { displayPopupImpl(); }
            protected void performAction() { performStartImpl(); }
        };
        start.setToolTipText(Bundle.ProfilerWindow_profileTooltip());
        start.setPushedToolTipText(Bundle.ProfilerWindow_profileRunningTooltip());
        start.setPopupToolTipText(Bundle.ProfilerWindow_configureTooltip());
        toolbar.add(start);
        
        stop = new JButton(ProfilerSessions.StopAction.getInstance());
        stop.setHideActionText(true);
        toolbar.add(stop);
        
//        statusBar = new ProfilerStatus(session).getToolbar();
//        statusBar.getComponent().setVisible(false); // TODO: read last state
//        toolbar.add(statusBar);
        
        toolbar.addFiller();
        
        settingsButton = new SettingsPresenter();
        toolbar.add(settingsButton);
        
        container = new JPanel(new BorderLayout(0, 0));
        add(container, BorderLayout.CENTER);
        
        JPanel welcomePanel = new WelcomePanel(configure.getText(), start.getText(), features.getAvailable()) {
            public void highlightItem(final String text) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { configure.doClick(85); }
                });
            }
        };
        
        boolean showHint = ProfilerIDESettings.getInstance().getShowNoDataHint();
        featuresView = new FeaturesView(welcomePanel, showHint ? command : null);
        container.add(featuresView, BorderLayout.CENTER);
        
        features.addListener(new ProfilerFeatures.Listener() {
            void featuresChanged(ProfilerFeature changed) { updateFeatures(changed); }
            void settingsChanged(boolean valid) { updateSettings(valid); }
        });
        updateFeatures(null);
        updateSettings(features.settingsValid());
        
        featuresView.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateFeatureToolbar();
            }
        });
        updateFeatureToolbar();
        
        session.addListener(new SimpleProfilingStateAdapter() {
            public void update() {
                updateWindowIcon();
                updateButtons();
            }
        });
        updateButtons();
        
        registerActions();
        
        status = ProfilerStatus.forSession(session);
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) status.startSessionLogging();
                    else status.stopSessionLogging();
                }
            }
        });
        if (isShowing()) status.startSessionLogging();
    }
    
    private void registerActions() {
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        
        final String filterKey = org.netbeans.lib.profiler.ui.swing.FilterUtils.FILTER_ACTION_KEY;
        Action filterAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerFeature feature = featuresView.getSelectedFeature();
                JPanel resultsUI = feature == null ? null : feature.getResultsUI();
                if (resultsUI == null) return;
                
                Action action = resultsUI.getActionMap().get(filterKey);
                if (action != null && action.isEnabled()) action.actionPerformed(e);
            }
        };
        ActionsSupport.registerAction(filterKey, filterAction, actionMap, inputMap);
        
        final String findKey = SearchUtils.FIND_ACTION_KEY;
        Action findAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ProfilerFeature feature = featuresView.getSelectedFeature();
                JPanel resultsUI = feature == null ? null : feature.getResultsUI();
                if (resultsUI == null) return;
                
                Action action = resultsUI.getActionMap().get(findKey);
                if (action != null && action.isEnabled()) action.actionPerformed(e);
            }
        };
        ActionsSupport.registerAction(findKey, findAction, actionMap, inputMap);
    }
    
    
    private void updateWindowName() {
       Lookup.Provider project = session.getProject();
        if (project == null) {
            setDisplayName(Bundle.ProfilerWindow_captionExternal());
        } else {
            String projectN = ProjectUtilities.getDisplayName(project);
            FileObject file = session.getFile();
            setDisplayName(file == null ? Bundle.ProfilerWindow_captionProject(projectN) :
                                          Bundle.ProfilerWindow_captionFile(projectN, file.getNameExt()));
        } 
    }
    
    private void updateWindowIcon() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (session.inProgress()) setIcon(Icons.getImage(ProfilerIcons.PROFILE_RUNNING));
                else setIcon(Icons.getImage(ProfilerIcons.PROFILE_INACTIVE));
            }
        });
    }
    
    private void updateButtons() {
        int state = session.getState();
        start.setPushed(state != Profiler.PROFILING_INACTIVE);
        start.setPopupEnabled(state != Profiler.PROFILING_IN_TRANSITION);
    }
    
    
    private void updateFeatures(ProfilerFeature changed) {
        // TODO: optimize!
        // TODO: restore focused component if possible
        ProfilerFeature restore = featuresView.getSelectedFeature();
        featuresView.removeFeatures();
        Set<ProfilerFeature> selected = features.getActivated();
        for (ProfilerFeature feature : selected) featuresView.addFeature(feature);
        if (changed != null && selected.contains(changed)) featuresView.selectFeature(changed);
        else featuresView.selectFeature(restore);
        featuresView.repaint();
    }
    
    private void updateSettings(boolean valid) {
        start.setEnabled(valid);
        
        boolean _configure = features.getActivated().isEmpty();
        start.setVisible(!_configure);
        stop.setVisible(!_configure);
        configure.setVisible(_configure);
        
        if (session.inProgress()) session.doModify(features.getSettings());
    }
    
    private void updateFeatureToolbar() {
        if (featureToolbar != null) toolbar.remove(featureToolbar);
        if (applicationToolbar != null) toolbar.remove(applicationToolbar);
        
        ProfilerFeature selected = featuresView.getSelectedFeature();
        featureToolbar = selected == null ? null : selected.getToolbar();
        if (featureToolbar != null) toolbar.add(featureToolbar, toolbar.getComponentCount() - 2); // add before filler & settingsButton
        settingsButton.setFeature(selected);
        
        if (selected != null) toolbar.add(getApplicationToolbar(), toolbar.getComponentCount() - 2); // add before filler & settingsButton
        else applicationToolbar = null;
        
        doLayout();
        repaint();
    }
    
    private ProfilerToolbar getApplicationToolbar() {
        if (applicationToolbar == null) {
            applicationToolbar = ProfilerToolbar.create(true);

            applicationToolbar.addSpace(2);
            applicationToolbar.addSeparator();
            applicationToolbar.addSpace(5);

            JLabel apLabel = new GrayLabel(Bundle.ProfilerWindow_application());
            applicationToolbar.add(apLabel);
            
            applicationToolbar.addSpace(2);
            
            JButton apThreadDumpButton = new JButton(TakeThreadDumpAction.getInstance());
            apThreadDumpButton.setHideActionText(true);
//            apThreadDumpButton.setText(Bundle.ProfilerWindow_threadDump());
            applicationToolbar.add(apThreadDumpButton);
            
            applicationToolbar.addSpace(3);
            
            JButton apHeapDumpButton = new JButton(HeapDumpAction.getInstance());
            apHeapDumpButton.setHideActionText(true);
//            apHeapDumpButton.setText(Bundle.ProfilerWindow_heapDump());
            applicationToolbar.add(apHeapDumpButton);
            
            applicationToolbar.addSpace(3);
            
            JButton apGCButton = new JButton(RunGCAction.getInstance());
            apGCButton.setHideActionText(true);
//            apGCButton.setText(Bundle.ProfilerWindow_gc());
            applicationToolbar.add(apGCButton);
        }
        return applicationToolbar;
    }
    
    private boolean configureAttachSettings(boolean partially) {
        AttachSettings settings = AttachWizard.getDefault().configure(attachSettings, partially);
        if (settings == null) return false; // cancelled by the user
        
        attachSettings = settings;
        final AttachSettings as = new AttachSettings();
        attachSettings.copyInto(as);
        final Lookup.Provider lp = session.getProject();
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                Properties p = new Properties();
                as.store(p);
                try {
                    ProfilerStorage.saveProjectProperties(p, lp, "attach"); // NOI18N
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
            
        return true;
    }
    
    private void performStartImpl() {
        featuresView.resetNoDataHint();
        
        start.setPushed(true);
        
        final ProfilingSettings _profilingSettings = features.getSettings();
        if (_profilingSettings == null) { // #250237 ?
            ProfilerDialogs.displayError(Bundle.ProfilerWindow_noFeature());
            start.setPushed(false);
            return;
        }
        
        if (session.isAttach()) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    if (AttachWizard.getDefault().configured(attachSettings)) {
                        performDoStartImpl(_profilingSettings, attachSettings);
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                if (!configureAttachSettings(false)) start.setPushed(false);
                                else performDoStartImpl(_profilingSettings, attachSettings);
                            }
                        });
                    }
                }
            });
        } else {
            performDoStartImpl(_profilingSettings, null);
        }
    }
    
    private void performDoStartImpl(final ProfilingSettings ps, final AttachSettings as) {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() { if (!session.doStart(ps, as)) start.setPushed(false); }
        });
    }
    
    
    // --- Profile/Attach popup ------------------------------------------------
    
    private void displayPopupImpl() {
        final Set<ProfilerFeature> _features = features.getAvailable();
        final Set<ProfilerFeature> _selected = features.getActivated();
        final List<ToggleButtonMenuItem> _items = new ArrayList();
        
        // --- Features listener ---
        final ProfilerFeatures.Listener listener = new ProfilerFeatures.Listener() {
            void featuresChanged(ProfilerFeature changed) {
                int index = 0;
                for (ProfilerFeature feature : _features) {
                    ToggleButtonMenuItem item = _items.get(index++);
                    if (item == null) item = _items.get(index++);
                    item.setPressed(_selected.contains(feature));
                }
            }
            void settingsChanged(boolean valid) {}
        };
        
        // --- Popup menu ---
        final StayOpenPopupMenu popup = new StayOpenPopupMenu() {
            public void setVisible(boolean visible) {
                if (visible) features.addListener(listener);
                else features.removeListener(listener);
                super.setVisible(visible);
            }
        };
        popup.setLayout(new GridBagLayout());
        if (!UIUtils.isAquaLookAndFeel()) {
            popup.setForceBackground(true);
            Color background = UIUtils.getProfilerResultsBackground();
            popup.setBackground(new Color(background.getRGB())); // JPopupMenu doesn't seem to follow ColorUIResource
        }
        
        // --- Feature items ---
        int lastPosition = -1;
        for (final ProfilerFeature feature : _features) {
            int currentPosition = feature.getPosition();
            if (lastPosition == -1) lastPosition = currentPosition;
            if (currentPosition - lastPosition > 1) _items.add(null);
            lastPosition = currentPosition;
            
            _items.add(new ToggleButtonMenuItem(feature.getName(), feature.getIcon()) {
                {
                    setPressed(_selected.contains(feature));
                }
                protected void fireActionPerformed(ActionEvent e) {
                    features.toggleActivated(feature);
                    if (features.isSingleFeatured() && isPressed())
                        /*if (session.inProgress())*/ popup.setVisible(false);
                }
            });
        }
        
        // --- Other controls ---
        final boolean _project = session.getProject() != null;
//        final boolean _file = session.getFile() != null;
        final boolean _attach = session.isAttach();
//        
        JMenuItem attachProject = null;
        if (_attach) {
            String nameA = _project ? Bundle.ProfilerWindow_setupAttachProject() :
                                      Bundle.ProfilerWindow_setupAttachProcess();
            attachProject = new JMenuItem(nameA) {
                {
                    setEnabled(!session.inProgress());
                }
                protected void fireActionPerformed(ActionEvent event) {
                    configureAttachSettings(true);
                }
            };
        }
        
        JCheckBoxMenuItem singleFeature = new StayOpenPopupMenu.CheckBoxItem(Bundle.ProfilerWindow_multipleFeatures()) {
            { setSelected(!features.isSingleFeatured()); }
            protected void fireItemStateChanged(ItemEvent event) {
                features.setSingleFeatured(!isSelected());
            }
        };
        
        JCheckBoxMenuItem usePPoints = new StayOpenPopupMenu.CheckBoxItem(Bundle.ProfilerWindow_usePPoints()) {
            {
                setSelected(features.getUseProfilingPoints());
                setEnabled(!session.inProgress());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                features.setUseProfilingPoints(isSelected());
            }
        };
        
        // --- Popup crowding ---
        int left = 12;
        int labl = 5;
        int y = 0;
        GridBagConstraints c;
        
        if (_attach) {
            JLabel projectL = new JLabel(Bundle.ProfilerWindow_targetSection(), JLabel.LEADING);
            projectL.setFont(popup.getFont().deriveFont(Font.BOLD));
            c = new GridBagConstraints();
            c.gridy = y++;
            c.insets = new Insets(5, labl, 5, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            popup.add(projectL, c);

            c = new GridBagConstraints();
            c.gridy = y++;
            c.gridx = 0;
            c.insets = new Insets(0, left, 0, 5);
            c.gridwidth = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            popup.add(attachProject, c);
        }
                
        JLabel profileL = new JLabel(Bundle.ProfilerWindow_profileSection(), JLabel.LEADING);
        profileL.setFont(popup.getFont().deriveFont(Font.BOLD));
        c = new GridBagConstraints();
        c.gridy = y++;
        c.insets = new Insets(_attach ? 8 : 5, labl, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        popup.add(profileL, c);
        
        for (ToggleButtonMenuItem item : _items) {
            if (item == null) {
                JPanel p = new JPanel(null);
                p.setOpaque(false);
                c = new GridBagConstraints();
                c.gridy = y++;
                c.insets = new Insets(5, 0, 5, 0);
                c.fill = GridBagConstraints.HORIZONTAL;
                popup.add(p, c);
            } else {
                c = new GridBagConstraints();
                c.gridy = y++;
                c.insets = new Insets(0, left, 0, 5);
                c.fill = GridBagConstraints.HORIZONTAL;
                popup.add(item, c);
            }
        }

        JLabel settingsL = new JLabel(Bundle.ProfilerWindow_settingsSection(), JLabel.LEADING);
        settingsL.setFont(popup.getFont().deriveFont(Font.BOLD));
        c = new GridBagConstraints();
        c.gridy = y++;
        c.insets = new Insets(8, labl, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        popup.add(settingsL, c);

        c = new GridBagConstraints();
        c.gridy = y++;
        c.insets = new Insets(0, left, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        popup.add(singleFeature, c);

        if (_project) {
            c = new GridBagConstraints();
            c.gridy = y++;
            c.insets = new Insets(0, left, 0, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            popup.add(usePPoints, c);
        }
        
        if (plugins.hasPlugins()) {
            JLabel pluginsL = new JLabel(Bundle.ProfilerWindow_pluginsSection(), JLabel.LEADING);
            pluginsL.setFont(popup.getFont().deriveFont(Font.BOLD));
            c = new GridBagConstraints();
            c.gridy = y++;
            c.insets = new Insets(8, labl, 5, 5);
            c.fill = GridBagConstraints.HORIZONTAL;
            popup.add(pluginsL, c);
            
            for (JMenuItem menu : plugins.menuItems()) {
                c = new GridBagConstraints();
                c.gridy = y++;
                c.insets = new Insets(0, left, 0, 5);
                c.fill = GridBagConstraints.HORIZONTAL;
                popup.add(menu, c);
            }
        }

        JPanel footer = new JPanel(null);
        footer.setOpaque(false);
        c = new GridBagConstraints();
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(3, 0, 0, 0);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        popup.add(footer, c);
        
//        MenuElement preselect = null;
//        if (preselectItem != null) {
//            for (Component comp : popup.getComponents()) {
//                if (comp instanceof JMenuItem) {
//                    JMenuItem mi = (JMenuItem)comp;
//                    if (mi.getText().contains(preselectItem)) {
//                        preselect = mi;
//                        break;
//                    }
//                }
//            }
//            preselectItem = null;
//        }
//        
//        if (preselect instanceof JRadioButtonMenuItem)
//            ((JRadioButtonMenuItem)preselect).setSelected(true);
        
        JComponent button = start.isVisible() ? start : configure;
        popup.show(button, 0, button.getHeight());
        
//        if (preselect != null) MenuSelectionManager.defaultManager().setSelectedPath(
//                               new MenuElement[] { popup, preselect });
    }
    
    
    // --- TopComponent --------------------------------------------------------
    
    private final String ID = "ProfilerWindow"; // NOI18N
    
    boolean closing = false;
    
    public boolean canClose() {
        if (closing) return true;
        if (!super.canClose()) return false;
        closing = true;
        closing = session.close();
        return closing;
    }
    
    public void open() {
        WindowManager windowManager = WindowManager.getDefault();
        if (windowManager.findMode(this) == null) { // needs docking
            Mode mode = windowManager.findMode(Bundle.ProfilerWindow_mode());
            if (mode != null) mode.dockInto(this);
        }
        super.open();
    }
    
    protected void componentOpened() {
        super.componentOpened();
        SnapshotsWindow.instance().sessionOpened(session);
    }
    
    protected void componentShowing() {
        super.componentShowing();
        SnapshotsWindow.instance().sessionActivated(session);
    }
    
    protected void componentHidden() {
        super.componentHidden();
        SnapshotsWindow.instance().sessionDeactivated(session);
    }
    
    protected void componentClosed() {
        super.componentOpened();
        SnapshotsWindow.instance().sessionClosed(session);
    }
    
    protected void componentActivated() {
        super.componentActivated();
        if (status != null) status.startSessionLogging();
    }
    
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }
    
    protected String preferredID() {
        return ID;
    }
    
    public HelpCtx getHelpCtx() {
        ProfilerFeature selected = featuresView == null ? null :
                                   featuresView.getSelectedFeature();
        
        JPanel selectedUI = selected == null ? null : selected.getResultsUI();
        if (selectedUI == null && selected != null) selectedUI = selected.getSettingsUI();
        
        String helpCtx = selectedUI == null ? null :
                         (String)selectedUI.getClientProperty("HelpCtx.Key"); // NOI18N
        if (helpCtx == null) helpCtx = "ProfileWindow.HelpCtx"; // NOI18N
        
        return new HelpCtx(helpCtx);
    }
    
    
    // --- Private classes -----------------------------------------------------
    
    private static final class SettingsPresenter extends JToggleButton
                                                 implements ComponentListener {
        
        private JPanel settings;
        
        SettingsPresenter() {
            super(Icons.getIcon(GeneralIcons.SETTINGS));
            setToolTipText(Bundle.ProfilerWindow_settings());
            updateVisibility(false);
        }
        
        void setFeature(ProfilerFeature feature) {
            if (settings != null) settings.removeComponentListener(this);
            settings = feature == null ? null : feature.getSettingsUI();
            updateVisibility(false);
            if (settings != null) settings.addComponentListener(this);
        }
        
        protected void fireActionPerformed(ActionEvent e) {
            if (settings != null) {
                settings.setVisible(isSelected());
                settings.getParent().setVisible(isSelected());
            }
        }
        
        void cleanup() {
            settings.removeComponentListener(this);
        }
        
        private void updateVisibility(boolean parent) {
            setVisible(settings != null);
            setSelected(settings != null && settings.isVisible());
            if (parent) settings.getParent().setVisible(settings.isVisible());
        }
        
        public void componentShown(ComponentEvent e) { updateVisibility(true); }

        public void componentHidden(ComponentEvent e) { updateVisibility(true); }
        
        public void componentResized(ComponentEvent e) {}
        
        public void componentMoved(ComponentEvent e) {}
        
    }
    
}
