/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.ui.stp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;


/**
 *
 * @author Jiri Sedlacek
 */
public class WelcomePanel extends JPanel {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public WelcomePanel() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Private implementation ------------------------------------------------
    private void initComponents() {
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(SelectProfilingTask.BACKGROUND_COLOR);

        GridBagConstraints constraints;

        //    JLabel label = new JLabel("Attach Profiler");
        //    label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 3));
        //    label.setForeground(new Color(198, 129, 0));
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 0;
        //    constraints.gridy = 0;
        //    constraints.gridwidth = GridBagConstraints.REMAINDER;
        //    constraints.fill = GridBagConstraints.HORIZONTAL;
        //    constraints.anchor = GridBagConstraints.WEST;
        //    constraints.insets = new Insets(27, 30, 12, 20);
        //    add(label, constraints);
        //    
        //    JSeparator separator1 = Utils.createHorizontalSeparator();
        //    separator1.setBackground(getBackground());
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 0;
        //    constraints.gridy = 1;
        //    constraints.gridwidth = GridBagConstraints.REMAINDER;
        //    constraints.fill = GridBagConstraints.HORIZONTAL;
        //    constraints.anchor = GridBagConstraints.WEST;
        //    constraints.insets = new Insets(0, 20, 0, 20);
        //    add(separator1, constraints);
        //    
        //    JLabel tbdLabel = new JLabel("Select project to attach to...");
        //    tbdLabel.setForeground(Color.GRAY);
        //    tbdLabel.setFont(tbdLabel.getFont().deriveFont(Font.BOLD, tbdLabel.getFont().getSize2D() + 5));
        //    separator1.setBackground(getBackground());
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 0;
        //    constraints.gridy = 2;
        //    constraints.gridwidth = GridBagConstraints.REMAINDER;
        //    constraints.fill = GridBagConstraints.NONE;
        //    constraints.anchor = GridBagConstraints.CENTER;
        //    constraints.insets = new Insets(100, 0, 0, 0);
        //    add(tbdLabel, constraints);
        //    
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 0;
        //    constraints.gridy = 3;
        //    constraints.weightx = 1;
        //    constraints.weighty = 1;
        //    constraints.gridwidth = GridBagConstraints.REMAINDER;
        //    constraints.fill = GridBagConstraints.BOTH;
        //    constraints.anchor = GridBagConstraints.NORTHWEST;
        //    constraints.insets = new Insets(10, 20, 10, 20);
        //    add(Utils.createFillerPanel(), constraints);

        //    JScrollPane contentsScroll = new JScrollPane(contentsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //    contentsScroll.setBorder(BorderFactory.createEmptyBorder());
        //    contentsScroll.setOpaque(false);
        //    contentsScroll.getViewport().setOpaque(false);
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 0;
        //    constraints.gridy = 2;
        //    constraints.weightx = 1;
        //    constraints.weighty = 1;
        //    constraints.gridwidth = GridBagConstraints.REMAINDER;
        //    constraints.fill = GridBagConstraints.BOTH;
        //    constraints.anchor = GridBagConstraints.NORTHWEST;
        //    constraints.insets = new Insets(10, 20, 10, 20);
        //    add(contentsScroll, constraints);
        //    
        //    JSeparator separator2 = Utils.createHorizontalSeparator();
        //    separator2.setBackground(getBackground());
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 0;
        //    constraints.gridy = 3;
        //    constraints.gridwidth = GridBagConstraints.REMAINDER;
        //    constraints.fill = GridBagConstraints.HORIZONTAL;
        //    constraints.anchor = GridBagConstraints.WEST;
        //    constraints.insets = new Insets(0, 20, 0, 20);
        //    add(separator2, constraints);
        //    
        //    JLabel duplicate = new JLabel("Edit task");
        //    duplicate.setFont(duplicate.getFont().deriveFont(duplicate.getFont().getSize2D() - 1));
        //    duplicate.setForeground(Color.DARK_GRAY);
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 0;
        //    constraints.gridy = 4;
        //    constraints.gridwidth = 1;
        //    constraints.fill = GridBagConstraints.NONE;
        //    constraints.anchor = GridBagConstraints.WEST;
        //    constraints.insets = new Insets(3, 22, 4, 0);
        //    add(duplicate, constraints);
        //    
        ////    JLabel delete = new JLabel("Delete");
        ////    delete.setFont(delete.getFont().deriveFont(delete.getFont().getSize2D() - 1));
        ////    delete.setForeground(Color.DARK_GRAY);
        ////    constraints = new GridBagConstraints();
        ////    constraints.gridx = 1;
        ////    constraints.gridy = 4;
        ////    constraints.gridwidth = 1;
        ////    constraints.fill = GridBagConstraints.NONE;
        ////    constraints.anchor = GridBagConstraints.WEST;
        ////    constraints.insets = new Insets(3, 9, 4, 0);
        ////    add(delete, constraints);
        //    
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 2;
        //    constraints.gridy = 4;
        //    constraints.weightx = 1;
        //    constraints.gridwidth = 1;
        //    constraints.fill = GridBagConstraints.HORIZONTAL;
        //    constraints.anchor = GridBagConstraints.WEST;
        //    constraints.insets = new Insets(3, 0, 4, 0);
        //    add(Utils.createFillerPanel(), constraints);
        //    
        //    JLabel advanced = new JLabel("Advanced");
        //    advanced.setFont(advanced.getFont().deriveFont(advanced.getFont().getSize2D() - 1));
        //    advanced.setForeground(Color.DARK_GRAY);
        //    constraints = new GridBagConstraints();
        //    constraints.gridx = 3;
        //    constraints.gridy = 4;
        //    constraints.gridwidth = 1;
        //    constraints.fill = GridBagConstraints.NONE;
        //    constraints.anchor = GridBagConstraints.WEST;
        //    constraints.insets = new Insets(3, 0, 4, 22);
        //    add(advanced, constraints);
        //    
        //    duplicate.addMouseListener(new MouseAdapter() {
        //      public void mouseEntered(MouseEvent e) {
        //        e.getComponent().setForeground(Color.BLACK);
        //        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        //      }
        //      public void mouseExited(MouseEvent e)  {
        //        e.getComponent().setForeground(Color.DARK_GRAY);
        //        setCursor(Cursor.getDefaultCursor());
        //      }
        //    });
        //    
        ////    delete.addMouseListener(new MouseAdapter() {
        ////      public void mouseEntered(MouseEvent e) {
        ////        e.getComponent().setForeground(Color.BLACK);
        ////        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ////      }
        ////      public void mouseExited(MouseEvent e)  {
        ////        e.getComponent().setForeground(Color.DARK_GRAY);
        ////        setCursor(Cursor.getDefaultCursor());
        ////      }
        ////    });
        //    
        //    advanced.addMouseListener(new MouseAdapter() {
        //      public void mouseEntered(MouseEvent e) {
        //        e.getComponent().setForeground(Color.BLACK);
        //        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        //      }
        //      public void mouseExited(MouseEvent e)  {
        //        e.getComponent().setForeground(Color.DARK_GRAY);
        //        setCursor(Cursor.getDefaultCursor());
        //      }
        //    });
        //    
        ////    JScrollPane contentsScroll = new JScrollPane(contentsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        ////    contentsScroll.setBorder(BorderFactory.createEmptyBorder());
        ////    contentsScroll.setOpaque(false);
        ////
        ////    setLayout(new BorderLayout());
        ////    add(contentsScroll, BorderLayout.CENTER);
    }
}
