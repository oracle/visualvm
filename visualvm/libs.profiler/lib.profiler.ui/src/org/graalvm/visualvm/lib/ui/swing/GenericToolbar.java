/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 * Common superclass for custom toolbar implementations.
 * Implements various OS/LaF specific tweaks.
 *
 * @author Jiri Sedlacek
 */
public class GenericToolbar extends JToolBar {

    public GenericToolbar() { super(); tweak(); }

    public GenericToolbar(int orientation) { super(orientation); tweak(); }

    public GenericToolbar(String name) { super(name); tweak(); }

    public GenericToolbar(String name, int orientation) { super(name, orientation); tweak(); }


    private void tweak() {
        if (UIUtils.isGTKLookAndFeel() || UIUtils.isNimbusLookAndFeel()) {
            int axis = getOrientation() == VERTICAL ? BoxLayout.PAGE_AXIS :
                                                      BoxLayout.LINE_AXIS;
            setLayout(new BoxLayout(this, axis));
        }

        if (UIUtils.isNimbusLookAndFeel())
            setBorder(BorderFactory.createEmptyBorder(-2, 0, -2, 0));
        else if (UIUtils.isAquaLookAndFeel())
            setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
        
        if (UIUtils.isWindowsClassicLookAndFeel()) setRollover(true);
    }
    
    
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
                final AbstractButton ab = (AbstractButton)comp;
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
        } else if (UIUtils.isNimbusLookAndFeel()) {
            if (comp instanceof AbstractButton && !(comp instanceof JCheckBox) && !(comp instanceof JRadioButton)) {
                AbstractButton ab = (AbstractButton)comp;
                ab.setMargin(new Insets(2, 2, 2, 2));
            }
        } else if (UIUtils.isAquaLookAndFeel()) {
            if (comp instanceof AbstractButton && !(comp instanceof JCheckBox) && !(comp instanceof JRadioButton)) {
                AbstractButton ab = (AbstractButton)comp;
                ab.putClientProperty("JButton.buttonType", "segmentedTextured"); // NOI18N
                if (ab.getClientProperty("JButton.segmentPosition") == null) // NOI18N
                    ab.putClientProperty("JButton.segmentPosition", "only"); // NOI18N
                ab.setMargin(new Insets(-1, -1, -2, -1));
            }
        } else if (UIUtils.isWindowsClassicLookAndFeel()) {
            if (comp instanceof AbstractButton && !(comp instanceof JCheckBox) && !(comp instanceof JRadioButton)) {
                AbstractButton ab = (AbstractButton)comp;
                ab.setMargin(new Insets(1, 1, 1, 1));
            }
        }
        
        if (comp instanceof JButton) UIUtils.fixButtonUI((JButton) comp);
        
        super.addImpl(comp, constraints, index);
    }
    
    
    protected void paintComponent(Graphics g) {
        if (UIUtils.isGTKLookAndFeel() && getClientProperty("Toolbar.noGTKBorder") == Boolean.TRUE) return; // NOI18N
        super.paintComponent(g);
    }
    
    
    private static int PREFERRED_HEIGHT = -1;
    
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        if (PREFERRED_HEIGHT == -1) {
            GenericToolbar tb = new GenericToolbar();
            tb.setBorder(getBorder());
            tb.setBorderPainted(isBorderPainted());
            tb.setRollover(isRollover());
            tb.setFloatable(isFloatable());
            Icon icon = Icons.getIcon(GeneralIcons.SAVE);
            tb.add(new JButton("Button", icon)); // NOI18N
            tb.add(new JToggleButton("Button", icon)); // NOI18N
            tb.add(new JTextField("Text")); // NOI18N
            JComboBox c = new JComboBox();
            c.setEditor(new BasicComboBoxEditor());
            c.setRenderer(new BasicComboBoxRenderer());
            tb.add(c);
            tb.addSeparator();
            PREFERRED_HEIGHT = tb.getSuperPreferredSize().height;
        }
        dim.height = getParent() instanceof JToolBar ? 1 :
                     Math.max(dim.height, PREFERRED_HEIGHT);
        return dim;
    }
    
    private Dimension getSuperPreferredSize() {
        return super.getPreferredSize();
    }
    
    
    public void doLayout() {
        // #216443 - disabled/invisible/JLabel toolbar components
        //           break left/right arrow focus traversal
        for (Component component : getComponents())
            component.setFocusable(isFocusableComponent(component));
        super.doLayout();
    }
    
    protected boolean isFocusableComponent(Component component) {
        if (!component.isVisible()) return false;
//            if (!component.isEnabled()) return false;
        if (component instanceof JLabel) return false;
        if (component instanceof JPanel) return false;
        if (component instanceof JSeparator) return false;
        if (component instanceof JToolBar) return false;
        if (component instanceof Box.Filler) return false;
        return true;
    }
    
}
