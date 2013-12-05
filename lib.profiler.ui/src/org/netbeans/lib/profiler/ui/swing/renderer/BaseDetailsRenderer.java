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
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class BaseDetailsRenderer extends BaseRenderer {
    
    private static final int DEFAULT_DETAILS_GAP = 3;
    
    private final int detailsGap;
    
    private final ProfilerRenderer valueRenderer;
    private final LabelRenderer detailsRenderer;
    private final int detailsRendererWidth;
    
    
    public BaseDetailsRenderer(ProfilerRenderer valueRenderer, String refDetailsString) {
        this(valueRenderer, refDetailsString, DEFAULT_DETAILS_GAP);
    }
    
    public BaseDetailsRenderer(ProfilerRenderer valueRenderer, String refDetailsString, int detailsGap) {
        this.detailsGap = detailsGap;
        
        this.valueRenderer = valueRenderer;
        if (valueRenderer instanceof LabelRenderer)
            ((LabelRenderer)valueRenderer).setMargin(3, 3, 3, 0);
        
        detailsRenderer = new LabelRenderer();
        detailsRenderer.setMargin(3, 0, 3, 3);
        
        detailsRenderer.changeFontSize(-1);
        detailsRenderer.setForeground(UIUtils.getDisabledLineColor());
        detailsRenderer.setText(refDetailsString);
        detailsRendererWidth = detailsRenderer.getPreferredSize().width;
        
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.TRAILING);
    }
    
    
    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
        if (valueRenderer != null) valueRenderer.getComponent().setOpaque(isOpaque);
        if (detailsRenderer != null) detailsRenderer.setOpaque(isOpaque);
    }
    
    public void setForeground(Color foreground) {
        super.setForeground(foreground);
        if (valueRenderer != null) valueRenderer.getComponent().setForeground(foreground);
//        bracketRenderer.setForeground(foreground);
    }
    
    public void setBackground(Color background) {
        super.setBackground(background);
        if (valueRenderer != null) valueRenderer.getComponent().setBackground(background);
        detailsRenderer.setBackground(background);
    }
    
    public void setHorizontalAlignment(int alignment) {
        super.setHorizontalAlignment(alignment);
        JComponent mainRendererC = valueRenderer.getComponent();
        if (mainRendererC instanceof JLabel) ((JLabel)mainRendererC).setHorizontalAlignment(alignment);
        else if (valueRenderer instanceof BaseRenderer) ((BaseRenderer)valueRenderer).setHorizontalAlignment(alignment);
        detailsRenderer.setHorizontalAlignment(alignment);
    }
    
    public Dimension getPreferredSize() {
        Dimension preferredSize = valueRenderer.getComponent().getPreferredSize();
        preferredSize.width += detailsGap + detailsRendererWidth;
        return preferredSize;
    }
    
    public void setValue(Object value, int row) {
        String text = value.toString();
        
        String mainText;
        String bracketText;
        
        int bracketIndex = text.indexOf('('); // NOI18N
        
        if (bracketIndex == -1) {
            mainText = text;
            bracketText = ""; // NOI18N
        } else {
            mainText = text.substring(0, bracketIndex);
            bracketText = text.substring(bracketIndex);
        }
        
        valueRenderer.setValue(mainText, row);
        detailsRenderer.setValue(bracketText, row);
    }
    
    public void setValues(Object value, String details, int row) {
        valueRenderer.setValue(value, row);
        detailsRenderer.setValue(details, row);
    }
    
    public void paint(Graphics g) {
        super.paint(g);
        
        int xx = location.x + size.width - detailsRendererWidth;
        
        detailsRenderer.setSize(detailsRendererWidth, size.height);
        detailsRenderer.move(xx, location.y);
        detailsRenderer.paint(g);
        
        JComponent mainRendererC = valueRenderer.getComponent();
        Dimension mainSize = mainRendererC.getPreferredSize();
        xx -= detailsGap + mainSize.width;
        
        mainRendererC.setSize(mainSize.width, size.height);
        valueRenderer.move(xx, location.y);
        mainRendererC.paint(g);
    }
    
}
