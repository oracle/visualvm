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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import javax.swing.Icon;
import javax.swing.JLabel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class LabelRenderer extends JLabel {

    private final Insets insets;

    private FontMetrics fontMetrics;
    private String text;
    private Icon icon;
    private Color foreground;
    private boolean enabled;


    // --- Constructor ---------------------------------------------------------

    public LabelRenderer() {
        insets = new Insets(0, 0, 0, 0);

        setHorizontalAlignment(LEFT);
        setVerticalAlignment(TOP);
        setSize(Integer.MAX_VALUE, Integer.MAX_VALUE);

        setOpaque(false);
        setEnabled(true);
    }


    // --- Implementation ------------------------------------------------------

    public Insets getInsets() {
        return insets;
    }

    public Insets getInsets(Insets insets) {
        return this.insets;
    }

    public void setLocation(int x, int y) {
        insets.set(y, x, insets.bottom, insets.right);
    }

    public void resetLocation() {
        insets.set(0, 0, insets.bottom, insets.right);
    }


    // --- Peformance tweaks ---------------------------------------------------

    // Overridden for performance reasons.
    public void setText(String text) {
        this.text = text;
    }

    // Overridden for performance reasons.
    public String getText() {
        return text;
    }

    // Overridden for performance reasons.
    public void setIcon(Icon icon) {
        this.icon = icon;
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
        if (fontMetrics == null) fontMetrics = super.getFontMetrics(font);
        return fontMetrics;
    }

    // Overridden for performance reasons.
    public void setFont(Font font) {
        fontMetrics = null;
        super.setFont(font);
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

    // Overridden for performance reasons.
    public void paint(Graphics g) {
        Graphics componentGraphics = getComponentGraphics(g);
        Graphics co = (componentGraphics == null) ? null :
                      componentGraphics.create();
        try {
            paintComponent(co);
        } finally {
            co.dispose();
        }
    }

}
