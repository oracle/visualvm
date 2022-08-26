/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @author Tomas Hurka
 */
final class ThreadInfos {

    ThreadInfo[] threadInfos;
    int threadInfosLastIdx;
    ThreadInfo unknowThread = new ThreadInfo(-1, "Unknown", "N/A");     // NOI18N

    ThreadInfos() {
        reset();
    }

    boolean isEmpty() {
        if ((threadInfos == null) || (threadInfos.length == 0)) {
            return true;
        }
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo != null) {
                return false;
            }
        }
        return true;
    }

    void newThreadInfo(int threadId, String threadName, String threadClassName) {
        if ((threadId > threadInfosLastIdx) || (threadInfos == null)) {
            int newLen = threadId + 1;
            ThreadInfo[] newInfos = new ThreadInfo[newLen];
            if (threadInfos != null) {
                System.arraycopy(threadInfos, 0, newInfos, 0, threadInfos.length);
            }
            threadInfos = newInfos;
            threadInfosLastIdx = threadId;
        }
        threadInfos[threadId] = new ThreadInfo(threadId, threadName, threadClassName);
    }

    void reset() {
        threadInfos = null;
        threadInfosLastIdx = -1;
    }

    ThreadInfo getThreadInfo(int threadId) {
        if (threadId == -1) return unknowThread;
        if (!isEmpty()) {
            return threadInfos[threadId];
        }
        return null;
    }
}
