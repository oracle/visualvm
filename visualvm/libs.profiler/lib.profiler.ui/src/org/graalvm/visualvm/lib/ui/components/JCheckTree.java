/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.tree.CheckTreeCellRenderer;
import org.graalvm.visualvm.lib.ui.components.tree.CheckTreeNode;
import org.graalvm.visualvm.lib.ui.components.tree.TreeCellRendererPersistent;


/**
 *
 * @author Jiri Sedlacek
 */
public class JCheckTree extends JExtendedTree {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------

    // --- CheckTreeListener interface definition --------------------------------
    public static interface CheckTreeListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        void checkNodeToggled(TreePath node, boolean before);

        void checkTreeChanged(Collection<CheckTreeNode> changedNodes);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // ---------------------------------------------------------------------------

    // --- Custom TreeUI implementation ------------------------------------------
    private class CheckTreeUI extends BasicTreeUI {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected boolean isToggleEvent(MouseEvent event) {
            if (isOverCheckBox(event.getX(), event.getY())) {
                return false;
            }

            return super.isToggleEvent(event);
        }
    }

    // ---------------------------------------------------------------------------

    // --- Listeners implementation ----------------------------------------------
    private class PrivateComponentListener implements MouseListener, KeyListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                TreePath[] paths = getSelectionPaths();

                if ((paths != null) && (paths.length > 0)) {
                    Collection changedNodes = new ArrayList();

                    for (int i = 0; i < paths.length; i++) {
                        TreePath path = paths[i];

                        if ((path != null) && (path.getPathCount() > 0) && path.getLastPathComponent() instanceof CheckTreeNode
                                && (((CheckTreeNode) path.getLastPathComponent()).isLeaf() || (i == (paths.length - 1)))) {
                            fireNodeToggled(path, true);
                        }

                        changedNodes.addAll(togglePathState(path));
                        fireNodeToggled(path, false);
                    }

                    treeDidChange();
                    fireCheckTreeChanged(changedNodes);
                }
            }
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (isOverCheckBox(e.getX(), e.getY())) {
                TreePath path = getPathForLocation(e.getX(), e.getY());
                fireNodeToggled(path, true);

                Collection changedNodes = togglePathState(getPathForLocation(e.getX(), e.getY()));
                treeDidChange();
                fireNodeToggled(path, false);
                fireCheckTreeChanged(changedNodes);
            }
        }

        public void mouseReleased(MouseEvent e) {
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private PrivateComponentListener componentListener = new PrivateComponentListener();

    // ---------------------------------------------------------------------------

    // --- CheckTreeListener implementation --------------------------------------
    private Collection<CheckTreeListener> checkTreeListeners = new CopyOnWriteArraySet<>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of JCheckTree
     */
    public JCheckTree() {
        if (!UIUtils.isGTKLookAndFeel()) {
            setUI(new CheckTreeUI());
        }

        setCellRenderer(new CheckTreeCellRenderer());
        setModel(getDefaultTreeModel());
        addMouseListener(componentListener);
        addKeyListener(componentListener);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- CellTip support -------------------------------------------------------
    public Point getCellTipLocation() {
        if (rendererRect == null) {
            return null;
        }

        if (getCellRenderer() instanceof TreeCellRendererPersistent) {
            return new Point((rendererRect.getLocation().x + CheckTreeCellRenderer.getCheckBoxDimension().width) - 1,
                             rendererRect.getLocation().y - 1);
        } else {
            return super.getCellTipLocation();
        }
    }

    public void addCheckTreeListener(CheckTreeListener listener) {
        if (listener != null) {
            checkTreeListeners.add(listener);
        }
    }

    // ---------------------------------------------------------------------------

    // --- Static test frame -----------------------------------------------------
    public static void main(String[] args) {
        JFrame frame = new JFrame("JCheckTreeTester"); // NOI18N

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); // NOI18N
        } catch (Exception e) {
        }

        ;

        JCheckTree checkTree = new JCheckTree();
        checkTree.addCheckTreeListener(new CheckTreeListener() {
                public void checkTreeChanged(Collection changedNodes) {
                    System.out.println(changedNodes);
                }

                public void checkNodeToggled(TreePath path, boolean before) {
                    System.out.println("Node toggled"); // NOI18N
                }
            });

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new JScrollPane(checkTree));
        frame.pack();
        frame.setVisible(true);
    }

    public void processMouseEvent(MouseEvent e) {
        if (e instanceof MouseWheelEvent) {
            Component target = JCheckTree.this.getParent();
            if (target == null || !(target instanceof JViewport))
                target = JCheckTree.this;
            MouseEvent mwe = SwingUtilities.convertMouseEvent(
                    JCheckTree.this, (MouseWheelEvent)e, target);
            target.dispatchEvent((MouseWheelEvent)mwe);
        } else {
            super.processMouseEvent((MouseEvent)e);
        }
    }

    public void removeCheckTreeListener(CheckTreeListener listener) {
        checkTreeListeners.remove(listener);
    }

    // ---------------------------------------------------------------------------

    // --- Default model definition ----------------------------------------------
    protected static TreeModel getDefaultTreeModel() {
        CheckTreeNode root = new CheckTreeNode("JTree"); // NOI18N
        CheckTreeNode parent;

        parent = new CheckTreeNode("colors"); // NOI18N
        root.add(parent);
        parent.add(new CheckTreeNode("blue")); // NOI18N
        parent.add(new CheckTreeNode("violet")); // NOI18N
        parent.add(new CheckTreeNode("red")); // NOI18N
        parent.add(new CheckTreeNode("yellow")); // NOI18N

        parent = new CheckTreeNode("sports"); // NOI18N
        root.add(parent);
        parent.add(new CheckTreeNode("basketball")); // NOI18N
        parent.add(new CheckTreeNode("soccer")); // NOI18N
        parent.add(new CheckTreeNode("football")); // NOI18N
        parent.add(new CheckTreeNode("hockey")); // NOI18N

        parent = new CheckTreeNode("food"); // NOI18N
        root.add(parent);
        parent.add(new CheckTreeNode("hot dogs")); // NOI18N
        parent.add(new CheckTreeNode("pizza")); // NOI18N
        parent.add(new CheckTreeNode("ravioli")); // NOI18N
        parent.add(new CheckTreeNode("bananas")); // NOI18N

        return new DefaultTreeModel(root);
    }

    protected void processCellTipMouseMove(MouseEvent e) {
        // Identify treetable row and column at cursor
        TreePath currentTreePath = null;

        try {
            currentTreePath = getPathForLocation(e.getX(), e.getY()); // workaround for random Issue 113634
        } catch (Exception ex) {
        }

        // Return if cursor isn't at any cell
        if (currentTreePath == null) {
            CellTipManager.sharedInstance().setEnabled(false);
            lastTreePath = currentTreePath;

            return;
        }

        Component cellRenderer;
        Component cellRendererPersistent;
        int row = getRowForPath(lastTreePath);

        TreeCellRenderer treeCellRenderer = getCellRenderer();

        if (!(treeCellRenderer instanceof TreeCellRendererPersistent)) {
            return;
        }

        cellRenderer = treeCellRenderer.getTreeCellRendererComponent(JCheckTree.this, currentTreePath.getLastPathComponent(),
                                                                     false, isExpanded(row),
                                                                     getModel().isLeaf(currentTreePath.getLastPathComponent()),
                                                                     row, false);
        cellRendererPersistent = ((TreeCellRendererPersistent) treeCellRenderer).getTreeCellRendererComponentPersistent(JCheckTree.this,
                                                                                                                        currentTreePath
                                                                                                                        .getLastPathComponent(),
                                                                                                                        false,
                                                                                                                        isExpanded(row),
                                                                                                                        getModel()
                                                                                                                            .isLeaf(currentTreePath
                                                                                                                                    .getLastPathComponent()),
                                                                                                                        row, false);

        // Return if celltip is not supported for the cell
        if (cellRenderer == null) {
            CellTipManager.sharedInstance().setEnabled(false);
            lastTreePath = currentTreePath;

            return;
        }

        Point cellStart = getPathBounds(currentTreePath).getLocation();
        rendererRect = new Rectangle(cellStart.x, cellStart.y, cellRenderer.getPreferredSize().width,
                                     cellRenderer.getPreferredSize().height + 2);

        // Reset lastTreePath if over checkbox
        if (e.getX() <= (rendererRect.x + CheckTreeCellRenderer.getCheckBoxDimension().width)) {
            lastTreePath = new TreePath(new Object());
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        // Return if treetable cell is the same as in previous event
        if (currentTreePath == lastTreePath) {
            return;
        }

        lastTreePath = currentTreePath;

        if (!rendererRect.contains(e.getPoint())) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        // Return if cell contents is fully visible
        Rectangle visibleRect = getVisibleRect();

        if (((rendererRect.x + CheckTreeCellRenderer.getCheckBoxDimension().width) >= visibleRect.x)
                && ((rendererRect.x + rendererRect.width) <= (visibleRect.x + visibleRect.width))) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        while (cellTip.getComponentCount() > 0) {
            cellTip.remove(0);
        }

        cellTip.add(cellRendererPersistent, BorderLayout.CENTER);
        cellTip.setPreferredSize(new Dimension(cellRendererPersistent.getPreferredSize().width + 2, getRowHeight() + 2));

        CellTipManager.sharedInstance().setEnabled(true);
    }

    protected boolean shouldShowCellTipAt(Point position) {
        if (rendererRect == null) {
            return false;
        }

        return position.x > (rendererRect.x + CheckTreeCellRenderer.getCheckBoxDimension().width);
    }

    // ---------------------------------------------------------------------------

    // --- Utility methods -------------------------------------------------------
    private boolean isOverCheckBox(int x, int y) {
        TreePath path = getPathForLocation(x, y);

        if ((path == null) || (path.getPathCount() == 0) || !(path.getLastPathComponent() instanceof CheckTreeNode)
                || (x > (getPathBounds(path).x + CheckTreeCellRenderer.getCheckBoxDimension().width))) {
            return false;
        }

        return true;
    }

    private void fireCheckTreeChanged(Collection changedNodes) {
        if (changedNodes.size() > 0) {
            for (CheckTreeListener  l : checkTreeListeners) {
                l.checkTreeChanged(changedNodes);
            }
        }
    }

    private void fireNodeToggled(TreePath path, boolean before) {
        for (CheckTreeListener  l : checkTreeListeners) {
            l.checkNodeToggled(path, before);
        }
    }

    private Collection togglePathState(TreePath path) {
        return ((CheckTreeNode) path.getLastPathComponent()).toggleState();
    }

    // ---------------------------------------------------------------------------
}
