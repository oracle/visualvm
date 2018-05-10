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

import java.awt.Dimension;
import java.lang.reflect.Method;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.plaf.SeparatorUI;

/**
 * JSeparator applying a workaround for bad separator appearance on Mac OS X
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


    public Separator() {
        super();
    }

    public Separator(int orientation) {
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
