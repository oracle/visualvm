/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */
package com.sun.tools.visualvm.modules.sampler.cpu;

import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileProvider;
import org.netbeans.lib.profiler.results.cpu.MethodInfoMapper;
import org.netbeans.lib.profiler.results.cpu.StackTraceSnapshotBuilder;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import org.netbeans.lib.profiler.ui.cpu.CPUResUserActionsHandler;
import org.netbeans.lib.profiler.ui.cpu.LiveFlatProfileCollectorPanel;

/**
 *
 * @author Tomas Hurka
 */
public class SampledLivePanel extends LiveFlatProfileCollectorPanel {
    private MethodInfoMapper methodInfoMapper;
    private StackTraceSnapshotBuilder snapshotBuilder;
    
    public SampledLivePanel(StackTraceSnapshotBuilder builder) {
        super(null,new DummyHandler(),null);
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
                resTableModel.setRealColumnVisibility(4, false);
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
