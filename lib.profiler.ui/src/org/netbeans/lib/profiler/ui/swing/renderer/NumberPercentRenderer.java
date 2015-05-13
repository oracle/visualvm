/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
import java.text.Format;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class NumberPercentRenderer extends MultiRenderer {
    
    private final ProfilerRenderer valueRenderer;
    private final PercentRenderer percentRenderer;
    
    private final ProfilerRenderer[] renderers;
    
    private Dimension percentSize;
    
    
    public NumberPercentRenderer() {
        this((Format)null);
    }
    
    public NumberPercentRenderer(Format customFormat) {
        this(createNumberRenderer(customFormat));
    }
    
    public NumberPercentRenderer(ProfilerRenderer renderer) {
        valueRenderer = renderer;
        
        percentRenderer = new PercentRenderer() {
            public void setForeground(Color foreground) {
                if (foreground == null) foreground = Color.BLACK;
                super.setForeground(UIUtils.getDisabledForeground(foreground));
            }
            public Dimension getPreferredSize() {
                if (percentSize == null) percentSize = super.getPreferredSize();
                return percentSize;
            }
        };
        percentRenderer.changeFontSize(-1);
        percentRenderer.setMargin(3, 0, 3, 3);
        percentRenderer.setHorizontalAlignment(SwingConstants.TRAILING);
        
        percentRenderer.setMaxValue(100);
        percentRenderer.setValue(9999, -1);
        int fixedWidth = percentRenderer.getPreferredSize().width;
        percentSize.width = fixedWidth;
        
        renderers = new ProfilerRenderer[] { valueRenderer, percentRenderer };
        
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.TRAILING);
    }
    
    
    protected ProfilerRenderer[] valueRenderers() {
        return renderers;
    }
    
    
    public void setMaxValue(long maxValue) {
        percentRenderer.setMaxValue(maxValue);
    }
    
    public void setValue(Object value, int row) {
        valueRenderer.setValue(value, row);
        percentRenderer.setValue(value, row);
    }
    
    
    public void setDiffMode(boolean diffMode) {
        percentRenderer.setVisible(!diffMode);
        super.setDiffMode(diffMode);
    }
    
    
    public Dimension getPreferredSize() {
        Dimension dim = valueRenderer.getComponent().getPreferredSize();
        if (percentRenderer.isVisible()) dim.width += percentRenderer.getPreferredSize().width;
        return sharedDimension(dim);
    }
    
    
    private static ProfilerRenderer createNumberRenderer(Format customFormat) {
        NumberRenderer numberRenderer = new NumberRenderer(customFormat);
        numberRenderer.setMargin(3, 3, 3, 3);
        return numberRenderer;
    }
    
    
    public String toString() {
        if (!percentRenderer.isVisible()) return valueRenderer.toString();
        else return valueRenderer.toString() + " " + percentRenderer.toString(); // NOI18N
    }
    
}
