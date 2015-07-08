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
package org.netbeans.lib.profiler.ui.locks;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeNode;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.locks.LockCCTNode;
import org.netbeans.lib.profiler.results.locks.LockCCTProvider;
import org.netbeans.lib.profiler.results.locks.LockRuntimeCCTNode;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FlatToolBar;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.lib.profiler.ui.components.tree.EnhancedTreeCellRenderer;
import org.netbeans.lib.profiler.ui.results.DataView;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTableContainer;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTable;
import org.netbeans.lib.profiler.ui.swing.ProfilerTreeTableModel;
import org.netbeans.lib.profiler.ui.swing.SearchUtils;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.McsTimeRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberPercentRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.NumberRenderer;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public class LockContentionPanel extends DataView {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.locks.Bundle"); // NOI18N
    private static final String ENABLE_LOCKS_MONITORING = messages.getString("LockContentionPanel_EnableLocksMonitoring"); // NOI18N
    private static final String ENABLE_LOCKS_MONITORING_TOOLTIP = messages.getString("LockContentionPanel_EnableLocksMonitoringToolTip"); // NOI18N
    private static final String NO_PROFILING = messages.getString("LockContentionPanel_NoProfiling"); // NOI18N
    private static final String LOCKS_THREADS_COLUMN_NAME = messages.getString("LockContentionPanel_LocksThreadsColumnName"); // NOI18N
    private static final String LOCKS_THREADS_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_LocksThreadsColumnToolTip"); // NOI18N
    private static final String TIME_COLUMN_NAME = messages.getString("LockContentionPanel_TimeColumnName"); // NOI18N
    private static final String TIME_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_TimeColumnToolTip"); // NOI18N
    private static final String TIME_REL_COLUMN_NAME = messages.getString("LockContentionPanel_TimeRelColumnName"); // NOI18N
    private static final String TIME_REL_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_TimeRelColumnToolTip"); // NOI18N
    private static final String WAITS_COLUMN_NAME = messages.getString("LockContentionPanel_WaitsColumnName"); // NOI18N
    private static final String WAITS_COLUMN_TOOLTIP = messages.getString("LockContentionPanel_WaitsColumnToolTip"); // NOI18N
    private static final String DISPLAY_MODE = messages.getString("LockContentionPanel_DisplayMode"); // NOI18N
    private static final String MODE_THREADS = messages.getString("LockContentionPanel_ModeThreads"); // NOI18N
    private static final String MODE_MONITORS = messages.getString("LockContentionPanel_ModeMonitors"); // NOI18N
    // -----
    
    public static enum Aggregation { BY_THREADS, BY_MONITORS }
    
    private final ProfilerToolbar toolbar;
    
//    private final LocksTreeTableModel realTreeTableModel;
    private final LocksTreeTableModel treeTableModel;
    private final ProfilerTreeTable treeTable;
    private final ProfilerTableContainer treeTablePanel;
    private final JComboBox modeCombo;
    
    private final JPopupMenu tablePopup;
    private final JPopupMenu cornerPopup;
    
    private boolean sortingOrder = false;
    private int sortingColumn = 1;
    
    private int columnCount;
    
    private String[] columnNames;
    private TableCellRenderer[] columnRenderers;
    private EnhancedTreeCellRenderer treeCellRenderer = new LockContentionTreeCellRenderer();
    private String[] columnToolTips;
    private int[] columnWidths;
    
    private Aggregation aggregation = Aggregation.BY_THREADS;
    
    private boolean lockContentionEnabled;
    private final JPanel contentPanel;
    private final JPanel notificationPanel;
    private final JButton enableLockContentionButton;
    private final JLabel enableLockContentionLabel1;
    private final JLabel enableLockContentionLabel2;
    
    private LockRuntimeCCTNode root;
    private Listener cctListener;
    private long countsInMicrosec = 1;
    
    private final HideableBarRenderer hbrTime;
    private final HideableBarRenderer hbrWaits;

    private static final int MIN_UPDATE_DIFF = 900;
    private static final int MAX_UPDATE_DIFF = 1400;

    private final ProfilerClient client;
    private long lastupdate;
    private volatile boolean paused;
    private volatile boolean forceRefresh;
    
    public LockContentionPanel(ProfilerClient clnt) { 
    
        client = clnt;
        toolbar = ProfilerToolbar.create(true);
        
        JLabel modeLabel = new JLabel(DISPLAY_MODE);
        modeLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        toolbar.add(modeLabel);
        
        modeCombo = new JComboBox(new Object[] { MODE_THREADS, MODE_MONITORS }) {
            protected void fireActionEvent() {
                super.fireActionEvent();
                treeTable.clearSelection();
                prepareResults();
            }
            public Dimension getMaximumSize() {
                Dimension dim = getPreferredSize();
                dim.width += 20;
                return dim;
            }
        };
        modeCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                          final boolean isSelected, final boolean cellHasFocus) {
                DefaultListCellRenderer dlcr =
                        (DefaultListCellRenderer)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (MODE_THREADS.equals(value.toString())) {
                    dlcr.setIcon(Icons.getIcon(ProfilerIcons.THREAD));
                } else if (MODE_MONITORS.equals(value.toString())) {
                    dlcr.setIcon(Icons.getIcon(ProfilerIcons.WINDOW_LOCKS));
                }

                return dlcr;
            }
        });
        modeLabel.setLabelFor(modeCombo);
        toolbar.add(modeCombo);
        
        initColumnsData();
        
//        realTreeTableModel = new LocksTreeTableModel();
        treeTableModel = new LocksTreeTableModel();
        
        treeTable = new ProfilerTreeTable(treeTableModel, true, true, new int[] { 0 }) {
//            protected Object getValueForPopup(int row) {
//                if (row == -1) return null;
//                if (row >= getModel().getRowCount()) return null; // #239936
//                return Integer.valueOf(convertRowIndexToModel(row));
//            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
//                if (value != null) {
//                    final int row = ((Integer)value).intValue();
//                    final boolean sel = selected.contains(row);
//                    popup.add(new JMenuItem(sel ? BUNDLE().getString("ACT_UnselectThread") :
//                                                  BUNDLE().getString("ACT_SelectThread")) { // NOI18N
//                        protected void fireActionPerformed(ActionEvent e) {
//                            if (sel) selected.remove(row);
//                            else selected.add(row);
//                            threadsTableModel.fireTableDataChanged();
//                            if (!sel) showSelectedColumn();
//                        }
//                    });
//
//                    popup.addSeparator();
//                }
                
//                popup.add(new JMenuItem(FilterUtils.ACTION_FILTER) {
//                    protected void fireActionPerformed(ActionEvent e) { activateFilter(); }
//                });
                popup.add(createCopyMenuItem());
                popup.addSeparator();
                
                popup.add(new JMenuItem(SearchUtils.ACTION_FIND) {
                    protected void fireActionPerformed(ActionEvent e) { activateSearch(); }
                });
            }
        };
        treeTable.setRootVisible(false);
        treeTable.setShowsRootHandles(true);
        
        treeTable.providePopupMenu(true);
//        treeTable.addMouseListener(new MouseListener());
//        treeTable.addKeyListener(new KeyListener());
//        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        treeTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
//        treeTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
//        treeTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
//        treeTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
//        treeTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
//        treeTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
//        treeTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
//        treeTable.getTree().setLargeModel(true);
        
        LockContentionRenderer lcRenderer = new LockContentionRenderer();
        treeTable.setTreeCellRenderer(lcRenderer);
        
//        BarRenderer barRenderer = new BarRenderer();
//        treeTable.setDefaultColumnWidth(1, 100);
//        treeTable.setColumnRenderer(1, barRenderer);
//        treeTable.setColumnVisibility(1, false);
        
        Number refTime = new Long(123456);
        
//        BaseDetailsRenderer numberPercentRenderer = new BaseDetailsRenderer(new LabelRenderer(), "(100%)");
//        NumberBarRenderer numberPercentRenderer = new NumberBarRenderer(Formatters.millisecondsFormat()) {
//            public void setValue(Object value, int row) {
//                LockCCTNode lnode = (LockCCTNode)treeTable.getValueAt(row, 0);
//                setNumberValue(lnode.getTime(), row);
//                setBarValue(lnode.getTimeInPerCent(), row);
//            }
//        };
        
//        NumberRenderer nr = new NumberRenderer(Formatters.millisecondsFormat());
//        nr.setValue(refTime, -1);
        
        NumberPercentRenderer npr = new NumberPercentRenderer(new McsTimeRenderer());
//        npr.setValue(refTime, -1);
        hbrTime = new HideableBarRenderer(npr);
        hbrTime.setMaxValue(refTime.longValue());
        treeTable.setColumnRenderer(1, hbrTime);
        treeTable.setDefaultColumnWidth(1, hbrTime.getOptimalWidth());
        
        hbrWaits = new HideableBarRenderer(new NumberRenderer());
        hbrWaits.setMaxValue(1234567);
//        treeTable.setDefaultColumnWidth(3, hbrWaits.getOptimalWidth());
        treeTable.setColumnRenderer(2, hbrWaits);
        treeTable.setDefaultColumnWidth(2, hbrWaits.getMaxNoBarWidth());
        
        treeTable.setColumnToolTips(new String[] { LOCKS_THREADS_COLUMN_TOOLTIP,
                                                   TIME_REL_COLUMN_TOOLTIP,
                                                   WAITS_COLUMN_TOOLTIP });
        
//        NumberRenderer numberRenderer = new NumberRenderer();
//        numberRenderer.setValue(refTime, -1);
//        treeTable.setDefaultColumnWidth(3, numberRenderer.getPreferredSize().width);
//        treeTable.setColumnRenderer(3, numberRenderer);

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        treeTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        treeTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
        
        setColumnsData();
        
        treeTablePanel = new ProfilerTableContainer(treeTable, false, null);
//        treeTablePanel.clearBorders();
        
        notificationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 15));
        notificationPanel.setBackground(treeTable.getBackground());
        UIUtils.decorateProfilerPanel(notificationPanel);

        Border myRolloverBorder = new CompoundBorder(new FlatToolBar.FlatRolloverButtonBorder(Color.GRAY, Color.LIGHT_GRAY),
                                                     new FlatToolBar.FlatMarginBorder());

        enableLockContentionLabel1 = new JLabel(ENABLE_LOCKS_MONITORING);
        enableLockContentionLabel1.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 3));
        enableLockContentionLabel1.setForeground(Color.DARK_GRAY);

        enableLockContentionButton = new JButton(Icons.getIcon(ProfilerIcons.VIEW_LOCKS_32));
        enableLockContentionButton.setToolTipText(ENABLE_LOCKS_MONITORING_TOOLTIP);
        enableLockContentionButton.setContentAreaFilled(false);
        enableLockContentionButton.setMargin(new Insets(3, 3, 3, 3));
        enableLockContentionButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        enableLockContentionButton.setHorizontalTextPosition(SwingConstants.CENTER);
        enableLockContentionButton.setRolloverEnabled(true);
        enableLockContentionButton.setBorder(myRolloverBorder);
        enableLockContentionButton.getAccessibleContext().setAccessibleName(ENABLE_LOCKS_MONITORING_TOOLTIP);

        enableLockContentionLabel2 = new JLabel(NO_PROFILING);
        enableLockContentionLabel2.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
        enableLockContentionLabel2.setForeground(Color.DARK_GRAY);
        enableLockContentionLabel2.setVisible(false);

        notificationPanel.add(enableLockContentionLabel1);
        notificationPanel.add(enableLockContentionButton);
        notificationPanel.add(enableLockContentionLabel2);
        
        contentPanel = new JPanel(new CardLayout());
        contentPanel.add(notificationPanel, "DISABLED"); // NOI18N
        contentPanel.add(treeTablePanel, "ENABLED"); // NOI18N
        contentPanel.setOpaque(true);
        contentPanel.setBackground(UIUtils.getProfilerResultsBackground());
        
        add(contentPanel, BorderLayout.CENTER);
        
        tablePopup = createTablePopup();
        
        cornerPopup = new JPopupMenu();
//        treeTablePanel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createHeaderPopupCornerButton(cornerPopup));
        
        setDefaultSorting();
        prepareResults(); // Disables combo
        
        registerActions();
    }
    
    private void registerActions() {
        ActionMap map = getActionMap();
        
//        map.put(FilterUtils.FILTER_ACTION_KEY, new AbstractAction() {
//            public void actionPerformed(ActionEvent e) { activateFilter(); }
//        });
        
        map.put(SearchUtils.FIND_ACTION_KEY, new AbstractAction() {
            public void actionPerformed(ActionEvent e) { activateSearch(); }
        });
    }
    
    protected ProfilerTable getResultsComponent() {
        return treeTable;
    }
    
    protected boolean hasBottomFilterFindMargin() {
        return true;
    }
    
    
    public void setAggregation(Aggregation aggregation) {
        this.aggregation = aggregation;
        prepareResults();
    }
    
    public Aggregation getAggregation() {
        return aggregation;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    private void refreshData(final LockRuntimeCCTNode appRootNode) {
        if ((lastupdate + MIN_UPDATE_DIFF > System.currentTimeMillis() || paused) && !forceRefresh) return;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                root = appRootNode;
            }
        });
        prepareResults();
        forceRefresh = false;
    }

    public void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        if ((lastupdate + MAX_UPDATE_DIFF < System.currentTimeMillis() && !paused) || forceRefresh) {
            client.forceObtainedResultsDump(true);
        }
    }
    
    public void resetData() {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                root = null;
                treeTableModel.setRoot(LockCCTNode.EMPTY);
            }
        });
    }
    
    private class Listener implements LockCCTProvider.Listener {

        @Override
        public void cctEstablished(final RuntimeCCTNode appRootNode, boolean empty) {
            if (!empty && appRootNode instanceof LockRuntimeCCTNode) {
                refreshData((LockRuntimeCCTNode)appRootNode);
            }
        }

        @Override
        public void cctReset() {
            resetData();
        }  
    }
    
    
    public void addSaveViewAction(AbstractAction saveViewAction) {
        Component actionButton = toolbar.add(saveViewAction);
        toolbar.remove(actionButton);
        toolbar.add(actionButton, 0);
        toolbar.add(new JToolBar.Separator(), 1);
    }
    
    public void addExportAction(AbstractAction exportAction) {
        Component actionButton = toolbar.add(exportAction);
        toolbar.remove(actionButton);
        toolbar.add(actionButton, 0);
    }
    
    public void exportData(int exportedFileType, ExportDataDumper eDD, String viewName) {
        switch (exportedFileType) {
            case 1: exportCSV(",", eDD); break;  //NOI18N
            case 2: exportCSV(";", eDD); break;  //NOI18N
            case 3: exportXML(eDD, viewName); break;
            case 4: exportHTML(eDD, viewName); break;
        }
    }
    
    private void exportCSV(String separator, ExportDataDumper eDD) {
        // Header
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        String indent = "   "; // NOI18N

        for (int i = 0; i < (columnNames.length); i++) {
            result.append(quote).append(columnNames[i]).append(quote).append(separator);
        }
        result.deleteCharAt(result.length()-1);
        result.append(newLine);
        // Data
        
        LockCCTNode tempTop = null;
        String mode = modeCombo.getSelectedItem().toString();
        if (MODE_THREADS.equals(mode)) {
            tempTop = root.getThreads();
        } else if (MODE_MONITORS.equals(mode)) {
            tempTop = root.getMonitors();
        }
        if (tempTop!=null) {
            //Sort the new tree
//            tempTop.sortChildren(getSortBy(sortingColumn), sortingOrder);
            for (int i = 0; i < tempTop.getNChildren(); i++) {
                LockCCTNode top = tempTop.getChild(i);
                result.append(quote).append(top.getNodeName()).append(quote).append(separator);
                result.append(top.getTimeInPerCent()).append(separator);
                result.append(getTimeInMillis(top)).append(separator);
                result.append(top.getWaits()).append(newLine);
                if (top.getNChildren()>0) {
                    for (int j = 0; j < top.getNChildren(); j++) {
                        LockCCTNode leaf = top.getChild(j);
                        result.append(quote).append(indent).append(leaf.getNodeName()).append(quote).append(separator);
                        result.append(leaf.getTimeInPerCent()).append(separator);
                        result.append(getTimeInMillis(leaf)).append(separator);
                        result.append(leaf.getWaits()).append(newLine);
                    }
                }
            }
        }
        eDD.dumpData(result);
        eDD.close();
        }
        

    private void exportXML(ExportDataDumper eDD, String viewName) {
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        String indent = "   "; // NOI18N
        
        // Header
        StringBuffer result = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+newLine+"<ExportedView Name=\""+viewName+"\" type=\""+"tree"+"\">"+newLine+"<tree>"+newLine); // NOI18N

        // Data
        LockRuntimeCCTNode tempRoot = root;
        LockCCTNode tempTop;
        
        boolean threadFirst = MODE_THREADS.equals(modeCombo.getSelectedItem().toString());
        String first, second;
        if (threadFirst) {
            tempTop = tempRoot.getThreads();
            first="thread"; //NOI18N
            second="monitor"; //NOI18N
        } else {
            tempTop = tempRoot.getMonitors();
            second="thread"; //NOI18N
            first="monitor"; //NOI18N
        }
        if (tempTop!=null) {
            //Sort the new tree
//            tempTop.sortChildren(getSortBy(sortingColumn), sortingOrder);
            for (int i = 0; i < tempTop.getNChildren(); i++) {
                LockCCTNode top = tempTop.getChild(i);
                result.append(indent).append("<").append(first).append(">").append(newLine); // NOI18N
                result.append(indent).append(indent).append("<name>").append(quote).append(top.getNodeName()).append(quote).append("</name>").append(newLine); // NOI18N
                result.append(indent).append(indent).append("<time_relative>").append(top.getTimeInPerCent()).append("</time_relative>").append(newLine); // NOI18N
                result.append(indent).append(indent).append("<time>").append(getTimeInMillis(top)).append("</time>").append(newLine); // NOI18N
                result.append(indent).append(indent).append("<waits>").append(top.getWaits()).append("</waits>").append(newLine); // NOI18N
                if (top.getNChildren()>0) {
                    for (int j = 0; j < top.getNChildren(); j++) {
                        LockCCTNode leaf = top.getChild(j);
                        result.append(indent).append(indent).append("<").append(second).append(">").append(newLine); // NOI18N
                        result.append(indent).append(indent).append(indent).append("<name>").append(quote).append(leaf.getNodeName()).append(quote).append("</name>").append(newLine); // NOI18N
                        result.append(indent).append(indent).append(indent).append("<time_relative>").append(leaf.getTimeInPerCent()).append("</time_relative>").append(newLine); // NOI18N
                        result.append(indent).append(indent).append(indent).append("<time>").append(getTimeInMillis(leaf)).append("</time>").append(newLine); // NOI18N
                        result.append(indent).append(indent).append(indent).append("<waits>").append(leaf.getWaits()).append("</waits>").append(newLine); // NOI18N
                        result.append(indent).append(indent).append("</").append(second).append(">").append(newLine); // NOI18N
                    }
                }
                result.append(indent).append("</").append(first).append(">").append(newLine); // NOI18N
            }
        }
        
        result.append("</tree>").append(newLine).append("</ExportedView>"); // NOI18N
        eDD.dumpData(result);
        eDD.close();
        
    }

    private void exportHTML(ExportDataDumper eDD, String viewName) {
        // Header
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        String indent = "   "; // NOI18N
        StringBuffer result=new StringBuffer("<HTML><HEAD><meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" /><TITLE>"+viewName+"</TITLE><style type=\"text/css\">pre.method{overflow:auto;width:600;height:30;vertical-align:baseline}pre.parent{overflow:auto;width:400;height:30;vertical-align:baseline}td.method{text-align:left;width:600}td.parent{text-align:left;width:400}td.right{text-align:right;white-space:nowrap}</style></HEAD><BODY><table border=\"1\"><tr>"); // NOI18N
        
        // Data
        LockRuntimeCCTNode tempRoot = root;
        LockCCTNode tempTop = null;
        String mode = modeCombo.getSelectedItem().toString();
        if (MODE_THREADS.equals(mode)) {
            tempTop = tempRoot.getThreads();
        } else if (MODE_MONITORS.equals(mode)) {
            tempTop = tempRoot.getMonitors();
        }
        for (int i = 0; i < (columnNames.length); i++) {
            result.append("<td>").append(columnNames[i]).append("</td>");  // NOI18N
        }
        result.append("</tr>"); // NOI18N
        
        if (tempTop!=null) {
            //Sort the new tree
//            tempTop.sortChildren(getSortBy(sortingColumn), sortingOrder);
            for (int i = 0; i < tempTop.getNChildren(); i++) {
                LockCCTNode top = tempTop.getChild(i);
                result.append("<tr><td><pre>").append(top.getNodeName()).append("</pre></td>"); // NOI18N
                result.append("<td>").append(top.getTimeInPerCent()).append("%</td>"); // NOI18N
                result.append("<td>").append(getTimeInMillis(top)).append("</td>"); // NOI18N
                result.append("<td>").append(top.getWaits()).append("</td></tr>").append(newLine); // NOI18N
                if (top.getNChildren()>0) {
                    for (int j = 0; j < top.getNChildren(); j++) {
                        LockCCTNode leaf = top.getChild(j);
                        result.append("<tr><td><pre>").append(indent).append(leaf.getNodeName()).append("</pre></td>"); // NOI18N                        
                        result.append("<td>").append(leaf.getTimeInPerCent()).append("</td>"); // NOI18N
                        result.append("<td>").append(getTimeInMillis(leaf)).append("</td>"); // NOI18N
                        result.append("<td>").append(leaf.getWaits()).append("</td></tr>"); // NOI18N
                    }
                }
            }
            result.append("</table></BODY></HTML>"); // NOI18N
        }
        eDD.dumpData(result);
        eDD.close();
        }
    
    public boolean fitsVisibleArea() {
//        return !treeTablePanel.getScrollPane().getVerticalScrollBar().isEnabled();
        return true;
    }

    public boolean hasView() {
        return modeCombo.isEnabled();
    }
    
    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if (!hasView()) return null;
        if (onlyVisibleArea) {
//            return UIUtils.createScreenshot(treeTablePanel.getScrollPane());
            return UIUtils.createScreenshot(treeTablePanel);
        } else {
            return UIUtils.createScreenshot(treeTable);
        }
    }
    
    
    // NOTE: this method only sets sortingColumn, sortOrder and sortBy, it doesn't refresh UI!
    public void setDefaultSorting() {
        setSorting(1, SortableTableModel.SORT_ORDER_DESC);
    }
    
    // NOTE: this method only sets sortingColumn, sortOrder and sortBy, it doesn't refresh UI!
    public void setSorting(int sColumn, boolean sOrder) {
        setSorting(sColumn, sOrder, false);
    }
    
    public void setSorting(int sColumn, boolean sOrder, boolean refreshUI) {
//        if (!refreshUI && sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
//            setDefaultSorting();
//        } else {
//            sortingColumn = sColumn;
//            sortingOrder = sOrder;
//        }
//        if (refreshUI) {
//            treeTable.setSortingColumn(treeTableModel.getVirtualColumn(sColumn));
//            treeTable.setSortingOrder(sOrder);
//            treeTableModel.sortByColumn(sColumn, sOrder);
//        }
    }
    
    public int getSortingColumn() {
//        return treeTableModel.getRealColumn(treeTable.getSortingColumn());
        return 0;
    }

    public boolean getSortingOrder() {
//        return treeTable.getSortingOrder();
        return false;
    }
    
    
    public void prepareResults() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (root == null) return;
                
                LockCCTNode newRoot = null;
                switch (aggregation) {
                    case BY_THREADS:
                        newRoot = root.getThreads();
                        break;
                    case BY_MONITORS:
                        newRoot = root.getMonitors();
                        break;
                }
                
//                newRoot.sortChildren(getSortBy(sortingColumn), sortingOrder);
                hbrTime.setMaxValue(getTimeInMicroSec(newRoot));
                hbrWaits.setMaxValue(newRoot.getWaits());
                treeTableModel.setRoot(newRoot);
                lastupdate = System.currentTimeMillis();
            }
        });
    }
    
    
    public void profilingSessionFinished() {
        enableLockContentionButton.setEnabled(false);
        enableLockContentionButton.setVisible(false);
        enableLockContentionLabel1.setVisible(false);
        enableLockContentionLabel2.setVisible(true);
        if (cctListener != null) {
            Collection<? extends LockCCTProvider> locksCCTProviders = Lookup.getDefault().lookupAll(LockCCTProvider.class);
            assert !locksCCTProviders.isEmpty();
            for (LockCCTProvider provider : locksCCTProviders) {
                provider.removeListener(cctListener);
            }
            cctListener = null;
        }
    }

    public void profilingSessionStarted() {
        ProfilingSessionStatus session = TargetAppRunner.getDefault().getProfilingSessionStatus();
        countsInMicrosec = session.timerCountsInSecond[0] / 1000000L;
//        countsInMicrosec = 1;

        if (cctListener == null) {
            cctListener = new Listener();
            Collection<? extends LockCCTProvider> locksCCTProviders = Lookup.getDefault().lookupAll(LockCCTProvider.class);
            assert !locksCCTProviders.isEmpty();
            for (LockCCTProvider provider : locksCCTProviders) {
                provider.addListener(cctListener);
            }
        } else {
            treeTable.clearSelection();
            treeTableModel.setRoot(LockCCTNode.EMPTY);
//            treeTable.clearSelection();
//            treeTable.updateTreeTable();
        }
        
        enableLockContentionButton.setEnabled(true);
        enableLockContentionButton.setVisible(true);
        enableLockContentionLabel1.setVisible(true);
        enableLockContentionLabel2.setVisible(false);
    }
    
    public void lockContentionDisabled() {
        lockContentionEnabled = false;
        ((CardLayout)(contentPanel.getLayout())).show(contentPanel, "DISABLED"); // NOI18N
//        updateZoomButtonsEnabledState();
//        threadsSelectionCombo.setEnabled(false);
    }

    public void lockContentionEnabled() {
        lockContentionEnabled = true;
        ((CardLayout)(contentPanel.getLayout())).show(contentPanel, "ENABLED"); // NOI18N
//        updateZoomButtonsEnabledState();
//        threadsSelectionCombo.setEnabled(true);
    }
    
    public void addLockContentionListener(ActionListener listener) {
        enableLockContentionButton.addActionListener(listener);
    }
    
    public void removeLockContentionListener(ActionListener listener) {
        enableLockContentionButton.removeActionListener(listener);
    }
    
    private long getTimeInMicroSec(LockCCTNode node) {
        return node.getTime() / countsInMicrosec;
    }

    private String getTimeInMillis(LockCCTNode node) {
        long microSec = getTimeInMicroSec(node);
        return StringUtils.mcsTimeToString(microSec);
    }
    
    private void initColumnsData() {
        columnCount = 3;
        
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];

        columnNames[0] = LOCKS_THREADS_COLUMN_NAME;
        columnToolTips[0] = LOCKS_THREADS_COLUMN_TOOLTIP;

//        columnNames[1] = TIME_COLUMN_NAME;
//        columnToolTips[1] = TIME_COLUMN_TOOLTIP;
        
        columnNames[1] = TIME_REL_COLUMN_NAME;
        columnToolTips[1] = TIME_REL_COLUMN_TOOLTIP;
        
        columnNames[2] = WAITS_COLUMN_NAME;
        columnToolTips[2] = WAITS_COLUMN_TOOLTIP;

        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 12; // NOI18N // initial width of data columns

        columnRenderers[0] = null;

//        columnWidths[1 - 1] = maxWidth;
//        columnRenderers[1] = new CustomBarCellRenderer(0, 100);
        
        columnWidths[1 - 1] = maxWidth;
        columnRenderers[1] = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnWidths[2 - 1] = maxWidth;
        columnRenderers[2] = new LabelTableCellRenderer(JLabel.TRAILING);
    }
    
    private void setColumnsData() {
//        treeTable.setTreeCellRenderer(treeCellRenderer);
//        
//        TableColumnModel colModel = treeTable.getColumnModel();
//
//        for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
//            int index = treeTableModel.getRealColumn(i);
//
//            if (index != 0) {
//                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
//                colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
//            }
//        }
    }
    
    
    protected void initColumnSelectorItems() {
//        cornerPopup.removeAll();
//
//        JCheckBoxMenuItem menuItem;
//
//        for (int i = 0; i < realTreeTableModel.getColumnCount(); i++) {
//            menuItem = new JCheckBoxMenuItem(realTreeTableModel.getColumnName(i));
//            menuItem.setActionCommand(Integer.valueOf(i).toString());
//            addMenuItemListener(menuItem);
//
//            if (treeTable != null) {
//                menuItem.setState(treeTableModel.isRealColumnVisible(i));
//
//                if (i == 0) {
//                    menuItem.setEnabled(false);
//                }
//            } else {
//                menuItem.setState(true);
//            }
//
//            cornerPopup.add(menuItem);
//        }
//
//        cornerPopup.pack();
    }
    
    private void addMenuItemListener(JCheckBoxMenuItem menuItem) {
//        menuItem.addActionListener(new java.awt.event.ActionListener() {
//                public void actionPerformed(java.awt.event.ActionEvent e) {
//                    boolean sortResults = false;
//                    int column = Integer.parseInt(e.getActionCommand());
//                    sortingColumn = treeTable.getSortingColumn();
//
//                    int realSortingColumn = treeTableModel.getRealColumn(sortingColumn);
//                    boolean isColumnVisible = treeTableModel.isRealColumnVisible(column);
//
//                    // Current sorting column is going to be hidden
//                    if ((isColumnVisible) && (column == realSortingColumn)) {
//                        // Try to set next column as a sortingColumn. If currentSortingColumn is the last column, set previous
//                        // column as a sorting Column (one column is always visible).
//                        sortingColumn = ((sortingColumn + 1) == treeTableModel.getColumnCount()) ? (sortingColumn - 1)
//                                                                                                 : (sortingColumn + 1);
//                        realSortingColumn = treeTableModel.getRealColumn(sortingColumn);
//                        sortResults = true;
//                    }
//
//                    treeTableModel.setRealColumnVisibility(column, !isColumnVisible);
//                    treeTable.createDefaultColumnsFromModel();
//                    treeTable.updateTreeTableHeader();
//                    sortingColumn = treeTableModel.getVirtualColumn(realSortingColumn);
//
//                    if (sortResults) {
//                        sortingOrder = treeTableModel.getInitialSorting(sortingColumn);
//                        treeTableModel.sortByColumn(sortingColumn, sortingOrder);
//                        treeTable.updateTreeTable();
//                    }
//
//                    treeTable.setSortingColumn(sortingColumn);
//                    treeTable.setSortingOrder(sortingOrder);
//                    treeTable.getTableHeader().repaint();
//                    setColumnsData();
//
//                    // TODO [ui-persistence]
//                }
//            });
    }
    
    
    public Component getToolbar() {
        return toolbar.getComponent();
    }
    
    
    private class LocksTreeTableModel extends ProfilerTreeTableModel.Abstract {
        
        private LocksTreeTableModel() {
            super(LockCCTNode.EMPTY);
//            super(LockCCTNode.EMPTY, true, sortingColumn, sortingOrder);
        }

        public boolean isCellEditable(TreeNode node, int columnIndex) {
            return false;
        }

        public Class getColumnClass(int column) {
            if (column == 0) {
                return JTree.class;
            } else if (column == 1) {
                return Long.class;
            } else if (column == 2) {
                return Long.class;
            }
            return null;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

//        public String getColumnToolTipText(int col) {
//            return columnToolTips[col];
//        }
//
//        public boolean getInitialSorting(int column) {
//            return column == 0;
//        }

//        public boolean isLeaf(Object node) {
//            return ((LockCCTNode)node).getNChildren() == 0;
//        }

        public Object getValueAt(TreeNode node, int columnIndex) {
            LockCCTNode lnode = (LockCCTNode)node;

            switch (columnIndex) {
                case 0:
                    return lnode;
                case 1:
                    return getTimeInMicroSec(lnode);
//                    return lnode;
//                    return getTimeInMillis(lnode) + " ms (" // NOI18N
//                    + percentFormat.format(lnode.getTimeInPerCent() / 100) + ")"; // NOI18N
                case 2:
                    return lnode.getWaits();
                    
                default:
                    return null;
            }
        }
        
        public void setValueAt(Object aValue, TreeNode node, int column) {}

//        public void sortByColumn(int column, boolean order) {
//            sortingColumn = column;
//            sortingOrder = order;
//
//            LockCCTNode _root = (LockCCTNode)root;
//            _root.sortChildren(getSortBy(column), order);
//        }
    }
    
    
//    private int getSortBy(int column) {
//        switch (column) {
//            case 0:
//                return LockCCTNode.SORT_BY_NAME;
//            case 1:
//                return LockCCTNode.SORT_BY_TIME;
//            case 2:
//                return LockCCTNode.SORT_BY_TIME;
//            case 3:
//                return LockCCTNode.SORT_BY_WAITS;
//            default:
//                return -1;
//        }
//    }
    
    private JPopupMenu createTablePopup() {
        JPopupMenu popup = new JPopupMenu();
        return popup;
    }
    
    private void showTablePopup(Component invoker, int x, int y) {
        tablePopup.show(invoker, x, y);
    }
    
    private class MouseListener extends MouseAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void updateSelection(int row) {
            treeTable.requestFocusInWindow();
            if (row != -1) treeTable.setRowSelectionInterval(row, row);
            else treeTable.clearSelection();
        }

        public void mousePressed(final MouseEvent e) {
            final int row = treeTable.rowAtPoint(e.getPoint());
            updateSelection(row);
            if (e.isPopupTrigger()) showTablePopup(e.getComponent(), e.getX(), e.getY());
        }

        public void mouseReleased(MouseEvent e) {
            int row = treeTable.rowAtPoint(e.getPoint());
            updateSelection(row);
            if (e.isPopupTrigger()) showTablePopup(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    private class KeyListener extends KeyAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                    || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                int selectedRow = treeTable.getSelectedRow();

                if (selectedRow != -1) {
                    Rectangle rowBounds = treeTable.getCellRect(selectedRow, 0, true);
                    showTablePopup(treeTable, rowBounds.x + (rowBounds.width / 2), rowBounds.y + (rowBounds.height / 2));
                }
            }
        }
    }
    
}
