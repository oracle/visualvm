/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.OverlayLayout;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeaderPanel extends JPanel {

    private JPanel clientContainer;
    private Header header;

    public HeaderPanel() {
        initComponents();
    }


    protected Object getRendererValue() { return null; }

    protected void setupRenderer(Component renderer) {}

    protected boolean isSelected() { return false; }

    protected boolean processMouseEvents() { return false; }


    public JPanel getClientContainer() {
        if (clientContainer == null) {
            clientContainer = Spacer.create();
            add(clientContainer, 0);
        }
        return clientContainer;
    }

    public boolean isOptimizedDrawingEnabled() {
        return clientContainer == null;
    }
    
    protected void processMouseEvent(MouseEvent e) {
        if (processMouseEvents()) header.processMouseEvent(e);
        if (!e.isConsumed()) super.processMouseEvent(e);
    }

    private void initComponents() {
        JTable impl = new JTable(new DefaultTableModel(new Object[] { "" }, 0)); // NOI18N
        TableColumnModel colMod = impl.getColumnModel();
        final TableColumn col = colMod.getColumn(0);
        impl.setFocusable(false);
        header = new Header(colMod);
        impl.setTableHeader(header);
        header.setResizingAllowed(false);
        header.setReorderingAllowed(false);

        final TableCellRenderer renderer = header.getDefaultRenderer();
        header.setDefaultRenderer(new TableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus,
                    int row, int column) {

                Component component = renderer.getTableCellRendererComponent(
                        table, getRendererValue(), isSelected(),
                        isSelected(), row, processMouseEvents() ? 0 : 1);

                setupRenderer(component);

                col.setWidth(header.getWidth());
                return component;
            }
        });

        JScrollPane scroll = new JScrollPane(impl, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
            public Dimension getPreferredSize() { return header.getPreferredSize(); }
            public void reshape(int x, int y, int width, int height) {
                header.setPreferredSize(new Dimension(width, height));
                super.reshape(x, y, width, height);
            }
        };
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());

        setLayout(new OverlayLayout(this));
        add(scroll);
    }

    private static class Header extends JTableHeader {
        Header(TableColumnModel model) { super(model); };
        public void processMouseEvent(MouseEvent e) { super.processMouseEvent(e); }
    }

}
