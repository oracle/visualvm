/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 */
package org.graalvm.visualvm.lib.ui.swing;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 * JPopupMenu which supports custom background color.
 *
 * @author Jiri Sedlacek
 */
public class ProfilerPopupMenu extends JPopupMenu {
    
    private boolean forceBackground;
    
    
    public ProfilerPopupMenu() {
        super();
    }

    public ProfilerPopupMenu(String label) {
        super(label);
    }
    
    
    // --- Tweaking UI ---------------------------------------------------------
    
    public JMenuItem add(JMenuItem menuItem) {
        if (forceBackground && !UIUtils.isOracleLookAndFeel()) menuItem.setOpaque(false);
        if (forceBackground && !UIUtils.isNimbusLookAndFeel()) menuItem.setForeground(getForeground());
        return super.add(menuItem);
    }
    
    public void add(Component comp, Object constraints) {
        if (forceBackground && !UIUtils.isOracleLookAndFeel() && comp instanceof JComponent)
            ((JComponent)comp).setOpaque(false);
        if (forceBackground && !UIUtils.isNimbusLookAndFeel()) comp.setForeground(getForeground());
        comp.setMinimumSize(comp.getPreferredSize());
        super.add(comp, constraints);
    }
    
    
    public void setForceBackground(boolean force) {
        if (!UIUtils.isNimbus() || !Boolean.TRUE.equals(UIManager.getBoolean("nb.dark.theme"))) // NOI18N
            this.forceBackground = force;
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (forceBackground) {
            g.setColor(getBackground());
            g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
        }
    }
    
}
