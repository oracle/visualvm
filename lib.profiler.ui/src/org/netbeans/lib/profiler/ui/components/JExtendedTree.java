/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.components;

import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.tree.TreeCellRendererPersistent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;


/**
 *
 * @author Jiri Sedlacek
 */
public class JExtendedTree extends JTree implements CellTipAware {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class PrivateComponentListener implements MouseListener, MouseMotionListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            // --- CellTip support ------------------
            CellTipManager.sharedInstance().setEnabled(false);
        }

        public void mouseExited(MouseEvent e) {
            // --- CellTip support ------------------
            // Return if mouseExit occured because of showing heavyweight celltip
            if (contains(e.getPoint()) && cellTip.isShowing()) {
                return;
            }

            CellTipManager.sharedInstance().setEnabled(false);
            lastTreePath = null;
        }

        public void mouseMoved(MouseEvent e) {
            // --- CellTip support ------------------
            processCellTipMouseMove(e);
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected JToolTip cellTip;
    protected Rectangle rendererRect;
    protected TreePath lastTreePath = null;
    private PrivateComponentListener componentListener = new PrivateComponentListener();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of JExtendedTree */
    public JExtendedTree() {
        addMouseListener(componentListener);
        addMouseMotionListener(componentListener);

        setRowHeight(UIUtils.getDefaultRowHeight()); // celltips require to have row height initialized!

        // --- CellTip support ------------------
        cellTip = createCellTip();
        cellTip.setBackground(getBackground());
        cellTip.setBorder(BorderFactory.createLineBorder(UIConstants.TABLE_VERTICAL_GRID_COLOR));
        cellTip.setLayout(new BorderLayout());

        CellTipManager.sharedInstance().registerComponent(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JToolTip getCellTip() {
        return cellTip;
    }

    public Point getCellTipLocation() {
        if (rendererRect == null) {
            return null;
        }

        return new Point(rendererRect.getLocation().x - 1, rendererRect.getLocation().y - 1);
    }

    public void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
    }

    protected JToolTip createCellTip() {
        return new JToolTip();
    }

    protected void processCellTipMouseMove(MouseEvent e) {
        // Identify treetable row and column at cursor
        TreePath currentTreePath = getPathForLocation(e.getX(), e.getY());

        // Return if treetable cell is the same as in previous event
        if (currentTreePath == lastTreePath) {
            return;
        }

        lastTreePath = currentTreePath;

        // Return if cursor isn't at any cell
        if (lastTreePath == null) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        Component cellRenderer;
        Component cellRendererPersistent;
        int row = getRowForPath(lastTreePath);

        TreeCellRenderer treeCellRenderer = getCellRenderer();

        if (!(treeCellRenderer instanceof TreeCellRendererPersistent)) {
            return;
        }

        cellRenderer = treeCellRenderer.getTreeCellRendererComponent(JExtendedTree.this, lastTreePath.getLastPathComponent(),
                                                                     false, isExpanded(row),
                                                                     getModel().isLeaf(lastTreePath.getLastPathComponent()), row,
                                                                     false);
        cellRendererPersistent = ((TreeCellRendererPersistent) treeCellRenderer).getTreeCellRendererComponentPersistent(JExtendedTree.this,
                                                                                                                        lastTreePath
                                                                                                                        .getLastPathComponent(),
                                                                                                                        false,
                                                                                                                        isExpanded(row),
                                                                                                                        getModel()
                                                                                                                            .isLeaf(lastTreePath
                                                                                                                                    .getLastPathComponent()),
                                                                                                                        row, false);

        // Return if celltip is not supported for the cell
        if (cellRenderer == null) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        Point cellStart = getPathBounds(lastTreePath).getLocation();
        rendererRect = new Rectangle(cellStart.x, cellStart.y, cellRenderer.getPreferredSize().width,
                                     cellRenderer.getPreferredSize().height + 2);

        if (!rendererRect.contains(e.getPoint())) {
            CellTipManager.sharedInstance().setEnabled(false);

            return;
        }

        // Return if cell contents is fully visible
        Rectangle visibleRect = getVisibleRect();

        if ((rendererRect.x >= visibleRect.x) && ((rendererRect.x + rendererRect.width) <= (visibleRect.x + visibleRect.width))) {
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
}
