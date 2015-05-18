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

package org.netbeans.lib.profiler.ui.swing.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.plaf.LabelUI;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * JLabel subclass to be used as a high-performance Table/Tree/List renderer.
 * Make sure you call setOpaque(true) for painting background.
 * For custom non-label Table/Tree/List renderer extend BaseRenderer.
 *
 * @author Jiri Sedlacek
 */
public class LabelRenderer extends JLabel implements ProfilerRenderer {
    
    // --- Constructor ---------------------------------------------------------
    
    public LabelRenderer() {
        this(false);
    }

    public LabelRenderer(boolean plain) {
        setEnabled(true);
        setHorizontalAlignment(LEADING);
        setVerticalAlignment(TOP);
        setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

        if (plain) {
            setOpaque(false);
        } else {
            setOpaque(true);
            setMargin(3, 3, 3, 3);
        }

        iconTextGap = super.getIconTextGap();
    }
    
    // --- Renderer ------------------------------------------------------------
    
    public void setValue(Object value, int row) {
        if (value == null) setText(""); // NOI18N
        else setText(value.toString());
    }
    
    public JComponent getComponent() {
        return this;
    }
    
    public String toString() {
        return getText();
    }
    
    // --- Appearance ----------------------------------------------------------
    
    private static final LabelRendererUI UI = new LabelRendererUI();
    private static final int DIRTY = Integer.MIN_VALUE;
    
    private Dimension preferredSize;
    
    private int iconWidth;
    private int iconHeight;
    private int iconTextGap;
    private int textWidth;
    private int fontAscent;
    
    public void setUI(LabelUI ui) {
        super.setUI(UI);
    }
    
    public Dimension getPreferredSize() {
        return sharedDimension(getPreferredSizeImpl());
    }
    
    public Dimension getPreferredSizeImpl() {
        if (preferredSize == null) preferredSize = new Dimension(DIRTY, DIRTY);
        
        if (preferredSize.width == DIRTY) {
            textWidth = text == null || text.isEmpty() ? 0 : fontMetrics.stringWidth(text);
            preferredSize.width = iconWidth + textWidth;
            preferredSize.width += margin.left + margin.right;
            if (iconWidth > 0 && textWidth > 0) preferredSize.width += iconTextGap;
        }

        if (preferredSize.height == DIRTY) {
            fontAscent = fontMetrics.getAscent();
            preferredSize.height = fontAscent + fontMetrics.getDescent();
            preferredSize.height += margin.top + margin.bottom;
        }
    
        return preferredSize;
    }

    private void resetPreferredSize(boolean width, boolean height) {
        if (preferredSize == null) return;
        if (width) preferredSize.width = DIRTY;
        if (height) preferredSize.height = DIRTY;
    }

    public void paint(Graphics g) {
        int xx = location.x;
        int h = size.height;
        int hh = getPreferredSizeImpl().height; // lazily computes dirty metrics
        
        if (background != null && isOpaque()) {
            g.setColor(background);
            g.fillRect(xx, location.y, size.width, h);
        }
        
        g.setFont(getFont());
        
        int hAlign = getHorizontalAlignment();
        if (hAlign == LEADING) {
            xx += margin.left;
        } else if (hAlign == CENTER) {
            int w = size.width - textWidth - iconWidth;
            if (textWidth > 0 && iconWidth > 0 ) w -= iconTextGap;
            xx += Math.max(margin.left, w / 2);
        } else {
            xx += size.width - margin.right - textWidth;
            if (iconWidth > 0 ) xx += - iconWidth - iconTextGap;
        }
        
        if (iconWidth > 0) {
            int yy = (h - iconHeight) / 2;
            icon.paintIcon(this, g, xx, location.y + yy);
            xx += iconWidth + iconTextGap;
        }
        
        if (textWidth > 0) {
            int yy = (h - hh - fontSizeDiff) / 2 + margin.top;
            UI.paintEnabledText(this, g, text, xx, location.y + yy + fontAscent);
        }
    }

    // --- Tools ---------------------------------------------------------------
    
    private Point sharedPoint;
    private Dimension sharedDimension;
    private Rectangle sharedRectangle;
    
    protected final Point sharedPoint(int x, int y) {
        if (sharedPoint == null) sharedPoint = new Point();
        sharedPoint.x = x;
        sharedPoint.y = y;
        return sharedPoint;
    }
    
    protected final Point sharedPoint(Point point) {
        return sharedPoint(point.x, point.y);
    }
    
    protected final Dimension sharedDimension(int width, int height) {
        if (sharedDimension == null) sharedDimension = new Dimension();
        sharedDimension.width = width;
        sharedDimension.height = height;
        return sharedDimension;
    }
    
    protected final Dimension sharedDimension(Dimension dimension) {
        return sharedDimension(dimension.width, dimension.height);
    }
    
    protected final Rectangle sharedRectangle(int x, int y, int width, int height) {
        if (sharedRectangle == null) sharedRectangle = new Rectangle();
        sharedRectangle.x = x;
        sharedRectangle.y = y;
        sharedRectangle.width = width;
        sharedRectangle.height = height;
        return sharedRectangle;
    }
    
    protected final Rectangle sharedRectangle(Rectangle rectangle) {
        return sharedRectangle(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }
    
    // --- Geometry ------------------------------------------------------------
    
    protected final Point location = new Point();
    protected final Dimension size = new Dimension();
    
    public void move(int x, int y) {
        location.x = x;
        location.y = y;
    }
    
    public Point getLocation() {
        return sharedPoint(location);
    }
    
    public int getX() {
        return location.x;
    }
    
    public int getY() {
        return location.y;
    }
    
    public void setSize(int w, int h) {
        size.width = w;
        size.height = h;
    }
    
    public Dimension getSize() {
        return sharedDimension(size);
    }
    
    public int getWidth() {
        return size.width;
    }
    
    public int getHeight() {
        return size.height;
    }
    
    public Rectangle getBounds() {
        return sharedRectangle(location.x, location.y, size.width, size.height);
    }
    
    public void reshape(int x, int y, int w, int h) {
        // ignore x, y: used only for move(x, y)
//        location.x = x;
//        location.y = y;
        size.width = w;
        size.height = h;
    }

    // --- Margins -------------------------------------------------------------
    
    private final Insets insets = new Insets(0, 0, 0, 0);
    private final Insets margin = new Insets(0, 0, 0, 0);
    
    public Insets getInsets() {
        return insets;
    }

    public Insets getInsets(Insets insets) {
        return this.insets;
    }
    
    public void setMargin(int top, int left, int bottom, int right) {
        margin.top = top;
        margin.left = left;
        margin.bottom = bottom;
        margin.right = right;
        resetPreferredSize(true, true);
    }
    
    public Insets getMargin() {
        return margin;
    }

    // --- Other peformance tweaks ---------------------------------------------
    
    private FontMetrics fontMetrics;
    private int fontSizeDiff;
    private String text;
    private Icon icon;
    private Color foreground;
    private Color background;
    private boolean enabled;

    public void setText(String text) {
        this.text = text;
        resetPreferredSize(true, false);
    }

    public String getText() {
        return text;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        iconWidth = icon == null ? 0 : icon.getIconWidth();
        iconHeight = icon == null ? 0 : icon.getIconHeight();
        resetPreferredSize(true, false); // Icon likely won't change height
    }

    public Icon getIcon() {
        return icon;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    public Color getForeground() {
        return foreground;
    }
    
    public void setBackground(Color background) {
        this.background = background;
    }

    public Color getBackground() {
        return background;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getDisplayedMnemonicIndex() {
        return -1;
    }

    public FontMetrics getFontMetrics(Font font) {
        return fontMetrics;
    }

    public void setFont(Font font) {
        super.setFont(font);
        fontMetrics = super.getFontMetrics(font);
        resetPreferredSize(true, true);
    }
    
    // Use to keep the baseline for various font-sized instances
    public void changeFontSize(int diff) {
        fontSizeDiff = diff;
        Font font = getFont();
        setFont(font.deriveFont(font.getSize2D() + diff));
    }

    public int getIconTextGap() {
        return iconTextGap;
    }


    public void setIconTextGap(int iconTextGap) {
        this.iconTextGap = iconTextGap;
        resetPreferredSize(true, false);
    }
    
    // --- Painting / Layout ---------------------------------------------------

    public void validate() {}

    public void revalidate() {}

    public void repaint(long tm, int x, int y, int width, int height) {}

    public void repaint(Rectangle r) {}

    public void repaint() {}

    public void setDisplayedMnemonic(int key) {}

    public void setDisplayedMnemonic(char aChar) {}

    public void setDisplayedMnemonicIndex(int index) {}
    
    // --- Events --------------------------------------------------------------

    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}

    public void firePropertyChange(String propertyName, char oldValue, char newValue) {}

    public void firePropertyChange(String propertyName, short oldValue, short newValue) {}

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {}

    public void firePropertyChange(String propertyName, long oldValue, long newValue) {}

    public void firePropertyChange(String propertyName, float oldValue, float newValue) {}

    public void firePropertyChange(String propertyName, double oldValue, double newValue) {}

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}


    private static final class LabelRendererUI extends BasicLabelUI {
        protected void paintEnabledText(JLabel l, Graphics g, String s, int textX, int textY) {
            super.paintEnabledText(l, g, s, textX, textY);
        }
    }

}
