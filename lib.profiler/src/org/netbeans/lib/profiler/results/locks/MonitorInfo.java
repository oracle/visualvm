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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.profiler.utils.StringUtils;

/**
 *
 * @author Tomas Hurka
 */
class MonitorInfo {

    final private int monitorId;
    private String className;
    private Map<ThreadInfo, OpenThread> openThreads;
    private Map<ThreadInfo, ThreadDetail> waitThreads;
    private Map<ThreadInfo, ThreadDetail> ownerThreads;

    MonitorInfo(int id) {
        monitorId = id;
        waitThreads = new HashMap();
        ownerThreads = new HashMap();
        openThreads = new HashMap();
        className = "*unknown*"; // NOI18N
    }
    
    MonitorInfo(int id, String cname) {
        this(id);
        className = StringUtils.userFormClassName(cname);
    }

    void setClassName(String cname) {
        className = StringUtils.userFormClassName(cname);
    }
    
   @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MonitorInfo) {
            MonitorInfo mi = (MonitorInfo) obj;
            return mi.monitorId == monitorId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return monitorId;
    }

    void openThread(ThreadInfo ti, ThreadInfo owner, long timeStamp0) {
        assert openThreads.get(ti) == null;
        openThreads.put(ti, new OpenThread(ti, owner, timeStamp0));
    }

    void closeThread(ThreadInfo ti, long timeStamp0) {
        OpenThread openThread = openThreads.remove(ti);
        assert openThread != null;
        long wait = timeStamp0 - openThread.timeStamp;
        addThread(waitThreads, ti, openThread.owner, wait);
        addThread(ownerThreads, openThread.owner, ti, wait);
    }

    private static void addThread(Map<ThreadInfo,ThreadDetail> threads, ThreadInfo master, ThreadInfo detail, long wait) {
        ThreadDetail td = threads.get(master);
        if (td == null) {
            td = new ThreadDetail(master);
            threads.put(master, td);
        }
        td.addWait(detail, wait);
    }

    void timeAdjust(ThreadInfo ti, long timeDiff) {
        OpenThread openThread = openThreads.get(ti);
        assert openThread != null;
        openThread.timeAdjust(timeDiff);
    }

    List<ThreadDetail> cloneWaitThreadDetails() {
        return cloneThreadDetails(waitThreads);
    }

    List<ThreadDetail> cloneOwnerThreadDetails() {
        return cloneThreadDetails(ownerThreads);
    }

    static List<ThreadDetail> cloneThreadDetails(Map<ThreadInfo,ThreadDetail> threads) {
        List details = new ArrayList(threads.size());
        for (ThreadDetail d : threads.values()) {
            details.add(new ThreadDetail(d));
        }
        return details;

    }

    String getName() {
        return new StringBuffer(className).append('(').append(Integer.toHexString(monitorId)).append(')').toString(); // NOI18N
    }

    private static class OpenThread {

        private final ThreadInfo threadInfo;
        private final ThreadInfo owner;
        private long timeStamp;

        OpenThread(ThreadInfo ti, ThreadInfo ownerTi, long ts) {
            threadInfo = ti;
            owner = ownerTi;
            timeStamp = ts;
        }

        private void timeAdjust(long timeDiff) {
            timeStamp += timeDiff;
        }
    }

    static class ThreadDetail {

        final ThreadInfo threadInfo;
        private Map<ThreadInfo, ThreadDetail> threads;
        long count;
        long waitTime;

        ThreadDetail(ThreadInfo ti) {
            threadInfo = ti;
            threads = new HashMap();
        }

        ThreadDetail(ThreadDetail d) {
            threadInfo = d.threadInfo;
            count = d.count;
            waitTime = d.waitTime;
            threads = new HashMap();
            for (ThreadDetail td : d.threads.values()) {
                threads.put(td.threadInfo, new ThreadDetail(td));
            }
        }

        List<ThreadDetail> cloneThreadDetails() {
            return MonitorInfo.cloneThreadDetails(threads);
        }

        void addWait(ThreadInfo ti, long wait) {
            waitTime += wait;
            count++;
            if (ti != null) {
                addThread(ti, wait);
            }
        }
        
        private void addThread(ThreadInfo ti, long wait) {
            ThreadDetail td = threads.get(ti);
            
            if (td == null) {
                td = new ThreadDetail(ti);
                threads.put(ti, td);
            }
            td.addWait(null, wait);
        }
    }
}
