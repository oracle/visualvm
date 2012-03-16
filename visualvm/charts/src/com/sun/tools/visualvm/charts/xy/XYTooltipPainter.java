/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.charts.xy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.swing.Utils;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;

/**
 *
 * @author Jiri Sedlacek
 */
public class XYTooltipPainter extends JPanel {

    private static Color BACKGROUND_COLOR = Utils.forceSpeed() ?
                                            new Color(80, 80, 80) :
                                            new Color(0, 0, 0, 170);

    private JLabel caption;
    private JLabel[] valuePainters;

    private XYTooltipModel model;

    private boolean initialized;


    public XYTooltipPainter(XYTooltipModel model) {

        this.model = model;
        initialized = false;

    }


    public void update(List<ItemSelection> selectedItems) {
        if (!initialized) initComponents();
        
        int rowsCount = model.getRowsCount();
        if (selectedItems.size() != rowsCount)
            throw new IllegalStateException("Rows and selected items don't match"); // NOI18N

        XYItemSelection selection = (XYItemSelection)selectedItems.get(0);
        long timestamp = selection.getItem().getXValue(selection.getValueIndex());
        caption.setText(model.getTimeValue(timestamp));

        for (int i = 0; i < rowsCount; i++) {
            XYItemSelection sel = (XYItemSelection)selectedItems.get(i);
            long itemValue = sel.getItem().getYValue(sel.getValueIndex());
            valuePainters[i].setText(model.getRowValue(i, itemValue));
        }
    }


    protected void paintComponent(Graphics g) {
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }


    private void initComponents() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setLayout(new GridBagLayout());
        GridBagConstraints constraints;

        caption = new JLabel();
        caption.setFont(SimpleXYChartUtils.smallerFont(caption.getFont()));
        caption.setForeground(Color.WHITE);
        caption.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(caption, constraints);

        int count = model.getRowsCount();
        valuePainters = new JLabel[count];
        for (int i = 0; i < count; i++) {

            JLabel itemLabel = new JLabel();
            itemLabel.setText(model.getRowName(i));
            itemLabel.setFont(SimpleXYChartUtils.smallerFont(itemLabel.getFont()));
            itemLabel.setForeground(Color.WHITE);
            itemLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = i + 1;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(itemLabel, constraints);

            JLabel valueLabel = new JLabel();
            valuePainters[i] = valueLabel;
            valueLabel.setFont(SimpleXYChartUtils.smallerFont(valueLabel.getFont()));
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = i + 1;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 8, 0, 0);
            add(valueLabel, constraints);

            final Dimension ZERO = new Dimension(0, 0);
            JPanel valueSpacer = new JPanel(null) {
                public Dimension getPreferredSize() { return ZERO; }
            };
            valueSpacer.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = i + 1;
            constraints.weightx = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(valueSpacer, constraints);

        }

        initialized = true;
    }

}
