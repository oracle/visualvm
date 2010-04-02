/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl.timeline;

import com.sun.tools.visualvm.modules.tracer.impl.swing.LegendFont;
import java.awt.Color;
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
final class TimelineTooltipPainter extends JPanel {

    private static Color SELECTION_FOREGROUND = Color.BLACK;
    private static Color SELECTION_BACKGROUND = Utils.forceSpeed() ?
                                            new Color(255, 255, 255) :
                                            new Color(255, 255, 255, 170);
    private static Color HOVER_FOREGROUND = Color.WHITE;
    private static Color HOVER_BACKGROUND = Utils.forceSpeed() ?
                                            new Color(80, 80, 80) :
                                            new Color(0, 0, 0, 170);

    private JLabel[] valueNames;
    private JLabel[] valuePainters;
    private JLabel[] unitsPainters;

    private final boolean selection;
    private final Color foreground;
    private final Color background;
    private boolean initialized;


    TimelineTooltipPainter(boolean selection) {
        this.selection = selection;

        foreground = selection ? SELECTION_FOREGROUND : HOVER_FOREGROUND;
        background = selection ? SELECTION_BACKGROUND : HOVER_BACKGROUND;

        initialized = false;
    }


    void update(Model rowModel, List<ItemSelection> selectedItems) {
        if (!initialized) initComponents(rowModel);
        
        int rowsCount = rowModel.getRowsCount();
        for (int i = 0; i < rowsCount; i++) {
            XYItemSelection sel = (XYItemSelection)selectedItems.get(i);
            long itemValue = sel.getItem().getYValue(sel.getValueIndex());
            valueNames[i].setText(rowModel.getRowName(i));
            valuePainters[i].setText(rowModel.getRowValue(i, itemValue));
            unitsPainters[i].setText(rowModel.getRowUnits(i));
        }
    }


    protected void paintComponent(Graphics g) {
        g.setColor(background);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
        if (selection) {
            g.setColor(foreground);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }


    private void initComponents(Model rowModel) {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setLayout(new GridBagLayout());
        GridBagConstraints constraints;

        int count = rowModel.getRowsCount();
        valueNames = new JLabel[count];
        valuePainters = new JLabel[count];
        unitsPainters = new JLabel[count];
        for (int i = 0; i < count; i++) {

            JLabel itemLabel = new JLabel();
            valueNames[i] = itemLabel;
            itemLabel.setFont(new LegendFont());
            itemLabel.setForeground(foreground);
            itemLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(itemLabel, constraints);

            JLabel valueLabel = new JLabel();
            valuePainters[i] = valueLabel;
            valueLabel.setFont(new LegendFont());
            valueLabel.setForeground(foreground);
            valueLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 8, 0, 0);
            add(valueLabel, constraints);

            JLabel unitsLabel = new JLabel();
            unitsPainters[i] = unitsLabel;
            unitsLabel.setFont(new LegendFont());
            unitsLabel.setForeground(foreground);
            unitsLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = i;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 3, 0, 0);
            add(unitsLabel, constraints);

            JPanel valueSpacer = new JPanel(null);
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

        initialized = true;
    }


    static interface Model {

        public int    getRowsCount      ();
        public String getRowName        (int index);
        public String getRowValue       (int index, long itemValue);
        public String getRowUnits       (int index);

    }

}
