/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.synth.SynthContext;
import sun.swing.plaf.synth.SynthIcon;

/**
 *
 * @author Jiri Sedlacek
 */
public class CategoryList extends JPanel {

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
        setLayout(new VerticalLayout());

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
            itemsContainer.setLayout(new VerticalLayout());

            for (int i = 0; i < items.length; i++)
                itemsContainer.add(items[i]);

            add(menuBar, BorderLayout.NORTH);
            add(itemsContainer, BorderLayout.CENTER);

            updateState();

        }

        public void setEnabled(boolean enabled) {
//            super.setEnabled(enabled);
            Component[] components = getComponents();
            for (Component c : components)
                if (!(c instanceof JMenuBar)) {
//                    System.err.println(">>> enabled " + enabled + " set for " + c);
                    c.setEnabled(enabled);
                }
        }

        private void updateState() {
            headerLabel.setIcon(expanded ? expandedIcon : collapsedIcon);
            itemsContainer.setVisible(expanded);
        }

    }


    private static Icon centeredIcon(final Icon icon, final int width, final int height) {
        Icon centeredIcon = null;

        if (icon instanceof SynthIcon) {
            try {
                centeredIcon = new SynthIcon() {
                    private final SynthIcon sicon = (SynthIcon)icon;
                    public void paintIcon(SynthContext sc, Graphics grphcs, int x, int y, int w, int h) {
                        try {
                            int dw = SynthIcon.getIconWidth(sicon, sc);
                            int dh = SynthIcon.getIconHeight(sicon, sc);
                            int dx = width - dw;
                            int dy = height - dh;
                            SynthIcon.paintIcon(sicon, sc, grphcs, x + dx/2, y + dy/2, dw + 2, dh + 2);
                        } catch (Throwable t) {
                            try { sicon.paintIcon(sc, grphcs, x, y, w, h); } catch (Throwable th) {}
                        }
                    }
                    public int getIconWidth(SynthContext sc)  { return width;  }
                    public int getIconHeight(SynthContext sc) { return height; }
                };
            } catch (Throwable t) {}
        }

        if (centeredIcon == null) {
            centeredIcon = new Icon() {
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    int dx = width - icon.getIconWidth();
                    int dy = height - icon.getIconHeight();
                    icon.paintIcon(c, g, x + dx/2, y + dy/2);
                }
                public int getIconWidth()  { return width;  }
                public int getIconHeight() { return height; }
            };
        }

        return centeredIcon;
    }

}
