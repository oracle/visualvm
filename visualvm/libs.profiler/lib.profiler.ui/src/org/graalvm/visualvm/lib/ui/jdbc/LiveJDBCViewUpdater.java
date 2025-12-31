/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.jdbc.JdbcResultsSnapshot;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri
 */
public class LiveJDBCViewUpdater {

    private static final int MIN_UPDATE_DIFF = 900;
    private static final int MAX_UPDATE_DIFF = 1400;


    private CCTHandler handler;

    private final LiveJDBCView jdbcView;
    private final ProfilerClient client;

    private volatile boolean paused;
    private volatile boolean forceRefresh;



    public LiveJDBCViewUpdater(LiveJDBCView jdbcView, ProfilerClient client) {
        this.jdbcView = jdbcView;
        this.client = client;
    }



    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public void update() throws ClientUtils.TargetAppOrVMTerminated {
        if (handler == null) handler = CCTHandler.registerUpdater(this);

        if (forceRefresh || (!paused && jdbcView.getLastUpdate() + MAX_UPDATE_DIFF < System.currentTimeMillis()))
            client.forceObtainedResultsDump(true);
    }

    public void cleanup() {
        if (handler != null) handler.unregisterUpdater(this);
        handler = null;
    }


    private void updateData() throws ClientUtils.TargetAppOrVMTerminated {
        if (!forceRefresh && (paused || jdbcView.getLastUpdate() + MIN_UPDATE_DIFF > System.currentTimeMillis())) return;
        
        JdbcResultsSnapshot data = client.getStatus().getInstrMethodClasses() == null ?
                            null : client.getJdbcProfilingResultsSnapshot(false);
        jdbcView.setData(data);
        
        forceRefresh = false;
    }
    
    private void resetData() {
        jdbcView.resetData();
    }
    
    
    @ServiceProvider(service=JdbcCCTProvider.Listener.class)
    public static class CCTHandler implements JdbcCCTProvider.Listener {
        
        private final List<LiveJDBCViewUpdater> updaters = new ArrayList<>();
        
        
        public static CCTHandler registerUpdater(LiveJDBCViewUpdater updater) {
            CCTHandler handler = Lookup.getDefault().lookup(CCTHandler.class);
            
            if (handler.updaters.isEmpty()) {
                Collection<? extends JdbcCCTProvider> jdbcCCTProviders = Lookup.getDefault().lookupAll(JdbcCCTProvider.class);
                assert !jdbcCCTProviders.isEmpty();
                for (JdbcCCTProvider provider : jdbcCCTProviders) provider.addListener(handler);
            }
            
            handler.updaters.add(updater);
            return handler;
        }
        
        public void unregisterUpdater(LiveJDBCViewUpdater updater) {
            updaters.remove(updater);
            
            if (updaters.isEmpty()) {
                Collection<? extends JdbcCCTProvider> jdbcCCTProviders = Lookup.getDefault().lookupAll(JdbcCCTProvider.class);
                assert !jdbcCCTProviders.isEmpty();
                for (JdbcCCTProvider provider : jdbcCCTProviders) provider.removeListener(this);
            }
        }
        

        public final void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
           if (!empty) {
                for (LiveJDBCViewUpdater updater : updaters) try {
                    updater.updateData();
                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                    Logger.getLogger(LiveJDBCView.class.getName()).log(Level.FINE, null, ex);
                }
            }
        }

        public final void cctReset() {
            for (LiveJDBCViewUpdater updater : updaters) updater.resetData();
        }

    }
    
}
