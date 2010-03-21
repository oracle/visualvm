/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

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

    private static final LabelUI UI = new BasicLabelUI();

    private int x;
    private int y;
    private Insets insets;
    private Dimension preferredSize;

    private FontMetrics fontMetrics;
    private String text;
    private Icon icon;
    private Color foreground;
    private boolean enabled;


    // --- Constructor ---------------------------------------------------------

    public LabelRenderer() {
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(TOP);
        setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

        setOpaque(false);
        setEnabled(true);
    }


    // --- Implementation ------------------------------------------------------

    public Insets getInsets() {
        if (insets == null) insets = new Insets(0, 0, 0, 0);
        return insets;
    }

    public Insets getInsets(Insets insets) {
        return getInsets();
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Dimension getPreferredSize() {
        if (preferredSize == null) preferredSize = new Dimension();
        return preferredSize;
    }

    public void setUI(LabelUI ui) {
        super.setUI(UI);
    }

    private void updatePreferredSize() {
        if (ui == null || fontMetrics == null) return;
        Dimension dim = getPreferredSize();
        dim.width = ui.getPreferredSize(this).width;
        dim.height = fontMetrics.getAscent() + fontMetrics.getDescent();
    }

    protected void prePaint(Graphics g) {}
    protected void postPaint(Graphics g) {}

    public void paint(Graphics g) {
        Graphics cg = getComponentGraphics(g);
        boolean translate = x < 0 || y < 0;
        
        if (translate) cg.translate(x, y);
        else insets.set(y, x, 0, 0);

        prePaint(g);
        ui.update(cg, this);
        postPaint(g);
        
        if (!translate) insets.set(0, 0, 0, 0);
        else cg.translate(-x, -y);
    }


    // --- Peformance tweaks ---------------------------------------------------

    // Overridden for performance reasons.
    public void setText(String text) {
        this.text = text;
        updatePreferredSize();
    }

    // Overridden for performance reasons.
    public String getText() {
        return text;
    }

    // Overridden for performance reasons.
    public void setIcon(Icon icon) {
        this.icon = icon;
        updatePreferredSize();
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
        updatePreferredSize();
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

}
