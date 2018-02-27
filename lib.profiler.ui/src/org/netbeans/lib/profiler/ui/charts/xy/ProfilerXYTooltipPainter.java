/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2015 Oracle and/or its affiliates. All rights reserved.
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
