/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.swing;

import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TransparentToolBar extends JPanel {

    private static final boolean NEEDS_PANEL = needsPanel();
    private final JToolBar toolbar;
    private final ItemListener listener = new ItemListener();

    public TransparentToolBar() {
        toolbar = NEEDS_PANEL ? null : new JToolBar();
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
                                listener.refresh((AbstractButton) c);
                            }
                        }
                    }
                }
            }
        });
    }

    public void addItem(JComponent c) {
        c.setOpaque(false);
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

    private static boolean needsPanel() {
        return UIManager.getLookAndFeel().getID().equals("GTK"); //NOI18N
    }

    private static final class ItemListener extends MouseAdapter implements ChangeListener, FocusListener {

        private static final String PROP_HOVERED = "BUTTON_HOVERED"; // NOI18N
        private static Color BACKGROUND_COLOR;

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
            b.setBackground(getBackgroundColor(b));
            boolean hovered = Boolean.TRUE.equals(b.getClientProperty(PROP_HOVERED));
            boolean filled = b.isEnabled() && (hovered || b.isSelected() || b.isFocusOwner());
            b.setOpaque(filled);
            b.setContentAreaFilled(filled);
            b.repaint();
        }

        private static Color getBackgroundColor(JComponent jc) {
            if (BACKGROUND_COLOR == null) {
                Container c = jc;
                while (BACKGROUND_COLOR == null && c != null) {
                    if (c instanceof DataViewComponent) {
                        BACKGROUND_COLOR = c.getBackground();
                    } else {
                        c = c.getParent();
                    }
                }
            }
            return BACKGROUND_COLOR;
        }
    }
}
