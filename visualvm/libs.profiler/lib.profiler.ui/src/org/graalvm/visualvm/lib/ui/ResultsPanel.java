/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;


/**
 * The very base functionality common for various types of results displays.
 *
 * @author Ian Formanek
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public abstract class ResultsPanel extends JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.Bundle"); // NOI18N
    private static final String CORNER_BUTTON_TOOLTIP = messages.getString("ResultsPanel_CornerButtonToolTip"); // NOI18N
                                                                                                                // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected NumberFormat intFormat;
    protected NumberFormat percentFormat;
    private boolean internalCornerButtonClick = false; // flag for closing columns popup by pressing cornerButton

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ResultsPanel() {
        setLayout(new BorderLayout());

        intFormat = NumberFormat.getIntegerInstance();
        intFormat.setGroupingUsed(true);

        percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(0);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Generate the data that can then be presented in this panel */
    public abstract void prepareResults();

    protected JButton createHeaderPopupCornerButton(final JPopupMenu headerPopup) {
        final JButton cornerButton = new JButton(Icons.getIcon(GeneralIcons.HIDE_COLUMN));
        cornerButton.setToolTipText(CORNER_BUTTON_TOOLTIP);
        cornerButton.setDefaultCapable(false);

        if (UIUtils.isWindowsClassicLookAndFeel()) {
            cornerButton.setMargin(new Insets(0, 0, 2, 2));
        } else if (UIUtils.isWindowsXPLookAndFeel()) {
            cornerButton.setMargin(new Insets(0, 0, 0, 1));
        } else if (UIUtils.isMetalLookAndFeel()) {
            cornerButton.setMargin(new Insets(0, 0, 2, 1));
        }

        cornerButton.addKeyListener(new KeyAdapter() {
                public void keyPressed(final KeyEvent evt) {
                    if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
                        showColumnSelectionPopup(headerPopup, cornerButton);
                    }
                }
            });

        cornerButton.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent mouseEvent) {
                    if (headerPopup.isVisible()) {
                        internalCornerButtonClick = true;
                        cornerButton.getModel().setArmed(false);
                    } else {
                        internalCornerButtonClick = false;

                        if (mouseEvent.isPopupTrigger()) {
                            showColumnSelectionPopup(headerPopup, cornerButton);
                        }
                    }
                }

                public void mouseClicked(MouseEvent mouseEvent) {
                    if (SwingUtilities.isLeftMouseButton(mouseEvent) && (!internalCornerButtonClick)) {
                        showColumnSelectionPopup(headerPopup, cornerButton);
                    }
                }
            });

        return cornerButton;
    }

    /**
     * Creates instance of JScrollPane and adds it to the BorderLayout.CENTER of the ResultsPanel.
     */
    protected JScrollPane createScrollPane() {
        JScrollPane jScrollPane = new JScrollPane();
        add(jScrollPane, BorderLayout.CENTER);

        return jScrollPane;
    }

    /**
     * Creates instance of JScrollPane and adds it to the BorderLayout.CENTER of the ResultsPanel.
     * Sets ScrollBar policies JScrollPane.VERTICAL_SCROLLBAR_ALWAYS and JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
     * and enables vertical ScrollBar only if needed.
     */
    protected JScrollPane createScrollPaneVerticalScrollBarAlways() {
        final JScrollPane jScrollPane = createScrollPane();
        jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // Enable vertical scrollbar only if needed
        final JScrollBar vScrollbar = jScrollPane.getVerticalScrollBar();
        vScrollbar.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                vScrollbar.setEnabled(ResultsPanel.this.isEnabled() &&
                vScrollbar.getVisibleAmount() < vScrollbar.getMaximum());
            }
        });

        return jScrollPane;
    }

    protected abstract void initColumnSelectorItems();

    private void showColumnSelectionPopup(final JPopupMenu headerPopup, final JButton cornerButton) {
        initColumnSelectorItems();
        headerPopup.show(cornerButton, cornerButton.getWidth() - headerPopup.getPreferredSize().width, cornerButton.getHeight());
    }
}
