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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicButtonUI;
import org.netbeans.lib.profiler.charts.swing.Utils;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeaderButton extends HeaderPanel {

    private static final HeaderButtonUI UI = new HeaderButtonUI();

    private final JButton button;


    public HeaderButton(String text, Icon icon) {
        JPanel panel = super.getClientContainer();
        panel.setLayout(new BorderLayout());
        button = new JButton(text, icon) {
            protected void processMouseEvent(MouseEvent e) {
                super.processMouseEvent(e);
                if (!isEnabled()) return;
                HeaderButton.this.processMouseEvent(e);
            }
            protected void fireActionPerformed(ActionEvent e) {
                performAction(e);
            }
        };
        panel.add(button, BorderLayout.CENTER);

        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setUI(UI);
    }

    public void setToolTipText(String text) {
        button.setToolTipText(text);
    }

    public void setEnabled(boolean enabled) {
        button.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    public void reset() {
        processMouseEvent(new MouseEvent(this, MouseEvent.MOUSE_EXITED,
                          System.currentTimeMillis(), 0, -1, -1, 0, false));
    }

    protected boolean processMouseEvents() { return true; }

    protected void performAction(ActionEvent e) {}
    
    public void setUI(ButtonUI ui) { if (ui == UI) super.setUI(ui); }


    private static class HeaderButtonUI extends BasicButtonUI {

        private static final Color FOCUS_COLOR = Color.BLACK;
        private static final Stroke FOCUS_STROKE =
                new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL,
                                0, new float[] {0, 2}, 0);
        private static final Color PRESSED_FOREGROUND =
                Utils.checkedColor(new Color(100, 100, 100, 70));

        protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect,
                                  Rectangle textRect, Rectangle iconRect) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setStroke(FOCUS_STROKE);
            g2.setColor(FOCUS_COLOR);
            g2.drawRect(2, 2, b.getWidth() - 5, b.getHeight() - 5);
        }

        protected void paintButtonPressed(Graphics g, AbstractButton b) {
            g.setColor(PRESSED_FOREGROUND);
            g.fillRect(0, 0, b.getWidth(), b.getHeight());
        }

    }

}
