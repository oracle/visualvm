/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results.locks;

import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author Tomas Hurka
 */
class ThreadLockCCTNode extends LockCCTNode {

    // I18N String constants
    private static final String WAIT_MONITORS_LBL;
    private static final String WAIT_MONITORS_OWNER_LBL;
    private static final String OWNER_MONITORS_LBL;
    private static final String OWNER_MONITORS__WAIT_LBL;

    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.locks.Bundle"); // NOI18N
        WAIT_MONITORS_LBL = messages.getString("ThreadLockCCTNode_WaitMonitors"); // NOI18N
        WAIT_MONITORS_OWNER_LBL = messages.getString("ThreadLockCCTNode_WaitMonitorsOwner"); // NOI18N
        OWNER_MONITORS_LBL = messages.getString("ThreadLockCCTNode_OwnerMonitors"); // NOI18N
        OWNER_MONITORS__WAIT_LBL = messages.getString("ThreadLockCCTNode_OwnerMonitorsWait"); // NOI18N
    }

    private final ThreadInfo ti;
    private final List<ThreadInfo.MonitorDetail> waitMonitors;
    private final List<ThreadInfo.MonitorDetail> ownerMonitors;
    private MonitorsCCTNode waitNode;
    private long allTime;
    private long allCount;

    ThreadLockCCTNode(LockCCTNode parent, ThreadInfo key, List<List<ThreadInfo.MonitorDetail>> value) {
        super(parent);
        assert value.size() == 2;
        ti = key;
        waitMonitors = value.get(0);
        ownerMonitors = value.get(1);
    }

    @Override
    public String getNodeName() {
        return ti.getName();
    }

    @Override
    public long getTime() {
        if (allTime == 0) {
            summarize();
        }
        return allTime;
    }

    @Override
    public long getWaits() {
        if (allCount == 0) {
            summarize();
        }
        return allCount;
    }
    
    @Override
    public boolean isThreadLockNode() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ThreadLockCCTNode) {
            return ti.equals(((ThreadLockCCTNode)obj).ti);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ti.hashCode();
    }

    private void summarize() {
        getChildren();
        if (waitNode != null) {
            allTime = waitNode.getTime();
            allCount = waitNode.getWaits();
        }
    }

    @Override
    void computeChildren() {
        super.computeChildren();
        if (!waitMonitors.isEmpty()) {
            waitNode = new MonitorsCCTNode(this, WAIT_MONITORS_LBL, WAIT_MONITORS_OWNER_LBL, waitMonitors);
            addChild(waitNode);
        }
        if (!ownerMonitors.isEmpty()) {
            addChild(new MonitorsCCTNode(this, OWNER_MONITORS_LBL, OWNER_MONITORS__WAIT_LBL, ownerMonitors));
        }
    }
    
    class MonitorsCCTNode extends LockCCTNode {

        private final List<ThreadInfo.MonitorDetail> monitors;
        private final String name;
        private final String threadNameFormat;
        private long allTime;
        private long allCount;
        
        MonitorsCCTNode(ThreadLockCCTNode p, String n, String tnf, List<ThreadInfo.MonitorDetail> ms) {
            super(p);
            name = n;
            threadNameFormat = tnf;
            monitors = ms;
        }
        
        @Override
        public String getNodeName() {
            return name;
        }

        @Override
        public long getTime() {
            if (allTime == 0) {
                summarize();
            }
            return allTime;
       }

        @Override
        public long getWaits() {
            if (allCount == 0) {
                summarize();
            }
            return allCount;
       }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MonitorsCCTNode) {
                MonitorsCCTNode mn = (MonitorsCCTNode) obj;
                return name.equals(mn.name) && getParent().equals(mn.getParent());
            }
            return false;
        }

        @Override
        void computeChildren() {
            super.computeChildren();
            for (ThreadInfo.MonitorDetail md : monitors) {
                addChild(new MonitorDetailsCCTNode(this, threadNameFormat, md));
            }
        }

        private void summarize() {
            for (ThreadInfo.MonitorDetail md : monitors) {
                allTime += md.waitTime;
                allCount += md.count;
            }
        }        
    }
    
    class MonitorDetailsCCTNode extends LockCCTNode {
        
        private final ThreadInfo.MonitorDetail monitorDetail;
        private final String threadNameFormat;
        
        private MonitorDetailsCCTNode(LockCCTNode p, String tnf, ThreadInfo.MonitorDetail md) {
            super(p);
            threadNameFormat = tnf;
            monitorDetail = md;
        }

        @Override
        public boolean isMonitorNode() {
            return true;
        }

        @Override
        public String getNodeName() {
            return monitorDetail.monitor.getName();
        }

        @Override
        public long getTime() {
            return monitorDetail.waitTime;
        }

        @Override
        public long getWaits() {
            return monitorDetail.count;
        }

        @Override
        public int hashCode() {
            return monitorDetail.monitor.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MonitorDetailsCCTNode) {
                MonitorDetailsCCTNode mn = (MonitorDetailsCCTNode) obj;
                return monitorDetail.monitor.equals(mn.monitorDetail.monitor) && getParent().equals(mn.getParent());
            }
            return false;
        }

        @Override
        void computeChildren() {
            super.computeChildren();
             for (MonitorInfo.ThreadDetail td : monitorDetail.cloneThreadDetails()) {
                addChild(new MonitorCCTNode.ThreadDetailLockCCTNode(this, threadNameFormat, td));
            }
        }
    }
}
