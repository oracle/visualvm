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
import java.awt.Graphics;
import java.awt.Rectangle;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;

/**
 *
 * @author Jiri Sedlacek
 */
public class BarRenderer extends BaseRenderer implements RelativeRenderer {
    
    private static final Color COLOR_POS = new Color(225, 130, 130);
    private static final Color COLOR_NEG = new Color(130, 225, 130);
    
    private static final int X_MARGIN = 2;
    private static final int Y_MARGIN = 3;
    
    private static final Rectangle BAR_RECT = new Rectangle();
    
    private long maxValue;
    private float value;
    
    protected boolean renderingDiff;
    
    
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
        if (value == null) this.value = 0;
        else this.value = maxValue == 0 ? 0 : ((Number)value).floatValue() / maxValue;
    }
    
    
    public void setDiffMode(boolean diffMode) {
        renderingDiff = diffMode;
    }

    public boolean isDiffMode() {
        return renderingDiff;
    }
    
    
    public void paint(Graphics g) {
        super.paint(g);
        
        BAR_RECT.x = location.x + X_MARGIN;
        BAR_RECT.y = location.y + Y_MARGIN;
        BAR_RECT.height = size.height - Y_MARGIN * 2;
        
        int width = size.width - X_MARGIN * 2;
        
        if (renderingDiff) {
            Color color = value < 0 ? COLOR_NEG : COLOR_POS;
            int width2 = width / 2;
            
            if (value <= -1) {
                g.setColor(color);
                g.fillRect(BAR_RECT.x, BAR_RECT.y, width2, BAR_RECT.height);
                
                g.setColor(brighter(color));
                g.fillRect(BAR_RECT.x + width2, BAR_RECT.y, width - width2, BAR_RECT.height);
            } else if (value >= 1) {
                g.setColor(brighter(color));
                g.fillRect(BAR_RECT.x, BAR_RECT.y, width2, BAR_RECT.height);
                
                g.setColor(color);
                g.fillRect(BAR_RECT.x + width2, BAR_RECT.y, width - width2, BAR_RECT.height);
            } else {
                g.setColor(brighter(color));
                g.fillRect(BAR_RECT.x, BAR_RECT.y, width, BAR_RECT.height);

                BAR_RECT.width = (int)(width2 * Math.min(Math.abs(value), 1));
                if (BAR_RECT.width > 0) {
                    g.setColor(color);
                    if (value < 0) {
                        g.fillRect(BAR_RECT.x + width2 - BAR_RECT.width, BAR_RECT.y, BAR_RECT.width, BAR_RECT.height);
                    } else {
                        g.fillRect(BAR_RECT.x + width2, BAR_RECT.y, BAR_RECT.width, BAR_RECT.height);
                    }
                }
            }
        } else {
            BAR_RECT.width = (int)(width * Math.min(value, 1));
            if (BAR_RECT.width > 0) {
                g.setColor(COLOR_POS);
                g.fillRect(BAR_RECT.x, BAR_RECT.y, BAR_RECT.width, BAR_RECT.height);
            }

            if (BAR_RECT.width < width) {
                BAR_RECT.x += BAR_RECT.width;
                BAR_RECT.width = width - BAR_RECT.width;
                g.setColor(brighter(COLOR_POS));
                g.fillRect(BAR_RECT.x, BAR_RECT.y, BAR_RECT.width, BAR_RECT.height);
            }
        }
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
    
}
