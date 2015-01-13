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
import java.awt.Font;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class NormalBoldGrayRenderer extends MultiRenderer {
    
    private final LabelRenderer normalRenderer;
    private final LabelRenderer boldRenderer;
    private final LabelRenderer grayRenderer;
    
    private final ProfilerRenderer[] renderers;
    
    
    public NormalBoldGrayRenderer() {
        normalRenderer = new LabelRenderer(true);
        normalRenderer.setMargin(3, 3, 3, 0);
        
        boldRenderer = new LabelRenderer(true);
        boldRenderer.setMargin(3, 0, 3, 0);
        Font font = boldRenderer.getFont();
        boldRenderer.setFont(font.deriveFont(Font.BOLD));
        
        grayRenderer = new LabelRenderer(true) {
            public void setForeground(Color foreground) {
                if (foreground == null) foreground = Color.BLACK;
                super.setForeground(UIUtils.getDisabledForeground(foreground));
            }
        };
        grayRenderer.setMargin(3, 0, 3, 3);
        
        renderers = new ProfilerRenderer[] { normalRenderer, boldRenderer, grayRenderer };
        
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.LEADING);
    }

    
    protected ProfilerRenderer[] valueRenderers() {
        return renderers;
    }
    
    
    protected void setNormalValue(String value) {
        normalRenderer.setText(value);
    }
    
    protected final String getNormalValue() {
        return normalRenderer.getText();
    }
    
    protected void setBoldValue(String value) {
        boldRenderer.setText(value);
    }
    
    protected final String getBoldValue() {
        return boldRenderer.getText();
    }
    
    protected void setGrayValue(String value) {
        grayRenderer.setText(value);
    }
    
    protected final String getGrayValue() {
        return grayRenderer.getText();
    }
    
    // Invoke after values are set!
    protected void setIcon(Icon icon) {
        String text = normalRenderer.getText();
        if (text == null || text.isEmpty()) {
            normalRenderer.setIcon(null);
            text = boldRenderer.getText();
            if (text == null || text.isEmpty()) {
                boldRenderer.setIcon(null);
                grayRenderer.setIcon(icon);
            } else {
                boldRenderer.setIcon(icon);
                grayRenderer.setIcon(null);
            }
        } else {
            normalRenderer.setIcon(icon);
            boldRenderer.setIcon(null);
            grayRenderer.setIcon(null);
        }
    }
    
    
    public String toString() {
        return normalRenderer.toString() + boldRenderer.toString() + grayRenderer.toString();
    }
    
}
