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

package org.netbeans.lib.profiler.ui.components.table;

import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.JTableHeader;


/**
 * A subclass of JPanel that provides additional fuctionality for displaying JExtendedTable.
 * JExtendedTablePanel provides JScrollPane for displaying JExtendedTable and customized Viewport.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class JExtendedTablePanel extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    //-----------------------------------------------------------------------
    // Custom ExtendedTable Viewport
    private class CustomExtendedTableViewport extends JViewport {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JTableHeader tableHeader;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CustomExtendedTableViewport(JExtendedTable extendedTable) {
            super();
            setView(extendedTable);
            updateBackgroundColor();
            this.tableHeader = extendedTable.getTableHeader();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            if (UIConstants.SHOW_TABLE_VERTICAL_GRID) {
                paintVerticalLines(g);
            }
        }

        private int getEmptySpaceY() {
            if (getView() == null) {
                return 0;
            }

            return getView().getHeight();
        }

        private void paintVerticalLines(Graphics g) {
            int emptySpaceY = getEmptySpaceY();

            if (emptySpaceY > 0) {
                g.setColor(UIConstants.TABLE_VERTICAL_GRID_COLOR);

                int cellX = 0;
                int cellWidth;

                for (int i = 0; i < extendedTable.getColumnModel().getColumnCount(); i++) {
                    cellWidth = extendedTable.getColumnModel().getColumn(i).getWidth();
                    g.drawLine((cellX + cellWidth) - 1, emptySpaceY, (cellX + cellWidth) - 1, getHeight() - 1);
                    cellX += cellWidth;
                }
            }
        }

        private void updateBackgroundColor() {
            setBackground(extendedTable.isEnabled() ? extendedTable.getBackground()
                                                    : UIManager.getColor("TextField.inactiveBackground")); // NOI18N
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    CustomExtendedTableViewport extendedTableViewport;
    protected JExtendedTable extendedTable;
    protected JScrollPane extendedTableScrollPane;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of JExtendedTablePanel */
    public JExtendedTablePanel(JExtendedTable extendedTable) {
        super(new BorderLayout());
        this.extendedTable = extendedTable;

        initComponents();
        hookHeaderColumnResize();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setCorner(String key, java.awt.Component corner) {
        extendedTableScrollPane.setCorner(key, corner);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        updateVerticalScrollbar();
        extendedTable.setEnabled(enabled);
        extendedTableViewport.updateBackgroundColor();
    }

    public JScrollPane getScrollPane() {
        return extendedTableScrollPane;
    }

    private void hookHeaderColumnResize() {
        if (extendedTable.getTableHeader() != null) {
            extendedTable.getTableHeader().getColumnModel().addColumnModelListener(new TableColumnModelListener() {
                    public void columnAdded(TableColumnModelEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnMoved(TableColumnModelEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnRemoved(TableColumnModelEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnMarginChanged(ChangeEvent e) {
                        extendedTableViewport.repaint();
                    }

                    public void columnSelectionChanged(ListSelectionEvent e) {
                    } // Ignored
                });

        }
    }

    private void initComponents() {
        setBorder(BorderFactory.createLoweredBevelBorder());

        extendedTableScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        extendedTableScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        extendedTableViewport = new CustomExtendedTableViewport(extendedTable);
        extendedTableScrollPane.setViewport(extendedTableViewport);
        extendedTableScrollPane.addMouseWheelListener(extendedTable);
        // Enable vertical scrollbar only if needed
        JScrollBar vScrollbar = extendedTableScrollPane.getVerticalScrollBar();
        vScrollbar.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                updateVerticalScrollbar();
            }
        });

        add(extendedTableScrollPane, BorderLayout.CENTER);
    }

    private void updateVerticalScrollbar() {
        JScrollBar vScrollbar = extendedTableScrollPane.getVerticalScrollBar();
        vScrollbar.setEnabled(JExtendedTablePanel.this.isEnabled() &&
                              vScrollbar.getVisibleAmount() < vScrollbar.getMaximum());
    }
    
}
