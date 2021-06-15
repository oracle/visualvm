/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.lib.ui.cpu;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.cpu.CPUResultsSnapshot;
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
