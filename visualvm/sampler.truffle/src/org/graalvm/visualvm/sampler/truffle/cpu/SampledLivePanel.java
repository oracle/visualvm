/*
 *  Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.sampler.truffle.cpu;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
import org.graalvm.visualvm.lib.jfluid.results.cpu.FlatProfileProvider;
import org.graalvm.visualvm.lib.jfluid.results.cpu.MethodInfoMapper;
import org.graalvm.visualvm.lib.jfluid.results.cpu.StackTraceSnapshotBuilder;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.graalvm.visualvm.lib.ui.cpu.CPUResUserActionsHandler;
import org.graalvm.visualvm.lib.ui.cpu.LiveFlatProfileCollectorPanel;

/**
 *
 * @author Tomas Hurka
 */
final class SampledLivePanel extends LiveFlatProfileCollectorPanel {
    private MethodInfoMapper methodInfoMapper;
    private StackTraceSnapshotBuilder snapshotBuilder;
    
    public SampledLivePanel(StackTraceSnapshotBuilder builder) {
        super(null,new DummyHandler(),null,true);
        methodInfoMapper = builder.getMapper();
        snapshotBuilder = builder;
    }
    
    public FlatProfileProvider getFlatProfileProvider() {
        InstrumentationFilter filter = snapshotBuilder.getFilter();
        boolean twoTimeStamps = snapshotBuilder.collectionTwoTimeStamps();
        CCTFlattener flattener = new CCTFlattener(twoTimeStamps,methodInfoMapper,filter);
        RuntimeCPUCCTNode rootNode = (RuntimeCPUCCTNode) snapshotBuilder.getAppRootNode();
        return new FlatProfileBuilder(rootNode, flattener);
    }

    protected String[] getMethodClassNameAndSig(int methodId, int currentView) {
        String className = methodInfoMapper.getInstrMethodClass(methodId);

        if (currentView == CPUResultsSnapshot.METHOD_LEVEL_VIEW) {
            String methodName = methodInfoMapper.getInstrMethodName(methodId);
            String methodSig = methodInfoMapper.getInstrMethodSignature(methodId);

            return new String[] { className, methodName, methodSig };
        }

        return new String[] { className, null, null };
    }

    protected void obtainResults() {
        super.obtainResults();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                resTable.createDefaultColumnsFromModel();
                resTableModel.setTable(resTable);
                setColumnsData();
            }
        });
    }

    private void setColumnsData() {
        TableColumnModel colModel = resTable.getColumnModel();
        for (int i = 0; i < resTableModel.getColumnCount(); i++) {
            int index = resTableModel.getRealColumn(i);
            if (index != 0)
                colModel.getColumn(i).setPreferredWidth(columnWidths[index - 1]);
            colModel.getColumn(i).setCellRenderer(columnRenderers[index]);
        }
    }
    
    private static final class DummyHandler extends CPUResUserActionsHandler.Adapter {

        public void addMethodToRoots(final String className, final String methodName, final String methodSig) {
            throw new IllegalStateException("addMethodToRoots");    // NOI18N
        }

        public void showReverseCallGraph(final CPUResultsSnapshot snapshot, final int threadId, final int methodId, int view,
                                         int sortingColumn, boolean sortingOrder) {
            throw new IllegalStateException("showReverseCallGraph");    // NOI18N
        }

        public void showSourceForMethod(final String className, final String methodName, final String methodSig) {
            // no-op
        }

        public void viewChanged(int viewType) {
            throw new IllegalStateException("viewChanged"); // NOI18N
        }
    }

}
