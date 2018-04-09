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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.plaf.LabelUI;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 *
 * @author Jiri Sedlacek
 */
public class LabelRenderer extends JLabel {

    private static final LabelRendererUI UI = new LabelRendererUI();
    private static final int DIRTY = Integer.MIN_VALUE;

    private int x;
    private int y;
    private Insets insets = new Insets(0, 0, 0, 0);
    private Dimension preferredSize;

    private FontMetrics fontMetrics;
    private String text;
    private Icon icon;
    private Color foreground;
    private boolean enabled;

    private int iconWidth;
    private int iconHeight;
    private int iconTextGap;
    private int textWidth;
    private int fontAscent;


    // --- Constructor ---------------------------------------------------------

    public LabelRenderer() {
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(TOP);
        setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

        setOpaque(false);
        setEnabled(true);

        iconTextGap = super.getIconTextGap();
    }


    // --- Implementation ------------------------------------------------------

    public Insets getInsets() {
        return insets;
    }

    public Insets getInsets(Insets insets) {
        return this.insets;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Dimension getPreferredSize() {
        if (preferredSize == null) preferredSize = new Dimension(DIRTY, DIRTY);

        if (preferredSize.width == DIRTY) {
            textWidth = text == null || text.isEmpty() ? 0 : fontMetrics.stringWidth(text);
            preferredSize.width = iconWidth + textWidth;
            if (iconWidth > 0 && textWidth > 0) preferredSize.width += iconTextGap;
        }

        if (preferredSize.height == DIRTY) {
            fontAscent = fontMetrics.getAscent();
            preferredSize.height = fontAscent + fontMetrics.getDescent();
        }

        return preferredSize;
    }

    public void setUI(LabelUI ui) {
        super.setUI(UI);
    }

    private void resetPreferredSize(boolean width, boolean height) {
        if (preferredSize == null) return;
        if (width) preferredSize.width = DIRTY;
        if (height) preferredSize.height = DIRTY;
    }

    protected void prePaint(Graphics g, int x, int y) {}
    protected void postPaint(Graphics g, int x, int y) {}

    public void paint(Graphics g) {
        Graphics cg = getComponentGraphics(g);

        prePaint(cg, x, y);

        int xx = x;
        if (iconWidth > 0) {
            int yy = (preferredSize.height - iconHeight) / 2;
            icon.paintIcon(this, cg, xx, y + yy);
            xx += iconWidth + iconTextGap;
        }
        if (textWidth > 0)
            UI.paintEnabledText(this, cg, text, xx, y + fontAscent);

        postPaint(cg, x, y);
    }


    // --- Peformance tweaks ---------------------------------------------------

    // Overridden for performance reasons.
    public void setText(String text) {
        this.text = text;
        resetPreferredSize(true, false);
    }

    // Overridden for performance reasons.
    public String getText() {
        return text;
    }

    // Overridden for performance reasons.
    public void setIcon(Icon icon) {
        int oldIconWidth = iconWidth;
        iconWidth = icon == null ? 0 : icon.getIconWidth();
        iconHeight = icon == null ? 0 : icon.getIconHeight();
        this.icon = icon;
        if (oldIconWidth != iconWidth) resetPreferredSize(true, false);
    }

    // Overridden for performance reasons.
    public Icon getIcon() {
        return icon;
    }

    // Overridden for performance reasons.
    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    // Overridden for performance reasons.
    public Color getForeground() {
        return foreground;
    }

    // Overridden for performance reasons.
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Overridden for performance reasons.
    public boolean isEnabled() {
        return enabled;
    }

    // Overridden for performance reasons.
    public int getDisplayedMnemonicIndex() {
        return -1;
    }

    // Overridden for performance reasons.
    public FontMetrics getFontMetrics(Font font) {
        return fontMetrics;
    }

    // Overridden for performance reasons.
    public void setFont(Font font) {
        super.setFont(font);
        fontMetrics = super.getFontMetrics(font);
        resetPreferredSize(true, true);
    }

    // Overridden for performance reasons.
    public int getIconTextGap() {
        return iconTextGap;
    }


    // Overridden for performance reasons.
    public void setIconTextGap(int iconTextGap) {
        this.iconTextGap = iconTextGap;
        resetPreferredSize(true, false);
    }

    // Overridden for performance reasons.
    public void validate() {}

    // Overridden for performance reasons.
    public void revalidate() {}

    // Overridden for performance reasons.
    public void repaint(long tm, int x, int y, int width, int height) {}

    // Overridden for performance reasons.
    public void repaint(Rectangle r) {}

    // Overridden for performance reasons.
    public void repaint() {}

    // Overridden for performance reasons.
    public void setDisplayedMnemonic(int key) {}

    // Overridden for performance reasons.
    public void setDisplayedMnemonic(char aChar) {}

    // Overridden for performance reasons.
    public void setDisplayedMnemonicIndex(int index) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

    // Overridden for performance reasons.
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}


    private static class LabelRendererUI extends BasicLabelUI {
        protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {
            super.paintEnabledText(l, g, s, textX, textY);
        }
    }

}
