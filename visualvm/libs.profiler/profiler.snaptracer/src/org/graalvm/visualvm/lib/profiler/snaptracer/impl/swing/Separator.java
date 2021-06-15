/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.swing;

import java.awt.Dimension;
import java.lang.reflect.Method;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.plaf.SeparatorUI;

/**
 * JSeparator applying a workaround for bad separator appearance on macOS
 * broken by the NetBeans Platform.
 *
 * @author Jiri Sedlacek
 */
final class Separator extends JSeparator {

    private static final String SEPARATOR_UI = "SeparatorUI"; // NOI18N
    private static final String MAC_OS_X_SEPARATOR_UI =
            "com.apple.laf.AquaPopupMenuSeparatorUI"; // NOI18N
    private static final String MAC_OS_X_SEPARATOR_UI_NB =
            "org.netbeans.swing.plaf.aqua.AquaSeparatorUI"; // NOI18N
    private static Class<SeparatorUI> MAC_OS_X_SEPARATOR_UI_CLASS;
    private static final String MAC_OS_X_SEPARATOR_COLOR_KEY =
            "InternalFrame.inactiveTitleForeground"; // NOI18N

    private boolean separatorUIInitialized = false;
    private SeparatorUI macOsXSeparatorUI;


    static {
        if (MAC_OS_X_SEPARATOR_UI_NB.
                equals(UIManager.getDefaults().get(SEPARATOR_UI))) {
            try {
                MAC_OS_X_SEPARATOR_UI_CLASS =
                        (Class<SeparatorUI>)Class.forName(MAC_OS_X_SEPARATOR_UI);
            } catch (Throwable e) {
                MAC_OS_X_SEPARATOR_UI_CLASS = null;
            }
        }
    }


    Separator() {
        super();
    }

    Separator(int orientation) {
        super(orientation);
    }


    public void setUI(SeparatorUI ui) {
        synchronized(this) {
            if (!separatorUIInitialized) {
                macOsXSeparatorUI = createCustomUI(this);
                separatorUIInitialized = true;
            }
        }

        if (macOsXSeparatorUI == null) {
            super.setUI(ui);
        } else {
            super.setUI(macOsXSeparatorUI);
            setForeground(UIManager.getColor(MAC_OS_X_SEPARATOR_COLOR_KEY));
        }
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }


    private static SeparatorUI createCustomUI(JComponent separator) {
        if (MAC_OS_X_SEPARATOR_UI_CLASS != null) {
            try {
                Method m = MAC_OS_X_SEPARATOR_UI_CLASS.getDeclaredMethod(
                           "createUI", JComponent.class); // NOI18N
                return (SeparatorUI)m.invoke(null, separator);
            } catch (Throwable e) {
                return null;
            }
        } else {
            return null;
        }
    }

}
