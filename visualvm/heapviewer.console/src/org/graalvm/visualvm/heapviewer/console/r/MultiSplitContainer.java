/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.console.r;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Copy of org.graalvm.visualvm.heapviewer.swing.MultiSplitContainer
 * TODO: make this a public class and share it to prevent duplication.
 *
 * @author Jiri Sedlacek
 */
class MultiSplitContainer extends JPanel {
    
    private static final int DIVIDER_SIZE = 6;
    private static final Color SEPARATOR_COLOR = UIManager.getColor("Separator.foreground"); // NOI18N
    
    private final Map<Component, Double> componentsWeights = new HashMap<>();
    private final List<Integer> dividerOffsets = new ArrayList<>();
    private final List<Component> visibleComponents = new ArrayList<>();
    
    
    MultiSplitContainer() {
        super(null);
        
        MouseHandler mHandler = new MouseHandler();
        addMouseListener(mHandler);
        addMouseMotionListener(mHandler);
    }
    
    public void doLayout() {
        if (visibleComponents.isEmpty()) return;
       
        Insets insets = getInsets();        
        int visiblesCount = visibleComponents.size();
        
        if (visiblesCount == 1) {
            visibleComponents.get(0).setBounds(insets.left, insets.top, getWidth() - insets.left - insets.right, getHeight() - insets.top - insets.bottom);
        } else {
            Dimension size = getSize();     
            int h = size.height - insets.top - insets.bottom;
            
            int x = insets.left;
            Component comp;
        
            int dividersCount = dividerOffsets.size();
            for (int i = 0; i < dividersCount; i++) {
                int xx = dividerOffsets.get(i);
                comp = visibleComponents.get(i);
                comp.setBounds(x, insets.top, xx - x, h);
                comp.validate();
                x = xx + DIVIDER_SIZE;
            }
            
            x = dividerOffsets.get(dividersCount - 1) + DIVIDER_SIZE;
            comp = visibleComponents.get(visiblesCount - 1);
            comp.setBounds(x, insets.top, getWidth() - insets.right - x, h);
            comp.validate();
        }
    }
    
    public void invalidate() {
        super.invalidate();
        setupLayout();
        doLayout();
        repaint();
    }
    
    
    public void paint(Graphics g) {
        super.paint(g);
        
        if (!dividerOffsets.isEmpty()) {
            Insets i = getInsets();
            g.setColor(SEPARATOR_COLOR);
            for (int o : dividerOffsets) {
                g.drawLine(o, i.top, o, getHeight() - i.bottom);
                o += DIVIDER_SIZE - 1;
                g.drawLine(o, i.top, o, getHeight() - i.bottom);
            }
        }
    }
    
    
    protected void addImpl(Component comp, Object constraints, int index) {
        componentsWeights.put(comp, constraints instanceof Number ? ((Number)constraints).doubleValue() : 0.5d);
        super.addImpl(comp, constraints, index);
        invalidate();
    }
    
    public void remove(int index) {
        Component comp = getComponent(index);
        super.remove(index);
        componentsWeights.remove(comp);
        invalidate();
    }
    
    
    private void setupLayout() {
        dividerOffsets.clear();
        visibleComponents.clear();
            
        Dimension size = getSize();
        if (size.height == 0 || size.width == 0) return; // container too small
        
        double visRel = 0;
        
        for (Component c : getComponents()) {
            if (c.isVisible()) {
                visibleComponents.add(c);
                visRel += componentsWeights.get(c);
            }
        }
        
        int visiblesCount = visibleComponents.size();
        if (visiblesCount < 2) return; // no visible divider
        
        Insets insets = getInsets();
        int w = size.width - insets.left - insets.right - ((visiblesCount - 1) * DIVIDER_SIZE);
        
        int x = insets.left;
        
        for (int i = 0; i < visiblesCount - 1; i++) {
            x += (w / visRel * componentsWeights.get(visibleComponents.get(i)));
            dividerOffsets.add(x);
            x += DIVIDER_SIZE;
        }
    }
    
    
    private class MouseHandler extends MouseAdapter {
        
        private boolean inDivider;
        private int cursor;
        private boolean canDrag;
        private int offsetIdx;
        private int dragX;
        private int offsetX;
        
        private int minX;
        private int maxX;
        private Component c1;
        private Component c2;
        private double relWidth;
        
        public void mouseEntered(MouseEvent e) {
            if (!canDrag) updateMouse(e, true);
        }
        
        public void mouseExited(MouseEvent e) {
            if (!canDrag) updateMouse(e, false);
        }
        
        public void mouseMoved(MouseEvent e) {
            updateMouse(e, true);
        }
        
        public void mouseDragged(MouseEvent e) {
            if (canDrag) {
                int oldX = dividerOffsets.get(offsetIdx);
                
                int dx = dragX - e.getX();
                int newX = oldX - dx;
                newX = Math.max(newX, minX);
                newX = Math.min(newX, maxX);
                dividerOffsets.set(offsetIdx, newX);
                
                double c1width = c1.getWidth();
                double c1Rel = c1width * relWidth / (c1width + c2.getWidth());
                componentsWeights.put(c1, c1Rel);
                componentsWeights.put(c2, relWidth - c1Rel);
                
                dragX = newX + offsetX;
                
                doLayout();
                repaint();
            }
        }
        
        public void mousePressed(MouseEvent e) {
            if (inDivider) {
                canDrag = true;
                dragX = e.getX();
                offsetX = dragX - dividerOffsets.get(offsetIdx);
                
                Insets insets = getInsets();
                minX = offsetIdx == 0 ? insets.left : dividerOffsets.get(offsetIdx - 1) + DIVIDER_SIZE;
                maxX = offsetIdx == dividerOffsets.size() - 1 ? getWidth() - insets.right - DIVIDER_SIZE :
                                                                dividerOffsets.get(offsetIdx + 1) - DIVIDER_SIZE;
                
                c1 = visibleComponents.get(offsetIdx);
                c2 = visibleComponents.get(offsetIdx + 1);
                relWidth = componentsWeights.get(c1) + componentsWeights.get(c2);
            }
        }
        
        public void mouseReleased(MouseEvent e) {
            canDrag = false;
            offsetIdx = -1;
            c1 = null;
            c2 = null;
            updateMouse(e, inDivider);
        }
        
        
        private void updateMouse(MouseEvent e, boolean onContainer) {
            inDivider = false;
            int origCursor = cursor;
            cursor = Cursor.DEFAULT_CURSOR;
            if (onContainer) {
                int x = e.getX();
                for (int i = 0; i < dividerOffsets.size(); i++) {
                    int divx = dividerOffsets.get(i);
                    if (x >= divx && x <= divx + DIVIDER_SIZE - 1) {
                        inDivider = true;
                        offsetIdx = i;
                        cursor = Cursor.E_RESIZE_CURSOR;
                        break;
                    }
                }
            }
            if (origCursor != cursor) setCursor(Cursor.getPredefinedCursor(cursor));
        }
        
    }
    
}
