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

package org.graalvm.visualvm.lib.jfluid.results.cpu;

import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.CCTFlattener;

//import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.NodeMarker;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNodeProcessor;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.CCTResultsFilter;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.TimeCollector;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.SimpleCPUCCTNode;


/**
 *
 * @author Jaroslav Bachorik
 */
public class FlatProfileBuilder implements FlatProfileProvider, CPUCCTProvider.Listener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(FlatProfileBuilder.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private CCTFlattener flattener;
    private FlatProfileContainer lastFlatProfile = null;
    private ProfilerClient client;
    private SimpleCPUCCTNode appNode;

    private TimeCollector collector = null;
    private CCTResultsFilter filter = null;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setContext(ProfilerClient client, TimeCollector collector, CCTResultsFilter filter) {
        if (this.client != null) {
            this.collector = null;
            this.filter = null;
            this.client.registerFlatProfileProvider(null);
        }

        if (client != null) {
            this.collector = collector;
            this.filter = filter;
            flattener = new CCTFlattener(client, filter);
            client.registerFlatProfileProvider(this);
        } else {
            flattener = null;
        }

        this.client = client;
        appNode = null;
    }

    public synchronized void cctEstablished(RuntimeCCTNode appRootNode, boolean empty) {
        if (empty) return;
        
        if (appRootNode instanceof SimpleCPUCCTNode) {
            appNode = (SimpleCPUCCTNode) appRootNode;
        } else {
            appNode = null;
        }
    }

    public synchronized void cctReset() {
        appNode = null;
    }

    public synchronized FlatProfileContainer createFlatProfile() {
        if (appNode == null) {
            return null;
        }

        client.getStatus().beginTrans(false);

        try {
            RuntimeCCTNodeProcessor.process(
                appNode, 
                filter,
                flattener,
                collector
            );
            
            lastFlatProfile = flattener.getFlatProfile();

        } finally {
            client.getStatus().endTrans();
        }

        return lastFlatProfile;
    }
}
