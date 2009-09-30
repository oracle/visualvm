/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.charts;

import java.awt.Color;
import java.util.Iterator;
import java.util.Vector;


/**
 *
 * @author Jiri Sedlacek
 */
public class DynamicPieChartModel extends AbstractPieChartModel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected Color[] itemColors;
    protected String[] itemNames;
    protected double[] itemValues;
    protected double[] itemValuesRel;
    protected boolean hasData = false;
    protected int itemCount = 0;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Color getItemColor(int index) {
        return itemColors[index];
    }

    // --- Abstract PieChartModel ------------------------------------------------
    public int getItemCount() {
        return itemCount;
    }

    public String getItemName(int index) {
        return itemNames[index];
    }

    public double getItemValue(int index) {
        return itemValues[index];
    }

    public double getItemValueRel(int index) {
        return itemValuesRel[index];
    }

    public void setItemValues(double[] itemValues) {
        if (itemValues.length != itemCount) {
            hasData = false;
            throw new RuntimeException("Unexpected number of values."); // NOI18N
        } else {
            this.itemValues = itemValues;
            updateItemValuesRel();
        }

        ;

        fireChartDataChanged();
    }

    public boolean isSelectable(int index) {
        return true;
    }

    public boolean hasData() {
        return hasData;
    }

    public void setupModel(String[] itemNames, Color[] itemColors) {
        this.itemNames = itemNames;
        this.itemColors = itemColors;

        if (itemNames.length != itemColors.length) {
            itemCount = 0;
            throw new RuntimeException("Counts of item names and item colors don't match."); // NOI18N
        } else {
            itemCount = itemNames.length;
        }

        itemValues = null;
        itemValuesRel = new double[itemCount];
        hasData = false;
    }

    // --- Private Implementation ------------------------------------------------

    // computes relative item values
    // O(n) = 2n
    private void updateItemValuesRel() {
        double sum = 0d;

        // compute sum of all item values
        for (int i = 0; i < itemValues.length; i++) {
            sum += itemValues[i];
        }

        // compute new relative item values
        if (sum == 0) {
            for (int i = 0; i < itemValues.length; i++) {
                itemValuesRel[i] = 0;
            }

            hasData = false;
        } else {
            for (int i = 0; i < itemValues.length; i++) {
                itemValuesRel[i] = itemValues[i] / sum;
            }

            hasData = true;
        }
    }
}
