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
