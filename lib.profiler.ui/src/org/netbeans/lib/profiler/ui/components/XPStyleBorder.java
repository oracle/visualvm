/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.components;

import org.netbeans.lib.profiler.ui.UIUtils;
import java.awt.*;
import javax.swing.border.AbstractBorder;


/**
 *
 * @author  Jiri Sedlacek
 */
public class XPStyleBorder extends AbstractBorder {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static boolean isNimbus = UIUtils.isNimbus();
            
    public static final int BORDER_STATE_DEFAULT = 10;
    public static final int BORDER_STATE_FOCUSED = 20;
    public static final int BORDER_STATE_SELECTED = 30;
    private static final Color OUTLINE_CLR = new Color(0, 0, 0);
    private static final Color FOCUSED_TOP_LIGHT_CLR = isNimbus ? new Color(200, 212, 222) : new Color(206, 231, 255);
    private static final Color FOCUSED_TOP_DARK_CLR = isNimbus ? new Color(200, 212, 222) : new Color(188, 212, 246);
    private static final Color FOCUSED_BOTTOM_LIGHT_CLR = isNimbus ? new Color(200, 212, 222) : new Color(137, 173, 228);
    private static final Color FOCUSED_BOTTOM_DARK_CLR = isNimbus ? new Color(200, 212, 222) : new Color(105, 130, 238);
    private static final Color SELECTED_TOP_LIGHT_CLR = isNimbus ? new Color(182, 203, 222) : new Color(255, 240, 207);
    private static final Color SELECTED_TOP_DARK_CLR = isNimbus ? new Color(182, 203, 222) : new Color(253, 216, 137);
    private static final Color SELECTED_BOTTOM_LIGHT_CLR = isNimbus ? new Color(182, 203, 222) : new Color(248, 178, 48);
    private static final Color SELECTED_BOTTOM_DARK_CLR = isNimbus ? new Color(182, 203, 222) : new Color(229, 151, 0);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Color DEFAULT_BOTTOM_DARK_CLR;
    private Color DEFAULT_BOTTOM_LIGHT_CLR;
    private Color DEFAULT_TOP_DARK_CLR;
    private Color DEFAULT_TOP_LIGHT_CLR;
    private Color backgroundColor;
    private Color backgroundFade;
    private Color startColor;
    private Color stopColor;
    private int borderState;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of CustomTaskButtonBorder */
    public XPStyleBorder(Color foreground, Color background) {
        super();
        setForegroundColor(foreground);
        setBackgroundColor(background);
        setSelected();
    }

    public XPStyleBorder(Color foreground, Color background, int state) {
        this(foreground, background);
        setBorderState(state);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setBackgroundColor(Color background) {
        backgroundColor = background;
        backgroundFade = UIUtils.getSafeColor(((3 * background.getRed()) + (1 * OUTLINE_CLR.getRed())) / 4,
                                              ((3 * background.getGreen()) + (1 * OUTLINE_CLR.getGreen())) / 4,
                                              ((3 * background.getBlue()) + (1 * OUTLINE_CLR.getBlue())) / 4);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public static Insets getBorderInsets() {
        return new Insets(4, 4, 4, 4);
    }

    public static XPStyleBorder getDefaultInstance(Color foreground, Color background) {
        return new XPStyleBorder(foreground, background, BORDER_STATE_DEFAULT);
    }

    public static XPStyleBorder getFocusedInstance(Color foreground, Color background) {
        return new XPStyleBorder(foreground, background, BORDER_STATE_FOCUSED);
    }

    public Insets getBorderInsets(Component c) {
        return XPStyleBorder.getBorderInsets();
    }

    public boolean isBorderOpaque() {
        return false;
    }

    public void setBorderState(int state) {
        borderState = state;
    }

    public int getBorderState() {
        return borderState;
    }

    public void setFocused() {
        setBorderState(XPStyleBorder.BORDER_STATE_FOCUSED);
    }

    public boolean isFocused() {
        return getBorderState() == XPStyleBorder.BORDER_STATE_FOCUSED;
    }

    public void setForegroundColor(Color foreground) {
        DEFAULT_TOP_LIGHT_CLR = UIUtils.getSafeColor(foreground.getRed() + 15, foreground.getGreen() + 15,
                                                     foreground.getBlue() + 15);
        DEFAULT_TOP_DARK_CLR = UIUtils.getSafeColor(foreground.getRed() + 8, foreground.getGreen() + 8, foreground.getBlue() + 8);
        DEFAULT_BOTTOM_LIGHT_CLR = UIUtils.getSafeColor(foreground.getRed() - 11, foreground.getGreen() - 11,
                                                        foreground.getBlue() - 11);
        DEFAULT_BOTTOM_DARK_CLR = UIUtils.getSafeColor(foreground.getRed() - 25, foreground.getGreen() - 25,
                                                       foreground.getBlue() - 25);
    }

    public Color getForegroundColor() {
        return UIUtils.getSafeColor(Math.max(DEFAULT_TOP_LIGHT_CLR.getRed() - 15, 0),
                                    Math.max(DEFAULT_TOP_LIGHT_CLR.getGreen() - 15, 0),
                                    Math.max(DEFAULT_TOP_LIGHT_CLR.getBlue() - 15, 0));
    }

    public static XPStyleBorder getSelectedInstance(Color foreground, Color background) {
        return new XPStyleBorder(foreground, background, BORDER_STATE_SELECTED);
    }

    public void setDefault() {
        setBorderState(XPStyleBorder.BORDER_STATE_DEFAULT);
    }

    public boolean isDefault() {
        return getBorderState() == XPStyleBorder.BORDER_STATE_DEFAULT;
    }

    public void setSelected() {
        setBorderState(XPStyleBorder.BORDER_STATE_SELECTED);
    }

    public boolean isSelected() {
        return getBorderState() == XPStyleBorder.BORDER_STATE_SELECTED;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (!(g instanceof Graphics2D)) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        // Background points for "rounded" edges
        g2d.setColor(backgroundColor);
        g2d.drawLine(x, y, x, y);
        g2d.drawLine(x, (y + height) - 1, x, (y + height) - 1);
        g2d.drawLine((x + width) - 1, y, (x + width) - 1, y);
        g2d.drawLine((x + width) - 1, (y + height) - 1, (x + width) - 1, (y + height) - 1);

        // Fade points for smooth "rounded" edges
        g2d.setColor(backgroundFade);
        g2d.drawLine(x + 1, y, x + 1, y);
        g2d.drawLine(x, y + 1, x, y + 1);
        g2d.drawLine(x + 1, (y + height) - 1, x + 1, (y + height) - 1);
        g2d.drawLine(x, (y + height) - 2, x, (y + height) - 2);
        g2d.drawLine((x + width) - 1, y + 1, (x + width) - 1, y + 1);
        g2d.drawLine((x + width) - 2, y, (x + width) - 2, y);
        g2d.drawLine((x + width) - 2, (y + height) - 1, (x + width) - 2, (y + height) - 1);
        g2d.drawLine((x + width) - 1, (y + height) - 2, (x + width) - 1, (y + height) - 2);

        // Points connecting outer black borders
        g2d.setColor(OUTLINE_CLR);
        g2d.drawLine(x + 1, y + 1, x + 1, y + 1);
        g2d.drawLine((x + width) - 2, y + 1, (x + width) - 2, y + 1);
        g2d.drawLine(x + 1, (y + height) - 2, x + 1, (y + height) - 2);
        g2d.drawLine((x + width) - 2, (y + height) - 2, (x + width) - 2, (y + height) - 2);

        // Outer black borders
        g2d.setColor(OUTLINE_CLR);
        g2d.drawLine(x + 2, y, (x + width) - 3, y);
        g2d.drawLine(x, y + 2, x, (y + height) - 3);
        g2d.drawLine((x + width) - 1, y + 2, (x + width) - 1, (y + height) - 3);
        g2d.drawLine(x + 2, (y + height) - 1, (x + width) - 3, (y + height) - 1);

        // Top light line
        switch (borderState) {
            case BORDER_STATE_DEFAULT:
                g2d.setColor(DEFAULT_TOP_LIGHT_CLR);

                break;
            case BORDER_STATE_FOCUSED:
                g2d.setColor(FOCUSED_TOP_LIGHT_CLR);

                break;
            case BORDER_STATE_SELECTED:
                g2d.setColor(SELECTED_TOP_LIGHT_CLR);

                break;
        }

        g.drawLine(x + 2, y + 1, (x + width) - 3, y + 1);

        // Top dark lines
        switch (borderState) {
            case BORDER_STATE_DEFAULT:
                g2d.setColor(DEFAULT_TOP_DARK_CLR);

                break;
            case BORDER_STATE_FOCUSED:
                g2d.setColor(FOCUSED_TOP_DARK_CLR);

                break;
            case BORDER_STATE_SELECTED:
                g2d.setColor(SELECTED_TOP_DARK_CLR);

                break;
        }

        g2d.drawLine(x + 1, y + 2, (x + width) - 2, y + 2);
        g2d.drawLine(x + 1, y + 3, (x + width) - 2, y + 3);

        // Bottom light lines
        switch (borderState) {
            case BORDER_STATE_DEFAULT:
                g2d.setColor(DEFAULT_BOTTOM_LIGHT_CLR);

                break;
            case BORDER_STATE_FOCUSED:
                g2d.setColor(FOCUSED_BOTTOM_LIGHT_CLR);

                break;
            case BORDER_STATE_SELECTED:
                g2d.setColor(SELECTED_BOTTOM_LIGHT_CLR);

                break;
        }

        g2d.drawLine(x + 1, (y + height) - 4, (x + width) - 2, (y + height) - 4);
        g2d.drawLine(x + 1, (y + height) - 3, (x + width) - 2, (y + height) - 3);

        // Bottom dark line
        switch (borderState) {
            case BORDER_STATE_DEFAULT:
                g2d.setColor(DEFAULT_BOTTOM_DARK_CLR);

                break;
            case BORDER_STATE_FOCUSED:
                g2d.setColor(FOCUSED_BOTTOM_DARK_CLR);

                break;
            case BORDER_STATE_SELECTED:
                g2d.setColor(SELECTED_BOTTOM_DARK_CLR);

                break;
        }

        g2d.drawLine(x + 2, (y + height) - 2, (x + width) - 3, (y + height) - 2);

        // Side gradients
        switch (borderState) {
            case BORDER_STATE_DEFAULT:
                startColor = DEFAULT_TOP_DARK_CLR;
                stopColor = DEFAULT_BOTTOM_LIGHT_CLR;

                break;
            case BORDER_STATE_FOCUSED:
                startColor = FOCUSED_TOP_DARK_CLR;
                stopColor = FOCUSED_BOTTOM_LIGHT_CLR;

                break;
            case BORDER_STATE_SELECTED:
                startColor = SELECTED_TOP_DARK_CLR;
                stopColor = SELECTED_BOTTOM_LIGHT_CLR;

                break;
        }

        g2d.setPaint(new GradientPaint(x + 1, y + 3, startColor, x + 1, (y + height) - 5, stopColor));
        g2d.fillRect(x + 1, y + 3, 3, height - 7);
        g2d.fillRect((x + width) - 4, y + 3, 3, height - 7);
    }
}
