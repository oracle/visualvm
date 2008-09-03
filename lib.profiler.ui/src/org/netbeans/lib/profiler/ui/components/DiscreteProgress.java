/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class DiscreteProgress extends JPanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Color disabledColor = new Color(220, 220, 220);
    private Color enabledColor = new Color(128, 128, 255);
    private int activeUnits = 0;
    private int totalUnits = 10;
    private int unitHeight = 13;
    private int unitWidth = 10;
    
    private JProgressBar progressDelegate;
    private DefaultBoundedRangeModel progressDelegateModel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DiscreteProgress() {
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setActiveUnits(int activeUnits) {
        if (progressDelegateModel != null) {
            this.activeUnits = activeUnits;
            progressDelegateModel.setValue(activeUnits);
        } else if (this.activeUnits != activeUnits) {
            this.activeUnits = activeUnits;
            repaint();
        }
    }

    public int getActiveUnits() {
        return activeUnits;
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public Dimension getPreferredSize() {
        return new Dimension(((totalUnits * unitWidth) + totalUnits) - 1 + 4, unitHeight + 4);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        ;

        DiscreteProgress progress = new DiscreteProgress();

        JFrame testFrame = new JFrame("Decimal Progress Test Frame"); // NOI18N
        testFrame.getContentPane().add(progress);
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.pack();
        testFrame.setVisible(true);
    }

    public void paintComponent(Graphics g) {
        if (progressDelegate == null) {
            Insets insets = getInsets();
            int offsetX = insets.left;
            int offsetY = insets.top;

            for (int i = 0; i < totalUnits; i++) {
                g.setColor((i < activeUnits) ? enabledColor : disabledColor);
                g.fillRect(offsetX + (i * unitWidth) + i, offsetY, unitWidth, unitHeight);
            }
        } else {
            super.paintComponent(g);
        }
    }
    
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
//        if (UIUtils.isNimbus()) {
            progressDelegateModel = new DefaultBoundedRangeModel(4, 1, 0, 10);
            progressDelegate = new JProgressBar(progressDelegateModel);
            add(progressDelegate, BorderLayout.CENTER);
//        } else {
//            setBorder(new ThinBevelBorder(BevelBorder.LOWERED));
//            setOpaque(false);
//        }
    }
}
