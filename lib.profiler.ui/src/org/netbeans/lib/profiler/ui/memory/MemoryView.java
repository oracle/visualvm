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
import javax.swing.JPanel;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.memory.HeapHistogram;
import org.netbeans.lib.profiler.results.memory.MemoryCCTProvider;
import org.netbeans.lib.profiler.utils.StringUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class MemoryView extends JPanel {
    
    private final ProfilerClient client;
    
    private SampledTableView sampledView;
    private AllocTableView allocView;
    
    private JPanel currentView;
    
    
    public MemoryView(ProfilerClient client) {
        this.client = client;
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
            if (oacgb.getObjectsSizePerClass() != null) {
                int _nTrackedItems = status.getNInstrClasses();
                String[] _classNames = status.getClassNames();
                int[] _nTotalAllocObjects = client.getAllocatedObjectsCountResults();
                long[] _totalAllocObjectsSize = oacgb.getAllocObjectNumbers();

                if (_nTrackedItems > _nTotalAllocObjects.length)
                    _nTrackedItems = _nTotalAllocObjects.length;
                if (_nTrackedItems > _totalAllocObjectsSize.length)
                    _nTrackedItems = _totalAllocObjectsSize.length;
                
                for (int i = 0; i < _classNames.length; i++)
                    _classNames[i] = StringUtils.userFormClassName(_classNames[i]);

                allocView.setData(_nTrackedItems, _classNames, _nTotalAllocObjects,
                                  _totalAllocObjectsSize);
            }
        }
    }
    
    public void resetData() {
        if (currentView == null) return;
        
        if (currentView == sampledView) {
            sampledView.resetData();
        } else if (currentView == allocView) {
            allocView.resetData();
        }
    }
    
    
    public boolean hasSelection() {
        if (currentView == null) {
            return false;
        } else if (currentView == sampledView) {
            return sampledView.hasSelection();
        } else if (currentView == allocView) {
            return allocView.hasSelection();
        } else {
            return false;
        }
    }
    
    public String[] getSelections() {
        if (!hasSelection()) {
            return new String[0];
        } else if (currentView == sampledView) {
            return sampledView.getSelections();
        } else if (currentView == allocView) {
            return allocView.getSelections();
        } else {
            return null;
        }
    }
    
    
    private JPanel getView() {
        if (client.currentInstrTypeIsMemoryProfiling()) {
            if (allocView == null) allocView = new AllocTableView();
            return allocView;
        } else {
            if (sampledView == null) sampledView = new SampledTableView();
            return sampledView;
        }
    }
    
    private void initUI() {
        setLayout(new BorderLayout(0, 0));
    }
    
}
