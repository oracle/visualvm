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

/**
 *
 * @author Tomas Hurka
 */
class MonitorInfo {

    final private int monitorId;
    private String className;
    private Map<ThreadInfo, OpenThread> openThreads;
    private Map<ThreadInfo, ThreadDetail> threads;

    MonitorInfo(int id) {
        monitorId = id;
        threads = new HashMap();
        openThreads = new HashMap();
        className = "*unknown*"; // NOI18N
    }
    
    MonitorInfo(int id, String cname) {
        this(id);
        className = cname;
    }

    void setClassName(String cname) {
        className = cname;
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

    void openThread(ThreadInfo ti, long timeStamp0) {
        assert openThreads.get(ti) == null;
        openThreads.put(ti, new OpenThread(ti, timeStamp0));
    }

    void closeThread(ThreadInfo ti, long timeStamp0) {
        OpenThread openThread = openThreads.remove(ti);
        assert openThread != null;
        long wait = timeStamp0 - openThread.timeStamp;
        addThread(ti, wait);
    }

    private void addThread(ThreadInfo ti, long wait) {
        ThreadDetail td = threads.get(ti);
        if (td == null) {
            threads.put(ti, new ThreadDetail(ti, wait));
        } else {
            td.addWait(wait);
        }
    }

    void timeAdjust(ThreadInfo ti, long timeDiff) {
        OpenThread openThread = openThreads.get(ti);
        assert openThread != null;
        openThread.timeAdjust(timeDiff);
    }

    List<ThreadDetail> cloneThreadDetails() {
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
        private long timeStamp;

        OpenThread(ThreadInfo ti, long ts) {
            threadInfo = ti;
            timeStamp = ts;
        }

        private void timeAdjust(long timeDiff) {
            timeStamp += timeDiff;
        }
    }

    static class ThreadDetail {

        final ThreadInfo threadInfo;
        long count;
        long waitTime;

        private ThreadDetail(ThreadInfo ti, long wait) {
            threadInfo = ti;
            waitTime = wait;
            count = 1;
        }

        ThreadDetail(ThreadDetail d) {
            threadInfo = d.threadInfo;
            count = d.count;
            waitTime = d.waitTime;
        }

        private void addWait(long wait) {
            waitTime += wait;
            count++;
        }
    }
}
