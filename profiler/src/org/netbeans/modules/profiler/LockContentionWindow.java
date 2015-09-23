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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.ResultsView;
import org.netbeans.lib.profiler.ui.locks.LockContentionPanel;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;


/** An IDE TopComponent to display lock contention data.
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LockContentionWindow_WindowName=Lock Contention",
    "LockContentionWindow_WindowAccessDescr=Shows lock contention details"
})
public final class LockContentionWindow extends ProfilerTopComponent {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final String HELP_CTX_KEY = "LockContentionWindow.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static LockContentionWindow defaultInstance;
    private static final Image windowIcon = Icons.getImage(ProfilerIcons.WINDOW_LOCKS);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final ResultsView lockView;
    private final LockContentionPanel locksPanel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Initializes the window */
    LockContentionWindow() {
        // constructor must run in EDT
        assert SwingUtilities.isEventDispatchThread();
        setName(Bundle.LockContentionWindow_WindowName());
        setIcon(windowIcon);
        getAccessibleContext().setAccessibleDescription(Bundle.LockContentionWindow_WindowAccessDescr());
        setLayout(new BorderLayout());
        lockView = new ResultsView();
        add(lockView, BorderLayout.CENTER);

        locksPanel = new LockContentionPanel(null);
//        locksPanel.addSaveViewAction(new SaveViewAction(new SaveView()));
//        locksPanel.addExportAction(new ExportAction(new Exporter(),null));

        lockView.addView("Locks", null, "Locks", locksPanel, locksPanel.getToolbar());
//        lockView.addView(Bundle.ThreadsWindow_ThreadsTableTabName(), null,
//                Bundle.ThreadsWindow_ThreadsTableTabDescr(), threadsTablePanel, threadsTablePanel.getToolbar());
//        lockView.addView(Bundle.ThreadsWindow_ThreadsDetailsTabName(), null,
//                Bundle.ThreadsWindow_ThreadsDetailsTabDescr(), threadsDetailsPanelContainer, threadsDetailsPanel.getToolbar());

        profilingStateChanged(Profiler.getDefault().getProfilingState());
        updateLocksView();

        setFocusable(true);
        setRequestFocusEnabled(true);

        lockView.addChangeListener(new Listener());
        locksPanel.addLockContentionListener(new LockContentionListener());
//        Profiler.getDefault().addProfilingStateListener(new ProfilingStateAdapter() {
//            public void profilingStateChanged(final ProfilingStateEvent e) {
//                LockContentionWindow.this.profilingStateChanged(e.getNewState());
//            }
//            public void lockContentionMonitoringChanged() {
//                updateLocksView();
//            }
//        });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static LockContentionWindow getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new LockContentionWindow();
        }

        return defaultInstance;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
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

    protected String preferredID() {
        return this.getClass().getName();
    }

    protected Component defaultFocusOwner() {
        return null;
    }

    public void showView() {
//        lockView.selectView(threadsTimelinePanelContainer);
        open();
        requestActive();
    }

    private void updateLocksView() {
        if (Profiler.getDefault().getLockContentionMonitoringEnabled()) {
            locksPanel.lockContentionEnabled();
        } else {
            locksPanel.lockContentionDisabled();
        }
    }

    
    private void profilingStateChanged(final boolean enable) {
        if (enable) {
            locksPanel.profilingSessionStarted();
        } else {
            locksPanel.profilingSessionFinished();
        }
    }

    private void profilingStateChanged(final int profilingState) {
        if (profilingState == Profiler.PROFILING_RUNNING) {
            profilingStateChanged(true);
        } else {
            profilingStateChanged(false);
        }
    }
    
//    private class Exporter implements ExportAction.ExportProvider {
//
//        @Override
//        public void exportData(int exportedFileType, ExportDataDumper eDD) {
//            locksPanel.exportData(exportedFileType, eDD, getViewName());
//        }
//
//        @Override
//        public String getViewName() {
//            return Bundle.LockContentionWindow_WindowName();
//        }
//
//        @Override
//        public boolean hasExportableView() {
//            return locksPanel.hasView();
//        }
//
//        @Override
//        public boolean hasLoadedSnapshot() {
//            return false;
//        }
//    }

    private class LockContentionListener implements ActionListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            Profiler.getDefault().setLockContentionMonitoringEnabled(true);
            locksPanel.lockContentionEnabled();
        }
    }

    private class Listener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
//        SwingUtilities.invokeLater(new Runnable() { // must be invoked lazily to override default focus of first component
//                public void run() {
//                    Component selectedView = lockView.getSelectedView();
//                    if (selectedView != null) {
//                        selectedView.requestFocus(); // move focus to results table when tab is switched
//                    }
//                }
//            });
        }
    }
    
//    private class SaveView implements SaveViewAction.ViewProvider {
//
//        @Override
//        public BufferedImage getViewImage(boolean onlyVisibleArea) {
//            return locksPanel.getCurrentViewScreenshot(onlyVisibleArea);
//        }
//
//        @Override
//        public String getViewName() {
//            return Bundle.LockContentionWindow_WindowName();
//        }
//
//        @Override
//        public boolean fitsVisibleArea() {
//            return locksPanel.fitsVisibleArea();
//        }
//
//        @Override
//        public boolean hasView() {
//            return locksPanel.hasView();
//        } 
//    }
}
