/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components;

import java.awt.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicToolBarUI;
import org.graalvm.visualvm.lib.ui.UIUtils;


/**
 * A special version of ToolBar that has transparent background and rollover button style.
 * Can be used e.g. on on a white background form-like UI.
 *
 * @author Ian Formanek
 */
public class FlatToolBar extends JToolBar {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class FlatMarginBorder extends AbstractBorder {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Insets getBorderInsets(Component c) {
            return getBorderInsets(c, new Insets(0, 0, 0, 0));
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            Insets margin = null;

            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                margin = b.getMargin();
            }

            insets.top = (margin != null) ? margin.top : 0;
            insets.left = (margin != null) ? margin.left : 0;
            insets.bottom = (margin != null) ? margin.bottom : 0;
            insets.right = (margin != null) ? margin.right : 0;

            return insets;
        }
    }

    /**
     * Special thin border for rollover toolbar buttons.
     */
    public static class FlatRolloverButtonBorder extends AbstractBorder {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Color normal;
        private Color pressed;
        private Color roll;
        private boolean borderPainted = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public FlatRolloverButtonBorder(Color pressed, Color roll) {
            super();
            this.pressed = pressed;
            this.roll = roll;
            this.borderPainted = false;
        }

        public FlatRolloverButtonBorder(Color pressed, Color roll, Color normal) {
            super();
            this.pressed = pressed;
            this.roll = roll;
            this.normal = normal;
            this.borderPainted = true;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Insets getBorderInsets(Component c) {
            return getBorderInsets(c, new Insets(0, 0, 0, 0));
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            // leave room for default visual
            insets.top = 2;
            insets.left = insets.bottom = insets.right = 3;

            return insets;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();

            if ((model.isRollover() && !(model.isPressed() && !model.isArmed())) || model.isSelected()) {
                Color oldColor = g.getColor();
                g.translate(x, y);

                if ((model.isPressed() && model.isArmed()) || model.isSelected()) {
                    // Draw the pressed button
                    g.setColor(pressed);
                    g.drawRect(0, 0, w - 1, h - 1);
                } else {
                    // Draw a rollover button
                    g.setColor(roll);
                    g.drawRect(0, 0, w - 1, h - 1);
                }

                g.translate(-x, -y);
                g.setColor(oldColor);
            } else if (borderPainted) {
                Color oldColor = g.getColor();
                g.translate(x, y);
                g.setColor(normal);
                g.drawRect(0, 0, w - 1, h - 1);
                g.translate(-x, -y);
                g.setColor(oldColor);
            }
        }
    }

    private static class MyToolBarUI extends BasicToolBarUI {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private AbstractBorder myRolloverBorder;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        MyToolBarUI() {
            myRolloverBorder = new CompoundBorder(new FlatRolloverButtonBorder(Color.GRAY, Color.LIGHT_GRAY),
                                                  new FlatMarginBorder());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected void setBorderToRollover(Component c) {
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;

                if (b.getBorder() instanceof UIResource) {
                    b.setBorder(myRolloverBorder);
                }

                b.setRolloverEnabled(true);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int BUTTON_STYLE_HORIZONTAL = 1;
    public static final int BUTTON_STYLE_VERICAL = 2;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    int buttonStyle = BUTTON_STYLE_HORIZONTAL;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a horizontal toolbar with horizontal button style (i.e. icon left to text)
     */
    public FlatToolBar() {
        this(HORIZONTAL, BUTTON_STYLE_HORIZONTAL);
    }

    /**
     * Creates a toolbar with specified orientation and horizontal button style (i.e. icon left to text)
     *
     * @see JToolBar#HORIZONTAL
     * @see JToolBar#VERTICAL
     * @param orientation
     */
    public FlatToolBar(int orientation) {
        this(orientation, BUTTON_STYLE_HORIZONTAL);
    }

    /**
     * Creates a toolbar with specified orientation and button style
     *
     * @see JToolBar#HORIZONTAL
     * @see JToolBar#VERTICAL
     *
     * @param orientation
     * @param buttonStyle
     */
    public FlatToolBar(int orientation, int buttonStyle) {
        super(orientation);
        this.buttonStyle = buttonStyle;

        if (!UIUtils.isGTKLookAndFeel()) {
            setUI(new MyToolBarUI());
        }

        setFloatable(false);
        setOpaque(false);
        putClientProperty("JToolBar.isRollover", Boolean.TRUE); //NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp instanceof AbstractButton) {
            AbstractButton ab = (AbstractButton) comp;
            ab.setContentAreaFilled(false);
            ab.setMargin(new Insets(3, 3, 3, 3));

            if (buttonStyle == BUTTON_STYLE_VERICAL) {
                ab.setVerticalTextPosition(SwingConstants.BOTTOM);
                ab.setHorizontalTextPosition(SwingConstants.CENTER);
            }
        }

        super.addImpl(comp, constraints, index);
    }

    protected JButton createActionComponent(Action a) {
        JButton b = super.createActionComponent(a);

        if (buttonStyle == BUTTON_STYLE_VERICAL) {
            b.putClientProperty("hideActionText", Boolean.FALSE); //NOI18N

            String iconBase = (String) a.getValue("iconBase"); //NOI18N

            if (iconBase != null) {
                try {
                    System.err.println("URL for: " + insertBeforeSuffix(iconBase, "32")); //NOI18N

                    URL url = a.getClass().getResource(insertBeforeSuffix(iconBase, "32")); //NOI18N
                    System.err.println("is: " + url); //NOI18N
                    b.setIcon(new ImageIcon(url));
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }

        return b;
    }

    static String insertBeforeSuffix(String path, String toInsert) {
        String withoutSuffix = path;
        String suffix = ""; // NOI18N

        if (path.lastIndexOf('.') >= 0) { //NOI18N
            withoutSuffix = path.substring(0, path.lastIndexOf('.')); //NOI18N
            suffix = path.substring(path.lastIndexOf('.'), path.length()); //NOI18N
        }

        return withoutSuffix + toInsert + suffix;
    }
}
