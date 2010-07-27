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

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public final class HeaderLabel extends HeaderPanel {

    public static final int DEFAULT_HEIGHT = computeHeight();


    private String text;
    private int hAlign = SwingConstants.CENTER;


    public HeaderLabel() {
        this(""); // NOI18N
    }

    public HeaderLabel(String text) {
        this.text = text;
    }


    public final void setText(String text) {
        this.text = text;
        repaint();
    }

    public final String getText() {
        return text;
    }

    public final void setHorizontalAlignment(int align) {
        hAlign = align;
        repaint();
    }

    public final int getHorizontalAlignment() {
        return hAlign;
    }


    protected Object getRendererValue() {
        return getText();
    }


    protected void setupRenderer(Component renderer) {
        if (renderer instanceof JLabel) {
            JLabel label = (JLabel)renderer;
            label.setHorizontalAlignment(hAlign);
        }
    }


    public Dimension getPreferredSize() {
        Dimension dim = getPreferredSizeSuper();
        dim.height = DEFAULT_HEIGHT;
        return dim;
    }

    private Dimension getPreferredSizeSuper() {
        return super.getPreferredSize();
    }


    private static int computeHeight() {
        int height = new HeaderLabel("X").getPreferredSizeSuper().height; // NOI18N
        if (UIUtils.isMetalLookAndFeel()) height += 4;
//        else if (UISupport.isAquaLookAndFeel()) height += 6;
        return height;
    }

}
