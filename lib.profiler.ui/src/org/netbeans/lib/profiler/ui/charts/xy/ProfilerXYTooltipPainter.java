/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.netbeans.lib.profiler.ui.charts.xy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import org.netbeans.lib.profiler.charts.ItemSelection;
import org.netbeans.lib.profiler.charts.swing.RoundBorder;
import org.netbeans.lib.profiler.charts.xy.XYItemSelection;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerXYTooltipPainter extends JPanel {

    private JLabel caption;
    private JLabel[] valuePainters;
    private JLabel[] unitsPainters;
    private JLabel[] extraValuePainters;
    private JLabel[] extraUnitsPainters;

    private ProfilerXYTooltipModel model;

    private boolean initialized;


    public ProfilerXYTooltipPainter(float lineWidth, Color lineColor,
                                       Color fillColor, ProfilerXYTooltipModel
                                       model) {

        this.model = model;
        initialized = false;

        Border rb = new RoundBorder(lineWidth, lineColor, fillColor, 10, 7);
        Border eb = BorderFactory.createEmptyBorder(0, 5, 0, 5);
        setBorder(BorderFactory.createCompoundBorder(rb, eb));

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
            unitsPainters[i].setText(model.getRowUnits(i, itemValue));
        }

        int extraRowsCount = model.getExtraRowsCount();
        for (int i = 0; i < extraRowsCount; i++) {
            extraValuePainters[i].setText(model.getExtraRowValue(i));
            extraUnitsPainters[i].setText(model.getExtraRowUnits(i));
        }
    }


    private void initComponents() {
        setOpaque(false);

        setLayout(new GridBagLayout());
        GridBagConstraints constraints;

        float defaultFontSize;

        caption = new JLabel();
        defaultFontSize = caption.getFont().getSize2D();
        caption.setFont(caption.getFont().deriveFont(
                        defaultFontSize - 1));
        caption.setForeground(Color.DARK_GRAY);
        caption.setOpaque(false);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 4, 0);
        add(caption, constraints);

        int count = model.getRowsCount();
        valuePainters = new JLabel[count];
        unitsPainters = new JLabel[count];
        for (int i = 0; i < count; i++) {

            JLabel itemLabel = new JLabel();
            itemLabel.setText(model.getRowName(i));
            itemLabel.setFont(itemLabel.getFont().deriveFont(
                              defaultFontSize + 1));
            itemLabel.setForeground(model.getRowColor(i));
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
            valueLabel.setFont(itemLabel.getFont().deriveFont(
                               Font.BOLD, defaultFontSize + 1));
            valueLabel.setForeground(model.getRowColor(i));
            valueLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = i + 1;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 8, 0, 0);
            add(valueLabel, constraints);

            JLabel unitLabel = new JLabel();
            unitsPainters[i] = unitLabel;
            unitLabel.setFont(unitLabel.getFont().deriveFont(
                              defaultFontSize + 1));
            unitLabel.setForeground(model.getRowColor(i));
            unitLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = i + 1;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 4, 0, 0);
            add(unitLabel, constraints);

            JPanel valueSpacer = new JPanel(null);
            valueSpacer.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = i + 1;
            constraints.weightx = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(valueSpacer, constraints);

        }

        int extraCount = model.getExtraRowsCount();

        if (extraCount > 0) {
            JPanel separator = new JPanel(null) {
                public Dimension getPreferredSize() {
                    return new Dimension(super.getPreferredSize().width, 1);
                }
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                public Dimension getMaximumSize() {
                    return getPreferredSize();
                }
            };
            separator.setBackground(Color.GRAY);
            separator.setOpaque(true);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = count + 1;
            constraints.weighty = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(7, 0, 7, 0);
            add(separator, constraints);
        }

        extraValuePainters = new JLabel[extraCount];
        extraUnitsPainters = new JLabel[extraCount];
        for (int i = 0; i < extraCount; i++) {

            JLabel extraItemLabel = new JLabel();
            extraItemLabel.setText(model.getExtraRowName(i));
            extraItemLabel.setFont(extraItemLabel.getFont().deriveFont(
                                   defaultFontSize - 1));
            extraItemLabel.setForeground(model.getRowColor(i));
            extraItemLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = count + i + 2;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(extraItemLabel, constraints);

            JLabel extraValueLabel = new JLabel();
            extraValuePainters[i] = extraValueLabel;
            extraValueLabel.setFont(extraItemLabel.getFont().deriveFont(
                                    Font.BOLD, defaultFontSize - 1));
            extraValueLabel.setForeground(model.getExtraRowColor(i));
            extraValueLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = count + i + 2;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 8, 0, 0);
            add(extraValueLabel, constraints);

            JLabel extraUnitLabel = new JLabel();
            extraUnitsPainters[i] = extraUnitLabel;
            extraUnitLabel.setFont(extraUnitLabel.getFont().deriveFont(
                                   defaultFontSize - 1));
            extraUnitLabel.setForeground(model.getExtraRowColor(i));
            extraUnitLabel.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = count + i + 2;
            constraints.gridwidth = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.insets = new Insets(0, 4, 0, 0);
            add(extraUnitLabel, constraints);

            JPanel extraValueSpacer = new JPanel(null);
            extraValueSpacer.setOpaque(false);
            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = count + i + 2;
            constraints.weightx = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(extraValueSpacer, constraints);

        }

        initialized = true;
    }

}
