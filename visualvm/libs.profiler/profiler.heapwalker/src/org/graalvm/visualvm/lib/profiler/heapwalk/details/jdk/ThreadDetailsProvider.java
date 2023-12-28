/*
 * Copyright (c) 1997, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.util.Locale;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public final class ThreadDetailsProvider extends DetailsProvider.Basic {
    private static final String VIRTUAL_THREAD_MASK = "java.lang.VirtualThread";                    // NOI18N

    public ThreadDetailsProvider() {
        super(Thread.class.getName() + "+", ThreadGroup.class.getName() + "+",
                VIRTUAL_THREAD_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case VIRTUAL_THREAD_MASK:
                StringBuilder sb = new StringBuilder("[#");                     // NOI18N
                sb.append(instance.getValueOfField("tid"));                     // NOI18N
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                if (!name.isEmpty()) {
                    sb.append(",");                                             // NOI18N
                    sb.append(name);
                }
                sb.append("]/");                                                // NOI18N
                Instance carrier = (Instance) instance.getValueOfField("carrierThread");  // NOI18N
                if (carrier != null) {
                    // include the carrier thread state and name when mounted
                    Instance holder = (Instance) carrier.getValueOfField("holder"); // NOI18N
                    Integer threadStatus = (Integer)holder.getValueOfField("threadStatus"); // NOI18N
                    String stateAsString = toThreadState(threadStatus.intValue()).toString();
                    sb.append(stateAsString.toLowerCase(Locale.ROOT));
                    sb.append('@');
                    sb.append(DetailsUtils.getInstanceString(carrier));
                }
                // include virtual thread state when not mounted
                if (carrier == null) {
                    String stateAsString = getThreadState(instance);
                    sb.append(stateAsString.toLowerCase(Locale.ROOT));
                }
                return sb.toString();
            default:
                return DetailsUtils.getInstanceFieldString(instance, "name");   // NOI18N
        }
    }

    /** taken from sun.misc.VM
     *
     * Returns Thread.State for the given threadStatus
     */
    public static Thread.State toThreadState(int threadStatus) {
        if ((threadStatus & JVMTI_THREAD_STATE_RUNNABLE) != 0) {
            return Thread.State.RUNNABLE;
        } else if ((threadStatus & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER) != 0) {
            return Thread.State.BLOCKED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_INDEFINITELY) != 0) {
            return Thread.State.WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT) != 0) {
            return Thread.State.TIMED_WAITING;
        } else if ((threadStatus & JVMTI_THREAD_STATE_TERMINATED) != 0) {
            return Thread.State.TERMINATED;
        } else if ((threadStatus & JVMTI_THREAD_STATE_ALIVE) == 0) {
            return Thread.State.NEW;
        } else {
            return Thread.State.RUNNABLE;
        }
    }

     /* The threadStatus field is set by the VM at state transition
     * in the hotspot implementation. Its value is set according to
     * the JVM TI specification GetThreadState function.
     */
    private final static int JVMTI_THREAD_STATE_ALIVE = 0x0001;
    private final static int JVMTI_THREAD_STATE_TERMINATED = 0x0002;
    private final static int JVMTI_THREAD_STATE_RUNNABLE = 0x0004;
    private final static int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 0x0400;
    private final static int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 0x0010;
    private final static int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 0x0020;

    private String getThreadState(Instance virtualThread) {
        Integer state = (Integer)virtualThread.getValueOfField("state");
        switch (state) {
            case NEW: return "new"; // NOI18N
            case STARTED: return "started"; //NOI18N
            case RUNNABLE: return "runnable";     // runnable-unmounted
            case RUNNING: return "running";     // runnable-mounted
            case PARKING: return "parking";
            case PARKED: return "parked";     // unmounted
            case PINNED: return "pinned";     // mounted
            case YIELDING: return "yelding";     // Thread.yield
            case TERMINATED: return "terminated";  // final state
            default: return "unknown";   // NOI18N
        }
    }

    /*
     * Virtual thread state and transitions:
     *
     *      NEW -> STARTED         // Thread.start
     *  STARTED -> TERMINATED      // failed to start
     *  STARTED -> RUNNING         // first run
     *
     *  RUNNING -> PARKING         // Thread attempts to park
     *  PARKING -> PARKED          // cont.yield successful, thread is parked
     *  PARKING -> PINNED          // cont.yield failed, thread is pinned
     *
     *   PARKED -> RUNNABLE        // unpark or interrupted
     *   PINNED -> RUNNABLE        // unpark or interrupted
     *
     * RUNNABLE -> RUNNING         // continue execution
     *
     *  RUNNING -> YIELDING        // Thread.yield
     * YIELDING -> RUNNABLE        // yield successful
     * YIELDING -> RUNNING         // yield failed
     *
     *  RUNNING -> TERMINATED      // done
     */
    private static final int NEW      = 0;
    private static final int STARTED  = 1;
    private static final int RUNNABLE = 2;     // runnable-unmounted
    private static final int RUNNING  = 3;     // runnable-mounted
    private static final int PARKING  = 4;
    private static final int PARKED   = 5;     // unmounted
    private static final int PINNED   = 6;     // mounted
    private static final int YIELDING = 7;     // Thread.yield
    private static final int TERMINATED = 99;  // final state

    // can be suspended from scheduling when unmounted
    private static final int SUSPENDED = 1 << 8;
    private static final int RUNNABLE_SUSPENDED = (RUNNABLE | SUSPENDED);
    private static final int PARKED_SUSPENDED   = (PARKED | SUSPENDED);
}
