/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.common.event.ProfilingStateAdapter;
import org.netbeans.lib.profiler.ui.ResultsView;
import org.netbeans.lib.profiler.ui.threads.ThreadsPanel;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;


/** An IDE TopComponent to display profiling results.
 *
 * TODO: I18N, update names of existing keys
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ThreadsWindow_ThreadsWindowName=Threads",
    "ThreadsWindow_ThreadsTimelineTabName=Timeline",
    "ThreadsWindow_ThreadsTableTabName=Table",
    "ThreadsWindow_ThreadsDetailsTabName=Details",
    "ThreadsWindow_ThreadsTimelineTabDescr=Timeline showing application threads and their states",
    "ThreadsWindow_ThreadsTableTabDescr=Table showing statistics about application threads and their states",
    "ThreadsWindow_ThreadsDetailsTabDescr=List of application threads with detailed status data",
    "ThreadsWindow_ThreadsAccessDescr=Profiler threads timeline and details"
})
public final class ThreadsWindow extends ProfilerTopComponent implements ActionListener, /*ChangeListener,*/
                                                                 SaveViewAction.ViewProvider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final String HELP_CTX_KEY = "ThreadsWindow.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static ThreadsWindow defaultInstance;
    private static final Image windowIcon = Icons.getImage(ProfilerIcons.WINDOW_THREADS);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final ResultsView threadsView;
    private final ThreadsPanel threadsPanel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Initializes the window */
    public ThreadsWindow() {
        setName(Bundle.ThreadsWindow_ThreadsWindowName());
        setIcon(windowIcon);
        getAccessibleContext().setAccessibleDescription(Bundle.ThreadsWindow_ThreadsAccessDescr());
        setLayout(new BorderLayout());
        threadsView = new ResultsView();
        add(threadsView, BorderLayout.CENTER);

        threadsPanel = new ThreadsPanel(Profiler.getDefault().getThreadsManager(), new SaveViewAction(this));
        threadsPanel.addThreadsMonitoringActionListener(this);

        threadsView.addView(Bundle.ThreadsWindow_ThreadsTimelineTabName(), null,
                Bundle.ThreadsWindow_ThreadsTimelineTabDescr(), threadsPanel, threadsPanel.getToolbar());

        profilingStateChanged(Profiler.getDefault().getProfilingState());
        updateThreadsView();

        setFocusable(true);
        setRequestFocusEnabled(true);

        Profiler.getDefault().addProfilingStateListener(new ProfilingStateAdapter(){
            @Override
            public void profilingStateChanged(final ProfilingStateEvent e) {
                ThreadsWindow.this.profilingStateChanged(e.getNewState());
            }

            @Override
            public void threadsMonitoringChanged() {
                updateThreadsView();
            }
        });
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
            CommonUtils.runInEventDispatchThread(new Runnable() {
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
        return threadsPanel.getCurrentViewScreenshot(onlyVisibleArea);
    }

    public String getViewName() {
        return "threads"; // NOI18N
    }
    
    protected Component defaultFocusOwner() {
        return threadsPanel;
    }

    public boolean fitsVisibleArea() {
        return threadsPanel.fitsVisibleArea();
    }

    // --- Export Current View action support ------------------------------------
    public boolean hasView() {
        return threadsPanel.hasView();
    }
    
    public void showThreads() {
        open();
        requestActive();
    }

    private void updateThreadsView() {
        if (Profiler.getDefault().getThreadsMonitoringEnabled()) {
            threadsPanel.threadsMonitoringEnabled();
        } else {
            threadsPanel.threadsMonitoringDisabled();
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
