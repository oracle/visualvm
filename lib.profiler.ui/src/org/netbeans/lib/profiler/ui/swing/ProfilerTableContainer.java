/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerTableContainer extends JPanel {
    
    private static final String PROP_COLUMN = "column"; // NOI18N
    
    private JPanel scrollersPanel;

    public ProfilerTableContainer(final ProfilerTable table, boolean decorated,
                                  ColumnChangeAdapter adapter) {
        super(new BorderLayout());
        
        JScrollPane sp = new JScrollPane(table) {
            protected JViewport createViewport() {
                if (getViewport() == null) return customViewport(table);
                else return super.createViewport();
            }
        };
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        configureVerticalScrollBar(sp.getVerticalScrollBar());
        if (!decorated) {
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setViewportBorder(BorderFactory.createEmptyBorder());
        }
        add(sp, BorderLayout.CENTER);
        
        final ProfilerColumnModel cModel = table._getColumnModel();
        
        Set<Integer> scrollableColumns = table.getScrollableColumns();
        if (scrollableColumns != null && !scrollableColumns.isEmpty()) {
            scrollersPanel = new JPanel(null) {
                public void doLayout() {
                    int height = getHeight();
                    if (height > 0) for (Component component : getComponents()) {
                        JScrollBar scroller = (JScrollBar)component;
                        int column = table.convertColumnIndexToView(getColumn(scroller));
                        Rectangle rect = table.getTableHeader().getHeaderRect(column);
                        scroller.setBounds(rect.x, 0, rect.width, height);
                        scroller.doLayout();
                    }
                }
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    d.height = 0;
                    for (Component component : getComponents()) {
                        JScrollBar scroller = (JScrollBar)component;
                        if (cModel.isColumnVisible(getColumn(scroller)))
                            d.height = Math.max(d.height, scroller.getPreferredSize().height);
                    }
                    return d;
                }
            };

            for (final Integer column : scrollableColumns) {
                final JScrollBar scroller = new JScrollBar(JScrollBar.HORIZONTAL) {
                    private boolean adjusting = false;
                    {
                        putClientProperty(PROP_COLUMN, column);
                    }
                    public void setValue(int value) {
                        super.setValue(value);
                        updateColumnOffset(value);
                    }
                    public void setValues(int value, int extent, int min, int max) {
                        if (adjusting) return;
                        setEnabled(extent < max);
                        if (isTrackingEnd()) value = max - extent;
                        super.setValues(value, extent, min, max);
                        updateColumnOffset(value);
                    }
                    public void setValueIsAdjusting(boolean b) {
                        adjusting = b;
                        super.setValueIsAdjusting(b);
                        if (!adjusting) updateHorizontalScrollBars(table, column, false);
                    }
                    private void updateColumnOffset(int value) {
                        table.setColumnOffset(column, value);
                    }
                    private boolean isTrackingEnd() {
                        return isEnabled() && getValue() + getVisibleAmount() >= getMaximum();
                    }
                };
                scrollersPanel.add(scroller);
            }
            
            cModel.addColumnChangeListener(new ColumnChangeAdapter() {
                public void columnWidthChanged(int column, int oldWidth, int newWidth) {
                    if (table.isScrollableColumn(column))
                        updateHorizontalScrollBars(table, column, true);
                }
                public void columnPreferredWidthChanged(int column, int oldWidth, int newWidth) {
                    if (table.isScrollableColumn(column))
                        updateHorizontalScrollBars(table, column, false);
                }
            });

            cModel.addColumnModelListener(new TableColumnModelListener() {
                public void columnAdded(TableColumnModelEvent e) {}
                public void columnRemoved(TableColumnModelEvent e) {}
                public void columnMoved(TableColumnModelEvent e) { process(e); }
                public void columnMarginChanged(ChangeEvent e) {}
                public void columnSelectionChanged(ListSelectionEvent e) {}
                private void process(TableColumnModelEvent e) {
                    if (e.getFromIndex() != e.getToIndex())
                        updateHorizontalScrollBars(table, -1, true);
                }
            });

            add(scrollersPanel, BorderLayout.SOUTH);
        }
        
        if (adapter != null) cModel.addColumnChangeListener(adapter);
    }
    
    private int getColumn(JScrollBar scroller) {
        return (Integer)scroller.getClientProperty(PROP_COLUMN);
    }
    
    private JScrollBar getScroller(int column) {
        for (Component component : scrollersPanel.getComponents()) {
            JScrollBar scroller = (JScrollBar)component;
            if (getColumn(scroller) == column) return scroller;
        }
        return null;
    }
    
    private JViewport customViewport(final JTable table) {
        return new JViewport() {
            {
                setBackground(table.getBackground());
            }
            public void paint(Graphics g) {
                super.paint(g);
                
                Component view = getView();
                if (view == table) {
                    if (!listening) hookColumns();
                    
                    int height = getHeight();
                    int viewHeight = view.getHeight();
                    if (height > viewHeight) {
                        g.setColor(table.getGridColor());
                        JTableHeader header = table.getTableHeader();
                        for (int i = 0; i < table.getColumnCount(); i++) {
                            Rectangle rect = header.getHeaderRect(i);
                            if (rect.width > 0) g.drawLine(rect.x + rect.width - 1, viewHeight,
                                                           rect.x + rect.width - 1, height - 1);
                        }
                    }
                }
            }
            private boolean listening;
            private void hookColumns() {
                table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
                    public void columnAdded(TableColumnModelEvent e) { repaint(); }
                    public void columnRemoved(TableColumnModelEvent e) { repaint(); }
                    public void columnMoved(TableColumnModelEvent e) { repaint(); }
                    public void columnMarginChanged(ChangeEvent e) { repaint(); }
                    public void columnSelectionChanged(ListSelectionEvent e) { repaint(); }
                });
                listening = true;
            }
        };
    }
    
    private void configureVerticalScrollBar(final JScrollBar scrollBar) {
        scrollBar.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                scrollBar.setEnabled(ProfilerTableContainer.this.isEnabled() &&
                          scrollBar.getVisibleAmount() < scrollBar.getMaximum());
            }
        });
    }
    
    private void updateHorizontalScrollBars(ProfilerTable table, int column, boolean layout) {
        if (column != -1) {
            JScrollBar scroll = getScroller(column);
            int offset = table.getColumnOffset(column);
            int columnPref = table.getColumnPreferredWidth(column);
            int _column = table.convertColumnIndexToView(column);
            int columnAct = table.getTableHeader().getHeaderRect(_column).width;
            if (columnPref > columnAct) {
                int value = Math.min(offset, columnPref - columnAct);
                scroll.setValues(value, columnAct, 0, columnPref);
            } else {
                scroll.setValues(0, 0, 0, 0);
            }
        }
        
        if (layout) {
            doLayout();
            scrollersPanel.doLayout();
            repaint();
        }
    }
    
    
    public static class ColumnChangeAdapter implements ProfilerColumnModel.Listener {
        
        public void columnOffsetChanged(int column, int oldOffset, int newOffset) {}
        
        public void columnWidthChanged(int column, int oldWidth, int newWidth) {}
        
        public void columnPreferredWidthChanged(int column, int oldWidth, int newWidth) {}
        
    }
    
}
