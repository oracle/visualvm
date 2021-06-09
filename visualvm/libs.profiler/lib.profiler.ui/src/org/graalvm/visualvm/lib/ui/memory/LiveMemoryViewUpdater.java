/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.ui.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryCCTProvider;
import org.graalvm.visualvm.lib.jfluid.results.memory.MemoryResultsSnapshot;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class LiveMemoryViewUpdater {
    
    private static final int MIN_UPDATE_DIFF = 900;
    private static final int MAX_UPDATE_DIFF = 1400;
    
    
    private CCTHandler handler;
    
    private final LiveMemoryView memoryView;
    private final ProfilerClient client;
    
    private volatile boolean paused;
    private volatile boolean forceRefresh;
    
    
    public LiveMemoryViewUpdater(LiveMemoryView memoryView, ProfilerClient client) {
        this.memoryView = memoryView;
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
        if (forceRefresh || (!paused && memoryView.getLastUpdate() + MAX_UPDATE_DIFF < System.currentTimeMillis()))
            switch (client.getCurrentInstrType()) {
                case CommonConstants.INSTR_NONE_MEMORY_SAMPLING:
                    updateData();
                    break;
                case CommonConstants.INSTR_OBJECT_LIVENESS:
                case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
                    if (memoryView.getLastUpdate() + MAX_UPDATE_DIFF < System.currentTimeMillis()) {
                        client.forceObtainedResultsDump(true);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid profiling instr. type: " + client.getCurrentInstrType()); // NOI18N
            }
    }
    
    public void cleanup() {
        handler.unregisterUpdater(this);
        handler = null;
    }
    
    
    private void updateData() throws ClientUtils.TargetAppOrVMTerminated {
        if (!forceRefresh && (paused || memoryView.getLastUpdate() + MIN_UPDATE_DIFF > System.currentTimeMillis())) return;
        
        MemoryResultsSnapshot snapshot = client.getMemoryProfilingResultsSnapshot(false);

        // class names in VM format
        MemoryView.userFormClassNames(snapshot);

        // class names in VM format
        GenericFilter filter = client.getSettings().getInstrumentationFilter();
        
        memoryView.setData(snapshot, filter);
        
        forceRefresh = false;
    }
    
    private void resetData() {
        memoryView.resetData();
    }
    
    
    @ServiceProvider(service=MemoryCCTProvider.Listener.class)
    public static final class CCTHandler implements MemoryCCTProvider.Listener {

        private final List<LiveMemoryViewUpdater> updaters = new ArrayList();
        
        
        public static CCTHandler registerUpdater(LiveMemoryViewUpdater updater) {
            CCTHandler handler = Lookup.getDefault().lookup(CCTHandler.class);
            handler.updaters.add(updater);
            return handler;
        }
        
        public void unregisterUpdater(LiveMemoryViewUpdater updater) {
            updaters.remove(updater);
        }
        
        
        public void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
            if (!empty) {
                for (LiveMemoryViewUpdater updater : updaters) try {
                    updater.updateData();
                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
//                } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
                    Logger.getLogger(LiveMemoryView.class.getName()).log(Level.FINE, null, ex);
                }
            }
        }

        public void cctReset() {
            for (LiveMemoryViewUpdater updater : updaters) updater.resetData();
        }
    }
    
}
