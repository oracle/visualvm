/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.global.TransactionalSupport;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ThreadInfos {

    public ThreadInfo[] threadInfos;
    String[] threadNames;
    String[] threadClassNames;
    int threadInfosLastIdx;
    private TransactionalSupport transaction = new TransactionalSupport();

    public ThreadInfos() {
        reset();
    }

    public void beginTrans(boolean mutable) {
        transaction.beginTrans(mutable);
    }

    boolean beginTrans(boolean mutable, boolean failEarly) {
        return transaction.beginTrans(mutable, failEarly);
    }

    public void endTrans() {
        transaction.endTrans();
    }

    public boolean isEmpty() {
        beginTrans(false);
        try {
            if ((threadInfos == null) || (threadInfos.length == 0)) {
                return true;
            }
            for (int i = 0; i < threadInfos.length; i++) {
                if ((threadInfos[i] != null) && (threadInfos[i].stack != null) && (threadInfos[i].stack[0] != null) && (threadInfos[i].stack[0].getChildren() != null) && (threadInfos[i].stack[0].getChildren().length > 0)) {
                    return false;
                }
            }
            return true;
        } finally {
            endTrans();
        }
    }

    public String[] getThreadNames() {
        beginTrans(false);
        try {
            return threadNames;
        } finally {
            endTrans();
        }
    }

    public void newThreadInfo(int threadId, String threadName, String threadClassName) {
        beginTrans(true);
        try {
            if ((threadId > threadInfosLastIdx) || (threadInfos == null)) {
                int newLen = threadId + 1;
                ThreadInfo[] newInfos = new ThreadInfo[newLen];
                String[] newNames = new String[newLen];
                String[] newClassNames = new String[newLen];
                if (threadInfos != null) {
                    System.arraycopy(threadInfos, 0, newInfos, 0, threadInfos.length);
                    System.arraycopy(threadNames, 0, newNames, 0, threadNames.length);
                    System.arraycopy(threadClassNames, 0, newClassNames, 0, threadNames.length);
                }
                threadInfos = newInfos;
                threadNames = newNames;
                threadClassNames = newClassNames;
                threadInfosLastIdx = threadId;
            }
            if (threadInfos[threadId] == null) {
                threadInfos[threadId] = new ThreadInfo(threadId);
                threadNames[threadId] = threadName;
                threadClassNames[threadId] = threadClassName;
            }
        } finally {
            endTrans();
        }
    }

    public void reset() {
        beginTrans(true);
        try {
            threadInfos = null;
            threadNames = null;
            threadClassNames = null;
            threadInfosLastIdx = -1;
        } finally {
            endTrans();
        }
    }
}
