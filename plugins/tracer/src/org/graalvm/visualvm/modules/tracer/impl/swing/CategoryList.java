/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

import com.sun.tools.visualvm.uisupport.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 *
 * @author Jiri Sedlacek
 */
public final class CategoryList extends JPanel {

    private static final Icon expandedIcon;
    private static final Icon collapsedIcon;

    static {
        Icon expanded = UIManager.getIcon("Tree.expandedIcon"); // NOI18N
        Icon collapsed = UIManager.getIcon("Tree.collapsedIcon"); // NOI18N
        int width = Math.max(expanded.getIconWidth(), collapsed.getIconWidth());
        int height = Math.max(expanded.getIconHeight(), collapsed.getIconHeight());
        expandedIcon = centeredIcon(expanded, width, height);
        collapsedIcon = centeredIcon(collapsed, width, height);
    }


    public CategoryList(String[] categories, String[] tooltips,
                    boolean[] initialStates, Component[][] items) {

        setOpaque(false);
        setLayout(new VerticalLayout(false));

        int captionsCount = categories.length;
        for (int i = 0; i < captionsCount; i++)
            add(new Category(categories[i], tooltips[i], initialStates[i],
                             items[i], i, captionsCount));

    }


    public void setEnabled(boolean enabled) {
        Component[] components = getComponents();
        for (Component c : components) c.setEnabled(enabled);
    }


    private static class Category extends JPanel {

        private boolean expanded;

        private final JLabel headerLabel;
        private final JPanel itemsContainer;


        public Category(String caption, String tooltip,
                        boolean initialState, Component[] items,
                        int index, int categoriesCount) {

            expanded = initialState;

            setOpaque(false);
            setLayout(new BorderLayout());

            headerLabel = new JLabel(caption);
            headerLabel.setForeground(new JMenuItem().getForeground());
            headerLabel.setToolTipText(tooltip);
            headerLabel.setIconTextGap(5);
            headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            headerLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
            headerLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    expanded = !expanded;
                    updateState();
                }
            });
            
            JMenuBar menuBar = new JMenuBar();
            menuBar.setBorder(BorderFactory.createEmptyBorder());
            menuBar.setBorderPainted(false);
            menuBar.setLayout(new BorderLayout());
            menuBar.add(headerLabel, BorderLayout.CENTER);
            
            itemsContainer = new JPanel() {
                public void setEnabled(boolean enabled) {
                    Component[] components = getComponents();
                    for (Component c : components) c.setEnabled(enabled);
                }
            };
            itemsContainer.setOpaque(false);
            itemsContainer.setLayout(new VerticalLayout(false));

            for (int i = 0; i < items.length; i++)
                itemsContainer.add(items[i]);

            add(menuBar, BorderLayout.NORTH);
            add(itemsContainer, BorderLayout.CENTER);

            updateState();

        }

        public void setEnabled(boolean enabled) {
            Component[] components = getComponents();
            for (Component c : components)
                if (!(c instanceof JMenuBar)) c.setEnabled(enabled);
        }

        private void updateState() {
            headerLabel.setIcon(expanded ? expandedIcon : collapsedIcon);
            itemsContainer.setVisible(expanded);
        }

    }


    private static Icon centeredIcon(final Icon icon, final int width, final int height) {
        JLabel l = new JLabel(icon);
        l.setIconTextGap(0);
        l.setBorder(null);
        l.setSize(width, height);
        
        BufferedImage img = new BufferedImage(l.getWidth(), l.getHeight(), BufferedImage.TYPE_INT_ARGB);
        l.paint(img.getGraphics());
        
        return new ImageIcon(img);
    }

}
