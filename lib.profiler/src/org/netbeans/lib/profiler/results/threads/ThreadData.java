/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.results.threads;

import org.netbeans.lib.profiler.global.CommonConstants;
import java.awt.*;


/**
 * A representation of the thread timeline data for a single thread
 *
 * @author Misha Dmitriev
 */
public class ThreadData {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final byte NO_STATE = 127;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object dataLock = new Object();
    private String className;
    private String name;
    private byte[] threadStates; // Array of states corresponding to above timestamps
                                 // @GuardedBy dataLock

    // @GuardedBy dataLock
    private long[] timeStamps; // Array of points in time at which thread's state changes
                               // @GuardedBy dataLock

    // @GuardedBy dataLock
    private int capacity;
    private int curSize;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ThreadData(String name, String className) {
        synchronized (dataLock) {
            capacity = 50;
            timeStamps = new long[capacity];
            threadStates = new byte[capacity];
            curSize = 0;
        }

        this.name = name;
        this.className = className;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getClassName() {
        return className;
    }

    public byte getFirstState() {
        synchronized (dataLock) {
            if (curSize == 0) {
                return NO_STATE;
            } else {
                return threadStates[0];
            }
        }
    }

    public long getFirstTimeStamp() {
        synchronized (dataLock) {
            if (curSize == 0) {
                return 0;
            } else {
                return timeStamps[0];
            }
        }
    }

    public byte getLastState() {
        synchronized (dataLock) {
            if (curSize == 0) {
                return NO_STATE;
            } else {
                return threadStates[curSize - 1];
            }
        }
    }

    public long getLastTimeStamp() {
        synchronized (dataLock) {
            if (curSize == 0) {
                return 0;
            } else {
                return timeStamps[curSize - 1];
            }
        }
    }

    public String getName() {
        return name;
    }

    public byte getStateAt(int idx) {
        synchronized (dataLock) {
            return threadStates[idx];
        }
    }

    public static Color getThreadStateColor(int threadState) {
        switch (threadState) {
            case CommonConstants.THREAD_STATUS_UNKNOWN:
                return CommonConstants.THREAD_STATUS_UNKNOWN_COLOR;
            case CommonConstants.THREAD_STATUS_ZOMBIE:
                return CommonConstants.THREAD_STATUS_ZOMBIE_COLOR;
            case CommonConstants.THREAD_STATUS_RUNNING:
                return CommonConstants.THREAD_STATUS_RUNNING_COLOR;
            case CommonConstants.THREAD_STATUS_SLEEPING:
                return CommonConstants.THREAD_STATUS_SLEEPING_COLOR;
            case CommonConstants.THREAD_STATUS_MONITOR:
                return CommonConstants.THREAD_STATUS_MONITOR_COLOR;
            case CommonConstants.THREAD_STATUS_WAIT:
                return CommonConstants.THREAD_STATUS_WAIT_COLOR;
            default:
                return CommonConstants.THREAD_STATUS_UNKNOWN_COLOR;
        }
    }

    public Color getThreadStateColorAt(int idx) {
        synchronized (dataLock) {
            return getThreadStateColor(threadStates[idx]);
        }
    }

    public long getTimeStampAt(int idx) {
        synchronized (dataLock) {
            return timeStamps[idx];
        }
    }

    public void add(long timeStamp, byte threadState) {
        synchronized (dataLock) {
            if (curSize == capacity) {
                long[] oldStamps = timeStamps;
                byte[] oldStates = threadStates;
                int oldCapacity = capacity;
                capacity = capacity * 2;
                timeStamps = new long[capacity];
                threadStates = new byte[capacity];
                System.arraycopy(oldStamps, 0, timeStamps, 0, oldCapacity);
                System.arraycopy(oldStates, 0, threadStates, 0, oldCapacity);
            }

            timeStamps[curSize] = timeStamp;
            threadStates[curSize] = threadState;
            curSize++;
        }
    }

    public void clearStates() {
        synchronized (dataLock) {
            capacity = 50;
            timeStamps = new long[capacity];
            threadStates = new byte[capacity];
            curSize = 0;
        }
    }

    public int size() {
        synchronized (dataLock) {
            return curSize;
        }
    }
}
