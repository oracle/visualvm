/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Graphics;
import java.awt.Rectangle;
import org.netbeans.lib.profiler.ui.Formatters;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;

/**
 *
 * @author Jiri Sedlacek
 */
public class BarRenderer extends BaseRenderer {
    
//    private static final Color COLOR = new Color(195, 41, 41);
    private static final Color COLOR = new Color(225, 130, 130);
    
    private static final int X_MARGIN = 2;
    private static final int Y_MARGIN = 3;
    
    private static final LabelRenderer LABEL;
    private static final Dimension LABEL_REF;
    
    private static final Rectangle BAR_RECT;
    
    private long maxValue;
    private float value;
    private boolean paintBar = true;
    private boolean paintText = false;
    
    
    static {
        LABEL = new LabelRenderer(true);
        LABEL.changeFontSize(-1);
        LABEL.setText(getValue(99.9f / 100, 100));
        LABEL_REF = new Dimension(LABEL.getPreferredSize());
        LABEL_REF.width += X_MARGIN * 2;
        
        BAR_RECT = new Rectangle();
    }
    
    
    public BarRenderer() {
        maxValue = 100;
        value = 0;
        
        setOpaque(true);
        putClientProperty(ProfilerTable.PROP_NO_HOVER, this);
    }
    
    
    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }
    
    public long getMaxValue() {
        return maxValue;
    }
    
    public void setValue(Object value, int row) {
        this.value = maxValue == 0 ? 0 : ((Number)value).floatValue() / maxValue;
    }
    
    public void setPaintBar(boolean paintBar) {
        this.paintBar = paintBar;
    }
    
    public void setPaintText(boolean paintText) {
        this.paintText = paintText;
    }
    
    
    public Dimension getPreferredSize() {
        return sharedDimension(LABEL_REF);
    }
    
    
    public void paint(Graphics g) {
        // Paint background
        super.paint(g);
        
//        if (paintText) {
//            // Setup label
//            LABEL.setForeground(UIUtils.getDisabledForeground(getForeground()));
//            LABEL.setText(getValue(value, maxValue));
//            LABEL.setSize(LABEL.getPreferredSize());
//
//            // Paint text using foreground color
//            int textX = LABEL_REF.width - LABEL.getWidth() - X_MARGIN * 2;
//            int textY = (size.height - LABEL_REF.height + 1) / 2;
//            LABEL.move(location.x + textX, location.y + textY);
//            LABEL.paint(g);
//        }
        
        // Paint bar if visible
        if (!paintBar) return;
        BAR_RECT.width = (int)((size.width - X_MARGIN * 2) * Math.min(value, 1));
//        if (BAR_RECT.width > 0) {
            g.setColor(COLOR);
            
            // Paint bar
            BAR_RECT.x = location.x + X_MARGIN;
            BAR_RECT.y = location.y + Y_MARGIN;
            BAR_RECT.height = size.height - Y_MARGIN * 2;
            g.fillRect(BAR_RECT.x, BAR_RECT.y, BAR_RECT.width, BAR_RECT.height);
            
//            g.setColor(lighter(COLOR));
            g.setColor(brighter(COLOR));
//            g.setColor(new Color(235, 235, 235));
//            g.setColor(UIUtils.getDisabledForeground(getForeground()));
//            g.setColor(UIUtils.getDarker(UIUtils.getProfilerResultsBackground()));
            g.fillRect(BAR_RECT.x + BAR_RECT.width, BAR_RECT.y, size.width - X_MARGIN - BAR_RECT.x - BAR_RECT.width, BAR_RECT.height);
            
//            if (paintText) {
//                // Paint text using contrast color
//                Shape clip = g.getClip();
//                g.setClip(clip.getBounds().intersection(BAR_RECT));
//                LABEL.setForeground(Color.WHITE);
//                LABEL.paint(g);
//                g.setClip(clip);
//            }
//        }
    }
    
    private static final double FACTOR = 0.55d;
//    private static final double FACTOR = 0.20d;
    
    private static Color brighter(Color c) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();

        int i = (int)(1.0/(1.0-FACTOR));
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return new Color(Math.min((int)(r/FACTOR), 255),
                         Math.min((int)(g/FACTOR), 255),
                         Math.min((int)(b/FACTOR), 255));
    }
    
    private static Color lighter(Color color) {
        int f = 20;
        
        int r = color.getRed() + f;
        if (r > 255) r -= f * 2;
        
        int g = color.getGreen() + f;
        if (g > 255) g -= f * 2;
        
        int b = color.getBlue() + f;
        if (b > 255) b -= f * 2;
        
        return new Color(r, g, b);
    }
    
    
    private static final String NUL = Formatters.percentFormat().format(0);
    private static final String NAN = NUL.replace("0", "-"); // NOI18N
    
    private static String getValue(float value, long maxValue) {
        StringBuilder b = new StringBuilder();
        b.append("("); // NOI18N
        
        if (maxValue == 0) {
            b.append(NAN);
        } else if (value == 0) {
            b.append(NUL);
        } else {
            b.append(Formatters.percentFormat().format(value));
        }
        
        b.append(")"); // NOI18N
        return b.toString();
    }
    
}
