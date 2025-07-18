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
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.filters.TextFilter;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.ui.UIUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class FilteringToolbar extends InvisibleToolbar {

    private TextFilter filter;

    private final List<Component> hiddenComponents = new ArrayList<>();
    private final AbstractButton filterButton;

    public FilteringToolbar(String name) {
        if (!UIUtils.isNimbusLookAndFeel())
            setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        filterButton = new JToggleButton(Icons.getIcon(GeneralIcons.FILTER)) {
            protected void fireActionPerformed(ActionEvent e) {
                if (isSelected()) showFilter(); else hideFilter();
            }
        };
        filterButton.setToolTipText(name);
        add(filterButton);
    }


    protected abstract void filterChanged();


    public final boolean isAll() {
        return filter == null;
    }

    public final boolean passes(String value) {
        return filter == null ? true : filter.passes(value);
    }

    public final GenericFilter getFilter() {
        TextFilter copy = new TextFilter();
        if (filter != null) copy.copyFrom(filter);
        return copy;
    }

    
    private void filterChanged(String value) {
        if (value == null) {
            filter = null;
        } else {
            if (filter == null) filter = new TextFilter();
            filter.setValue(value);
        }
        filterChanged();
    }

    private void showFilter() {
        filterButton.setSelected(true);

        final JTextField f = new JTextField();
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { changed(); }
            public void removeUpdate(DocumentEvent e)  { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { filterChanged(f.getText().trim()); }
        });
        f.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { if (esc(e)) hideFilter(); }
            public void keyReleased(KeyEvent e) { esc(e); }
            private boolean esc(KeyEvent e) {
                boolean esc = e.getKeyCode() == KeyEvent.VK_ESCAPE;
                if (esc) e.consume();
                return esc;
            }
        });

        for (int i = 1; i < getComponentCount(); i++)
            hiddenComponents.add(getComponent(i));

        for (Component c : hiddenComponents) remove(c);

        add(Box.createHorizontalStrut(3));
        add(f);
        f.requestFocusInWindow();

        invalidate();
        revalidate();
        doLayout();
        repaint();
    }

    private void hideFilter() {
        filterChanged(null);

        remove(2);
        remove(1);
        for (Component c : hiddenComponents) add(c);

        filterButton.setSelected(false);
        filterButton.requestFocusInWindow();

        invalidate();
        revalidate();
        doLayout();
        repaint();
    }

}
