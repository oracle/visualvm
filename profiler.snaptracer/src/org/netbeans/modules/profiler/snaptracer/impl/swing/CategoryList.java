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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.synth.SynthContext;
import sun.swing.plaf.synth.SynthIcon;

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
        Icon centeredIcon = null;

        try {
            if (icon instanceof SynthIcon) {
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
            }
        } catch (Throwable t) {}

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
