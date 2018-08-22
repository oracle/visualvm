/*
 * TabbedCaptionSupport.java
 *
 * Created on Dec 11, 2007, 2:20:48 PM
 *
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui.components;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultButtonModel;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import org.graalvm.visualvm.uisupport.UISupport;

/**
 *
 * @author Jiri Sedlacek
 */
class DisplayAreaSupport {

    static final Color BORDER_COLOR_NORMAL = !UISupport.isDarkResultsBackground() ? new Color(192, 192, 192) : new Color(64, 64, 64);
    static final Color BORDER_COLOR_HIGHLIGHT = !UISupport.isDarkResultsBackground() ? new Color(128, 128, 128) : new Color(90, 90, 90);
    static final Color BACKGROUND_COLOR_NORMAL = !UISupport.isDarkResultsBackground() ? new Color(245, 245, 245) : new Color(50, 50, 50);
    static final Color BACKGROUND_COLOR_HIGHLIGHT = !UISupport.isDarkResultsBackground() ? new Color(235, 235, 235) : new Color(55, 55, 55);

    static final Color COLOR_NONE = new Color(0, 0, 0);
    static final Color TABS_SEPARATOR = new Color(UIManager.getColor("Label.foreground").getRGB()); // NOI18N

    static final int TABBUTTON_MARGIN_TOP = 3;
    static final int TABBUTTON_MARGIN_LEFT = 8;
    static final int TABBUTTON_MARGIN_BOTTOM = 3;
    static final int TABBUTTON_MARGIN_RIGHT = 8;

    private static final Color TABBUTTON_FOCUS_COLOR = Color.BLACK;
    private static final Stroke TABBUTTON_FOCUS_STROKE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 0, new float[] {0, 2}, 0);

    static class TabButton extends JButton {

        TabButton(String text, String description) {
            super(text);
            setModel(new DefaultButtonModel() {
                public boolean isPressed() { return false; }
            });
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder(TABBUTTON_MARGIN_TOP, TABBUTTON_MARGIN_LEFT, TABBUTTON_MARGIN_BOTTOM, TABBUTTON_MARGIN_RIGHT));
            setToolTipText(description);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Dimension size = getSize();
            Graphics2D g2 = (Graphics2D)g;
            if( hasFocus() && isEnabled() ) {
                g2.setStroke(TABBUTTON_FOCUS_STROKE);
                g2.setColor(TABBUTTON_FOCUS_COLOR);
                g2.drawRect(2, 2, size.width - 5, size.height - 5);
            }
        }

    }

    static class TabButtonContainer extends JPanel {

        private TabButton tabButton;

        TabButtonContainer(TabButton tabButton) {
            this.tabButton = tabButton;
            setOpaque(true);
            setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
            setBackground(BACKGROUND_COLOR_NORMAL);
            setBorder(TabbedCaptionBorder.get(null, null, null, null));
            add(tabButton, BorderLayout.CENTER);
        }

        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            tabButton.setEnabled(enabled);
        }

        void updateTabButton(int index, int selectedIndex, int buttonsCount) {
            if (buttonsCount == 1) {
                tabButton.setFocusable(false);
                tabButton.setCursor(Cursor.getDefaultCursor());
                setBackground(BACKGROUND_COLOR_NORMAL);
                setBorder(TabbedCaptionBorder.get(BORDER_COLOR_NORMAL, BORDER_COLOR_NORMAL, COLOR_NONE, COLOR_NONE));
            } else if (index == selectedIndex) {
                tabButton.setFocusable(true);
                tabButton.setCursor(Cursor.getDefaultCursor());
                setBackground(BACKGROUND_COLOR_HIGHLIGHT);
                setBorder(TabbedCaptionBorder.get(BORDER_COLOR_HIGHLIGHT, BORDER_COLOR_HIGHLIGHT, COLOR_NONE, BORDER_COLOR_HIGHLIGHT));
            } else {
                tabButton.setFocusable(true);
                tabButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setBackground(BACKGROUND_COLOR_NORMAL);
                Color topColor = BORDER_COLOR_NORMAL;
                Color leftColor = index == 0 ? BORDER_COLOR_NORMAL : null;
                Color bottomColor = BORDER_COLOR_HIGHLIGHT;
                Color rightColor = index == selectedIndex - 1 ? null : index == buttonsCount - 1 ? COLOR_NONE : TABS_SEPARATOR;
                setBorder(TabbedCaptionBorder.get(topColor, leftColor, bottomColor, rightColor));
            }
        }

    }

    static class TabbedCaptionBorder implements Border {

        private static final Set<TabbedCaptionBorder> borders = new HashSet();

        private Color COLOR_TOP;
        private Color COLOR_LEFT;
        private Color COLOR_BOTTOM;
        private Color COLOR_RIGHT;
        private Insets insets;


        static TabbedCaptionBorder get(Color colorTop, Color colorLeft, Color colorBottom, Color colorRight) {
            for (TabbedCaptionBorder border : borders)
                // Note: identity must be used for the comparison!
                if (border.COLOR_TOP == colorTop &&
                    border.COLOR_LEFT == colorLeft &&
                    border.COLOR_BOTTOM == colorBottom &&
                    border.COLOR_RIGHT == colorRight) return border;

            TabbedCaptionBorder border = new TabbedCaptionBorder(colorTop, colorLeft, colorBottom, colorRight);
            borders.add(border);
            return border;
        }

        private TabbedCaptionBorder(Color colorTop, Color colorLeft, Color colorBottom, Color colorRight) {
            COLOR_TOP = colorTop;
            COLOR_LEFT = colorLeft;
            COLOR_BOTTOM = colorBottom;
            COLOR_RIGHT = colorRight;

            insets = new Insets(
                    COLOR_TOP == null ? 0 : 1,
                    COLOR_LEFT == null ? 0 : 1,
                    COLOR_BOTTOM == null ? 0 : 1,
                    COLOR_RIGHT == null ? 0 : 1);
        }


        public Insets getBorderInsets(Component c) { return insets; }

        public boolean isBorderOpaque() { return true; }

        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            if (COLOR_LEFT != null && COLOR_LEFT != COLOR_NONE) {
                g.setColor(COLOR_LEFT);
                if (COLOR_LEFT == TABS_SEPARATOR) {
                    g.drawLine(x, y + TABBUTTON_MARGIN_TOP + 2, x, y + height - TABBUTTON_MARGIN_BOTTOM - 4);
                } else {
                    g.drawLine(x, y, x, y + height - 1);
                }
            }
            if (COLOR_RIGHT != null && COLOR_RIGHT != COLOR_NONE) {
                g.setColor(COLOR_RIGHT);
                if (COLOR_RIGHT == TABS_SEPARATOR) {
                    g.drawLine(x + width - 1, y + TABBUTTON_MARGIN_TOP + 2, x + width - 1, y + height - TABBUTTON_MARGIN_BOTTOM - 4);
                } else {
                    g.drawLine(x + width - 1, y, x + width - 1, y + height - 1);
                }
            }
            if (COLOR_TOP != null && COLOR_TOP != COLOR_NONE) {
                g.setColor(COLOR_TOP);
                g.drawLine(x, y, x + width - 1, y);
            }
            if (COLOR_BOTTOM != null && COLOR_BOTTOM != COLOR_NONE) {
                g.setColor(COLOR_BOTTOM);
                g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
            }
        }

    }

    private static class ThinBevelBorder extends BevelBorder {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private ThinBevelBorder(int bevelType, Color highlight, Color shadow) {
            super(bevelType, highlight.brighter(), highlight, shadow, shadow.brighter());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = 1;

            return insets;
        }

        protected void paintLoweredBevel(Component c, Graphics g, int x, int y, int width, int height) {
            if (!c.isEnabled()) {
                return;
            }

            Color oldColor = g.getColor();
            int h = height;
            int w = width;

            g.translate(x, y);

            g.setColor(getShadowOuterColor(c));
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 1, 0);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(1, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, 1, w - 1, h - 2);

            g.translate(-x, -y);
            g.setColor(oldColor);
        }

        protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height) {
            if (!c.isEnabled()) {
                return;
            }

            Color oldColor = g.getColor();
            int h = height;
            int w = width;

            g.translate(x, y);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 1, 0);

            g.setColor(getShadowOuterColor(c));
            g.drawLine(0, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, 0, w - 1, h - 2);

            g.translate(-x, -y);
            g.setColor(oldColor);
        }
    }

    static class ImageIconButton extends JButton implements MouseListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        private static final Border LOWERED_BORDER = new ThinBevelBorder(BevelBorder.LOWERED, Color.WHITE, Color.GRAY);
        private static final Border RAISED_BORDER = new ThinBevelBorder(BevelBorder.RAISED, Color.WHITE, Color.GRAY);
        private boolean rollover = false;
        private boolean pressed = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ImageIconButton(ImageIcon icon) {
            super();

            GrayFilter enabledFilter = new GrayFilter(true, 35);
            ImageProducer prod = new FilteredImageSource(icon.getImage().getSource(), enabledFilter);
            Icon grayIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));
            GrayFilter disabledFilter = new GrayFilter(true, 60);
            prod = new FilteredImageSource(icon.getImage().getSource(), disabledFilter);

            Icon disabledIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));

            setIcon(grayIcon);
            setRolloverIcon(icon);
            setPressedIcon(icon);
            setDisabledIcon(disabledIcon);
            setIconTextGap(0);
            setBorder(EMPTY_BORDER);
            setFocusPainted(false);
            setContentAreaFilled(false);

            addMouseListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            rollover = true;

            if (pressed) {
                setBorder(LOWERED_BORDER);
            } else {
                setBorder(RAISED_BORDER);
            }
        }

        public void mouseExited(MouseEvent e) {
            rollover = false;
            setBorder(EMPTY_BORDER);
        }

        public void mousePressed(MouseEvent e) {
            pressed = true;
            setBorder(LOWERED_BORDER);
        }

        public void mouseReleased(MouseEvent e) {
            pressed = false;

            if (rollover) {
                setBorder(RAISED_BORDER);
            } else {
                setBorder(EMPTY_BORDER);
            }
        }

        public Dimension getPreferredSize() {
            return new Dimension(TABBUTTON_MARGIN_LEFT + TABBUTTON_MARGIN_RIGHT, TABBUTTON_MARGIN_LEFT + TABBUTTON_MARGIN_RIGHT);
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Dimension size = getSize();
            Graphics2D g2 = (Graphics2D)g;
            if( hasFocus() && isEnabled() ) {
                g2.setStroke(TABBUTTON_FOCUS_STROKE);
                g2.setColor(TABBUTTON_FOCUS_COLOR);
                g2.drawRect(2, 2, size.width - 5, size.height - 5);
            }
        }
    }

}
