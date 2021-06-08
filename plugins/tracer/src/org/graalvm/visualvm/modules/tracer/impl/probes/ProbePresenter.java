/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer.impl.probes;

import org.graalvm.visualvm.modules.tracer.TracerProbe;
import org.graalvm.visualvm.modules.tracer.TracerProbeDescriptor;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProbePresenter extends JLabel {

    private static final Color SELECTED_FILTER = new Color(0, 0, 200, 40);
    private static final float[] FRACTIONS = new float[] { 0.0f, 0.49f, 0.51f, 1.0f };
    private static final Color[] COLORS = new Color[] { new Color(250, 251, 252, 120),
                                                        new Color(237, 240, 242, 120),
                                                        new Color(229, 233, 236, 125),
                                                        new Color(215, 221, 226, 130) };
    private static final Color BACKGROUND = UIManager.getColor("Panel.background"); // NOI18N

    private LinearGradientPaint gradientPaint;

    private static final boolean GRADIENT = !Utils.forceSpeed();
    private boolean isSelected = false;

    public ProbePresenter(TracerProbe p, TracerProbeDescriptor d) {
        super(d.getProbeName(), d.getProbeIcon(), JLabel.LEADING);
        
        // --- ToolTips support
        // Note: cannot use setToolTipText here, breaks mouseListener on parent
//        setToolTipText(d.getProbeDescription());
        // Let's store the tooltip in client property and resolve it from parent
        putClientProperty("ToolTipHelper", d.getProbeDescription()); // NOI18N
        // ---
        
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
        if (GRADIENT) gradientPaint = new LinearGradientPaint(0, 0, 0, h - 1,
                                                              FRACTIONS, COLORS);
        super.reshape(x, y, w, h);
    }


    protected void paintComponent(Graphics g) {
        int y = getHeight() - 1;

        ((Graphics2D)g).setPaint(GRADIENT ? gradientPaint : BACKGROUND);
        g.fillRect(0, 0, getWidth(), y);
        
        if (isSelected) {
            g.setColor(SELECTED_FILTER);
            g.fillRect(0, 0, getWidth(), y);
        }

        super.paintComponent(g);
    }

}
