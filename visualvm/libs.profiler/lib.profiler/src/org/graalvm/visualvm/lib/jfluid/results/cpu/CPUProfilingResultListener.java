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

import java.util.List;
import org.graalvm.visualvm.lib.jfluid.results.locks.LockProfilingResultListener;


/**
 *
 * @author Jaroslav Bachorik
 */
public interface CPUProfilingResultListener extends LockProfilingResultListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final int METHODTYPE_NORMAL = 1;
    static final int METHODTYPE_ROOT = 2;
    static final int METHODTYPE_MARKER = 3;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    void methodEntry(final int methodId, final int threadId, final int methodType, final long timeStamp0, final long timeStamp1,
            final List parameters, final int[] methodIds);

    void methodEntryUnstamped(final int methodId, final int threadId, final int methodType, final List parameters, final int[] methodIds);

    void methodExit(final int methodId, final int threadId, final int methodType, final long timeStamp0, final long timeStamp1, final Object retVal);

    void methodExitUnstamped(final int methodId, final int threadId, final int methodType);

    void servletRequest(final int threadId, final int requestType, final String servletPath, final int sessionId);

    void sleepEntry(final int threadId, final long timeStamp0, final long timeStamp1);

    void sleepExit(final int threadId, final long timeStamp0, final long timeStamp1);

    void threadsResume(final long timeStamp0, final long timeStamp1);

    void threadsSuspend(final long timeStamp0, final long timeStamp1);

    void waitEntry(final int threadId, final long timeStamp0, final long timeStamp1);

    void waitExit(final int threadId, final long timeStamp0, final long timeStamp1);

    void parkEntry(final int threadId, final long timeStamp0, final long timeStamp1);

    void parkExit(final int threadId, final long timeStamp0, final long timeStamp1);
}
