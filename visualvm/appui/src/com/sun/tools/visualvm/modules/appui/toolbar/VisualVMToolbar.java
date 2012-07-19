/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.modules.appui.toolbar;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class VisualVMToolbar extends ProfilerToolbar {

    static int preferredHeight = -1;
    final JComponent component;
    final JToolBar toolbar;

    VisualVMToolbar(boolean showSeparator) {
        toolbar = new JToolBar() {

            public Component add(Component comp) {
                if (comp instanceof JButton) {
                    UIUtils.fixButtonUI((JButton) comp);
                }
                return super.add(comp);
            }

            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                if (preferredHeight == -1) {
                    JToolBar tb = new JToolBar();
                    tb.setBorder(toolbar.getBorder());
                    tb.setBorderPainted(toolbar.isBorderPainted());
                    tb.setRollover(toolbar.isRollover());
                    tb.setFloatable(toolbar.isFloatable());
                    Icon icon = Icons.getIcon(GeneralIcons.SAVE);
                    JButton b = new JButton("Button", icon); // NOI18N
                    tb.add(b);
                    JToggleButton t = new JToggleButton("Button", icon); // NOI18N
                    tb.add(t);
                    JComboBox c = new JComboBox();
                    c.setEditor(new BasicComboBoxEditor());
                    c.setRenderer(new BasicComboBoxRenderer());
                    tb.add(c);
                    tb.addSeparator();
                    preferredHeight = tb.getPreferredSize().height;
                }
                dim.height = Math.max(dim.height, preferredHeight);
                return dim;
            }
        };
        toolbar.setBorder(BorderFactory.createEmptyBorder(9, 5, 5, 5));
        toolbar.setBorderPainted(false);
        toolbar.setRollover(true);
        toolbar.setFloatable(false);
        toolbar.setOpaque(false);

        if (showSeparator) {
            component = new JPanel(new BorderLayout(0, 0));
            component.setOpaque(false);
            component.add(toolbar, BorderLayout.CENTER);
            component.add(UIUtils.createHorizontalLine(toolbar.getBackground()), BorderLayout.SOUTH);
        } else {
            component = toolbar;
        }
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public Component add(Action action) {
        return toolbar.add(action);
    }

    @Override
    public Component add(Component component) {
        return toolbar.add(component);
    }

    @Override
    public Component add(Component component, int index) {
        return toolbar.add(component, index);
    }

    @Override
    public void addSeparator() {
        toolbar.addSeparator();
    }

    @Override
    public void addSpace(int width) {
        toolbar.addSeparator(new Dimension(width, 0));
    }

    @Override
    public void addFiller() {
        JPanel toolbarFiller = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0)) {

            public Dimension getPreferredSize() {
                if (UIUtils.isGTKLookAndFeel() || UIUtils.isNimbusLookAndFeel()) {
                    int currentWidth = toolbar.getSize().width;
                    int minimumWidth = toolbar.getMinimumSize().width;
                    int extraWidth = currentWidth - minimumWidth;
                    return new Dimension(Math.max(extraWidth, 0), 0);
                } else {
                    return super.getPreferredSize();
                }
            }
        };
        toolbarFiller.setOpaque(false);
        toolbar.add(toolbarFiller);
    }

    @Override
    public void remove(Component component) {
        toolbar.remove(component);
    }

    @Override
    public void remove(int index) {
        toolbar.remove(index);
    }

    @Override
    public int getComponentCount() {
        return toolbar.getComponentCount();
    }
}
