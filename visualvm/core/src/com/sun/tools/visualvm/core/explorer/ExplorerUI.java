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
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 *
 * @author Jiri Sedlacek
 */
class ExplorerUI extends JPanel {
    
    private static ExplorerUI instance;
    
    private boolean vetoTreeExpansion = false;
    
    
    public static ExplorerUI instance() {
        if (instance == null) instance = new ExplorerUI();
        return instance;
    }
    
    private ExplorerUI() {
        initComponents();
    }
    
    public JTree getTree() {
        return explorerTree;
    }
    
    
    private void performDefaultAction(TreePath path) {
        if (path == null) return;
    
        ExplorerNode node = (ExplorerNode)path.getLastPathComponent();
        DataSource dataSource = node.getUserObject();
        Action defaultAction = getDefaultAction(dataSource);
        if (defaultAction == null) return;
    
        defaultAction.actionPerformed(new ActionEvent(dataSource, 0, "Default Action"));
    }
    
    private static Action getDefaultAction(DataSource dataSource) {
        return ExplorerContextMenuFactory.sharedInstance().getDefaultActionFor(dataSource);
    }
            
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // explorerTree
        explorerTree = new JTree(ExplorerModelBuilder.getInstance().getModel()) {
            protected void processMouseEvent(MouseEvent e) {
                vetoTreeExpansion = false;
                if (e.getModifiers() == InputEvent.BUTTON1_MASK && e.getClickCount() >= getToggleClickCount()) {
                    TreePath path = getPathForLocation(e.getX(), e.getY());
                    ExplorerNode node = path != null ? (ExplorerNode)path.getLastPathComponent() : null;
                    if (node != null && getDefaultAction(node.getUserObject()) != null && getPathBounds(path).contains(e.getX(), e.getY())) vetoTreeExpansion = true;
                }
                super.processMouseEvent(e);
            }
        };
        explorerTree.setRootVisible(false);
        explorerTree.setShowsRootHandles(true);
        explorerTree.setRowHeight(getTreeRowHeight());
        explorerTree.setCellRenderer(new ExplorerNodeRenderer());
        explorerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        explorerTree.addMouseListener(new ExplorerTreeMouseAdapter());
        
        // explorerTreeScrollPane
        JScrollPane explorerTreeScrollPane = new JScrollPane(explorerTree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        explorerTreeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        explorerTreeScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        
        // Keyboard actions definition
        String DEFAULT_ACTION_KEY = "DEFAULT_ACTION"; // NOI18N
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), DEFAULT_ACTION_KEY); // NOI18N
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), DEFAULT_ACTION_KEY); // NOI18N
        getActionMap().put(DEFAULT_ACTION_KEY, new AbstractAction() { public void actionPerformed(ActionEvent e) { performDefaultAction(explorerTree.getSelectionPath()); }}); // NOI18N
        
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
        return new JLabel("XXX").getPreferredSize().height + 4;
    }
    
    
    private JTree explorerTree;
    
    
    private class ExplorerTreeMouseAdapter extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                // Select path on location or clear selection
                TreePath path = explorerTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) explorerTree.setSelectionPath(path);
                else explorerTree.clearSelection();
            }
        }
    
        public void mouseClicked(MouseEvent e) {
            if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
                
                // Determine the node for which to display context menu
                TreePath selectedPath = explorerTree.getSelectionPath();
                ExplorerNode selectedNode = selectedPath != null ? (ExplorerNode)selectedPath.getLastPathComponent() : null;
        
                // Create popup menu and display it
                JPopupMenu popupMenu = ExplorerContextMenuFactory.sharedInstance().createPopupMenuFor(selectedNode == null ? DataSource.ROOT : selectedNode.getUserObject());
                if (popupMenu != null) popupMenu.show(explorerTree, e.getX(), e.getY());
                
            } else if (e.getModifiers() == InputEvent.BUTTON1_MASK && e.getClickCount() == explorerTree.getToggleClickCount()) {
                performDefaultAction(explorerTree.getPathForLocation(e.getX(), e.getY()));
            }
        }
    }

}
