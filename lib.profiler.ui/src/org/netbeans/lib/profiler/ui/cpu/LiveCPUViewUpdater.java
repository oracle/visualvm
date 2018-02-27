/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.cpu;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class LiveCPUViewUpdater {
    
    private static final int MIN_UPDATE_DIFF = 900;
    private static final int MAX_UPDATE_DIFF = 1400;
    
    
    private CCTHandler handler;
    
    private final LiveCPUView cpuView;
    private final ProfilerClient client;
    
    private volatile boolean paused;
    private volatile boolean forceRefresh;
    
    
    
    public LiveCPUViewUpdater(LiveCPUView cpuView, ProfilerClient client) {
        this.cpuView = cpuView;
        this.client = client;
        
        handler = CCTHandler.registerUpdater(this);
    }
    
    
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }
    
    public void update() throws ClientUtils.TargetAppOrVMTerminated {
        if (forceRefresh || (!paused && cpuView.getLastUpdate() + MAX_UPDATE_DIFF < System.currentTimeMillis()))
            client.forceObtainedResultsDump(true);
    }
    
    public void cleanup() {
        handler.unregisterUpdater(this);
        handler = null;
    }
    
    
    private void updateData() throws ClientUtils.TargetAppOrVMTerminated, CPUResultsSnapshot.NoDataAvailableException {
        if (!forceRefresh && (paused || cpuView.getLastUpdate() + MIN_UPDATE_DIFF > System.currentTimeMillis())) return;
        
        boolean sampling = client.getCurrentInstrType() == ProfilerClient.INSTR_NONE_SAMPLING;
        CPUResultsSnapshot data = client.getStatus().getInstrMethodClasses() == null ?
                           null : client.getCPUProfilingResultsSnapshot(false);
        cpuView.setData(data, sampling);
        
        forceRefresh = false;
    }
    
    private void resetData() {
        cpuView.resetData();
    }
    
    
    @ServiceProvider(service=CPUCCTProvider.Listener.class)
    public static class CCTHandler implements CPUCCTProvider.Listener {
        
        private final List<LiveCPUViewUpdater> updaters = new ArrayList();
        
        
        public static CCTHandler registerUpdater(LiveCPUViewUpdater updater) {
            CCTHandler handler = Lookup.getDefault().lookup(CCTHandler.class);
            handler.updaters.add(updater);
            return handler;
        }
        
        public void unregisterUpdater(LiveCPUViewUpdater updater) {
            updaters.remove(updater);
        }
        

        public final void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
            if (!empty) {
                for (LiveCPUViewUpdater updater : updaters) try {
                    updater.updateData();
                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
                    Logger.getLogger(LiveCPUView.class.getName()).log(Level.FINE, null, ex);
                }
            }
        }

        public final void cctReset() {
            for (LiveCPUViewUpdater updater : updaters) updater.resetData();
        }

    }
    
}
