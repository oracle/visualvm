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

package org.netbeans.modules.profiler.v2.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.features.ProfilerFeature;
import org.netbeans.modules.profiler.v2.features.ProfilerFeatures;
import org.netbeans.modules.profiler.v2.session.ProjectSession;
import org.netbeans.modules.profiler.v2.ui.components.DropdownButton;
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
    "ProfilerWindow_profile=Profile",
    "ProfilerWindow_terminateCaption=Terminate Profiling Session",
    "ProfilerWindow_terminateMsg=Terminate profiling session?",
    "ProfilerWindow_loadingSession=Loading session features...",
    "ProfilerWindow_createCustom=Create custom...",
    "#NOI18N",
    "ProfilerWindow_mode=editor"
})
public final class ProfilerWindow extends ProfilerTopComponent {
    
    // --- Static --------------------------------------------------------------
    
    private static Map<ProjectSession, Reference<ProfilerWindow>> MAP;
    
    public static ProfilerWindow forSession(ProjectSession session) {
        // To be called in EDT only - synchronization & UI creation
        assert SwingUtilities.isEventDispatchThread();
        
        if (MAP == null) {
            MAP = new HashMap();
        } else {
            // Remove once multiple profiling sessions are supported
            for (ProjectSession otherSession : MAP.keySet()) {
                if (!session.equals(otherSession)) {
                    Reference<ProfilerWindow> reference = MAP.get(otherSession);
                    ProfilerWindow window = reference == null ? null : reference.get();
                    if (window != null && !window.close()) return window;
                    else MAP.remove(otherSession);
                }
            }
        }
        
        Reference<ProfilerWindow> reference = MAP.get(session);
        ProfilerWindow window = reference == null ? null : reference.get();
        
        if (window == null) {
            window = new ProfilerWindow(session);
            MAP.put(session, new WeakReference(window));
        }
        
        return window;
    }
    
    
    // --- Instance ------------------------------------------------------------
    
    private final ProjectSession session;
    
    private ProfilerWindow(ProjectSession session) {
        this.session = session;
        
        Lookup.Provider project = session.getProject();
        String windowName = project == null ? Bundle.ProfilerWindow_profile() :
                                       ProjectUtilities.getDisplayName(project);
        setDisplayName(windowName);
        updateIcon();
        
        setFocusable(true);
        setRequestFocusEnabled(true);
        
        createUI();
    }
    
    
    private ProjectSession getSession() {
        return session;
    }
    
    
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
    
    protected void componentClosed() {
        MAP.remove(getSession());
        super.componentClosed();
    }
    
    
    // --- Implementation ------------------------------------------------------
    
    private JPanel topContainer;
    private ProfilerToolbar toolbar;
    private ProfilerToolbar featureToolbar;
    
    private DropdownButton start;
    private JButton stop;
    private SettingsPresenter settingsButton;
    private JPanel settingsUI;
    private JPanel resultsUI;
    
    private AttachSettings attachSettings;
    
    private ChangeListener listener;
    
    
    private void createUI() {
        setLayout(new BorderLayout(0, 0));
        setFocusable(false);
        
        topContainer = new JPanel(new BorderLayout(0, 0));
        topContainer.setOpaque(false);
        add(topContainer, BorderLayout.NORTH);
        
        toolbar = ProfilerToolbar.create(true);
        topContainer.add(toolbar.getComponent(), BorderLayout.NORTH);
        
        final JLabel loading = new JLabel(Bundle.ProfilerWindow_loadingSession());
        loading.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        toolbar.add(loading);
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                final ProfilerFeature[] modes = ProfilerFeatures.getFeatures(session);
                final ProfilerFeature selected = ProfilerFeatures.getSelectedFeatures(modes, session);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        toolbar.remove(loading);
                        popupulateUI(modes, selected);
                    }
                });
            }
        });
    }
    
    private void popupulateUI(final ProfilerFeature[] features, final ProfilerFeature selected) {
        start = new DropdownButton(selected.getName(), Icons.getIcon(GeneralIcons.START), true) {
            protected void populatePopup(JPopupMenu popup) { populatePopupImpl(popup); }
            protected void performAction() { performStartImpl(); }
        };
        toolbar.add(start);
        
        stop = new JButton(Icons.getIcon(GeneralIcons.STOP)) {
            protected void fireActionPerformed(ActionEvent e) { performStopImpl(); }
        };
        toolbar.add(stop);
        
        toolbar.addFiller();
        
        setCurrentFeature(selected);
        setAvailableFeatures(features);
        
        session.addListener(new ProjectSession.Listener() {
            public void stateChanged(ProjectSession.State oldState, ProjectSession.State newState) {
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
    
    private void performStartImpl() {
        start.setPushed(true);
        session.start(currentFeature.getSettings(), attachSettings);
    }
    
    private void performStopImpl() {
        stop.setEnabled(false);
        session.terminate();
    }
    
    private void populatePopupImpl(JPopupMenu popup) {        
        for (final ProfilerFeature mode : getAvailableFeatures()) {
            if (mode == null) popup.addSeparator();
            else popup.add(new JRadioButtonMenuItem(mode.getName(), mode == getCurrentFeature()) {
                protected void fireActionPerformed(ActionEvent e) { setCurrentFeature(mode); }
            });
        }
        
        if (popup.getComponentCount() > 0) popup.addSeparator();
        popup.add(new JMenuItem(Bundle.ProfilerWindow_createCustom()));
    }
    
    
    private void updateButtons() {
        ProjectSession.State state = session.getState();
        start.setPushed(state != ProjectSession.State.INACTIVE);
//        start.setEnabled(state == ProjectSession.State.INACTIVE);
        stop.setEnabled(state == ProjectSession.State.RUNNING);
    }
    
    
    private ProfilerFeature currentFeature;
    private ProfilerFeature[] availableFeatures;
    
    private void setCurrentFeature(ProfilerFeature feature) {
        if (currentFeature == feature) return;
        
        if (currentFeature != null) currentFeature.detachedFromSession(session);
        if (listener != null && currentFeature != null)
            currentFeature.removeChangeListener(listener);
        
        detachResultsUI();
        detachToolbar();
        detachSettingsUI();
        
        currentFeature = feature;
        start.setText(currentFeature.getName());
        currentFeature.attachedToSession(session);
        
        attachSettingsUI();
        attachToolbar();
        attachResultsUI();
        
        revalidate();
        repaint();
        
        if (session.inProgress()) session.modify(currentFeature.getSettings());
        
        if (listener == null) listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (session.inProgress()) session.modify(currentFeature.getSettings());
            }
        };
        currentFeature.addChangeListener(listener);
    }
    
    private ProfilerFeature getCurrentFeature() {
        return currentFeature;
    }
    
    private void setAvailableFeatures(ProfilerFeature[] features) {
        availableFeatures = features;
    }
    
    private ProfilerFeature[] getAvailableFeatures() {
        return availableFeatures;
    }
    
    
    // --- Toolbar -------------------------------------------------------------
    
    public void attachToolbar() {
        if (settingsButton != null) toolbar.add(settingsButton, 3);
        
        featureToolbar = currentFeature.getToolbar();
        if (featureToolbar != null) toolbar.add(featureToolbar, 2);
    }
    
    private void detachToolbar() {
        if (featureToolbar != null) {
            toolbar.remove(featureToolbar);
            featureToolbar = null;
        }
        
        if (settingsButton != null) {
            settingsButton.cleanup();
            toolbar.remove(settingsButton);
        }
    }
    
    private void attachSettingsUI() {
        JPanel settings = currentFeature.getSettingsUI();
        if (settings != null) {
            settingsUI = new JPanel(new BorderLayout(0, 0));
            Color orig = settingsUI.getBackground();
            settingsUI.setOpaque(true);
            settingsUI.setBackground(UIUtils.getProfilerResultsBackground());
            settingsUI.add(settings, BorderLayout.CENTER);
            settingsUI.add(UIUtils.createHorizontalLine(orig), BorderLayout.SOUTH);
            settingsUI.setVisible(settings.isVisible());
            topContainer.add(settingsUI, BorderLayout.SOUTH);
            
            settingsButton = new SettingsPresenter(settings, settingsUI);
        }
    }
    
    private void detachSettingsUI() {
        if (settingsUI != null) {
            topContainer.remove(settingsUI);
            settingsUI = null;
            
            settingsButton = null;
        }
    }
    
    private void attachResultsUI() {
        resultsUI = currentFeature.getResultsUI();
        add(resultsUI, BorderLayout.CENTER);
    }
    
    private void detachResultsUI() {
        if (resultsUI != null) {
            remove(resultsUI);
            resultsUI = null;
        }
    }
    
    
    // --- TopComponent --------------------------------------------------------
    
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
        
        private final JPanel settings;
        private final JPanel container;
        
        SettingsPresenter(JPanel settings, JPanel container) {
            super(Icons.getIcon(GeneralIcons.SETTINGS));
            
            this.settings = settings;
            this.container = container;
            
            settings.addComponentListener(this);
            updateVisibility(settings.isVisible());
        }
        
        protected void fireActionPerformed(ActionEvent e) {
            updateVisibility(isSelected());
        }
        
        void cleanup() {
            settings.removeComponentListener(this);
        }
        
        private void updateVisibility(boolean visible) {
            setSelected(visible);
            settings.setVisible(visible);
            container.setVisible(visible);
            container.revalidate();
            container.repaint();
        }
        
        public void componentShown(ComponentEvent e) { updateVisibility(true); }

        public void componentHidden(ComponentEvent e) { updateVisibility(false); }
        
        public void componentResized(ComponentEvent e) {}
        
        public void componentMoved(ComponentEvent e) {}
        
    }
    
}
