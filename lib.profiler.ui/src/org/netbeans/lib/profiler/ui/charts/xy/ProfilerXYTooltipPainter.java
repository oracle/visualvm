/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
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
        caption.setEnabled(false);
        defaultFontSize = caption.getFont().getSize2D();
        caption.setFont(caption.getFont().deriveFont(
                        defaultFontSize - 1));
//        caption.setForeground(Color.DARK_GRAY);
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
