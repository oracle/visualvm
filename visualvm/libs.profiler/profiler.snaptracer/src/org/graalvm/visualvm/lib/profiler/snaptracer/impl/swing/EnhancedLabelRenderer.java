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

package org.netbeans.modules.profiler.snaptracer.impl.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.Border;

/**
 *
 * @author Jiri Sedlacek
 */
public final class EnhancedLabelRenderer extends LabelRenderer {

    private static final EnhancedInsets EMPTY_INSETS = new EnhancedInsets();

    private EnhancedInsets marginInsets;
    private EnhancedInsets borderInsets;
    private Border border;
    private Color background;


    public void setMargin(Insets marginInsets) {
        if (marginInsets == null) this.marginInsets = EMPTY_INSETS;
        else this.marginInsets = new EnhancedInsets(marginInsets);
    }

    // Overridden for performance reasons.
    public void setBorder(Border border) {
        this.border = border;
        if (border == null) borderInsets = EMPTY_INSETS;
        else borderInsets = new EnhancedInsets(border.getBorderInsets(this));
    }

    // Overridden for performance reasons.
    public Border getBorder() {
        return border;
    }

    // Overridden for performance reasons.
    public void setBackground(Color background) {
        this.background = background;
    }

    // Overridden for performance reasons.
    public Color getBackground() {
        return background;
    }

    private EnhancedInsets getMarginInsets() {
        if (marginInsets == null) marginInsets = EMPTY_INSETS;
        return marginInsets;
    }

    private EnhancedInsets getBorderInsets() {
        if (borderInsets == null) borderInsets = EMPTY_INSETS;
        return borderInsets;
    }


    protected void prePaint(Graphics g, int x, int y) {
        if (background != null) {
            g.setColor(background);
            EnhancedInsets margin = getMarginInsets();
            Dimension size = getPreferredSize();
            g.fillRect(x - margin.left,
                       y - margin.top,
                       size.width + margin.width(),
                       size.height + margin.height());
        }
    }

    protected void postPaint(Graphics g, int x, int y) {
        if (border != null) {
            EnhancedInsets bi = getBorderInsets();
            EnhancedInsets margin = getMarginInsets();
            Dimension size = getPreferredSize();
            border.paintBorder(this, g,
                               x - margin.left - bi.left,
                               y - margin.top - bi.top,
                               size.width + margin.width() + bi.width(),
                               size.height + margin.height() + bi.height());
        }
    }


    private static class EnhancedInsets extends Insets {

        public EnhancedInsets() {
            this(0, 0, 0, 0);
        }
        
        public EnhancedInsets(Insets insets) {
            this(insets.top, insets.left, insets.bottom, insets.right);
        }

        public EnhancedInsets(int top, int left, int bottom, int right) {
            super(top, left, bottom, right);
        }


        public int width() {
            return left + right;
        }

        public int height() {
            return top + bottom;
        }

    }

}
