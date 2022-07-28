/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.uisupport;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * 1.3.8 - added initial support for vertical toolbar, not fully implemented
 *
 * @author Jiri Sedlacek
 */
public final class TransparentToolBar extends JPanel {

    private static Boolean NEEDS_PANEL;
    
    private static int PREFERRED_HEIGHT = -1;
    private static int BUTTON_HEIGHT = -1;
    
    private final JToolBar toolbar;
    private final ItemListener listener = new ItemListener();
    
    private final boolean horizontal;

    
    public TransparentToolBar() {
        this(true);
    }
    
    public TransparentToolBar(boolean horizontal) {
        this.horizontal = horizontal;
        
        setOpaque(false);
        setBorder(createToolBarBorder(horizontal));
        
        if (needsPanel()) {
            // Toolbar is a JPanel (GTK)
            toolbar = null;
            setLayout(new BoxLayout(this, horizontal ? BoxLayout.X_AXIS :
                                                       BoxLayout.Y_AXIS));
        } else {
            // Toolbar is a JToolBar (default)
            toolbar = createToolBar(horizontal);
            toolbar.setBorder(BorderFactory.createEmptyBorder());
            setLayout(new BorderLayout());
            add(toolbar, BorderLayout.CENTER);
        }
        
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) {
                        removeHierarchyListener(this);
                        for (Component c : getComponents()) {
                            if (c instanceof AbstractButton) {
                                listener.refresh((AbstractButton)c);
                            }
                        }
                    }
                }
            }
        });
    }
    
    public Dimension getPreferredSize() {
        Dimension dim = getPreferredSizeSuper();
        if (horizontal) {
            if (PREFERRED_HEIGHT == -1) {
                TransparentToolBar tb = new TransparentToolBar();
                Icon icon = new Icon() {
                    public int getIconWidth() { return 16; }
                    public int getIconHeight() { return 16; }
                    public void paintIcon(Component c, Graphics g, int x, int y) {}
                };
                JButton b = new JButton("Button", icon); // NOI18N
                tb.addItem(b);
                JToggleButton t = new JToggleButton("Button", icon); // NOI18N
                tb.addItem(t);
                JComboBox<?> c = new JComboBox<>();
                c.setEditor(new BasicComboBoxEditor());
                c.setRenderer(new BasicComboBoxRenderer());
                tb.addItem(c);
                tb.addSeparator();
                PREFERRED_HEIGHT = tb.getPreferredSizeSuper().height;
            }
            dim.height = Math.max(dim.height, PREFERRED_HEIGHT);
        }
        return dim;
    }
    
    private Dimension getPreferredSizeSuper() {
        return super.getPreferredSize();
    }
    
    public Component addItem(Action action) {
        return addItem(createActionComponent(action));
    }
    
    public Component addItem(Component c) {
        return addItem(c, -1);
    }

    public Component addItem(Component c, int index) {
        if (c instanceof JComponent)
            ((JComponent)c).setOpaque(false);

        if (c instanceof JButton)
            ((JButton)c).setDefaultCapable(false);
        
        if (UISupport.isAquaLookAndFeel() && c instanceof AbstractButton)
            ((AbstractButton)c).putClientProperty("JButton.buttonType", "gradient"); // NOI18N

        if (toolbar != null) {
            toolbar.add(c, index);
        } else {
            add(c, index);
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                b.addMouseListener(listener);
                b.addChangeListener(listener);
                b.addFocusListener(listener);
                b.setRolloverEnabled(true);
                listener.refresh(b);
            }
        }
        repaint();
        
        return c;
    }

    public void removeItem(Component c) {
        if (toolbar != null) {
            toolbar.remove(c);
        } else {
            if (c instanceof AbstractButton) {
                c.removeMouseListener(listener);
                ((AbstractButton) c).removeChangeListener(listener);
                c.removeFocusListener(listener);
            }
            remove(c);
        }
        repaint();
    }
    
    public void removeItem(int index) {
        if (toolbar != null) {
            toolbar.remove(index);
        } else {
            removeItem(getComponent(index));
        }
    }
    
    public int getItemsCount() {
	if (toolbar != null) {
            return toolbar.getComponentCount();
        } else {
            return super.getComponentCount();
        }
    }
    
    public void addSeparator() {
        JToolBar.Separator separator = new JToolBar.Separator();
        separator.setOrientation(horizontal ? JToolBar.Separator.VERTICAL :
                                              JToolBar.Separator.HORIZONTAL);
        addItem(separator);
    }
    
    public void addSpace(int width) {
        Dimension dim = horizontal ? new Dimension(width, 0) : new Dimension(0, width);
        JToolBar.Separator separator = new JToolBar.Separator(dim);
        separator.setOrientation(horizontal ? JToolBar.Separator.VERTICAL :
                                              JToolBar.Separator.HORIZONTAL);
        addItem(separator);
    }
    
    public void addFiller() {
        Dimension minDim = new Dimension(0, 0);
        Dimension maxDim = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Box.Filler filler = new Box.Filler(minDim, minDim, maxDim) {
            protected void paintComponent(Graphics g) {}
        };
        addItem(filler);
    }
    
    public static JComponent withSeparator(TransparentToolBar toolbar) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(false);
        panel.add(toolbar, BorderLayout.CENTER);
        panel.add(new SeparatorLine(true), toolbar.horizontal ? BorderLayout.SOUTH :
                                                                BorderLayout.EAST);
        return panel;
    }
    
    
    private JButton createActionComponent(Action a) {
        JButton b = new JButton();
        if (a != null && (a.getValue(Action.SMALL_ICON) != null ||
                          a.getValue(Action.LARGE_ICON_KEY) != null)) {
            b.setHideActionText(true);
        }
        b.setHorizontalTextPosition(JButton.CENTER);
        b.setVerticalTextPosition(JButton.BOTTOM);
        b.setAction(a);
        return b;
    }
    
    private static JToolBar createToolBar(final boolean horizontal) {
        JToolBar tb = new JToolBar(horizontal ? JToolBar.HORIZONTAL : JToolBar.VERTICAL) {
            public void layout() {
                super.layout();
                if (horizontal) {
                    if (BUTTON_HEIGHT == -1)
                        BUTTON_HEIGHT = getButtonHeight();
                    Insets i = getInsets();
                    int height = getHeight() - i.top - i.bottom;
                    for (Component comp : getComponents()) {
                        if (comp.isVisible() && (comp instanceof JButton || comp instanceof JToggleButton)) {
                            Rectangle b = comp.getBounds();
                            b.height = BUTTON_HEIGHT;
                            b.y = i.top + (height - b.height) / 2;
                            comp.setBounds(b);
                        }
                    }
                }
            }
        };
        if (UISupport.isNimbusLookAndFeel())
            tb.setLayout(new BoxLayout(tb, horizontal ? BoxLayout.X_AXIS :
                                                        BoxLayout.Y_AXIS));
        tb.setBorderPainted(false);
        tb.setFloatable(false);
        tb.setRollover(true);
        tb.setOpaque(false);
        return tb;
    }
    
    private static Border createToolBarBorder(boolean horizontal) {
        if (horizontal) {
            if (UISupport.isAquaLookAndFeel()) {
                return BorderFactory.createEmptyBorder(-1, 2, -1, 2);
            } else if (UISupport.isNimbusLookAndFeel()) {
                return BorderFactory.createEmptyBorder(1, 2, 1, 2);
            } else {
                return BorderFactory.createEmptyBorder(2, 2, 2, 2);
            }
        } else {
            if (UISupport.isAquaLookAndFeel()) {
                return BorderFactory.createEmptyBorder(-1, 0, -1, 0);
            } else {
                return BorderFactory.createEmptyBorder();
            }
        }
    }
    
    private static int getButtonHeight() {
        Icon icon = new Icon() {
            public int getIconWidth() { return 16; }
            public int getIconHeight() { return 16; }
            public void paintIcon(Component c, Graphics g, int x, int y) {}
        };
        
        JButton b = new JButton("Button", icon); // NOI18N
        JToolBar tb = new JToolBar();
        tb.setBorder(BorderFactory.createEmptyBorder());
        tb.setBorderPainted(false);
        tb.add(b);
        int bsize = tb.getPreferredSize().height;
        
        JToggleButton t = new JToggleButton("Button", icon); // NOI18N
        tb = new JToolBar();
        tb.setBorder(BorderFactory.createEmptyBorder());
        tb.setBorderPainted(false);
        tb.add(t);
        int tbsize = tb.getPreferredSize().height;
        
        if (UISupport.isAquaLookAndFeel())
            return Math.max(bsize, tbsize) + 4;
        else if (UISupport.isMetalLookAndFeel())
            return Math.max(bsize, tbsize) - 2;
        
        return Math.max(bsize, tbsize);
    }
    

    private static boolean needsPanel() {
        if (NEEDS_PANEL == null) NEEDS_PANEL = UISupport.isGTKLookAndFeel();
        return NEEDS_PANEL;
    }

            
    private static final class ItemListener extends MouseAdapter implements ChangeListener, FocusListener {

        private static final String PROP_HOVERED = "BUTTON_HOVERED"; // NOI18N

        public void mouseEntered(MouseEvent e) {
            AbstractButton b = (AbstractButton) e.getSource();
            b.putClientProperty(PROP_HOVERED, Boolean.TRUE);
            refresh(b);
        }

        public void mouseExited(MouseEvent e) {
            AbstractButton b = (AbstractButton) e.getSource();
            b.putClientProperty(PROP_HOVERED, Boolean.FALSE);
            refresh(b);
        }

        public void stateChanged(ChangeEvent e) {
            refresh((AbstractButton) e.getSource());
        }

        public void focusGained(FocusEvent e) {
            refresh((AbstractButton) e.getSource());
        }

        public void focusLost(FocusEvent e) {
            refresh((AbstractButton) e.getSource());
        }

        private void refresh(final AbstractButton b) {
            b.setBackground(UISupport.getDefaultBackground());
            boolean hovered = Boolean.TRUE.equals(b.getClientProperty(PROP_HOVERED));
            boolean filled = b.isEnabled() && (hovered || b.isSelected() || b.isFocusOwner());
            b.setOpaque(filled);
            b.setContentAreaFilled(filled);
            b.repaint();
        }
        
    }
}
