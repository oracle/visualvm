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

package org.netbeans.modules.profiler.v2.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class TitledMenuSeparator extends JPanel {
    
    public TitledMenuSeparator(String text) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel l = new JLabel(text);
        l.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        if (UIUtils.isWindowsLookAndFeel()) l.setOpaque(true);
        l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() - 1));
        if (UIUtils.isWindowsLookAndFeel()) l.setForeground(UIUtils.getDisabledLineColor());
        
        add(l, BorderLayout.WEST);
        
        if (UIUtils.isGTKLookAndFeel()) {
            add(UIUtils.createHorizontalSeparator(), BorderLayout.CENTER);
        } else {
            JComponent sep = new JPopupMenu.Separator();
            add(sep, BorderLayout.CENTER);
            
            if (UIUtils.isOracleLookAndFeel()) {
                setOpaque(true);
                setBackground(sep.getBackground());
                l.setForeground(sep.getForeground());
            }
        }
    }
    
    public void doLayout() {
        super.doLayout();
        Component c = getComponent(1);
        
        int h = c.getPreferredSize().height;
        Rectangle b = c.getBounds();
        
        b.y = (b.height - h) / 2;
        b.height = h;
        c.setBounds(b);
    }

    public Dimension getPreferredSize() {
        Dimension d = getComponent(0).getPreferredSize();
        d.width += 25;
        return d;
    }

}
