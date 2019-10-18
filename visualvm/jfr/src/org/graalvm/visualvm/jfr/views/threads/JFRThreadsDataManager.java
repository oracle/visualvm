/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.threads;

import java.util.Collection;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadData;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadsDataManager;

/**
 *
 * @author Jiri Sedlacek
 */
class JFRThreadsDataManager extends ThreadsDataManager {
    
    private long startTime;
    private long endTime;
    
    private ThreadData[] threadData = new ThreadData[0];
    
    
    void setData(long startTime, long endTime, Collection<ThreadData> threadData) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.threadData = threadData.toArray(this.threadData);
        fireDataChanged();
    }
    
    
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    public int getThreadsCount() { return threadData.length; }
    public String getThreadName(int index) { return threadData[index].getName(); }
    public String getThreadClassName(int index) { return threadData[index].getClassName(); }

    public ThreadData getThreadData(int index) { return threadData[index]; }

}
