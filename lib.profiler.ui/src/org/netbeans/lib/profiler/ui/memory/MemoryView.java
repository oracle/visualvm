/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui.memory;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.memory.HeapHistogram;
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.utils.StringUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class MemoryView extends JPanel {
    
    private final ProfilerClient client;
    private final boolean showSourceSupported;
    
    private SampledTableView sampledView;
    private AllocTableView allocView;
    private LivenessTableView livenessView;
    
    private JPanel currentView;
    
    private final Set<String> selection;
    
    
    public MemoryView(ProfilerClient client, Set<String> selection, boolean showSourceSupported) {
        this.client = client;
        this.selection = selection;
        this.showSourceSupported = showSourceSupported;
        
        initUI();
    }
    
    
    public void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        JPanel newView = getView();
        if (newView != currentView) {
            removeAll();
            resetData();
            currentView = newView;
            add(currentView, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
        
        if (currentView == sampledView) {
            HeapHistogram histogram = client.getHeapHistogram();
            if (histogram != null) sampledView.setData(histogram);
        } else if (currentView == allocView) {
            client.forceObtainedResultsDump(true);
            ProfilingSessionStatus status = client.getStatus();
            MemoryCCTProvider oacgb = client.getMemoryCCTProvider();
            if (oacgb == null) throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
            if (oacgb.getObjectsSizePerClass() != null) {
                int _nTrackedItems = status.getNInstrClasses();
                int[] _nTotalAllocObjects = client.getAllocatedObjectsCountResults();
                long[] _totalAllocObjectsSize = oacgb.getAllocObjectNumbers();

                if (_nTrackedItems > _nTotalAllocObjects.length)
                    _nTrackedItems = _nTotalAllocObjects.length;
                if (_nTrackedItems > _totalAllocObjectsSize.length)
                    _nTrackedItems = _totalAllocObjectsSize.length;
                
                String[] _classNames = status.getClassNames();
                for (int i = 0; i < _classNames.length; i++)
                    _classNames[i] = StringUtils.userFormClassName(_classNames[i]);

                allocView.setData(_nTrackedItems, _classNames, _nTotalAllocObjects,
                                  _totalAllocObjectsSize);
            }
        } else if (currentView == livenessView) {
            client.forceObtainedResultsDump(true);
            ProfilingSessionStatus status = client.getStatus();
            MemoryCCTProvider olcgb = client.getMemoryCCTProvider();
            if (olcgb == null) throw new ClientUtils.TargetAppOrVMTerminated(ClientUtils.TargetAppOrVMTerminated.VM);
            if (olcgb.getObjectsSizePerClass() != null) {
                MemoryCCTProvider.ObjectNumbersContainer onc = olcgb.getLivenessObjectNumbers();
                long[] _nTrackedAllocObjects = onc.nTrackedAllocObjects;
                int[] _nTrackedLiveObjects = onc.nTrackedLiveObjects;
                long[] _trackedLiveObjectsSize = onc.trackedLiveObjectsSize;
                float[] _avgObjectAge = onc.avgObjectAge;
                int[] _maxSurvGen = onc.maxSurvGen;
                int _nInstrClasses = onc.nInstrClasses;

                if (((_nTrackedLiveObjects == null) && (_nTrackedAllocObjects == null)) ||
                     (_avgObjectAge == null) || (_maxSurvGen == null)) return;

                int[] _nTotalAllocObjects = client.getAllocatedObjectsCountResults();

                int _nTrackedItems = Math.min(_nTrackedAllocObjects.length, _nTrackedLiveObjects.length);
                _nTrackedItems = Math.min(_nTrackedItems, _trackedLiveObjectsSize.length);
                _nTrackedItems = Math.min(_nTrackedItems, _avgObjectAge.length);
                _nTrackedItems = Math.min(_nTrackedItems, _maxSurvGen.length);
                _nTrackedItems = Math.min(_nTrackedItems, _nInstrClasses);
                _nTrackedItems = Math.min(_nTrackedItems, _nTotalAllocObjects.length);

                for (int i = 0; i < _nTrackedItems; i++)
                    if (_nTrackedAllocObjects[i] == -1)
                        _nTotalAllocObjects[i] = 0;
                
                String[] _classNames = status.getClassNames();
                for (int i = 0; i < _classNames.length; i++)
                    _classNames[i] = StringUtils.userFormClassName(_classNames[i]);

                livenessView.setData(_nTrackedItems, _classNames, _nTrackedLiveObjects,
                                     _trackedLiveObjectsSize, _nTrackedAllocObjects,
                                     _avgObjectAge, _maxSurvGen, _nTotalAllocObjects);
            }
        }
    }
    
    public void resetData() {
        if (currentView == null) return;
        
        if (currentView == sampledView) {
            sampledView.resetData();
        } else if (currentView == allocView) {
            allocView.resetData();
        } else if (currentView == livenessView) {
            livenessView.resetData();
        }
    }
    
    
//    public boolean hasSelection() {
//        if (currentView == null) {
//            return false;
//        } else if (currentView == sampledView) {
//            return sampledView.hasSelection();
//        } else if (currentView == allocView) {
//            return allocView.hasSelection();
//        } else if (currentView == livenessView) {
//            return livenessView.hasSelection();
//        } else {
//            return false;
//        }
//    }
//    
//    public String[] getSelections() {
//        if (!hasSelection()) {
//            return new String[0];
//        } else if (currentView == sampledView) {
//            return sampledView.getSelections();
//        } else if (currentView == allocView) {
//            return allocView.getSelections();
//        } else if (currentView == livenessView) {
//            return livenessView.getSelections();
//        } else {
//            return null;
//        }
//    }
    
    public void refreshSelection() {
        if (sampledView != null) sampledView.refreshSelection();
        if (allocView != null) allocView.refreshSelection();
        if (livenessView != null) livenessView.refreshSelection();
    }
    
    
    public abstract void showSource(String value);
    
    public abstract void profileSingle(String value);
    
    public abstract void selectForProfiling(String[] value);
    
    public void popupShowing() {};
    
    public void popupHidden() {};
    
    
    private JPanel getView() {
        switch (client.getCurrentInstrType()) {
//            case CommonConstants.INSTR_NONE_MEMORY_SAMPLING:
//                if (sampledView == null) sampledView = new SampledTableView();
//                return sampledView;
            case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
                if (allocView == null) allocView = new AllocTableView(selection) {
                    protected void performDefaultAction(String value) {
                        if (showSourceSupported) showSource(value);
                    }
                    protected void populatePopup(JPopupMenu popup, String value) {
                        MemoryView.this.populatePopup(popup, value);
                    }
                    protected void popupShowing() { MemoryView.this.popupShowing(); }
                    protected void popupHidden()  { MemoryView.this.popupHidden(); }
                };
                return allocView;
            case CommonConstants.INSTR_OBJECT_LIVENESS:
                if (livenessView == null) livenessView = new LivenessTableView(selection) {
                    protected void performDefaultAction(String value) {
                        if (showSourceSupported) showSource(value);
                    }
                    protected void populatePopup(JPopupMenu popup, String value) {
                        MemoryView.this.populatePopup(popup, value);
                    }
                    protected void popupShowing() { MemoryView.this.popupShowing(); }
                    protected void popupHidden()  { MemoryView.this.popupHidden(); }
                };
                return livenessView;
            default:
                if (sampledView == null) sampledView = new SampledTableView(selection) {
                    protected void performDefaultAction(String value) {
                        if (showSourceSupported) showSource(value);
                    }
                    protected void populatePopup(JPopupMenu popup, String value) {
                        MemoryView.this.populatePopup(popup, value);
                    }
                    protected void popupShowing() { MemoryView.this.popupShowing(); }
                    protected void popupHidden()  { MemoryView.this.popupHidden(); }
                };
                return sampledView;
//                return null;
        }
    }
    
    private void populatePopup(JPopupMenu popup, final String value) {
        if (showSourceSupported) {
            popup.add(new JMenuItem("Go to Source") {
                { setEnabled(value != null); setFont(getFont().deriveFont(Font.BOLD)); }
                protected void fireActionPerformed(ActionEvent e) { showSource(value); }
            });
            popup.addSeparator();
        }
        
        popup.add(new JMenuItem("Profile Class") {
            { setEnabled(value != null); }
            protected void fireActionPerformed(ActionEvent e) { profileSingle(value); }
        });
        
        popup.addSeparator();
        popup.add(new JMenuItem("Select for Profiling") {
            { setEnabled(value != null); }
            protected void fireActionPerformed(ActionEvent e) { selectForProfiling(new String[] { value }); }
        });
    }
    
    
    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        
        // TODO: read last state?
        currentView = getView();
        add(currentView, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
    
}
