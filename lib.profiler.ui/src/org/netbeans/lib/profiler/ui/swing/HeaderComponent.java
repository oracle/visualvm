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

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * JComponent with TableHeader appearance.
 *
 * @author Jiri Sedlacek
 */
class HeaderComponent extends JComponent {
    
    private boolean isPressed;
    private boolean isSelected;
    
    HeaderComponent(final ActionListener listener) {
        if (listener != null) addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                isSelected = true;
                repaint();
            }
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                if (isSelected) {
                    repaint();
                    fireActionEvent(e);
                }
            }
            public void mouseEntered(MouseEvent e) {
                if (isPressed || !isButtonDown(e)) {
                    isSelected = true;
                    repaint();
                }
            }
            public void mouseExited(MouseEvent e) {
                if (isSelected) {
                    isSelected = false;
                    repaint();
                }
            }
            private void fireActionEvent(MouseEvent e) {
                ActionEvent ae = new ActionEvent(e.getSource(), e.getID(),
                                                 e.paramString(), e.getWhen(),
                                                 e.getModifiers());
                listener.actionPerformed(ae);
            }
            private boolean isButtonDown(MouseEvent e) {
                return SwingUtilities.isLeftMouseButton(e) ||
                       SwingUtilities.isMiddleMouseButton(e) ||
                       SwingUtilities.isRightMouseButton(e);
            }
        });
    }
    
    protected void paintComponent(Graphics g) {
        JTableHeader header = getHeader();
        setupHeader(header);
        TableCellRenderer renderer = header.getDefaultRenderer();
        JComponent component = (JComponent)renderer.getTableCellRendererComponent(
                               getTable(), "", isSelected && isPressed, isFocusOwner(), -1, 0); // NOI18N
        
        int height = header.getPreferredSize().height;
        component.setBounds(0, 0, getWidth(), height);
        component.setOpaque(false);
        getPainter().paintComponent(g, component, null, 0, 0, getWidth(), height, false);
    }
    
    private void setupHeader(JTableHeader h) {
        h.setDraggedColumn(isSelected && isPressed ? getColumn() : null);
        
        MouseEvent e = isSelected && !isPressed ?
                new MouseEvent(h, MouseEvent.MOUSE_ENTERED, 1, 0, 1, 1, 0, false) :
                new MouseEvent(h, MouseEvent.MOUSE_EXITED, 1, 0, 0, 0, 0, false);
        h.dispatchEvent(e);
    }
    
    
    private static CellRendererPane PAINTER;
    private static CellRendererPane getPainter() {
        if (PAINTER == null) PAINTER = new CellRendererPane();
        return PAINTER;
    }
    
    private static JTable REF_TABLE;
    private static JTable getTable() {
        if (REF_TABLE == null) REF_TABLE = new JTable(new Object[][] {{}},
                                                      new Object[] { " " }) { // NOI18N
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public boolean contains(int x, int y) { return x == 1 && y == 1; }
                };
            }
        };
        return REF_TABLE;
    }
    
    private static JTableHeader getHeader() {
        return getTable().getTableHeader();
    }
    
    private static TableColumn getColumn() {
        return getHeader().getColumnModel().getColumn(0);
    }
    
}
