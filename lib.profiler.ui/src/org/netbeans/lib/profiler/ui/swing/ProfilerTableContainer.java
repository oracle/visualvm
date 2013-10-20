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
import java.awt.Graphics;
import java.awt.Rectangle;
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

    public ProfilerTableContainer(final JTable table, boolean decorated) {
        super(new BorderLayout());
        
        JScrollPane sp = new JScrollPane(table) {
            protected JViewport createViewport() { return customViewport(table); }
        };
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        configureScrollBar(sp.getVerticalScrollBar());
        if (!decorated) {
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.setViewportBorder(BorderFactory.createEmptyBorder());
        }
        add(sp, BorderLayout.CENTER);
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
    
    private void configureScrollBar(final JScrollBar scrollBar) {
        scrollBar.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                scrollBar.setEnabled(ProfilerTableContainer.this.isEnabled() &&
                          scrollBar.getVisibleAmount() < scrollBar.getMaximum());
            }
        });
    }
    
}
