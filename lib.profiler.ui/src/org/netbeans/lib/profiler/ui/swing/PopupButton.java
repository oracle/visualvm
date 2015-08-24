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

package org.netbeans.lib.profiler.ui.swing;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 */
public class PopupButton extends SmallButton {
    
    private static final Icon DROPDOWN_ICON = Icons.getIcon(GeneralIcons.POPUP_ARROW);
    private static final int DROPDOWN_ICON_WIDTH = DROPDOWN_ICON.getIconWidth();
    private static final int DROPDOWN_ICON_HEIGHT = DROPDOWN_ICON.getIconHeight();
    
    private int iconOffset;
    private int popupAlign = SwingConstants.LEADING;
    
    
    {
        if (UIUtils.isMetalLookAndFeel()) iconOffset = 6;
        else if (UIUtils.isNimbusLookAndFeel()) iconOffset = 8;
        else iconOffset = 7;
        
        setHorizontalAlignment(LEADING);
    }
    
    
    public PopupButton() { super(); }

    public PopupButton(Icon icon) { super(icon); }

    public PopupButton(String text) { super(text); }

    public PopupButton(Action a) { super(a); }

    public PopupButton(String text, Icon icon) { super(text, icon); }
    
    
    public void setPopupAlign(int align) {
        popupAlign = align;
    }
    
    public int getPopupAlign() {
        return popupAlign;
    }
    
    
    protected void fireActionPerformed(ActionEvent e) {
        super.fireActionPerformed(e);
        displayPopup();
    }
    
    protected void displayPopup() {
        JPopupMenu menu = new JPopupMenu();
        populatePopup(menu);
        if (menu.getComponentCount() > 0) {
            Dimension size = menu.getPreferredSize();
            size.width = Math.max(size.width, getWidth());
            menu.setPreferredSize(size);
            int align = getPopupAlign();
            int x = align == SwingConstants.TRAILING ||
                    align == SwingConstants.RIGHT ?
                    getWidth() - size.width : 0;
            menu.show(this, x, getHeight());
        }
    }
    
    protected void populatePopup(JPopupMenu popup) {
        // Implementation here
    }
    
    
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.width += DROPDOWN_ICON_WIDTH + 5;
        return size;
    }
    
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
    
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
    
    public void addNotify() {
        super.addNotify();
        if (UIUtils.isWindowsLookAndFeel() && getParent() instanceof JToolBar) {
            if (getIcon() == NO_ICON) setIconTextGap(2);
            iconOffset = 5;
        }
    }
    
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        DROPDOWN_ICON.paintIcon(this, g, getWidth() - DROPDOWN_ICON_WIDTH - iconOffset,
                                        (getHeight() - DROPDOWN_ICON_HEIGHT) / 2);
    }
    
}
