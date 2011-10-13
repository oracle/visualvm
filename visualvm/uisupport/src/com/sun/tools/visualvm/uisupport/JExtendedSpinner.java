/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.uisupport;

import java.awt.Font;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;


/**
 * Copy of org.netbeans.lib.profiler.ui.components.JExtendedSpinner to be used
 * in VisualVM tool an plugins.
 *
 * @author Jiri Sedlacek
 */
public class JExtendedSpinner extends JSpinner {
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
}
