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
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ui.FeaturesView;
import org.netbeans.modules.profiler.v2.ui.ProfilerStatus;
import org.netbeans.modules.profiler.v2.ui.StayOpenPopupMenu;
import org.netbeans.modules.profiler.v2.ui.ToggleButtonMenuItem;
import org.netbeans.modules.profiler.v2.ui.WelcomePanel;
import org.netbeans.modules.profiler.v2.ui.DropdownButton;
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
    "ProfilerWindow_caption=Profile",
    "ProfilerWindow_profile=Profile",
    "ProfilerWindow_attach=Attach",
    "ProfilerWindow_terminateCaption=Terminate Profiling Session",
    "ProfilerWindow_terminateMsg=Terminate profiling session?",
    "ProfilerWindow_loadingSession=Loading session features...",
    "ProfilerWindow_createCustom=Create custom...",
    "#NOI18N",
    "ProfilerWindow_mode=editor"
})
class ProfilerWindow extends ProfilerTopComponent {    
    
    // --- Constructor ---------------------------------------------------------
    
    private final ProfilerSession session;
    
    ProfilerWindow(ProfilerSession session) {
        this.session = session;
        attachSettings = session.getAttachSettings();
        
        Lookup.Provider project = session.getProject();
        String windowName = project == null ? Bundle.ProfilerWindow_caption() :
                                       ProjectUtilities.getDisplayName(project);
        setDisplayName(windowName);
        updateIcon();
        
        createUI();
    }
    
    // --- API -----------------------------------------------------------------
    
    void updateSession() {
        
    }
    
    void selectFeature(ProfilerFeature feature) {
        if (featuresView != null) featuresView.selectFeature(feature);
    }
    
    // --- Implementation ------------------------------------------------------
    
    private ProfilerFeatures features;
    
    private ProfilerToolbar toolbar;
    private ProfilerToolbar featureToolbar;
    private ProfilerToolbar statusBar;
    private JPanel container;
    private FeaturesView featuresView;
    
    private DropdownButton start;
    private JButton stop;
    private SettingsPresenter settingsButton;
    
    private AttachSettings attachSettings;
    
    
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
                features = session.getFeatures();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toolbar.remove(loading);
                        popupulateUI();
                    }
                });
            }
        });
    }
    
    private void popupulateUI() {  
        String _name = attachSettings != null ? Bundle.ProfilerWindow_attach() :
                                                Bundle.ProfilerWindow_profile();
        start = new DropdownButton(_name, Icons.getIcon(GeneralIcons.START), true) {
            public void displayPopup() { displayPopupImpl(); }
            protected void performAction() { performStartImpl(); }
        };
        toolbar.add(start);
        
        stop = new JButton(Icons.getIcon(GeneralIcons.STOP)) {
            protected void fireActionPerformed(ActionEvent e) { performStopImpl(); }
        };
        toolbar.add(stop);
        
        statusBar = new ProfilerStatus(session).getToolbar();
        statusBar.getComponent().setVisible(false); // TODO: read last state
        toolbar.add(statusBar);
        
        toolbar.addFiller();
        
        settingsButton = new SettingsPresenter();
        toolbar.add(settingsButton);
        
        container = new JPanel(new BorderLayout(0, 0));
        add(container, BorderLayout.CENTER);
        
        JPanel welcomePanel = new WelcomePanel(features.getAvailable()) {
            public void showDropdown() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { start.clickPopup(); }
                });
            }
            
        };
        featuresView = new FeaturesView(welcomePanel);
        container.add(featuresView, BorderLayout.CENTER);
        
        features.addListener(new ProfilerFeatures.Listener() {
            void featuresChanged(ProfilerFeature changed) { updateFeatures(changed); }
            void settingsChanged(boolean valid) { updateSettings(valid); }
        });
        updateSettings(features.settingsValid());
        
        featuresView.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (featureToolbar != null) toolbar.remove(featureToolbar);
                ProfilerFeature selected = featuresView.getSelectedFeature();
                featureToolbar = selected == null ? null : selected.getToolbar();
                if (featureToolbar != null) toolbar.add(featureToolbar, 3);
                settingsButton.setFeature(selected);
                doLayout();
                repaint();
            }
        });
        
        session.addListener(new SimpleProfilingStateAdapter() {
            public void update() {
                updateIcon();
                updateButtons();
            }
        });
        updateButtons();
    }
    
    
    private void updateIcon() {
        if (session.inProgress()) setIcon(Icons.getImage(ProfilerIcons.PROFILE_RUNNING));
        else setIcon(Icons.getImage(ProfilerIcons.PROFILE_INACTIVE));
    }
    
    private void updateButtons() {
        int state = session.getState();
        start.setPushed(state != NetBeansProfiler.PROFILING_INACTIVE);
        stop.setEnabled(state == NetBeansProfiler.PROFILING_RUNNING);
    }
    
    
    private void updateFeatures(ProfilerFeature changed) {
        // TODO: optimize!
        // TODO: restore focused component if possible
        ProfilerFeature restore = featuresView.getSelectedFeature();
        featuresView.removeFeatures();
        Set<ProfilerFeature> selected = features.getSelected();
        for (ProfilerFeature feature : selected) featuresView.addFeature(feature);
        if (selected.contains(changed)) featuresView.selectFeature(changed);
        else featuresView.selectFeature(restore);
        featuresView.repaint();
    }
    
    private void updateSettings(boolean valid) {
        start.setEnabled(valid);
        if (session.inProgress()) session.doModify(__currentSettings());
    }
    
    private ProfilingSettings __currentSettings() {
        ProfilingSettings settings = features.getSettings();
        System.err.println();
        System.err.println("=================================================");
        System.err.print(settings == null ? "no settings" : settings.debug());
        System.err.println("=================================================");
        System.err.println();
        return settings;
    }
    
    private void performStartImpl() {
        start.setPushed(true);
        session.doStart(__currentSettings(), attachSettings);
    }
    
    private void performStopImpl() {
        stop.setEnabled(false);
        session.terminate();
    }
    
    
    // --- Profile/Attach popup ------------------------------------------------
    
    private void displayPopupImpl() {
        final Set<ProfilerFeature> _features = features.getAvailable();
        final Set<ProfilerFeature> _selected = features.getSelected();
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
                    features.toggleFeatureSelection(feature);
                    if (features.isSingleFeatureSelection() && isPressed())
                        /*if (session.inProgress())*/ popup.setVisible(false);
                }
            });
        }
        
        // --- Other controls ---
        JCheckBoxMenuItem singleFeature = new StayOpenPopupMenu.CheckBoxItem("Profile multiple features") {
            { setSelected(!features.isSingleFeatureSelection()); }
            protected void fireItemStateChanged(ItemEvent event) {
                features.setSingleFeatureSelection(!isSelected());
            }
        };
        
        JCheckBoxMenuItem showStatus = new StayOpenPopupMenu.CheckBoxItem("Show profiling status") {
            { setSelected(statusBar.getComponent().isVisible()); }
            protected void fireItemStateChanged(ItemEvent event) {
                statusBar.getComponent().setVisible(isSelected());
            }
        };
        
        JCheckBoxMenuItem usePPoints = new StayOpenPopupMenu.CheckBoxItem("Use defined Profiling Points") {
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
                
        JLabel profileL = new JLabel("Profile:", JLabel.LEADING);
        profileL.setFont(popup.getFont().deriveFont(Font.BOLD));
        c = new GridBagConstraints();
        c.gridy = y++;
        c.insets = new Insets(5, labl, 5, 5);
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

        JLabel settingsL = new JLabel("Settings:", JLabel.LEADING);
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

        c = new GridBagConstraints();
        c.gridy = y++;
        c.insets = new Insets(0, left, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        popup.add(showStatus, c);

        JLabel ppointsL = new JLabel("Profiling Points:", JLabel.LEADING);
        ppointsL.setFont(popup.getFont().deriveFont(Font.BOLD));
        c = new GridBagConstraints();
        c.gridy = y++;
        c.insets = new Insets(8, labl, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        popup.add(ppointsL, c);

        c = new GridBagConstraints();
        c.gridy = y++;
        c.insets = new Insets(0, left, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        popup.add(usePPoints, c);

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
        
        popup.show(start, 0, start.getHeight());
    }
    
    
    // --- TopComponent --------------------------------------------------------
    
    public boolean canClose() {
        if (!super.canClose()) return false;
        if (!session.inProgress()) return true;
        
        if (ProfilerDialogs.displayConfirmation(Bundle.ProfilerWindow_terminateMsg(),
                                                Bundle.ProfilerWindow_terminateCaption())) {
            session.terminate();
            return true;
        } else {
            return false;
        }
    }
    
    public void open() {
        WindowManager windowManager = WindowManager.getDefault();
        if (windowManager.findMode(this) == null) { // needs docking
            Mode mode = windowManager.findMode(Bundle.ProfilerWindow_mode());
            if (mode != null) mode.dockInto(this);
        }
        super.open();
    }
    
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }
    
    protected String preferredID() {
        return this.getClass().getName();
    }
    
    
    // --- Private classes -----------------------------------------------------
    
    private static final class SettingsPresenter extends JToggleButton
                                                 implements ComponentListener {
        
        private JPanel settings;
        
        SettingsPresenter() {
            super(Icons.getIcon(GeneralIcons.SETTINGS));
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
