/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCallGraphBuilder.ThreadInfo;
import org.netbeans.lib.profiler.results.cpu.cct.CCTFlattener;
import org.netbeans.lib.profiler.results.cpu.cct.CompositeCPUCCTWalker;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;

//import org.netbeans.lib.profiler.results.cpu.cct.NodeMarker;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.results.cpu.cct.CCTResultsFilter;
import org.netbeans.lib.profiler.results.cpu.cct.TimeCollector;


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
    private RuntimeCPUCCTNode appNode;

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
        
        if (appRootNode instanceof RuntimeCPUCCTNode) {
            appNode = (RuntimeCPUCCTNode) appRootNode;
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
            if (ThreadInfo.beginTrans(false, true)) {
                try {
                    CompositeCPUCCTWalker walker = new CompositeCPUCCTWalker();
                    int index = 0;
                    
                    if (filter != null) {
                        walker.add(index++, filter);
                    }
                    walker.add(index++, flattener);
                    
                    if (collector != null) {
                        walker.add(index++ , collector);
                    }

                    walker.walk(appNode);

                    lastFlatProfile = flattener.getFlatProfile();
                } finally {
                    ThreadInfo.endTrans();
                }
            }
        } finally {
            client.getStatus().endTrans();
        }

        return lastFlatProfile;
    }
}
