/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 * Common superclass for custom toolbar implementations.
 * Implements various OS/LaF specific tweaks.
 *
 * @author Jiri Sedlacek
 */
public class GenericToolbar extends JToolBar {
    
    public GenericToolbar() { super(); }
    
    public GenericToolbar(int orientation) { super(orientation); }
    
    public GenericToolbar(String name) { super(name); }
    
    public GenericToolbar(String name, int orientation) { super(name, orientation); }
    
    
    public void addSeparator() {
        if (!UIUtils.isMetalLookAndFeel()) {
            super.addSeparator();
        } else {
            final JSeparator separator = new JSeparator(JSeparator.VERTICAL);
            final int WDTH = separator.getPreferredSize().width;
            final Dimension SIZE = new Dimension(new JToolBar.Separator().getSeparatorSize().width, 12);
            JPanel panel = new JPanel(null) {
                public Dimension getPreferredSize() { return SIZE; }
                public Dimension getMaximumSize() { return SIZE; }
                public Dimension getMinimumSize() { return SIZE; }

                public void doLayout() {
                    int x = (getWidth() - WDTH) / 2;
                    int y = (getHeight()- SIZE.height) / 2;
                    separator.setBounds(x, y, WDTH, SIZE.height);
                }
            };
            panel.setOpaque(false);
            panel.add(separator);
            super.add(panel);
        }
    }
    
    protected void addImpl(Component comp, Object constraints, int index) {
        if (UIUtils.isMetalLookAndFeel()) {
            if (comp instanceof AbstractButton && !(comp instanceof JCheckBox) && !(comp instanceof JRadioButton)) {
                final AbstractButton ab = (AbstractButton) comp;
                ab.setMargin(new Insets(1, 1, 1, 1));
                if (ab.getClientProperty("MetalListener") == null) { // NOI18N
                    final ButtonModel bm = ab.getModel();
                    ChangeListener cl = new ChangeListener() {
                        public void stateChanged(ChangeEvent e) {
                            ab.setBorderPainted(bm.isArmed() || bm.isPressed() || bm.isRollover() || bm.isSelected());
                            ab.setContentAreaFilled(bm.isArmed() || bm.isPressed() || bm.isRollover() || bm.isSelected());
                        }
                    };
                    cl.stateChanged(null); // initialize the appearance tweaks
                    ab.getModel().addChangeListener(cl);
                    ab.putClientProperty("MetalListener", cl); // NOI18N
                }
            }
        }
        super.addImpl(comp, constraints, index);
    }
    
}
