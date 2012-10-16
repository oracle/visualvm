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

package org.netbeans.lib.profiler.ui.threads;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.DataManagerListener;
import org.netbeans.lib.profiler.results.threads.ThreadData;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.lib.profiler.ui.UIUtils;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import javax.swing.*;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.components.VerticalLayout;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;


/**
 * A panel to display list of thread detailed information.
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public class ThreadsDetailsPanel extends JPanel implements ActionListener, DataManagerListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.threads.Bundle"); // NOI18N
    private static final String TEXT_DISPLAY_ALL = messages.getString("ThreadsDetailsPanel_TextDisplayAll"); // NOI18N
    private static final String TEXT_DISPLAY_ALL_EX = messages.getString("ThreadsDetailsPanel_TextDisplayAllEx"); // NOI18N
    private static final String TEXT_DISPLAY_LIVE = messages.getString("ThreadsDetailsPanel_TextDisplayLive"); // NOI18N
    private static final String TEXT_DISPLAY_LIVE_EX = messages.getString("ThreadsDetailsPanel_TextDisplayLiveEx"); // NOI18N
    private static final String TEXT_DISPLAY_FINISHED = messages.getString("ThreadsDetailsPanel_TextDisplayFinished"); // NOI18N
    private static final String TEXT_DISPLAY_FINISHED_EX = messages.getString("ThreadsDetailsPanel_TextDisplayFinishedEx"); // NOI18N
    private static final String TEXT_DISPLAY_SELECTION = messages.getString("ThreadsDetailsPanel_TextDisplaySelection"); // NOI18N
    private static final String NO_CONTENT_MSG = messages.getString("ThreadsDetailsPanel_NoContentMsg"); // NOI18N
    private static final String EVENTQUEUE_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_EventQueueThreadDescr"); // NOI18N
    private static final String IMAGEFETCHER_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_ImageFetcherThreadDescr"); // NOI18N
    private static final String IMAGEANIMATOR_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_ImageAnimatorThreadDescr"); // NOI18N
    private static final String AWTWINDOWS_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_AwtWindowsThreadDescr"); // NOI18N
    private static final String AWTMOTIF_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_AwtMotifThreadDescr"); // NOI18N
    private static final String AWTSHUTDWN_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_AwtShutDwnThreadDescr"); // NOI18N
    private static final String MAIN_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_MainThreadDescr"); // NOI18N
    private static final String FINALIZER_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_FinalizerThreadDescr"); // NOI18N
    private static final String REFHANDLER_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_RefHandlerThreadDescr"); // NOI18N
    private static final String SIGDISPATCH_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_SigDispatchThreadDescr"); // NOI18N
    private static final String J2DISPOSER_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_J2DisposerThreadDescr"); // NOI18N
    private static final String TIMERQUEUE_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_TimerQueueThreadDescr"); // NOI18N
    private static final String USER_THREAD_DESCR = messages.getString("ThreadsDetailsPanel_UserThreadDescr"); // NOI18N
    private static final String COMBO_ACCESS_NAME = messages.getString("ThreadsDetailsPanel_ComboAccessName"); // NOI18N
    private static final String COMBO_ACCESS_DESCR = messages.getString("ThreadsDetailsPanel_ComboAccessDescr"); // NOI18N
    private static final String CONTENT_ACCESS_NAME = messages.getString("ThreadsDetailsPanel_ContentAccessName"); // NOI18N
    private static final String CONTENT_ACCESS_DESCR = messages.getString("ThreadsDetailsPanel_ContentAccessDescr"); // NOI18N
    private static final String SHOW_LABEL_TEXT = messages.getString("ThreadsPanel_ShowLabelText"); // NOI18N
                                                                                                    // -----
    private static final int DISPLAY_ALL = 0;
    private static final int DISPLAY_LIVE = 1;
    private static final int DISPLAY_FINISHED = 2;
    private static final int DISPLAY_SELECTED = 3;
    private static final int DISPLAY_ALL_EX = 4;
    private static final int DISPLAY_LIVE_EX = 5;
    private static final int DISPLAY_FINISHED_EX = 6;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList displayedPanels = new ArrayList(10);
    private ArrayList excludedThreads = new ArrayList(5);
    private ArrayList filteredThreads = new ArrayList(10);
    private DefaultComboBoxModel comboModel;
    private HashMap descriptions = new HashMap(20);
    private HashMap indexToDisplayedIndex = new HashMap(15);
    private HashMap unusedPanels = new HashMap(5); // <thread index, ThreadDetailsComponent>
    private JComboBox threadsSelectionCombo;
    private JPanel content;
    private JPanel noContentPanel;
    private JScrollPane scrollPane;
    private ProfilerToolbar buttonsToolBar;
    private ThreadsDataManager manager;
    private boolean internalChange = false;
    private boolean noContent = false;
    private boolean resetPerformed = true;
    private boolean supportsSleepingState; // internal flag indicating that threads monitoring engine correctly reports the "sleeping" state
    private int displayMode = DISPLAY_SELECTED;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ThreadsDetailsPanel(ThreadsDataManager manager, boolean supportsSleepingState) {
        this.manager = manager;
        this.supportsSleepingState = supportsSleepingState;

        noContentPanel = new JPanel();
        noContentPanel.setLayout(new BorderLayout());
        noContentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel noContentIcon = new JLabel(Icons.getIcon(ProfilerIcons.VIEW_THREADS_32));
        noContentIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        noContentIcon.setVerticalAlignment(SwingConstants.TOP);
        noContentIcon.setEnabled(false);

        JTextArea noContentText = new JTextArea(NO_CONTENT_MSG);
        noContentText.setFont(noContentText.getFont().deriveFont(14));

        noContentText.setEditable(false);
        noContentText.setEnabled(false);
        noContentText.setWrapStyleWord(true);
        noContentText.setLineWrap(true);
        noContentText.setBackground(noContentPanel.getBackground());

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(noContentIcon, BorderLayout.WEST);
        containerPanel.add(noContentText, BorderLayout.CENTER);
        noContentPanel.add(containerPanel, BorderLayout.NORTH);

        // create components
        threadsSelectionCombo = new JComboBox() {
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
                ;
            };
        threadsSelectionCombo.getAccessibleContext().setAccessibleName(COMBO_ACCESS_NAME);
        threadsSelectionCombo.getAccessibleContext().setAccessibleDescription(COMBO_ACCESS_DESCR);

        updateCombo();

        JLabel showLabel = new JLabel(SHOW_LABEL_TEXT);
        showLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        showLabel.setLabelFor(threadsSelectionCombo);

        int mnemCharIndex = 0;
        showLabel.setDisplayedMnemonic(showLabel.getText().charAt(mnemCharIndex));
        showLabel.setDisplayedMnemonicIndex(mnemCharIndex);

        buttonsToolBar = ProfilerToolbar.create(true);
        content = new JPanel() {
                public Dimension getPreferredSize() {
                    Dimension dim = super.getPreferredSize();

                    return new Dimension(Math.min(dim.width, scrollPane.getViewportBorderBounds().width), dim.height);
                }
            };
        content.getAccessibleContext().setAccessibleName(CONTENT_ACCESS_NAME);
        content.getAccessibleContext().setAccessibleName(CONTENT_ACCESS_DESCR);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        scrollPane = new JScrollPane(contentPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(30);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());

        // perform layout
        setLayout(new BorderLayout());
        content.setLayout(new VerticalLayout(0, 0));

        contentPanel.add(content, BorderLayout.NORTH);
        buttonsToolBar.add(showLabel);
        buttonsToolBar.add(threadsSelectionCombo);
        add(scrollPane, BorderLayout.CENTER);
        //add (scrollBar, BorderLayout.EAST);
        threadsSelectionCombo.addActionListener(this);
        manager.addDataListener(this);
        
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) dataChanged();
                }
            }
        });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getToolbar() {
        return buttonsToolBar.getComponent();
    }
    
    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(scrollPane);
        } else {
            return UIUtils.createScreenshot(content);
        }
    }

    public long getDataEndTime() {
        return manager.getEndTime();
    }

    public long getDataStartTime() {
        return manager.getStartTime();
    }

    public String getThreadClassName(int index) {
        return manager.getThreadClassName(index);
    }

    public ThreadData getThreadData(int index) {
        return manager.getThreadData(index);
    }

    public String getThreadDescription(int index) {
        String description = (String) descriptions.get(Integer.valueOf(index));

        if (description == null) {
            description = createDescription(manager.getThreadName(index));
            descriptions.put(Integer.valueOf(index), description);
        }

        return description;
    }

    // ---------------------------------------------------------------------------------------
    // Thread data
    public String getThreadName(int index) {
        return manager.getThreadName(index);
    }

    // ---------------------------------------------------------------------------------------
    // Listeners

    /** Invoked when one of the buttons is pressed */
    public void actionPerformed(ActionEvent e) {
        if (internalChange) {
            return;
        }

        if (e.getSource() == threadsSelectionCombo) {
            String threadSelection = (String) threadsSelectionCombo.getSelectedItem();
            int oldMode = displayMode;

            switch (threadsSelectionCombo.getSelectedIndex()) {
                case 0:
                    displayMode = DISPLAY_ALL;

                    break;
                case 1:
                    displayMode = DISPLAY_LIVE;

                    break;
                case 2:
                    displayMode = DISPLAY_FINISHED;

                    break;
                case 3:
                    displayMode = DISPLAY_SELECTED;

                    break;
                case 4:

                    if (threadSelection == TEXT_DISPLAY_ALL_EX) {
                        displayMode = DISPLAY_ALL_EX;
                    } else if (threadSelection == TEXT_DISPLAY_LIVE_EX) {
                        displayMode = DISPLAY_LIVE_EX;
                    } else if (threadSelection == TEXT_DISPLAY_FINISHED_EX) {
                        displayMode = DISPLAY_FINISHED_EX;
                    }
            }

            if (oldMode != displayMode) {
                switch (displayMode) {
                    case DISPLAY_ALL:
                        excludedThreads.clear();

                        break;
                    case DISPLAY_LIVE:
                        excludedThreads.clear();

                        break;
                    case DISPLAY_FINISHED:
                        excludedThreads.clear();

                        break;
                    case DISPLAY_SELECTED:
                        excludedThreads.clear();
                        filteredThreads.clear();

                        break;
                }

                updateCombo();
                dataChanged();
            }
        }
    }

    // --- Save Current View action support --------------------------------------
    public void addSaveViewAction(AbstractAction saveViewAction) {
        Component actionButton = buttonsToolBar.add(saveViewAction);
        buttonsToolBar.remove(actionButton);
        buttonsToolBar.add(actionButton, 0);
        buttonsToolBar.add(new JToolBar.Separator() ,1);
    }

    /** Called when data in manager change */
    public void dataChanged() {
        if (resetPerformed) {
            supportsSleepingState = manager.supportsSleepingStateMonitoring();
            resetPerformed = false;
        }

        UIUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    if (!isShowing()) {
                        return;
                    }

                    updateFilteredData();

                    if (updateDisplayedPanels()) {
                        // if the number of displayed panels changed, the scrollbar needs to be revalidated
                        content.invalidate();
                        revalidate();
                        repaint();
                    }
                }
            });
    }

    public void dataReset() {
        resetPerformed = true;
        filteredThreads.clear();
        excludedThreads.clear();
        descriptions.clear();
        content.removeAll();
        unusedPanels.clear();
        displayedPanels.clear();

        displayMode = DISPLAY_SELECTED;
        UIUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    updateCombo();

                    content.invalidate();
                    revalidate();
                    repaint();
                }
            });
    }

    public boolean fitsVisibleArea() {
        return !scrollPane.getVerticalScrollBar().isVisible();
    }

    public boolean hasView() {
        return !noContent && (content.getComponentCount() > 0);
    }

    /** Called by the ThreadDetailsComponent when the Hide button has been clicked */
    public void hideThreadDetails(int index) {
        if (displayMode == DISPLAY_SELECTED) {
            filteredThreads.remove(Integer.valueOf(index));
        } else {
            if (displayMode == DISPLAY_ALL) {
                displayMode = DISPLAY_ALL_EX;
                updateCombo();
            } else if (displayMode == DISPLAY_LIVE) {
                displayMode = DISPLAY_LIVE_EX;
                updateCombo();
            } else if (displayMode == DISPLAY_FINISHED) {
                displayMode = DISPLAY_FINISHED_EX;
                updateCombo();
            }

            excludedThreads.add(Integer.valueOf(index));
            updateFilteredData();
        }

        if (updateDisplayedPanels()) {
            content.invalidate();
            revalidate();
            repaint();
        }
    }

    public void showDetails(int[] indexes) {
        displayMode = DISPLAY_SELECTED;
        filteredThreads.clear();
        excludedThreads.clear();

        for (int i = 0; i < indexes.length; i++) {
            filteredThreads.add(Integer.valueOf(indexes[i]));
        }

        updateCombo();

        if (updateDisplayedPanels()) {
            content.invalidate();
            revalidate();
            repaint();
        }
    }

    private ThreadDetailsComponent getPanel(int threadIndex) {
        ThreadDetailsComponent tdcr = (ThreadDetailsComponent) unusedPanels.remove(Integer.valueOf(threadIndex));

        if (tdcr == null) {
            if (unusedPanels.size() > 0) {
                tdcr = (ThreadDetailsComponent) unusedPanels.remove(unusedPanels.keySet().iterator().next());
            } else {
                tdcr = new ThreadDetailsComponent(this, supportsSleepingState);
            }
        }

        tdcr.setIndex(threadIndex);

        return tdcr;
    }

    private String createDescription(String threadName) {
        if (threadName.startsWith("AWT-EventQueue-")) {
            return EVENTQUEUE_THREAD_DESCR; // NOI18N
        }

        if (threadName.startsWith("Image Fetcher ")) {
            return IMAGEFETCHER_THREAD_DESCR; // NOI18N
        }

        if (threadName.startsWith("Image Animator ")) {
            return IMAGEANIMATOR_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("AWT-Windows")) {
            return AWTWINDOWS_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("AWT-Motif")) {
            return AWTMOTIF_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("AWT-Shutdown")) {
            return AWTSHUTDWN_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("main")) {
            return MAIN_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("Finalizer")) {
            return FINALIZER_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("Reference Handler")) {
            return REFHANDLER_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("Signal Dispatcher")) {
            return SIGDISPATCH_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("Java2D Disposer")) {
            return J2DISPOSER_THREAD_DESCR; // NOI18N
        }

        if (threadName.equals("TimerQueue")) {
            return TIMERQUEUE_THREAD_DESCR; // NOI18N
        }

        return USER_THREAD_DESCR;
    }

    // @AWTRequired
    private void updateCombo() {
        internalChange = true;
        comboModel = new DefaultComboBoxModel(new Object[] {
                                                  TEXT_DISPLAY_ALL, TEXT_DISPLAY_LIVE, TEXT_DISPLAY_FINISHED,
                                                  TEXT_DISPLAY_SELECTION
                                              });

        int displayIndex = 0;

        switch (displayMode) {
            case DISPLAY_ALL:
                displayIndex = 0;

                break;
            case DISPLAY_LIVE:
                displayIndex = 1;

                break;
            case DISPLAY_FINISHED:
                displayIndex = 2;

                break;
            case DISPLAY_SELECTED:
                displayIndex = 3;

                break;
            case DISPLAY_ALL_EX:
                comboModel.addElement(TEXT_DISPLAY_ALL_EX);
                displayIndex = 4;

                break;
            case DISPLAY_LIVE_EX:
                comboModel.addElement(TEXT_DISPLAY_LIVE_EX);
                displayIndex = 4;

                break;
            case DISPLAY_FINISHED_EX:
                comboModel.addElement(TEXT_DISPLAY_FINISHED_EX);
                displayIndex = 4;

                break;
        }

        threadsSelectionCombo.setModel(comboModel);
        threadsSelectionCombo.setSelectedIndex(displayIndex);

        internalChange = false;
    }

    /** Updates the displayed panels.
     *
     * The filteredThreads contains threads that need to be displayed.
     * The displayedPanels arraylist contains panels that are currently displayed
     * The indexToDisplayedIndex map maps real thread indexes to those displayed
     *
     * @return true if the number of panels changed
     *
     * @AWTRequired
     **/
    private boolean updateDisplayedPanels() {
        boolean changed = false;
        int filteredSize = filteredThreads.size();

        if (filteredSize == 0) {
            if (!noContent) {
                noContent = true;
                remove(scrollPane);
                add(noContentPanel, BorderLayout.CENTER);
                invalidate();
                revalidate();
                repaint();
            }

            return changed;
        } else {
            if (noContent) {
                noContent = false;
                remove(noContentPanel);
                add(scrollPane, BorderLayout.CENTER);
                invalidate();
                revalidate();
                repaint();
            }
        }

        int displayedSize = displayedPanels.size();

        if (filteredSize > displayedSize) { // need to get & display new panels

            for (int i = displayedSize; i < filteredSize; i++) {
                ThreadDetailsComponent tdc = getPanel(((Integer) filteredThreads.get(i)).intValue());
                displayedPanels.add(tdc);
                content.add(tdc);
            }

            changed = true;
        } else if (filteredSize < displayedSize) { // need to remove some displayed panels

            for (int i = filteredSize; i < displayedSize; i++) {
                ThreadDetailsComponent tdc = (ThreadDetailsComponent) displayedPanels.remove(filteredSize);
                unusedPanels.put(Integer.valueOf(tdc.getIndex()), tdc);
                content.remove(tdc);
            }

            changed = true;
        }

        indexToDisplayedIndex.clear();

        int count = 0;

        for (Iterator it = filteredThreads.iterator(); it.hasNext();) {
            int indexToDisplay = ((Integer) it.next()).intValue();
            ((ThreadDetailsComponent) displayedPanels.get(count)).setIndex(indexToDisplay);
            indexToDisplayedIndex.put(Integer.valueOf(indexToDisplay), Integer.valueOf(count));
            count++;
        }

        return changed;
    }

    private boolean updateDisplayedPanels1() {
        // remove all displayed panels
        content.removeAll();

        indexToDisplayedIndex.clear();

        // put all of them to unused map
        for (int i = displayedPanels.size() - 1; i >= 0; i--) {
            ThreadDetailsComponent tdcr = (ThreadDetailsComponent) displayedPanels.remove(i);
            unusedPanels.put(Integer.valueOf(tdcr.getIndex()), tdcr);
        }

        int count = 0;

        for (Iterator it = filteredThreads.iterator(); it.hasNext();) {
            int indexToDisplay = ((Integer) it.next()).intValue();
            ThreadDetailsComponent tdcr = getPanel(indexToDisplay);
            content.add(tdcr);
            displayedPanels.add(tdcr);
            indexToDisplayedIndex.put(Integer.valueOf(indexToDisplay), Integer.valueOf(count++));
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------
    // Private methods

    /** Updates filteredThreads list per selection in the threadsSelectionCombo. */
    private void updateFilteredData() {
        if (displayMode == DISPLAY_SELECTED) {
            return;
        }

        filteredThreads.clear();

        if ((displayMode == DISPLAY_ALL) || (displayMode == DISPLAY_ALL_EX)) {
            for (int i = 0; i < manager.getThreadsCount(); i++) {
                filteredThreads.add(Integer.valueOf(i)); // thread with index "i" should be displayed
            }
        }

        if ((displayMode == DISPLAY_LIVE) || (displayMode == DISPLAY_LIVE_EX)) {
            // view live threads
            for (int i = 0; i < manager.getThreadsCount(); i++) {
                ThreadData threadData = manager.getThreadData(i);

                if (threadData.size() > 0) {
                    byte state = threadData.getLastState();

                    if (state != CommonConstants.THREAD_STATUS_ZOMBIE) {
                        filteredThreads.add(Integer.valueOf(i)); // thread with index "i" should be displayed
                    }
                }
            }
        }

        if ((displayMode == DISPLAY_FINISHED) || (displayMode == DISPLAY_FINISHED_EX)) {
            // view finished threads
            for (int i = 0; i < manager.getThreadsCount(); i++) {
                ThreadData threadData = manager.getThreadData(i);

                if (threadData.size() > 0) {
                    byte state = threadData.getLastState();

                    if (state == CommonConstants.THREAD_STATUS_ZOMBIE) {
                        filteredThreads.add(Integer.valueOf(i)); // thread with index "i" should be displayed
                    }
                } else {
                    // No state defined -> THREAD_STATUS_ZOMBIE assumed (thread could finish when monitoring was disabled)
                    filteredThreads.add(Integer.valueOf(i));
                }
            }
        }

        // process excludes
        if ((displayMode == DISPLAY_ALL_EX) || (displayMode == DISPLAY_LIVE_EX) || (displayMode == DISPLAY_FINISHED_EX)) {
            filteredThreads.removeAll(excludedThreads);
        }
    }
}
