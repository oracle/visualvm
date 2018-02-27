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

import java.awt.Component;
import java.text.NumberFormat;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class CustomComboRenderer implements ListCellRenderer {

    private final JComboBox combo;
    private final ListCellRenderer renderer;
    private final JLabel rendererL;


    private CustomComboRenderer(JComboBox combo) {
        this.combo = combo;
        renderer = combo.getRenderer();
        if (renderer instanceof JLabel) rendererL = (JLabel)renderer;
        else rendererL = null;

        this.combo.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)   { repaint(); }
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { repaint(); }
            public void popupMenuCanceled(PopupMenuEvent e)            { repaint(); }
            private void repaint() { CustomComboRenderer.this.combo.repaint(); }
        });
    }


    protected void setupRenderer(ListCellRenderer renderer, boolean popupVisible) {}

    protected void setupRenderer(JLabel renderer, boolean popupVisible) {}

    public abstract java.lang.String value(Object value);


    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected,
                                                  boolean cellHasFocus) {

        if (rendererL != null) setupRenderer(rendererL, combo.isPopupVisible());
        else setupRenderer(renderer, combo.isPopupVisible());
        
        return renderer.getListCellRendererComponent(list, value(value), index,
                                                     isSelected, cellHasFocus);
    }


    public static final class String extends CustomComboRenderer {

        public String(JComboBox combo) {
            super(combo);
        }

        public java.lang.String value(Object value) {
            return value == null ? "null" : value.toString(); // NOI18N
        }

    }


    public static final class Boolean extends CustomComboRenderer {

        public Boolean(JComboBox combo) {
            super(combo);
        }

        public java.lang.String value(Object value) {
            if (java.lang.Boolean.TRUE.equals(value)) return "enabled";
            if (java.lang.Boolean.FALSE.equals(value)) return "disabled";
            return "default";
        }

    }


    public static final class Number extends CustomComboRenderer {

        private final java.lang.String units;
        private final boolean lAlign;

        public Number(JComboBox combo, java.lang.String units, boolean lAlign) {
            super(combo);
            this.units = units;
            this.lAlign = lAlign;
        }

        protected void setupRenderer(JLabel renderer, boolean popupVisible) {
            if (popupVisible || !lAlign) renderer.setHorizontalAlignment(SwingConstants.TRAILING);
            else renderer.setHorizontalAlignment(SwingConstants.LEADING);
        }

        public java.lang.String value(Object value) {
            java.lang.String sunits = units == null ? "" : " " + units;
            return Integer.valueOf(-1).equals(value) ? "default" :
                   NumberFormat.getInstance().format(value) + sunits;
        }

    }

}
