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

package org.netbeans.modules.profiler;

import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.threads.ThreadsDetailsPanel;
import org.netbeans.lib.profiler.ui.threads.ThreadsPanel;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.ui.threads.ThreadsTablePanel;


/** An IDE TopComponent to display profiling results.
 *
 * TODO: I18N, update names of existing keys
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public final class ThreadsWindow extends TopComponent implements ProfilingStateListener, ActionListener, ChangeListener,
                                                                 SaveViewAction.ViewProvider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String THREADS_WINDOW_NAME = NbBundle.getMessage(ThreadsWindow.class, "ThreadsWindow_ThreadsWindowName"); // NOI18N
    private static final String THREADS_TIMELINE_TAB_NAME = NbBundle.getMessage(ThreadsWindow.class,
                                                                                "ThreadsWindow_ThreadsTimelineTabName"); // NOI18N
    private static final String THREADS_TABLE_TAB_NAME = NbBundle.getMessage(ThreadsWindow.class,
                                                                                "ThreadsWindow_ThreadsTableTabName"); // NOI18N
    private static final String THREADS_DETAILS_TAB_NAME = NbBundle.getMessage(ThreadsWindow.class,
                                                                               "ThreadsWindow_ThreadsDetailsTabName"); // NOI18N
    private static final String THREADS_TIMELINE_TAB_DESCR = NbBundle.getMessage(ThreadsWindow.class,
                                                                                 "ThreadsWindow_ThreadsTimelineTabDescr"); // NOI18N
    private static final String THREADS_TABLE_TAB_DESCR = NbBundle.getMessage(ThreadsWindow.class,
                                                                                 "ThreadsWindow_ThreadsTableTabDescr"); // NOI18N
    private static final String THREADS_DETAILS_TAB_DESCR = NbBundle.getMessage(ThreadsWindow.class,
                                                                                "ThreadsWindow_ThreadsDetailsTabDescr"); // NOI18N
    private static final String THREADS_ACCESS_DESCR = NbBundle.getMessage(ThreadsWindow.class, "ThreadsWindow_ThreadsAccessDescr"); // NOI18N
                                                                                                                                     // -----
    private static final String HELP_CTX_KEY = "ThreadsWindow.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static ThreadsWindow defaultInstance;
    private static final Image windowIcon = ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/threadsWindow.png"); // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final JPanel threadsTimelinePanelContainer;
    private final JTabbedPane tabs;
    private final ThreadsPanel threadsPanel;
    private final ThreadsTablePanel threadsTablePanel;
    private Component lastFocusOwner;
    private JPanel threadsDetailsPanelContainer;
    private ThreadsDetailsPanel threadsDetailsPanel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Initializes the window */
    public ThreadsWindow() {
        setName(THREADS_WINDOW_NAME);
        setIcon(windowIcon);
        getAccessibleContext().setAccessibleDescription(THREADS_ACCESS_DESCR);
        setLayout(new BorderLayout());
        tabs = new JTabbedPane();

        tabs.setTabPlacement(JTabbedPane.BOTTOM);
        add(tabs, BorderLayout.CENTER);

        final boolean tvmSupportsSleepingState = Platform.supportsThreadSleepingStateMonitoring(Profiler.getDefault()
                                                                                                        .getTargetAppRunner()
                                                                                                        .getProfilerEngineSettings()
                                                                                                        .getTargetJDKVersionString()); // TODO [project] - this is wrong - the JVM can change and the supportSleeping state as well

        threadsPanel = new ThreadsPanel(Profiler.getDefault().getThreadsManager(),
                                        new ThreadsPanel.ThreadsDetailsCallback() {
                /**
                 * Displays a panel with details about specified threads
                 * @param indexes array of int indexes for threads to display
                 */
                public void showDetails(final int[] indexes) {
                    threadsDetailsPanel.showDetails(indexes);
                    tabs.setSelectedComponent(threadsDetailsPanelContainer);
                }
            }, tvmSupportsSleepingState); // TODO [project] - this is wrong - the JVM can change and the supportSleeping state as well
        threadsTimelinePanelContainer = new JPanel() {
                public void requestFocus() {
                    threadsPanel.requestFocus();
                }
            };
        threadsTimelinePanelContainer.setLayout(new BorderLayout());
        threadsTimelinePanelContainer.add(threadsPanel, BorderLayout.CENTER);

        threadsPanel.addThreadsMonitoringActionListener(this);
        threadsPanel.addSaveViewAction(new SaveViewAction(this));
        
        threadsTablePanel = new ThreadsTablePanel(Profiler.getDefault().getThreadsManager(),
                new ThreadsTablePanel.ThreadsDetailsCallback() {
                public void showDetails(final int[] indexes) {
                    threadsDetailsPanel.showDetails(indexes);
                    tabs.setSelectedComponent(threadsDetailsPanelContainer);
                }
            }, tvmSupportsSleepingState);
        threadsTablePanel.addSaveViewAction(new SaveViewAction(this));

        threadsDetailsPanel = new ThreadsDetailsPanel(Profiler.getDefault().getThreadsManager(), tvmSupportsSleepingState);
        threadsDetailsPanelContainer = new JPanel() {
                public void requestFocus() {
                    threadsDetailsPanel.requestFocus();
                }
            };
        threadsDetailsPanelContainer.setLayout(new BorderLayout());
        threadsDetailsPanelContainer.add(threadsDetailsPanel, BorderLayout.CENTER);
        threadsDetailsPanel.addSaveViewAction(new SaveViewAction(this));

        tabs.addTab(THREADS_TIMELINE_TAB_NAME, null, threadsTimelinePanelContainer, THREADS_TIMELINE_TAB_DESCR);
        tabs.addTab(THREADS_TABLE_TAB_NAME, null, threadsTablePanel, THREADS_TABLE_TAB_DESCR);
        tabs.addTab(THREADS_DETAILS_TAB_NAME, null, threadsDetailsPanelContainer, THREADS_DETAILS_TAB_DESCR);

        profilingStateChanged(Profiler.getDefault().getProfilingState());
        threadsMonitoringChanged();

        // Fix for Issue 115062 (CTRL-PageUp/PageDown should move between snapshot tabs)
        tabs.getActionMap().getParent().remove("navigatePageUp"); // NOI18N
        tabs.getActionMap().getParent().remove("navigatePageDown"); // NOI18N

        // support for traversing subtabs using Ctrl-Alt-PgDn/PgUp
        getActionMap().put("PreviousViewAction",
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    moveToPreviousSubTab();
                }
            }); // NOI18N
        getActionMap().put("NextViewAction",
                           new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    moveToNextSubTab();
                }
            }); // NOI18N

        setFocusable(true);
        setRequestFocusEnabled(true);

        tabs.addChangeListener(this);
        Profiler.getDefault().addProfilingStateListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ThreadsWindow getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new ThreadsWindow();
        }

        return defaultInstance;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public void actionPerformed(final ActionEvent e) {
        Profiler.getDefault().setThreadsMonitoringEnabled(true);
    }

    public static void closeIfOpened() {
        if (defaultInstance != null) {
            IDEUtils.runInEventDispatchThread(new Runnable() {
                    public void run() {
                        if (defaultInstance.isOpened()) {
                            defaultInstance.close();
                        }
                    }
                });
        }
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    public BufferedImage getViewImage(boolean onlyVisibleArea) {
        if (tabs.getSelectedComponent() == threadsTimelinePanelContainer) {
            return threadsPanel.getCurrentViewScreenshot(onlyVisibleArea);
        } else if (tabs.getSelectedComponent() == threadsTablePanel) {
            return threadsTablePanel.getCurrentViewScreenshot(onlyVisibleArea);
        } else if (tabs.getSelectedComponent() == threadsDetailsPanelContainer) {
            return threadsDetailsPanel.getCurrentViewScreenshot(onlyVisibleArea);
        }

        return null;
    }

    public String getViewName() {
        if (tabs.getSelectedComponent() == threadsTimelinePanelContainer) {
            return "threads-timeline"; // NOI18N
        } else if (tabs.getSelectedComponent() == threadsTablePanel) {
            return "threads-table"; // NOI18N
        } else if (tabs.getSelectedComponent() == threadsDetailsPanelContainer) {
            return "threads-details"; // NOI18N
        }

        return null;
    }

    public void componentActivated() {
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        } else if (threadsPanel != null) {
            threadsPanel.requestFocus();
        }
    }

    public void componentDeactivated() {
        lastFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    }

    public boolean fitsVisibleArea() {
        if (tabs.getSelectedComponent() == threadsTimelinePanelContainer) {
            return threadsPanel.fitsVisibleArea();
        } else if (tabs.getSelectedComponent() == threadsTablePanel) {
            return threadsTablePanel.fitsVisibleArea();
        } else if (tabs.getSelectedComponent() == threadsDetailsPanelContainer) {
            return threadsDetailsPanel.fitsVisibleArea();
        }

        return true;
    }

    // --- Export Current View action support ------------------------------------
    public boolean hasView() {
        if (tabs.getSelectedComponent() == threadsTimelinePanelContainer) {
            return threadsPanel.hasView();
        } else if (tabs.getSelectedComponent() == threadsTablePanel) {
            return threadsTablePanel.hasView();
        } else if (tabs.getSelectedComponent() == threadsDetailsPanelContainer) {
            return threadsDetailsPanel.hasView();
        }

        return false;
    }

    public void instrumentationChanged(final int oldInstrType, final int currentInstrType) {
        // ignore
    }

    public void profilingStateChanged(final ProfilingStateEvent e) {
        profilingStateChanged(e.getNewState());
    }

    public void showThreads() {
        tabs.setSelectedComponent(threadsTimelinePanelContainer);
        open();
        requestActive();
    }

    public void stateChanged(ChangeEvent e) {
        SwingUtilities.invokeLater(new Runnable() { // must be invoked lazily to override default focus of first component
                public void run() {
                    if (tabs.getSelectedComponent() != null) {
                        tabs.getSelectedComponent().requestFocus(); // move focus to results table when tab is switched
                    }
                }
            });
    }

    public void threadsMonitoringChanged() {
        if (Profiler.getDefault().getThreadsMonitoringEnabled()) {
            threadsPanel.threadsMonitoringEnabled();
            tabs.setEnabledAt(1, true);
            tabs.setEnabledAt(2, true);
        } else {
            threadsPanel.threadsMonitoringDisabled();
            tabs.setSelectedIndex(0);
            tabs.setEnabledAt(1, false);
            tabs.setEnabledAt(2, false);
        }
    }

    /**
     * Subclasses are encouraged to override this method to provide preferred value
     * for unique TopComponent Id returned by getID. Returned value is used as starting
     * value for creating unique TopComponent ID.
     * Value should be preferably unique, but need not be.
     */
    protected String preferredID() {
        return this.getClass().getName();
    }

    private void moveToNextSubTab() {
        tabs.setSelectedIndex(UIUtils.getNextSubTabIndex(tabs, tabs.getSelectedIndex()));
    }

    private void moveToPreviousSubTab() {
        tabs.setSelectedIndex(UIUtils.getPreviousSubTabIndex(tabs, tabs.getSelectedIndex()));
    }

    private void profilingStateChanged(final boolean enable) {
        if (enable) {
            threadsPanel.profilingSessionStarted();
        } else {
            threadsPanel.profilingSessionFinished();
        }
    }

    private void profilingStateChanged(final int profilingState) {
        if (profilingState == Profiler.PROFILING_RUNNING) {
            profilingStateChanged(true);
        } else {
            profilingStateChanged(false);
        }
    }
}
