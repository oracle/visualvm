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

import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public class HideableBarRenderer extends MultiRenderer {
    
    private static final int BAR_MARGIN = 20;
    private static final int MIN_BAR_WIDTH = 20;
    private static final int MAX_BAR_WIDTH = 100;
    private static final int OPT_BAR_WIDTH = 50;
    
    private int maxRendererWidth;
    
    private final BarRenderer barRenderer;
    private final ProfilerRenderer mainRenderer;
    private final NumberPercentRenderer numberPercentRenderer;
    
    private final ProfilerRenderer[] valueRenderers;
    
    
    public HideableBarRenderer(ProfilerRenderer renderer) {
        this(renderer, renderer.getComponent().getPreferredSize().width);
    }
    
    public HideableBarRenderer(ProfilerRenderer renderer, int maxWidth) {
        maxRendererWidth = maxWidth;
        
        barRenderer = new BarRenderer();
        mainRenderer = renderer;
        numberPercentRenderer = renderer instanceof NumberPercentRenderer ?
                                (NumberPercentRenderer)renderer : null;
        
        valueRenderers = new ProfilerRenderer[] { barRenderer, mainRenderer };
        
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.TRAILING);
    }
    
    
    public void setMaxValue(long maxValue) {
        int oldDigits = Long.toString(barRenderer.getMaxValue()).length();
        int newDigits = Long.toString(maxValue).length();
        
        barRenderer.setMaxValue(maxValue);
        if (numberPercentRenderer != null) numberPercentRenderer.setMaxValue(maxValue);
        
        if (oldDigits < newDigits) {
            // Number of the same pow10 created using only digit '9'
//            int ref = (int)Math.pow(10, Math.ceil(Math.log10(maxValue + 1))) - 1;
            mainRenderer.setValue((long)Math.pow(10, newDigits) - 1, -1);
            int mainWidth = mainRenderer.getComponent().getPreferredSize().width;
            maxRendererWidth = Math.max(maxRendererWidth, mainWidth);
        }
    }
    
    public void setValue(Object value, int row) {
        barRenderer.setValue(value, row);
        mainRenderer.setValue(value, row);
    }
    

    protected ProfilerRenderer[] valueRenderers() {
        return valueRenderers;
    }
    
    protected int renderersGap() {
        return BAR_MARGIN;
    }
    
    
    public Dimension getPreferredSize() {
        return mainRenderer.getComponent().getPreferredSize();
    }
    
    public int getOptimalWidth() {
        return maxRendererWidth + renderersGap() + OPT_BAR_WIDTH;
    }
    
    public int getMaxNoBarWidth() {
        return maxRendererWidth + renderersGap() + MIN_BAR_WIDTH - 1;
    }
    
    public int getNoBarWidth() {
        return maxRendererWidth;
    }
    
    public void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(location.x, location.y, size.width, size.height);
        
        JComponent component = mainRenderer.getComponent();
        int componentWidth = component.getPreferredSize().width;
        int componentX = size.width - componentWidth;
        
        mainRenderer.move(location.x + componentX, location.y);
        component.setSize(componentWidth, size.height);
        component.paint(g);
        
        int freeWidth = size.width - maxRendererWidth - renderersGap();
        if (freeWidth >= MIN_BAR_WIDTH) {
            barRenderer.setSize(Math.min(freeWidth, MAX_BAR_WIDTH), size.height);
            barRenderer.move(location.x, location.y);
            barRenderer.paint(g);
        }
    }
    
    public String toString() {
        return mainRenderer.toString();
    }
    
}
