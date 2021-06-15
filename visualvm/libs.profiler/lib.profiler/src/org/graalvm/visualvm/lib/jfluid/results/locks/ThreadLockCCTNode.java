/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.locks;

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
        ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.locks.Bundle"); // NOI18N
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
    
    static class MonitorsCCTNode extends LockCCTNode {

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
    
    static class MonitorDetailsCCTNode extends LockCCTNode {
        
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
