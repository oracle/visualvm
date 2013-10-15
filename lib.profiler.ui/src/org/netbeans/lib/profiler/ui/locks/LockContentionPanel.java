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
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.TargetAppRunner;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.locks.LockCCTNode;
import org.netbeans.lib.profiler.results.locks.LockCCTProvider;
import org.netbeans.lib.profiler.results.locks.LockRuntimeCCTNode;
import org.netbeans.lib.profiler.ui.ResultsPanel;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.FlatToolBar;
import org.netbeans.lib.profiler.ui.components.JTreeTable;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.components.table.CustomBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelBracketTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.SortableTableModel;
import org.netbeans.lib.profiler.ui.components.tree.EnhancedTreeCellRenderer;
import org.netbeans.lib.profiler.ui.components.treetable.AbstractTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.ExtendedTreeTableModel;
import org.netbeans.lib.profiler.ui.components.treetable.JTreeTablePanel;
import org.netbeans.lib.profiler.ui.components.treetable.TreeTableModel;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public class LockContentionPanel extends ResultsPanel {
    
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
    
    private final ProfilerToolbar toolbar;
    
    private final LocksTreeTableModel realTreeTableModel;
    private final ExtendedTreeTableModel treeTableModel;
    private final JTreeTable treeTable;
    private final JTreeTablePanel treeTablePanel;
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
    
    private boolean lockContentionEnabled;
    private final JPanel contentPanel;
    private final JPanel notificationPanel;
    private final JButton enableLockContentionButton;
    private final JLabel enableLockContentionLabel1;
    private final JLabel enableLockContentionLabel2;
    
    private LockRuntimeCCTNode root;
    private Listener cctListener;
    private long countsInMicrosec;
    
    
    public LockContentionPanel() {        
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
        
        realTreeTableModel = new LocksTreeTableModel();
        treeTableModel = new ExtendedTreeTableModel(realTreeTableModel);
        
        treeTable = new JTreeTable(treeTableModel) {
                public void doLayout() {
                    int columnsWidthsSum = 0;
                    int realFirstColumn = -1;

                    TableColumnModel colModel = getColumnModel();

                    for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
                        if (treeTableModel.getRealColumn(i) == 0) {
                            realFirstColumn = i;
                        } else {
                            columnsWidthsSum += colModel.getColumn(i).getPreferredWidth();
                        }
                    }

                    if (realFirstColumn != -1) {
                        colModel.getColumn(realFirstColumn).setPreferredWidth(getWidth() - columnsWidthsSum);
                    }

                    super.doLayout();
                };
            };
        treeTable.getTree().setRootVisible(false);
        treeTable.getTree().setShowsRootHandles(true);
//        treeTable.addMouseListener(new MouseListener());
//        treeTable.addKeyListener(new KeyListener());
        treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treeTable.setGridColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);
        treeTable.setSelectionBackground(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        treeTable.setSelectionForeground(UIConstants.TABLE_SELECTION_FOREGROUND_COLOR);
        treeTable.setShowHorizontalLines(UIConstants.SHOW_TABLE_HORIZONTAL_GRID);
        treeTable.setShowVerticalLines(UIConstants.SHOW_TABLE_VERTICAL_GRID);
        treeTable.setRowMargin(UIConstants.TABLE_ROW_MARGIN);
        treeTable.setRowHeight(UIUtils.getDefaultRowHeight() + 2);
        treeTable.getTree().setLargeModel(true);

        // Disable traversing table cells using TAB and Shift+TAB
        Set keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        treeTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, keys);

        keys = new HashSet(treeTable.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        keys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_MASK));
        treeTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, keys);
        
        setColumnsData();
        
        treeTablePanel = new JTreeTablePanel(treeTable);
        treeTablePanel.clearBorders();
        
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
        
        add(contentPanel, BorderLayout.CENTER);
        
        tablePopup = createTablePopup();
        
        cornerPopup = new JPopupMenu();
        treeTablePanel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, createHeaderPopupCornerButton(cornerPopup));
        
        setDefaultSorting();
        prepareResults(); // Disables combo
    }
    
    private class Listener implements LockCCTProvider.Listener {

        @Override
        public void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
            if (!empty && appRootNode instanceof LockRuntimeCCTNode) {
                root = (LockRuntimeCCTNode)appRootNode;
                prepareResults();
            }
        }

        @Override
        public void cctReset() {
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
    
    private void
            exportCSV(String separator, ExportDataDumper eDD) {
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
            tempTop.sortChildren(getSortBy(sortingColumn), sortingOrder);
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
            tempTop.sortChildren(getSortBy(sortingColumn), sortingOrder);
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
            tempTop.sortChildren(getSortBy(sortingColumn), sortingOrder);
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
        return !treeTablePanel.getScrollPane().getVerticalScrollBar().isEnabled();
    }

    public boolean hasView() {
        return modeCombo.isEnabled();
    }
    
    public BufferedImage getCurrentViewScreenshot(boolean onlyVisibleArea) {
        if (!hasView()) return null;
        if (onlyVisibleArea) {
            return UIUtils.createScreenshot(treeTablePanel.getScrollPane());
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
        if (!refreshUI && sColumn == CommonConstants.SORTING_COLUMN_DEFAULT) {
            setDefaultSorting();
        } else {
            sortingColumn = sColumn;
            sortingOrder = sOrder;
        }
        if (refreshUI) {
            treeTable.setSortingColumn(treeTableModel.getVirtualColumn(sColumn));
            treeTable.setSortingOrder(sOrder);
            treeTableModel.sortByColumn(sColumn, sOrder);
        }
    }
    
    public int getSortingColumn() {
        return treeTableModel.getRealColumn(treeTable.getSortingColumn());
    }

    public boolean getSortingOrder() {
        return treeTable.getSortingOrder();
    }
    
    
    public void prepareResults() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                modeCombo.setEnabled(root != null);
                if (root == null) return;
                
                String mode = modeCombo.getSelectedItem().toString();
                LockCCTNode newRoot = null;
                if (MODE_THREADS.equals(mode)) {
                    newRoot = root.getThreads();
                } else if (MODE_MONITORS.equals(mode)) {
                    newRoot = root.getMonitors();
                }
                
                newRoot.sortChildren(getSortBy(sortingColumn), sortingOrder);
                treeTable.changeRoot(newRoot);
            }
        });
    }
    
    
    public void profilingSessionFinished() {
        enableLockContentionButton.setEnabled(false);
        enableLockContentionButton.setVisible(false);
        enableLockContentionLabel1.setVisible(false);
        enableLockContentionLabel2.setVisible(true);
    }

    public void profilingSessionStarted() {
        ProfilingSessionStatus session = TargetAppRunner.getDefault().getProfilingSessionStatus();
        countsInMicrosec = session.timerCountsInSecond[0] / 1000000L;

        if (cctListener == null) {
            cctListener = new Listener();
            LockCCTProvider cctProvider = Lookup.getDefault().lookup(LockCCTProvider.class);
            assert cctProvider != null;
            cctProvider.addListener(cctListener);
        } else {
            treeTableModel.setRoot(LockCCTNode.EMPTY);
            treeTable.clearSelection();
            treeTable.updateTreeTable();
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
    
    private String getTimeInMillis(LockCCTNode node) {
        long microSec = node.getTime() / countsInMicrosec;
        return StringUtils.mcsTimeToString(microSec);
    }
    
    private void initColumnsData() {
        columnCount = 4;
        
        columnWidths = new int[columnCount - 1]; // Width of the first column fits to width
        columnNames = new String[columnCount];
        columnToolTips = new String[columnCount];
        columnRenderers = new TableCellRenderer[columnCount];

        columnNames[0] = LOCKS_THREADS_COLUMN_NAME;
        columnToolTips[0] = LOCKS_THREADS_COLUMN_TOOLTIP;

        columnNames[1] = TIME_COLUMN_NAME;
        columnToolTips[1] = TIME_COLUMN_TOOLTIP;
        
        columnNames[2] = TIME_REL_COLUMN_NAME;
        columnToolTips[2] = TIME_REL_COLUMN_TOOLTIP;
        
        columnNames[3] = WAITS_COLUMN_NAME;
        columnToolTips[3] = WAITS_COLUMN_TOOLTIP;

        int maxWidth = getFontMetrics(getFont()).charWidth('W') * 12; // NOI18N // initial width of data columns

        columnRenderers[0] = null;

        columnWidths[1 - 1] = maxWidth;
        columnRenderers[1] = new CustomBarCellRenderer(0, 100);
        
        columnWidths[2 - 1] = maxWidth;
        columnRenderers[2] = new LabelBracketTableCellRenderer(JLabel.TRAILING);

        columnWidths[3 - 1] = maxWidth;
        columnRenderers[3] = new LabelTableCellRenderer(JLabel.TRAILING);
    }
    
    private void setColumnsData() {
        treeTable.setTreeCellRenderer(treeCellRenderer);
        
        TableColumnModel colModel = treeTable.getColumnModel();

        for (int i = 0; i < treeTableModel.getColumnCount(); i++) {
            int index = treeTableModel.getRealColumn(i);

            if (index != 0) {
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
                colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
            }
        }
    }
    
    
    protected void initColumnSelectorItems() {
        cornerPopup.removeAll();

        JCheckBoxMenuItem menuItem;

        for (int i = 0; i < realTreeTableModel.getColumnCount(); i++) {
            menuItem = new JCheckBoxMenuItem(realTreeTableModel.getColumnName(i));
            menuItem.setActionCommand(Integer.valueOf(i).toString());
            addMenuItemListener(menuItem);

            if (treeTable != null) {
                menuItem.setState(treeTableModel.isRealColumnVisible(i));

                if (i == 0) {
                    menuItem.setEnabled(false);
                }
            } else {
                menuItem.setState(true);
            }

            cornerPopup.add(menuItem);
        }

        cornerPopup.pack();
    }
    
    private void addMenuItemListener(JCheckBoxMenuItem menuItem) {
        menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    boolean sortResults = false;
                    int column = Integer.parseInt(e.getActionCommand());
                    sortingColumn = treeTable.getSortingColumn();

                    int realSortingColumn = treeTableModel.getRealColumn(sortingColumn);
                    boolean isColumnVisible = treeTableModel.isRealColumnVisible(column);

                    // Current sorting column is going to be hidden
                    if ((isColumnVisible) && (column == realSortingColumn)) {
                        // Try to set next column as a sortingColumn. If currentSortingColumn is the last column, set previous
                        // column as a sorting Column (one column is always visible).
                        sortingColumn = ((sortingColumn + 1) == treeTableModel.getColumnCount()) ? (sortingColumn - 1)
                                                                                                 : (sortingColumn + 1);
                        realSortingColumn = treeTableModel.getRealColumn(sortingColumn);
                        sortResults = true;
                    }

                    treeTableModel.setRealColumnVisibility(column, !isColumnVisible);
                    treeTable.createDefaultColumnsFromModel();
                    treeTable.updateTreeTableHeader();
                    sortingColumn = treeTableModel.getVirtualColumn(realSortingColumn);

                    if (sortResults) {
                        sortingOrder = treeTableModel.getInitialSorting(sortingColumn);
                        treeTableModel.sortByColumn(sortingColumn, sortingOrder);
                        treeTable.updateTreeTable();
                    }

                    treeTable.setSortingColumn(sortingColumn);
                    treeTable.setSortingOrder(sortingOrder);
                    treeTable.getTableHeader().repaint();
                    setColumnsData();

                    // TODO [ui-persistence]
                }
            });
    }
    
    
    public Component getToolbar() {
        return toolbar.getComponent();
    }
    
    
    private class LocksTreeTableModel extends AbstractTreeTableModel {
        
        private LocksTreeTableModel() {
            super(LockCCTNode.EMPTY, true, sortingColumn, sortingOrder);
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Class getColumnClass(int column) {
            if (column == 0) {
                return TreeTableModel.class;
            } else {
                return Object.class;
            }
        }

        public int getColumnCount() {
            return columnCount;
        }

        public String getColumnName(int columnIndex) {
            return columnNames[columnIndex];
        }

        public String getColumnToolTipText(int col) {
            return columnToolTips[col];
        }

        public boolean getInitialSorting(int column) {
            return column == 0;
        }

        public boolean isLeaf(Object node) {
            return ((LockCCTNode)node).getNChildren() == 0;
        }

        public Object getValueAt(Object object, int columnIndex) {
            LockCCTNode node = (LockCCTNode)object;

            switch (columnIndex) {
                case 0:
                    return node;
                case 1:
                    return node.getTimeInPerCent();
                case 2:
                    return getTimeInMillis(node) + " ms (" // NOI18N
                    + percentFormat.format(node.getTimeInPerCent() / 100) + ")"; // NOI18N
                case 3:
                    return node.getWaits();
                    
                default:
                    return null;
            }
        }

        public void sortByColumn(int column, boolean order) {
            sortingColumn = column;
            sortingOrder = order;

            LockCCTNode _root = (LockCCTNode)root;
            _root.sortChildren(getSortBy(column), order);
        }
    }
    
    
    private int getSortBy(int column) {
        switch (column) {
            case 0:
                return LockCCTNode.SORT_BY_NAME;
            case 1:
                return LockCCTNode.SORT_BY_TIME;
            case 2:
                return LockCCTNode.SORT_BY_TIME;
            case 3:
                return LockCCTNode.SORT_BY_WAITS;
            default:
                return -1;
        }
    }
    
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
