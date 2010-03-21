/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.tracer.impl.probes;

import com.sun.tools.visualvm.modules.tracer.TracerProbe;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProbePresenter extends JLabel {

    private static final Color SELECTED_FILTER = new Color(0, 0, 200, 40);
    private static final float[] FRACTIONS = new float[] { 0.1f, 0.5f, 0.55f, 0.8f };
    private static final Color[] COLORS = new Color[] { new Color(250, 250, 250, 110),
                                                        new Color(205, 205, 220, 30),
                                                        new Color(180, 180, 195, 30),
                                                        new Color(200, 200, 210, 110) };

    private LinearGradientPaint gradientPaint;
    
    private boolean isSelected = false;

    public ProbePresenter(TracerProbe p) {
        super(p.getDescriptor().getProbeName(), p.getDescriptor().getProbeIcon(), JLabel.LEADING);
        setIconTextGap(7);
        setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 1, 1, 1, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
    }

    
    public void setSelected(boolean selected) {
        if (isSelected == selected) return;
        isSelected = selected;
        repaint();
    }
    
    public boolean isSelected() {
        return isSelected;
    }


    public void reshape(int x, int y, int w, int h) {
        gradientPaint = new LinearGradientPaint(0, 0, 0, h - 1, FRACTIONS, COLORS);
        super.reshape(x, y, w, h);
    }


    protected void paintComponent(Graphics g) {
        int y = getHeight() - 1;

        ((Graphics2D)g).setPaint(gradientPaint);
        g.fillRect(0, 0, getWidth(), y);

        if (isSelected) {
            g.setColor(SELECTED_FILTER);
            g.fillRect(0, 0, getWidth(), y);
        }

        super.paintComponent(g);
    }

}
