/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */


package com.sun.tools.visualvm.core.explorer;


import com.sun.tools.visualvm.core.datasource.DataSource;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
class ExplorerComponent extends JPanel {

    private static final Color MAC_TREE_BG_FOCUSED = new Color(214, 221, 229);
    private static final Color MAC_TREE_BG_NOTFOCUSED = new Color(232, 232, 232);
    
    private static ExplorerComponent instance;
    
    private JTree explorerTree;
    private boolean vetoTreeExpansion = false;
    
    
    public static synchronized ExplorerComponent instance() {
        if (instance == null) instance = new ExplorerComponent();
        return instance;
    }
    
    private ExplorerComponent() {
        initComponents();
    }
    
    public JTree getTree() {
        return explorerTree;
    }


    public boolean requestFocusInWindow() {
        if (explorerTree != null) return explorerTree.requestFocusInWindow();
        else return super.requestFocusInWindow();
    }
    
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // explorerTree
        explorerTree = new JTree(ExplorerModelBuilder.getInstance().getModel()) {
            protected void processMouseEvent(MouseEvent e) {
                vetoTreeExpansion = false;
                if (e.getModifiers() == InputEvent.BUTTON1_MASK && e.getClickCount() >= getToggleClickCount()) {
                    Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().getSelectedDataSources();
                    if (getDefaultAction(selectedDataSources) != null) vetoTreeExpansion = true;
                }
                super.processMouseEvent(e);
            }
            public void updateUI() {
                super.updateUI();
                setCellRenderer(new ExplorerNodeRenderer());
            }
        };
        explorerTree.setRootVisible(false);
        explorerTree.setShowsRootHandles(true);
        explorerTree.setRowHeight(getTreeRowHeight());
        explorerTree.setCellRenderer(new ExplorerNodeRenderer());
        explorerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        explorerTree.addKeyListener(new ExplorerTreeKeyAdapter());
        explorerTree.addMouseListener(new ExplorerTreeMouseAdapter());

        // Aqua LaF customizations
        if (UIUtils.isAquaLookAndFeel()) {
            final Window mainWindow = WindowManager.getDefault().getMainWindow();
            final Window[] ownerWindow = new Window[1];
            final WindowFocusListener focusListener = new WindowFocusListener() {
                public void windowGainedFocus(WindowEvent e) { update(true, e); }
                public void windowLostFocus(WindowEvent e)   { update(false, e); }
                private void update(boolean hasFocus, WindowEvent e) {
                    Window oppositeWindow = e.getOppositeWindow();
                    boolean focus = hasFocus || oppositeWindow == mainWindow ||
                                    oppositeWindow == ownerWindow[0];
                    if (focus) explorerTree.setBackground(MAC_TREE_BG_FOCUSED);
                    else explorerTree.setBackground(MAC_TREE_BG_NOTFOCUSED);
                }
            };
            mainWindow.addWindowFocusListener(focusListener);
            explorerTree.addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                        Window newOwnerWindow = SwingUtilities.getWindowAncestor(explorerTree);
                        if (ownerWindow[0] == newOwnerWindow) return;
                        if (ownerWindow[0] != null && ownerWindow[0] != mainWindow)
                            ownerWindow[0].removeWindowFocusListener(focusListener);
                        ownerWindow[0] = newOwnerWindow;
                        if (ownerWindow[0] != null && ownerWindow[0] != mainWindow)
                            ownerWindow[0].addWindowFocusListener(focusListener);
                    }
                }
            });
        }
        
        // explorerTreeScrollPane
        JScrollPane explorerTreeScrollPane = new JScrollPane(explorerTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        explorerTreeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        explorerTreeScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        
        // Keyboard actions definition
        String DEFAULT_ACTION_KEY = "DEFAULT_ACTION"; // NOI18N
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), DEFAULT_ACTION_KEY); // NOI18N
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), DEFAULT_ACTION_KEY); // NOI18N
        getActionMap().put(DEFAULT_ACTION_KEY, new AbstractAction() { public void actionPerformed(ActionEvent e) { performDefaultAction(); }}); // NOI18N
        
        // Control tree expansion
        explorerTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                if (!vetoTreeExpansion) return;
                vetoTreeExpansion = false;
                throw new ExpandVetoException(event);
            }
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                if (!vetoTreeExpansion) return;
                vetoTreeExpansion = false;
                throw new ExpandVetoException(event);
            }
        });
        
        add(explorerTreeScrollPane, BorderLayout.CENTER);
    }
    
    private static int getTreeRowHeight() {
//        // NOTE: At least on GTK this returns -1
//        int rowHeight = UIManager.getInt("Tree.rowHeight");
//        if (rowHeight != -1) return rowHeight + 2; else 
        return new JLabel("XXX").getPreferredSize().height + 4; // NOI18N
    }
    
    
    private static Action getDefaultAction(Set<DataSource> dataSources) {
        return ExplorerContextMenuFactory.instance().getDefaultActionFor(dataSources);
    }
    
    private void performDefaultAction() {
        Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().getSelectedDataSources();
        Action defaultAction = getDefaultAction(selectedDataSources);
        if (defaultAction != null) defaultAction.actionPerformed(new ActionEvent(selectedDataSources, 0, "Default Action"));    // NOI18N
    }
    
    private void displayContextMenu(int x, int y) {
        JPopupMenu popupMenu = ExplorerContextMenuFactory.instance().createPopupMenu();
        if (popupMenu != null) popupMenu.show(explorerTree, x, y);
    }
    
    private class ExplorerTreeKeyAdapter extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                            || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                
                e.consume();
                
                int x;
                int y;
                TreePath path = explorerTree.getSelectionPath();
                
                if (path != null) {
                    Rectangle pathRect = explorerTree.getPathBounds(path);
                    x = pathRect.x;
                    y = pathRect.y;
                } else {
                    Point pathPoint = new Point(explorerTree.getWidth() / 3, explorerTree.getHeight() / 3);
                    x = pathPoint.x;
                    y = pathPoint.y;
                }
                
                displayContextMenu(x, y);
            }
        }
    }
    
    private class ExplorerTreeMouseAdapter extends MouseAdapter {
        private void updatePathSelection(TreePath path, MouseEvent e) {
            if (path != null) {
                if (!explorerTree.isPathSelected(path))
                    explorerTree.setSelectionPath(path);
            } else {
                explorerTree.clearSelection();
            }
        }

        public void mousePressed(MouseEvent e) {
            TreePath path = explorerTree.getPathForLocation(e.getX(), e.getY());
            updatePathSelection(path, e);
            if (e.isPopupTrigger()) displayContextMenu(e.getX(), e.getY());
        }
        
        public void mouseReleased(MouseEvent e) {
            TreePath path = explorerTree.getPathForLocation(e.getX(), e.getY());
            updatePathSelection(path, e);
            if (e.isPopupTrigger()) displayContextMenu(e.getX(), e.getY());
        }
    
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) &&
                    e.getClickCount() == explorerTree.getToggleClickCount()) {
                performDefaultAction();
            }
        }
    }

}
