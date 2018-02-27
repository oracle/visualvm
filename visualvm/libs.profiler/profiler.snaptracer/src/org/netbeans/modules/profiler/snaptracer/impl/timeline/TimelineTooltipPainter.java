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

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

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
import org.netbeans.modules.profiler.snaptracer.impl.swing.LegendFont;
import org.netbeans.modules.profiler.snaptracer.impl.swing.Spacer;

/**
 *
 * @author Jiri Sedlacek
 */
final class TimelineTooltipPainter extends JPanel {

    private static Color SELECTION_FOREGROUND = Color.BLACK;
    private static Color SELECTION_BACKGROUND = Utils.forceSpeed() ?
                                            new Color(255, 255, 255) :
                                            new Color(255, 255, 255, 225);
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


    TimelineTooltipPainter(boolean selection) {
        this.selection = selection;

        foreground = selection ? SELECTION_FOREGROUND : HOVER_FOREGROUND;
        background = selection ? SELECTION_BACKGROUND : HOVER_BACKGROUND;

        initUI();
    }


    void update(Model rowModel, List<ItemSelection> selectedItems) {
        int rowsCount = rowModel.getRowsCount();
        if (valueNames == null || valueNames.length != rowsCount)
            initComponents(rowsCount);
        
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


    private void initUI() {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        setLayout(new GridBagLayout());
    }

    private void initComponents(int rowsCount) {
        removeAll();
        
        valueNames = new JLabel[rowsCount];
        valuePainters = new JLabel[rowsCount];
        unitsPainters = new JLabel[rowsCount];
        
        GridBagConstraints constraints;

        for (int i = 0; i < rowsCount; i++) {
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

            constraints = new GridBagConstraints();
            constraints.gridx = 3;
            constraints.gridy = i;
            constraints.weightx = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.insets = new Insets(0, 0, 0, 0);
            add(Spacer.create(), constraints);
        }
    }


    static interface Model {

        public int    getRowsCount      ();
        public String getRowName        (int index);
        public String getRowValue       (int index, long itemValue);
        public String getRowUnits       (int index);

    }

}
