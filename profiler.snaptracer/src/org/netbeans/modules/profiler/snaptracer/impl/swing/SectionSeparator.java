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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Separator-like component to be used instead of TitledBorder to keep the UI
 * lightweight. Use UISupport.createSectionSeparator() method instead of instantiating
 * this class directly if creating sections for the Options panel.
 *
 * @author Jiri Sedlacek
 */
public final class SectionSeparator extends JPanel {

    /**
     * Creates new instance of SectionSeparator. Uses bold font by default.
     *
     * @param text separator text
     */
    public SectionSeparator(String text) {
        this(text, null);
    }

    /**
     * Creates new instance of SectionSeparator. Uses the provided font or default
     * font if no font is provided.
     *
     * @param text separator text
     * @param font font for the caption text or null for default font
     */
    public SectionSeparator(String text, Font font) {
        if (text == null) throw new IllegalArgumentException("Text cannot be null"); // NOI18N
        initComponents(text, font);
    }

    public void setForeground(Color foreground) {
        if (label == null) super.setForeground(foreground);
        else label.setForeground(foreground);
    }

    public Color getForeground() {
        if (label == null) return super.getForeground();
        else return label.getForeground();
    }

    public void setFont(Font font) {
        if (label == null) super.setFont(font);
        else label.setFont(font);
    }

    public Font getFont() {
        if (label == null) return super.getFont();
        else return label.getFont();
    }

    private void initComponents(String text, Font font) {
        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new GridBagLayout());
        setOpaque(false);

        label = new JLabel(text);
        label.setForeground(getForeground());
        if (font != null) label.setFont(font);
        else label.setFont(label.getFont().deriveFont(Font.BOLD));
        GridBagConstraints c1 = new GridBagConstraints();
        c1.weighty = 1d;
        add(label, c1);

        GridBagConstraints c2 = new GridBagConstraints();
        c2.weightx = 1d;
        c2.fill = GridBagConstraints.HORIZONTAL;
        c2.insets = new Insets(0, 4, 0, 0);
        add(new Separator(), c2);
    }

    private JLabel label;

}
