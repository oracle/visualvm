/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.ui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.ui.UIUtils;


/**
 * JSpinner with some bugfixes
 *
 * - setModel doesn't break font
 * - setModel doesn't break accessible name and description
 * - can propagate accessible name and description to its editor (JFormattedTextField)
 * - doesn't consume ESC key
 *
 * @author Jiri Sedlacek
 */
public class JExtendedSpinner extends JSpinner {

    private static int defaultSpinnerHeight = -1;

    public static int getDefaultSpinnerHeight() {
        if (defaultSpinnerHeight == -1) {
            defaultSpinnerHeight = new JTextField().getPreferredSize().height;
        }

        return defaultSpinnerHeight;
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JExtendedSpinner() {
        super();
        ((JSpinner.DefaultEditor) getEditor()).getTextField().setFont(UIManager.getFont("Label.font")); // NOI18N
        ((JSpinner.DefaultEditor) getEditor()).getTextField().addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(final java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        processKeyEvent(e);
                    }
                }
            });
        configureWheelListener();
    }

    public JExtendedSpinner(SpinnerModel model) {
        super(model);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().setFont(UIManager.getFont("Label.font")); // NOI18N
        ((JSpinner.DefaultEditor) getEditor()).getTextField().addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(final java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        processKeyEvent(e);
                    }
                }
            });
        configureWheelListener();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setModel(SpinnerModel model) {
        Font font = ((JSpinner.DefaultEditor) getEditor()).getTextField().getFont();
        String accessibleName = ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext().getAccessibleName();
        String accessibleDescription = ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
                                        .getAccessibleDescription();
        super.setModel(model);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().setFont(font);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext().setAccessibleName(accessibleName);
        ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
         .setAccessibleDescription(accessibleDescription);
    }

    public void fixAccessibility() {
        if (getAccessibleContext() != null) {
            ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
             .setAccessibleName(getAccessibleContext().getAccessibleName());
            ((JSpinner.DefaultEditor) getEditor()).getTextField().getAccessibleContext()
             .setAccessibleDescription(getAccessibleContext().getAccessibleDescription());
        }
    }
    
    
    public Dimension getPreferredSize() {
        if (UIUtils.isWindowsClassicLookAndFeel()) {
            return new Dimension(super.getPreferredSize().width, getDefaultSpinnerHeight());
        } else {
            return super.getPreferredSize();
        }
    }

    public Dimension getMinimumSize() {
        if (UIUtils.isWindowsClassicLookAndFeel()) {
            return getPreferredSize();
        } else {
            return super.getMinimumSize();
        }
    }
    
    
    private void configureWheelListener() {
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) return;
                Object newValue = (e.getWheelRotation() < 0 ?
                                   JExtendedSpinner.this.getNextValue() :
                                   JExtendedSpinner.this.getPreviousValue());
                if (newValue != null) JExtendedSpinner.this.setValue(newValue);
            }
        });
    }
}
