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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.ProfilingStateAdapter;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedRadioButton;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.v2.ui.ProjectSelector;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Mnemonics;
import org.openide.modules.OnStop;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProfilerSessions_actionNotSupported=Action not supported by the current profiling session.",
    "ProfilerSessions_loadingFeatures=Loading project features...",
    "ProfilerSessions_selectProject=&Select the project to profile:",
    "ProfilerSessions_selectFeature=Select Feature",
    "ProfilerSessions_selectHandlingFeature=Select the feature to handle the action:",
    "ProfilerSessions_selectProjectAndFeature=Select Project and Feature",
    "ProfilerSessions_finishingSession=Finishing previous session...",
    "ProfilerSessions_finishSessionCaption=Profile",
    "ProfilerSessions_cancel=Cancel",
    "ProfilerSessions_profileProject=&Profile project",
    "ProfilerSessions_attachProject=&Attach to project"
})
final class ProfilerSessions {
    
    // --- Find and configure session ------------------------------------------
    
    static void configure(final ProfilerSession session, final Lookup context, final String actionName) {
        final ProfilerFeatures _features = session.getFeatures();
        final Set<ProfilerFeature> compatA = ProfilerFeatures.getCompatible(
                                             _features.getAvailable(), context);
        if (compatA.isEmpty()) {
            // TODO: might offer creating a new profiling session if the current is not in progress
            ProfilerDialogs.displayInfo(Bundle.ProfilerSessions_actionNotSupported());
        } else {
            // Resolving selected features in only supported in EDT
            UIUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    Set<ProfilerFeature> compatS = ProfilerFeatures.getCompatible(
                                                   _features.getActivated(), context);

                    ProfilerFeature feature;
                    if (compatS.size() == 1) {
                        // Exactly one selected feature handles the action
                        feature = compatS.iterator().next();
                    } else if (!compatS.isEmpty()) {
                        // Multiple selected features handle the action
                        feature = selectFeature(compatS, actionName);
                    } else if (compatA.size() == 1) {
                        // Exactly one available feature handles the action
                        feature = compatA.iterator().next();
                    } else {
                        // Multiple available features handle the action
                        feature = selectFeature(compatA, actionName);
                    }

                    if (feature != null) {
                        _features.activateFeature(feature);
                        feature.configure(context);
                        session.selectFeature(feature);
                        session.open();
                    }
                }
            });
        }
    }
    
    static void createAndConfigure(final Lookup context, Lookup.Provider project, final String actionName) {
        if (project == null) project = ProjectUtilities.getMainProject();
        final Lookup.Provider _project = project;
        
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                UI ui = UI.createAndConfigure(context, _project);

                HelpCtx helpCtx = new HelpCtx("SelectFeatureDialog.HelpCtx"); // NOI18N
                String caption = actionName == null ? Bundle.ProfilerSessions_selectProjectAndFeature() : actionName;
                DialogDescriptor dd = new DialogDescriptor(ui, caption, true, new Object[]
                                                         { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION },
                                                           DialogDescriptor.OK_OPTION, DialogDescriptor.BOTTOM_ALIGN,
                                                           helpCtx, null);
                Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                d.setVisible(true);
                
                final ProfilerSession session = ui.selectedSession();

                if (dd.getValue() == DialogDescriptor.OK_OPTION) {
                    final ProfilerFeature feature = ui.selectedFeature();

                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            final ProfilerFeatures features = session.getFeatures();
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    features.activateFeature(feature);
                                    feature.configure(context);
                                    session.selectFeature(feature);
                                    session.open();
                                }
                            });
                        }
                    });
                } else {
                    if (session != null) session.close();
                }
                
                dd.setMessage(null); // Do not leak because of WindowsPopupMenuUI.mnemonicListener
                
            }
        });
    }
    
    private static ProfilerFeature selectFeature(Set<ProfilerFeature> features, String actionName) {
        UI ui = UI.selectFeature(features);

        HelpCtx helpCtx = new HelpCtx("SelectFeatureDialog.HelpCtx"); // NOI18N // TODO: should have a special one?
        String caption = actionName == null ? Bundle.ProfilerSessions_selectFeature() : actionName;
        DialogDescriptor dd = new DialogDescriptor(ui, caption, true, new Object[]
                                                 { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION },
                                                   DialogDescriptor.OK_OPTION, DialogDescriptor.BOTTOM_ALIGN,
                                                   helpCtx, null);
        Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);
        
        ProfilerFeature ret = dd.getValue() == DialogDescriptor.OK_OPTION ? ui.selectedFeature() : null;
        
        dd.setMessage(null); // Do not leak because of WindowsPopupMenuUI.mnemonicListener
        
        return ret;
    }
    
    
    private static class UI extends JPanel {
        
        private ProfilerFeature selectedFeature;
        private ProfilerSession selectedSession;
        
        static UI selectFeature(Set<ProfilerFeature> features) {
            return new UI(features);
        }
        
        static UI createAndConfigure(Lookup context, Lookup.Provider project) {
            return new UI(context, project);
        }
        
        
        ProfilerFeature selectedFeature() {
            return selectedFeature;
        }
        
        ProfilerSession selectedSession() {
            if (selectedSession != null)
                selectedSession.setAttach(attachProject.isSelected());
            return selectedSession;
        }
        
            
        UI(Set<ProfilerFeature> features) {
            super(new GridBagLayout());
            
            int y = 0;
            GridBagConstraints c;
            
            JLabel l = new JLabel(Bundle.ProfilerSessions_selectHandlingFeature());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(10, 10, 10, 10);
            add(l, c);
            
            ButtonGroup rbg = new ButtonGroup();
            for (final ProfilerFeature f : features) {
                JRadioButton rb = new JExtendedRadioButton(f.getName(), f.getIcon()) {
                    protected void fireItemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED)
                            selectedFeature = f;
                    }
                };
                rbg.add(rb);
                if (rbg.getSelection() == null) rb.setSelected(true);
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y++;
                c.anchor = GridBagConstraints.WEST;
                c.insets = new Insets(0, 20, 0, 10);
                add(rb, c);
            }
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(15, 0, 0, 300);
            add(UIUtils.createFillerPanel(), c);
        }
        
        private JPanel contents;
        private JRadioButton profileProject;
        private JRadioButton attachProject;
        private void repaintContents() {
            contents.invalidate();
            contents.revalidate();
            contents.repaint();
        }
        
        UI(final Lookup context, final Lookup.Provider project) {
            super(new GridBagLayout());
            
            int y = 0;
            GridBagConstraints c;
            
            JLabel l = new JLabel();
            Mnemonics.setLocalizedText(l, Bundle.ProfilerSessions_selectProject());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(10, 10, 10, 10);
            add(l, c);
            
            contents = new JPanel(new GridBagLayout());
            
            ProjectSelector.Populator populator = new ProjectSelector.Populator() {
                protected Lookup.Provider initialProject() { return project; }
            };
            ProjectSelector selector = new ProjectSelector(populator) {
                protected void selectionChanged() {
                    Lookup.Provider project = getProject();
                    refreshProfileAttach(project);
                    refreshFeatures(context, project);
                }
            };
            l.setLabelFor(selector);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 20, 4, 10);
            add(selector, c);
            
            ButtonGroup bg = new ButtonGroup();
            
            profileProject = new JRadioButton();
            bg.add(profileProject);
            profileProject.setSelected(true);
            Mnemonics.setLocalizedText(profileProject, Bundle.ProfilerSessions_profileProject());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.weightx = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 20, 10, 5);
            add(profileProject, c);
            
            attachProject = new JRadioButton();
            bg.add(attachProject);
            Mnemonics.setLocalizedText(attachProject, Bundle.ProfilerSessions_attachProject());
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = y;
            c.weightx = 0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(0, 0, 10, 10);
            add(attachProject, c);
            
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = y++;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 10, 0);
            add(UIUtils.createFillerPanel(), c);
            
            JLabel ll = new JLabel();
            Mnemonics.setLocalizedText(ll, Bundle.ProfilerSessions_selectHandlingFeature());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(10, 10, 10, 10);
            add(ll, c);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(0, 0, 0, 0);
            add(contents, c);
            
            JRadioButton ref = new JExtendedRadioButton("XXX"); // NOI18N
            final int refHeight = ref.getPreferredSize().height;
            JPanel filler = new JPanel(null) {
                public Dimension getPreferredSize() {
                    return new Dimension(300, refHeight * 2);
                }
            };
            filler.setOpaque(false);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(15, 0, 0, 0);
            add(filler, c);
            
            Lookup.Provider _project = selector.getProject();
            refreshProfileAttach(_project);
            refreshFeatures(context, _project);
        }
        
        private void refreshProfileAttach(Lookup.Provider project) {
            boolean fromExternal = !profileProject.isEnabled();
            profileProject.setEnabled(project != null);
            if (project == null) attachProject.setSelected(true);
            else if (fromExternal) profileProject.setSelected(true);
        }
        
        private void refreshFeatures(final Lookup context, final Lookup.Provider project) {
            contents.removeAll();
            
            JLabel l = new JLabel(Bundle.ProfilerSessions_loadingFeatures(), JLabel.CENTER);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(0, 20, 0, 10);
            contents.add(l, c);
            
            repaintContents();
            
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    
                    if (selectedSession != null) selectedSession.close();
                    
                    Lookup projectContext = project == null ? Lookup.EMPTY :
                                            Lookups.fixed(project);
                    selectedSession = ProfilerSession.forContext(projectContext);
                    final ProfilerFeatures features = selectedSession.getFeatures();
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            
                            contents.removeAll();
                            
                            int y = 0;
                            GridBagConstraints c;
                            
                            ButtonGroup rbg = new ButtonGroup();
                            for (final ProfilerFeature f : features.getAvailable()) {
                                if (f.supportsConfiguration(context)) {
                                    JRadioButton rb = new JExtendedRadioButton(f.getName(), f.getIcon()) {
                                        protected void fireItemStateChanged(ItemEvent e) {
                                            if (e.getStateChange() == ItemEvent.SELECTED)
                                                selectedFeature = f;
                                        }
                                    };
                                    rbg.add(rb);
                                    if (rbg.getSelection() == null) rb.setSelected(true);
                                    c = new GridBagConstraints();
                                    c.gridx = 0;
                                    c.gridy = y++;
                                    c.anchor = GridBagConstraints.WEST;
                                    c.insets = new Insets(0, 20, 0, 10);
                                    contents.add(rb, c);
                                }
                            }
                            
                            repaintContents();
                            
                        }
                    });
                    
                }
            });
        }
        
    }
    
    
    // --- Wait for profiler ---------------------------------------------------
    
    private static final int MIN_WAIT_WIDTH = 350;
    private static final int ENABLE_CANCEL_MS = 5000;
    private static volatile boolean waitingCancelled;
    
    
    static boolean waitForProfiler() {
        final Profiler profiler = Profiler.getDefault();
        if (profiler.getProfilingState() == Profiler.PROFILING_INACTIVE) return true;
        
        if (SwingUtilities.isEventDispatchThread()) {
            waitingCancelled = blockingWaitDialog(profiler, null);
        } else {
            final Object lock = new Object();
            synchronized(lock) { 
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { waitingCancelled = blockingWaitDialog(profiler, lock); }
                });
                try { lock.wait(); }
                catch (InterruptedException ex) {}
            }
        }
        
        return !waitingCancelled;
    }
    
    private static boolean blockingWaitDialog(Profiler profiler, Object lock) {
        try {
            if (profiler.getProfilingState() == Profiler.PROFILING_INACTIVE) return false;
            
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.setBorder(new EmptyBorder(15, 15, 15, 10));
            panel.add(new JLabel(Bundle.ProfilerSessions_finishingSession()), BorderLayout.NORTH);

            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);
            panel.add(progress, BorderLayout.SOUTH);
            
            Dimension ps = panel.getPreferredSize();
            panel.setPreferredSize(new Dimension(Math.max(ps.width, MIN_WAIT_WIDTH), ps.height));
            
            final JButton cancel = new JButton(Bundle.ProfilerSessions_cancel());
            cancel.setVisible(false);

            DialogDescriptor dd = new DialogDescriptor(panel, Bundle.ProfilerSessions_finishSessionCaption(),
                                      true, new Object[] { cancel }, null,
                                      DialogDescriptor.DEFAULT_ALIGN, null, null);
            final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
            final JDialog jd = d instanceof JDialog ? (JDialog)d : null;

            final ProfilingStateListener listener = new ProfilingStateAdapter() {
                public void profilingStateChanged(ProfilingStateEvent e) {
                    if (e.getNewState() == Profiler.PROFILING_INACTIVE) { d.setVisible(false); }
                }
            };
            profiler.addProfilingStateListener(listener);
            
            int closeOp = -1;
            if (jd != null) {
                closeOp = jd.getDefaultCloseOperation();
                jd.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            }
            
            final int _closeOp = closeOp;
            Timer timer = new Timer(ENABLE_CANCEL_MS, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (jd != null) jd.setDefaultCloseOperation(_closeOp);
                    cancel.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) { d.setVisible(false); }
                    });
                    cancel.setVisible(true);
                    d.pack();
                }
            });
            timer.setRepeats(false);
            timer.start();
            
            d.setLocationRelativeTo(WindowManager.getDefault().getMainWindow());
            d.setVisible(true);
            
            profiler.removeProfilingStateListener(listener);
            
            return dd.getValue() != null;
        } finally {
            if (lock != null) synchronized(lock) { lock.notifyAll(); }
        }
    }
    
    
    // --- Stop action ---------------------------------------------------------
    
    @NbBundle.Messages({
        "LBL_StopAction=Fini&sh Profiler Session",
        "HINT_StopAction=Finish profiler session (terminate profiled application)",
        "HINT_DetachAction=Finish profiler session (detach from profiled application)"
    })
    public static final class StopAction extends AbstractAction {
        
        // --- Singleton -------------------------------------------------------
        
        private static final class Singleton {
            final private static StopAction INSTANCE = new StopAction();
        }
        @ActionID(category="Profile", id="org.netbeans.modules.profiler.v2.ProfilerSessions.StopAction") // NOI18N
        @ActionRegistration(displayName="#LBL_StopAction", lazy=false) // NOI18N
        @ActionReferences({
            @ActionReference(path="Menu/Profile", position=300, separatorAfter=400), // NOI18N
            @ActionReference(path="Shortcuts", name="S-F2") // NOI18N
        })
        public static StopAction getInstance() { return Singleton.INSTANCE; }
        
        
        // --- Implementation --------------------------------------------------
        
        private ProfilerSession session;
        
        private final ProfilingStateListener listener = new SimpleProfilingStateAdapter() {
            protected void update() { updateState(); }
        };
        
        public void actionPerformed(ActionEvent e) {
            if (session != null) {
                setEnabled(false);
                final ProfilerSession sessionF = session;
        
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { if (!sessionF.doStop()) setEnabled(true); }
                });
            }
        }
        
        
        void setSession(final ProfilerSession _session) {
            if (session != null) session.removeListener(listener);
            session = _session;
            if (session != null) session.addListener(listener);

            putValue(SHORT_DESCRIPTION, session != null && session.isAttach() ?
                     Bundle.HINT_DetachAction() : Bundle.HINT_StopAction());
            updateState();
        }
        
        private void updateState() {
            setEnabled(session != null && session.getState() == Profiler.PROFILING_RUNNING);
        }

        private StopAction() {
            putValue(NAME, Bundle.LBL_StopAction());
            putValue(SHORT_DESCRIPTION, Bundle.HINT_StopAction());
            putValue(SMALL_ICON, Icons.getIcon(GeneralIcons.STOP));
            putValue("iconBase", Icons.getResource(GeneralIcons.STOP)); // NOI18N
            
            updateState();
        }
        
    }
    
    
    // --- Persist session -----------------------------------------------------
    
    @OnStop
    public static final class ExitHandler implements Runnable {
        
        public void run() {
            ProfilerSession current = ProfilerSession.currentSession();
            if (current != null) current.persistStorage(true);
        }
        
    }
    
}
