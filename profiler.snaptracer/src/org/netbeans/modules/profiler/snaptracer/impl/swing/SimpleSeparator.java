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
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SimpleSeparator extends JPanel implements SwingConstants {

    private final int orientation;
    private final Dimension preferredSize = new Dimension(1, 1);


    public SimpleSeparator() {
        this(HORIZONTAL);
    }

    public SimpleSeparator(int orientation) {
        super(null);
        this.orientation = orientation;
    }


    public void setPreferredSize(Dimension preferredSize) {
        this.preferredSize.width = preferredSize.width;
        this.preferredSize.height = preferredSize.height;
    }


    public Dimension getMinimumSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            return new Dimension(insets.left + insets.right,
                                 insets.top + insets.bottom + 1);
        else
            return new Dimension(insets.left + insets.right + 1,
                                 insets.top + insets.bottom);
    }

    public Dimension getMaximumSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            return new Dimension(Integer.MAX_VALUE,
                                 insets.top + insets.bottom + 1);
        else
            return new Dimension(insets.left + insets.right + 1,
                                 Integer.MAX_VALUE);
    }

    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            return new Dimension(Math.max(insets.left + insets.right, preferredSize.width),
                                 insets.top + insets.bottom + 1);
        else
            return new Dimension(insets.left + insets.right + 1,
                                 Math.max(insets.top + insets.bottom, preferredSize.height));
    }


    public void paint(Graphics g) {
        g.setColor(new Color(192, 192, 192));
        Insets insets = getInsets();
        if (orientation == HORIZONTAL)
            g.drawLine(insets.left, insets.top, getWidth() - insets.right, insets.top);
        else
            g.drawLine(insets.left, insets.top, insets.left, getHeight() - insets.bottom);
    }

}
