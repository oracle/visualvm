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

package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.openide.util.NbPreferences;


/**
 *
 * @author Jiri Sedlacek
 */
class CollapsibleSplitPane extends JSplitPane {
    
    private static final String PREF_COLLAPSED =
            "org.netbeans.modules.profiler.heapwalk.ui.CollapsibleSplitPane.collapsed"; // NOI18N
    private static final String PREF_LOCATION =
            "org.netbeans.modules.profiler.heapwalk.ui.CollapsibleSplitPane.location"; // NOI18N
    
    private int savedDividerLocation;
    
    
    CollapsibleSplitPane(Component top, Component bottom) {
        super(JSplitPane.VERTICAL_SPLIT, top, bottom);
        CustomSplitterUI cui = new CustomSplitterUI();
        super.setUI(cui);
        setResizeWeight(1);
        setBorder(BorderFactory.createEmptyBorder());
        
        savedDividerLocation = NbPreferences.forModule(CollapsibleSplitPane.class).
                         getInt(PREF_LOCATION, getPreferredSize().height - 150);
        cui.setCollapsed(NbPreferences.forModule(CollapsibleSplitPane.class).
                         getBoolean(PREF_COLLAPSED, false));
    }
    
    void setCollapsed(boolean collapsed) {
        getBottomComponent().setVisible(!collapsed);
        if (!collapsed) setDividerLocation(savedDividerLocation);
        
        NbPreferences.forModule(CollapsibleSplitPane.class).
                putBoolean(PREF_COLLAPSED, collapsed);
    }
    
    public void setDividerLocation(int location) {
        if (!((CustomSplitterUI)getUI()).isCollapsed()) {
            savedDividerLocation = location;
            NbPreferences.forModule(CollapsibleSplitPane.class).
                    putInt(PREF_LOCATION, savedDividerLocation);
        }
        super.setDividerLocation(location);
    }
    
    public void setUI(SplitPaneUI ui) {}
    
    public void updateUI() {}
    
    
    private static class CustomSplitterUI extends BasicSplitPaneUI {
        
        public BasicSplitPaneDivider createDefaultDivider() {
            return new CustomSplitterDivider(this);
        }
        
        public CollapsibleSplitPane getSplitPane() {
            return (CollapsibleSplitPane)super.getSplitPane();
        }
        
        void setCollapsed(boolean collapsed) {
            ((CustomSplitterDivider)getDivider()).setCollapsed(collapsed);
        }
        
        boolean isCollapsed() {
            return ((CustomSplitterDivider)getDivider()).isCollapsed();
        }
        
    }
    
    private static class CustomSplitterDivider extends BasicSplitPaneDivider {
        
        private boolean isCollapsed;
        
        public CustomSplitterDivider(CustomSplitterUI ui) {
            super(ui);
            setLayout(new BorderLayout());
            updateCursor();
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    setCollapsed(!isCollapsed);
                }
            });
        }


        public int getDividerSize() {
//            return 7;
            return 6;
        }
        
        public Border getBorder() {
            return null;
        }
        
         public CustomSplitterUI getBasicSplitPaneUI() {
            return (CustomSplitterUI)super.getBasicSplitPaneUI();
        }
        
        protected void setMouseOver(boolean mouseOver) {
            super.setMouseOver(mouseOver);
            repaint();
        }
        
        public void setCursor(Cursor cursor) {}
        
        void setCollapsed(boolean collapsed) {
            isCollapsed = collapsed;
            updateCursor();
            getBasicSplitPaneUI().getSplitPane().setCollapsed(isCollapsed);
            repaint();
        }
        
        boolean isCollapsed() {
            return isCollapsed;
        }
        
        private void updateCursor() {
            super.setCursor(isCollapsed ?
                  Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) :
                  Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
        }
        
        protected void prepareForDragging() {
            if (!isCollapsed) super.prepareForDragging();
        }

        protected void dragDividerTo(int location) {
            if (!isCollapsed) super.dragDividerTo(location);
        }

        protected void finishDraggingTo(int location) {
            if (!isCollapsed) super.finishDraggingTo(location);
        }
        
        public void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            int x = w / 2;
            
//            g.setColor(UIManager.getColor("controlShadow")); // NOI18N
//            g.drawLine(0, 0, w - 1, 0);
            
            Color b = isMouseOver() ?
                    UIManager.getColor("List.selectionBackground") : // NOI18N
                    getBackground();
            if (b == null) b = getBackground();
            g.setColor(b);
//            g.fillRect(0, 1, w, h - 1);
            g.fillRect(0, 0, w, h);
            
            Color c = isMouseOver() ?
                    UIManager.getColor("List.selectionForeground") : // NOI18N
                    UIManager.getColor("controlShadow"); // NOI18N
            if (c == null) c = getBackground().darker();
            g.setColor(c);
            
            if (isCollapsed) {
                int[] xPoints = new int[] { x - 5, x + 4, x };
//                int[] yPoints = new int[] { 6, 6, 1 };
                int[] yPoints = new int[] { 5, 5, 0 };
                g.fillPolygon(xPoints, yPoints, 3);
            } else {
                int[] xPoints = new int[] { x - 4, x + 4, x };
//                int[] yPoints = new int[] { 2, 2, 6 };
                int[] yPoints = new int[] { 1, 1, 5 };
                g.fillPolygon(xPoints, yPoints, 3);
            }
        }
        
    }
    
}
