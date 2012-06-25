/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.ui.cpu;

import javax.swing.JLabel;
import org.netbeans.lib.profiler.results.cpu.DiffFlatProfileContainer;
import org.netbeans.lib.profiler.ui.components.table.DiffBarCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.lib.profiler.utils.StringUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class DiffFlatProfilePanel extends SnapshotFlatProfilePanel {
    
    public DiffFlatProfilePanel(CPUResUserActionsHandler actionsHandler, boolean sampling) {
        super(actionsHandler, sampling);
    }
    
    
    protected boolean supportsReverseCallGraph() {
        return false;
    }

    protected boolean supportsSubtreeCallGraph() {
        return false;
    }
    
    
    protected Object computeValueAt(int row, int col) {
        long value;
        switch (col) {
            case 0:
                return flatProfileContainer.getMethodNameAtRow(row);
            case 1:
                return flatProfileContainer.getTimeInMcs0AtRow(row);
            case 2:
                value = flatProfileContainer.getTimeInMcs0AtRow(row);
                return (value > 0 ? "+" : "") + StringUtils.mcsTimeToString(value) + " ms"; // NOI18N
            case 3:
                if (collectingTwoTimeStamps) {
                    value = flatProfileContainer.getTimeInMcs1AtRow(row);
                    return (value > 0 ? "+" : "") + StringUtils.mcsTimeToString(value) + " ms"; // NOI18N
                } else {
                    value = flatProfileContainer.getNInvocationsAtRow(row);
                    return (value > 0 ? "+" : "") + intFormat.format(value); // NOI18N
                }
            case 4:
                value = flatProfileContainer.getNInvocationsAtRow(row);
                return (value > 0 ? "+" : "") + intFormat.format(value); // NOI18N
            default:
                return null;
        }
    }
    
    protected void initColumnsData() {
        super.initColumnsData();
        columnRenderers[2] = new LabelTableCellRenderer(JLabel.TRAILING);
    }
    
    protected void obtainResults() {
        super.obtainResults();
        DiffFlatProfileContainer container = (DiffFlatProfileContainer)flatProfileContainer;
        long min = container.getMinTime();
        long max = container.getMaxTime();
        columnRenderers[1] = new DiffBarCellRenderer(min, max);
        resTable.getColumnModel().getColumn(1).setCellRenderer(columnRenderers[1]);
    }
    
}
