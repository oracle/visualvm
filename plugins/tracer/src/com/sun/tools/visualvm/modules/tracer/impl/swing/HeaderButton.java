/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

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
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 *
 * @author Jiri Sedlacek
 */
public class HeaderButton extends HeaderPanel {

    private static final HeaderButtonUI UI = new HeaderButtonUI();

    private final JButton button;


    public HeaderButton(String text) {
        JPanel panel = super.getClientContainer();
        panel.setLayout(new BorderLayout());
        button = new JButton(text) {
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

    protected boolean processMouseEvents() { return true; }

    protected void performAction(ActionEvent e) {}
    
    public void setUI(ButtonUI ui) { if (ui == UI) super.setUI(ui); }


    private static class HeaderButtonUI extends BasicButtonUI {

        private static final Color FOCUS_COLOR = Color.BLACK;
        private static final Stroke FOCUS_STROKE =
                new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL,
                                0, new float[] {0, 2}, 0);

        protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
            Graphics2D g2 = (Graphics2D)g;
            g2.setStroke(FOCUS_STROKE);
            g2.setColor(FOCUS_COLOR);
            g2.drawRect(2, 2, b.getWidth() - 5, b.getHeight() - 5);
        }

        protected void paintButtonPressed(Graphics g, AbstractButton b) {
            g.setColor(new Color(100, 100, 100, 70));
            g.fillRect(0, 0, b.getWidth(), b.getHeight());
        }

    }

}
