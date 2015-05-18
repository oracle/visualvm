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
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class MultiRenderer extends BaseRenderer implements RelativeRenderer {
    
    private Dimension preferredSize;
    
    
    protected int renderersGap() { return 0; }
    
    protected abstract ProfilerRenderer[] valueRenderers();
    
    
    public void setDiffMode(boolean diffMode) {
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;
        
        for (ProfilerRenderer renderer : valueRenderers)
            if (renderer instanceof RelativeRenderer)
                ((RelativeRenderer)renderer).setDiffMode(diffMode);
    }

    public boolean isDiffMode() {
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return false;
        
        for (ProfilerRenderer renderer : valueRenderers)
            if (renderer instanceof RelativeRenderer)
                return ((RelativeRenderer)renderer).isDiffMode();
        
        return false;
    }
    
    
    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
        
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;
        
        for (ProfilerRenderer renderer : valueRenderers)
            renderer.getComponent().setOpaque(isOpaque);
    }
    
    public void setForeground(Color foreground) {
        super.setForeground(foreground);
        
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;
        
        for (ProfilerRenderer renderer : valueRenderers)
            renderer.getComponent().setForeground(foreground);
    }
    
    public void setBackground(Color background) {
        super.setBackground(background);
        
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers == null) return;
        
        for (ProfilerRenderer renderer : valueRenderers)
            renderer.getComponent().setBackground(background);
    }
    
    public Dimension getPreferredSize() {
        if (preferredSize == null) preferredSize = new Dimension();
        else preferredSize.setSize(0, 0);
        
        ProfilerRenderer[] valueRenderers = valueRenderers();
        if (valueRenderers != null) {
            int visible = 0;
            for (ProfilerRenderer renderer : valueRenderers) {
                JComponent component = renderer.getComponent();
                if (component.isVisible()) {
                    Dimension rendererSize = component.getPreferredSize();
                    preferredSize.width += rendererSize.width;
                    preferredSize.height = Math.max(preferredSize.height, rendererSize.height);
                    visible++;
                }
            }
            preferredSize.width += renderersGap() * (visible - 1);
        }
        
        return sharedDimension(preferredSize);
    }
    
    
    public void paint(Graphics g) {
        super.paint(g);
        
        int alignment = getHorizontalAlignment();
        int renderersGap = renderersGap();
        
        if (alignment == SwingConstants.LEADING || alignment == SwingConstants.LEFT) {
            
            int xx = location.x;
            
            for (ProfilerRenderer renderer : valueRenderers()) {
                JComponent component = renderer.getComponent();
                if (component.isVisible()) {
                    int componentWidth = component.getPreferredSize().width;
                    component.setSize(componentWidth, size.height);
                    renderer.move(xx, location.y);
                    component.paint(g);
                    xx += componentWidth + renderersGap;
                }
            }
            
        } else {
            
            int xx = location.x + size.width;
            
            ProfilerRenderer[] valueRenderers = valueRenderers();
            for (int i = valueRenderers.length - 1; i >= 0; i--) {
                ProfilerRenderer renderer = valueRenderers[i];
                JComponent component = renderer.getComponent();
                if (component.isVisible()) {
                    int componentWidth = component.getPreferredSize().width;
                    component.setSize(componentWidth, size.height);
                    xx -= componentWidth;
                    renderer.move(xx, location.y);
                    component.paint(g);
                    xx -= renderersGap;
                }
            }
            
        }
    }
    
}
