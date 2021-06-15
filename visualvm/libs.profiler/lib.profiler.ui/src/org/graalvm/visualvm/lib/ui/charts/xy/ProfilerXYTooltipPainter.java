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

package org.graalvm.visualvm.lib.ui.charts.xy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.charts.ItemSelection;
import org.graalvm.visualvm.lib.charts.swing.Utils;
import org.graalvm.visualvm.lib.charts.xy.XYItemSelection;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYTooltipPainter extends JPanel {

    private static Color BACKGROUND_COLOR = Utils.forceSpeed() ?
                                            new Color(80, 80, 80) :
                                            new Color(0, 0, 0, 170);

    private JLabel caption;
    private JLabel[] valuePainters;
    private JLabel[] extraValuePainters;

    private final ProfilerXYTooltipModel model;

    private boolean initialized;


    public ProfilerXYTooltipPainter(ProfilerXYTooltipModel model) {
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
        
        int extraRowsCount = model.getExtraRowsCount();
        for (int i = 0; i < extraRowsCount; i++)
            extraValuePainters[i].setText(model.getExtraRowValue(i));
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
        
        Color GRAY = new Color(230, 230, 230);

        caption = new JLabel();
        caption.setFont(smallerFont(caption.getFont()));
        caption.setForeground(GRAY);
        caption.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 5, 0);
        add(caption, constraints);

        final Dimension ZERO = new Dimension(0, 0);
        
        int count = model.getRowsCount();
        valuePainters = new JLabel[count];
        for (int i = 1; i <= count; i++) {
            JLabel itemLabel = new JLabel();
            itemLabel.setText(model.getRowName(i - 1));
            itemLabel.setForeground(Color.WHITE);
            itemLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(itemLabel, constraints);

            JLabel valueLabel = new JLabel();
            valuePainters[i - 1] = valueLabel;
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 8, 0, 0);
            add(valueLabel, constraints);
            
            JLabel itemUnits = new JLabel();
            String units = model.getRowUnits(i - 1);
            if (!units.isEmpty()) units = " " + units; // NOI18N
            itemUnits.setText(units);
            itemUnits.setForeground(Color.WHITE);
            itemUnits.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(itemUnits, constraints);

            JPanel valueSpacer = new JPanel(null) {
                public Dimension getPreferredSize() { return ZERO; }
            };
            valueSpacer.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = i;
            constraints.weightx = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(valueSpacer, constraints);
        }
        
        int extraCount = model.getExtraRowsCount();
        extraValuePainters = new JLabel[count];
        for (int i = 1; i <= extraCount; i++) {
            int top = i == 1 ? 5 : 0;
            
            JLabel maxItemLabel = new JLabel();
            maxItemLabel.setText(model.getExtraRowName(i - 1));
            maxItemLabel.setFont(smallerFont(maxItemLabel.getFont()));
            maxItemLabel.setForeground(GRAY);
            maxItemLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = count + i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(top, 0, 0, 0);
            add(maxItemLabel, constraints);

            JLabel extraValueLabel = new JLabel();
            extraValuePainters[i - 1] = extraValueLabel;
            extraValueLabel.setFont(smallerFont(extraValueLabel.getFont()));
            extraValueLabel.setForeground(GRAY);
            extraValueLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = count + i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(top, 8, 0, 0);
            add(extraValueLabel, constraints);
            
            JLabel maxItemUnits = new JLabel();
            String units = model.getExtraRowUnits(i - 1);
            if (!units.isEmpty()) units = " " + units; // NOI18N
            maxItemUnits.setText(units);
            maxItemUnits.setFont(smallerFont(maxItemUnits.getFont()));
            maxItemUnits.setForeground(GRAY);
            maxItemUnits.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = count + i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(top, 0, 0, 0);
            add(maxItemUnits, constraints);

            JPanel extraValueSpacer = new JPanel(null) {
                public Dimension getPreferredSize() { return ZERO; }
            };
            extraValueSpacer.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = count + i;
            constraints.weightx = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(top, 0, 0, 0);
            add(extraValueSpacer, constraints);
        }

        initialized = true;
    }
    
    
    private static Font smallerFont(Font font) {
        return new Font(font.getName(), font.getStyle(), font.getSize() - 2);
    }

}
