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

import org.graalvm.visualvm.lib.jfluid.results.AbstractDataFrameProcessor;
import org.graalvm.visualvm.lib.jfluid.results.ProfilingResultListener;

/**
 *
 * @author Tomas Hurka
 */
public abstract class AbstractLockDataFrameProcessor extends AbstractDataFrameProcessor {

    protected volatile int currentThreadId = -1;

    protected void fireMonitorEntry(final int threadId, final long timeStamp0, final long timeStamp1, final int monitorId, final int ownerThreadId) {
        foreachListener(new ListenerFunctor() {
            public void execute(ProfilingResultListener listener) {
                ((LockProfilingResultListener) listener).monitorEntry(threadId, timeStamp0, timeStamp1, monitorId, ownerThreadId);
            }
        });
    }

    protected void fireMonitorExit(final int threadId, final long timeStamp0, final long timeStamp1, final int monitorId) {
        foreachListener(new ListenerFunctor() {
            public void execute(ProfilingResultListener listener) {
                ((LockProfilingResultListener) listener).monitorExit(threadId, timeStamp0, timeStamp1, monitorId);
            }
        });
    }

    protected void fireNewMonitor(final int hash, final String className) {
        foreachListener(new ListenerFunctor() {
            public void execute(ProfilingResultListener listener) {
                ((LockProfilingResultListener) listener).newMonitor(hash, className);
            }
        });
    }

    protected void fireNewThread(final int threadId, final String threadName, final String threadClassName) {
        foreachListener(new ListenerFunctor() {
            public void execute(ProfilingResultListener listener) {
                ((LockProfilingResultListener) listener).newThread(threadId, threadName, threadClassName);
            }
        });
    }

    protected void fireAdjustTime(final int threadId, final long timeStamp0, final long timeStamp1) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((LockProfilingResultListener) listener).timeAdjust(threadId, timeStamp0, timeStamp1);
                }
            });
    }
}
