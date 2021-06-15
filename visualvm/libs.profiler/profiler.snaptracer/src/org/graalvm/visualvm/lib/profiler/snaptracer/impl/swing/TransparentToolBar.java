/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TransparentToolBar extends JPanel {

    private static Boolean NEEDS_PANEL;
    private static Boolean CUSTOM_FILLER;

    private final JToolBar toolbar;
    private final ItemListener listener = new ItemListener();


    public TransparentToolBar() {
        toolbar = needsPanel() ? null : new JToolBar();
        setOpaque(false);
        if (toolbar == null) {
            // Toolbar is a JPanel (GTK)
            setLayout(new HorizontalLayout(false));
        } else {
            // Toolbar is a JToolBar (default)
            toolbar.setBorderPainted(false);
            toolbar.setFloatable(false);
            toolbar.setRollover(true);
            toolbar.setOpaque(false);
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

    public void addItem(JComponent c) {
        c.setOpaque(false);

        if (c instanceof JButton)
            ((JButton)c).setDefaultCapable(false);

        if (toolbar != null) {
            toolbar.add(c);
        } else {
            add(c);
            if (c instanceof AbstractButton) {
                AbstractButton b = (AbstractButton) c;
                b.addMouseListener(listener);
                b.addChangeListener(listener);
                b.addFocusListener(listener);
                b.setRolloverEnabled(true);
            }
        }
    }

    public void removeItem(JComponent c) {
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
    }
    
    public void addSeparator() {
        JToolBar.Separator separator = new JToolBar.Separator();
        separator.setOrientation(JToolBar.Separator.VERTICAL);
        addItem(separator);
    }
    
    public void addFiller() {
        Dimension minDim = new Dimension(0, 0);
        Dimension maxDim = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        final boolean customFiller = customFiller();
        Box.Filler filler = new Box.Filler(minDim, minDim, maxDim) {
            public Dimension getPreferredSize() {
                if (customFiller) {
                    int currentWidth = TransparentToolBar.this.getSize().width;
                    int minimumWidth = TransparentToolBar.this.getMinimumSize().width;
                    int extraWidth = currentWidth - minimumWidth;
                    return new Dimension(Math.max(extraWidth, 0), 0);
                } else {
                    return super.getPreferredSize();
                }
            }
            protected void paintComponent(Graphics g) {}
        };
        addItem(filler);
    }
    

    private static boolean needsPanel() {
        if (NEEDS_PANEL == null) NEEDS_PANEL = UIUtils.isGTKLookAndFeel();
        return NEEDS_PANEL;
    }
    
    private static boolean customFiller() {
        if (CUSTOM_FILLER == null) CUSTOM_FILLER = UIUtils.isGTKLookAndFeel() ||
                                                  UIUtils.isNimbusLookAndFeel();
        return CUSTOM_FILLER;
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
            b.setBackground(UIUtils.getProfilerResultsBackground());
            boolean hovered = Boolean.TRUE.equals(b.getClientProperty(PROP_HOVERED));
            boolean filled = b.isEnabled() && (hovered || b.isSelected() || b.isFocusOwner());
            b.setOpaque(filled);
            b.setContentAreaFilled(filled);
            b.repaint();
        }
        
    }
}
