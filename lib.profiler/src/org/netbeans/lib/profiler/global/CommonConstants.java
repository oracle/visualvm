/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.global;

import java.util.ResourceBundle;


/**
 * Various Profiler engine-internal constants used in various parts of the system.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public interface CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    public static final String THREAD_STATUS_UNKNOWN_STRING = ResourceBundle.getBundle("org.netbeans.lib.profiler.global.Bundle").getString("CommonConstants_ThreadStatusUnknownString"); // NOI18N  // TODO CHECK: unused method
    public static final String THREAD_STATUS_ZOMBIE_STRING = ResourceBundle.getBundle("org.netbeans.lib.profiler.global.Bundle").getString("CommonConstants_ThreadStatusZombieString"); // NOI18N
    public static final String THREAD_STATUS_RUNNING_STRING = ResourceBundle.getBundle("org.netbeans.lib.profiler.global.Bundle").getString("CommonConstants_ThreadStatusRunningString"); // NOI18N
    public static final String THREAD_STATUS_SLEEPING_STRING = ResourceBundle.getBundle("org.netbeans.lib.profiler.global.Bundle").getString("CommonConstants_ThreadStatusSleepingString"); // NOI18N;
    public static final String THREAD_STATUS_MONITOR_STRING = ResourceBundle.getBundle("org.netbeans.lib.profiler.global.Bundle").getString("CommonConstants_ThreadStatusMonitorString"); // NOI18N
    public static final String THREAD_STATUS_WAIT_STRING = ResourceBundle.getBundle("org.netbeans.lib.profiler.global.Bundle").getString("CommonConstants_ThreadStatusWaitString"); // NOI18N
                                                                                        // -----

    /** Names of our own classes, in various forms */
    public static final String PROFILER_SERVER_SLASHED_CLASS_PREFIX = "org/netbeans/lib/profiler/server/"; // NOI18N
    public static final String PROFILER_DOTTED_CLASS_PREFIX = "org.netbeans.lib.profiler."; // NOI18N

    // State of the profiler server
    public static final int SERVER_RUNNING = 0;
    public static final int SERVER_INITIALIZING = 1;
    public static final int SERVER_PREPARING = 2;
    public static final int SERVER_INSTRUMENTING = 3;

    // Server progress constants
    public static final int SERVER_PROGRESS_INDETERMINATE = -1;
    public static final int SERVER_PROGRESS_WORKUNITS = 100;

    // Agent state
    public static final int AGENT_STATE_NOT_RUNNING = 0;
    public static final int AGENT_STATE_READY_DYNAMIC = 1;
    public static final int AGENT_STATE_READY_DIRECT = 2;
    public static final int AGENT_STATE_CONNECTED = 3;
    public static final int AGENT_STATE_DIFFERENT_ID = 4;
    public static final int AGENT_STATE_OTHER_SESSION_IN_PROGRESS = 5;

    // Agent Id
    public static final int AGENT_ID_ANY = -1;

    /** Numeric JDK versions that we currently distinguish between */
    public static final int JDK_15 = 2;
    public static final int JDK_16 = 3; // we treat JDK 1.5 the same as Tiger for now
    public static final int JDK_17 = 4; // we treat JDK 17 the same as JDK 1.5 for now
    public static final int JDK_18 = 6;
    public static final int JDK_CVM = 5;
    public static final int JDK_UNSUPPORTED = -1;

    /** Denoting strings for JDK versions that we currently distinguish between */
    public static final String JDK_15_STRING = "jdk15"; // NOI18N
    public static final String JDK_16_STRING = "jdk16"; // NOI18N
    public static final String JDK_17_STRING = "jdk17"; // NOI18N
    public static final String JDK_18_STRING = "jdk18"; // NOI18N
    public static final String JDK_CVM_STRING = "cvm";  // NOI18N
    public static final String JDK_UNSUPPORTED_STRING = "UNSUPPORTED_JDK"; // NOI18N

    /** Constants for determining 32/64bit architecture */
    public static final int ARCH_32 = 32;
    public static final int ARCH_64 = 64;

    /** Size of the event buffer, used to store/read rough profiling data */
    public static final int EVENT_BUFFER_SIZE_IN_BYTES = 1200000;

    // Codes of various profiling events, that are generated and stored in the buffer file by server and
    // then retrieved by tool
    public static final byte ROOT_ENTRY = 1;
    public static final byte ROOT_EXIT = 2;
    public static final byte MARKER_ENTRY = 3;
    public static final byte MARKER_EXIT = 4;
    public static final byte ADJUST_TIME = 5;
    public static final byte METHOD_ENTRY = 6;
    public static final byte METHOD_EXIT = 7;
    public static final byte THREADS_SUSPENDED = 8;
    public static final byte THREADS_RESUMED = 9;
    public static final byte RESET_COLLECTORS = 10;
    public static final byte NEW_THREAD = 11;
    public static final byte OBJ_ALLOC_STACK_TRACE = 12;
    public static final byte SET_FOLLOWING_EVENTS_THREAD = 13;
    public static final byte OBJ_LIVENESS_STACK_TRACE = 14;
    public static final byte OBJ_GC_HAPPENED = 15;

    // These are used by hybrid (instrumentation-sampling) CPU profiling
    public static final byte METHOD_ENTRY_UNSTAMPED = 16;
    public static final byte METHOD_EXIT_UNSTAMPED = 17;
    public static final byte MARKER_ENTRY_UNSTAMPED = 18;
    public static final byte MARKER_EXIT_UNSTAMPED = 19;
    public static final byte METHOD_ENTRY_WAIT = 20;
    public static final byte METHOD_EXIT_WAIT = 21;
    public static final byte METHOD_ENTRY_MONITOR = 22;
    public static final byte METHOD_EXIT_MONITOR = 23;
    public static final byte METHOD_ENTRY_SLEEP = 24;
    public static final byte METHOD_EXIT_SLEEP = 25;
    public static final byte BUFFEREVENT_PROFILEPOINT_HIT = 26;
    public static final byte SERVLET_DO_METHOD = 27;
    public static final byte THREAD_DUMP_START = 28;
    public static final byte THREAD_DUMP_END = 29;
    public static final byte THREAD_INFO_IDENTICAL = 30;
    public static final byte THREAD_INFO = 31;

    // The following are used when storing unstamped method entry/exit events in the "compact" format, when both
    // event code and method id are packed in a single char. See more comments in ProfilerRuntimeCPUSampledInstr.java
    public static final char MAX_METHOD_ID_FOR_COMPACT_FORMAT = 0x3FFF;
    public static final byte COMPACT_EVENT_FORMAT_BYTE_MASK = (byte) 0x80;
    public static final char METHOD_ENTRY_COMPACT_MASK = 0x8000;
    public static final byte METHOD_ENTRY_COMPACT_BYTE_MASK = (byte) 0x80;
    public static final char METHOD_EXIT_COMPACT_MASK = 0xC000;
    public static final byte METHOD_EXIT_COMPACT_BYTE_MASK = (byte) 0xC0;
    public static final char COMPACT_EVENT_METHOD_ID_MASK = 0x3FFF;

    // Target app instrumentation types
    public static final int INSTR_NONE = 0; // no instrumentation performed => no profiling data
    public static final int INSTR_CODE_REGION = 1; // instrument code region for CPU data
    public static final int INSTR_NONE_SAMPLING = 2; // no instrumentation performed, CPU sampling will be used
    public static final int INSTR_RECURSIVE_FULL = 3; // instrument for CPU data, full instrumentation (timestamps for each method entry/exit)
    public static final int INSTR_RECURSIVE_SAMPLED = 4; // instrument for CPU data, sampled data obtained (timestamps at periodic intervals only)
    public static final int INSTR_OBJECT_ALLOCATIONS = 5; // instrument for Memory data, allocations only
    public static final int INSTR_OBJECT_LIVENESS = 6; // instrument for Memory data, complete
    public static final int INSTR_NONE_MEMORY_SAMPLING = 7; // no instrumentation performed, Memory sampling will be used (live instances at periodic intervals only)
                                                       // These are just helpful constants, not actual instrumentation types
    public static final int INSTR_MEMORY_BASE = 5;
    public static final int INSTR_MAXNUMBER = 7;

    // Constants used to distinguish between "full" and "sampled" CPU instrumentation in the settings.
    // Internally they are translated into INSTR_RECURSIVE and INSTR_RECURSIVE_SAMPLED, respectively
    // CPU_SAMPLED is used for pure sampling without instrumentation, internally CPU_SAMPLED is 
    // translated to INSTR_NONE_SAMPLING
    public static final int CPU_INSTR_FULL = 0;
    public static final int CPU_INSTR_SAMPLED = 1;
    public static final int CPU_SAMPLED = 2;

    // Target app instrumentation schemes for CPU profiling
    public static final int INSTRSCHEME_LAZY = 1; // Lazy scheme (B in the NB Profiler papers/report)
    public static final int INSTRSCHEME_EAGER = 2; // Eager scheme (A in the above report)
    public static final int INSTRSCHEME_TOTAL = 3; // Total instrumentation

    // Names of special Java threads created by NB Profiler
    public static final String PROFILER_SERVER_THREAD_NAME = "*** Profiler Agent Communication Thread"; // NOI18N
    public static final String PROFILER_SPECIAL_EXEC_THREAD_NAME = "*** Profiler Agent Special Execution Thread"; // NOI18N
    public static final String PROFILER_SEPARATE_EXEC_THREAD_NAME = "*** JFluid Separate Command Execution Thread"; // NOI18N

    // Calibration-only run pseudo main class name
    public static final String CALIBRATION_PSEUDO_CLASS_NAME = "____Profiler+Calibration+Run____"; // NOI18N

    // These are used to signal to server that there is no defined root class/method to instrument. Instead, we instrument
    // run() methods of all threads spawned after instrumentation command is received.
    public static final String NO_CLASS_NAME = "*NO_CLASS_NAME*"; // NOI18N
    public static final String NO_METHOD_NAME = "*NO_METHOD_NAME*"; // NOI18N
    public static final String NO_METHOD_SIGNATURE = "*NO_METHOD_SIGNATURE*"; // NOI18N

    // Stuff used by various instrumentation-related classes
    static final String JAVA_LANG_REFLECT_METHOD_DOTTED_CLASS_NAME = "java.lang.reflect.Method"; // NOI18N
    static final String JAVA_LANG_REFLECT_METHOD_SLASHED_CLASS_NAME = "java/lang/reflect/Method"; // NOI18N
    static final String INVOKE_METHOD_NAME = "invoke"; // NOI18N
    static final String INVOKE_METHOD_SIGNATURE = "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;"; // NOI18N

    // Kinds of injections that we make into TA methods
    public static final int INJ_RECURSIVE_NORMAL_METHOD = 0;
    public static final int INJ_RECURSIVE_ROOT_METHOD = 1;
    public static final int INJ_RECURSIVE_MARKER_METHOD = 2;
    public static final int INJ_RECURSIVE_SAMPLED_NORMAL_METHOD = 3;
    public static final int INJ_RECURSIVE_SAMPLED_ROOT_METHOD = 4;
    public static final int INJ_RECURSIVE_SAMPLED_MARKER_METHOD = 5;
    public static final int INJ_REFLECT_METHOD_INVOKE = 6;
    public static final int INJ_SERVLET_DO_METHOD = 7;
    public static final int INJ_CODE_REGION = 8;
    public static final int INJ_OBJECT_ALLOCATIONS = 9;
    public static final int INJ_OBJECT_LIVENESS = 10;
    public static final int INJ_STACKMAP = 11;
    public static final int INJ_THROWABLE = 12;
    public static final int INJ_MAXNUMBER = 13;

    // Thread state constants. We currently use what is provided in JVMDI, and will probably have to change that once
    // we switch to JVMTI in JDK 5.0
    public static final byte THREAD_STATUS_UNKNOWN = -1; // Thread status is unknown.
    public static final byte THREAD_STATUS_ZOMBIE = 0; // Thread is waiting to die. Also used for "doesn't exist yet" and "dead"
    public static final byte THREAD_STATUS_RUNNING = 1; // Thread is runnable. Note that we unfortunately don't know whether it'
                                                        // s actually running or pre-empted by another thread...
    public static final byte THREAD_STATUS_SLEEPING = 2; // Thread is sleeping - Thread.sleep() or JVM_Sleep() was called
    public static final byte THREAD_STATUS_MONITOR = 3; // Thread is waiting on a java monitor
    public static final byte THREAD_STATUS_WAIT = 4; // Thread is waiting - Thread.wait() or JVM_MonitorWait() was called

    // Thread state color constants.

    /** Thread status is unknown. */
    public static final java.awt.Color THREAD_STATUS_UNKNOWN_COLOR = java.awt.Color.LIGHT_GRAY;

    /** Thread is waiting to die. Also used for "doesn't exist yet" and "dead" */
    public static final java.awt.Color THREAD_STATUS_ZOMBIE_COLOR = java.awt.Color.BLACK;

    /** Thread is runnable. Note that we unfortunately don't know whether it's actually running or
     * pre-empted by another thread...*/
    public static final java.awt.Color THREAD_STATUS_RUNNING_COLOR = new java.awt.Color(58, 228, 103);

    /** Thread is sleeping - Thread.sleep() or JVM_Sleep() was called */
    public static final java.awt.Color THREAD_STATUS_SLEEPING_COLOR = new java.awt.Color(155, 134, 221);

    /** Thread is waiting on a java monitor */
    public static final java.awt.Color THREAD_STATUS_MONITOR_COLOR = new java.awt.Color(255, 114, 102);

    /** Thread is waiting - Thread.wait() or JVM_MonitorWait() was called */
    public static final java.awt.Color THREAD_STATUS_WAIT_COLOR = new java.awt.Color(255, 228, 90);

    // Thread state description constants.
    // see I18N String constants at the top of this file

    // Modes for obtaining Thread states data
    public static final int MODE_THREADS_NONE = 0;
    public static final int MODE_THREADS_SAMPLING = 1;
    public static final int MODE_THREADS_EXACT = 2;
    
    // Constants for results filter types
    public static final int FILTER_NONE = 0;
    public static final int FILTER_STARTS_WITH = 10;
    public static final int FILTER_CONTAINS = 20;
    public static final int FILTER_NOT_CONTAINS = 25;
    public static final int FILTER_ENDS_WITH = 30;
    public static final int FILTER_EQUALS = 40;
    public static final int FILTER_REGEXP = 50;

    // Default sorting column constant, means that target component decides itself which column will be used for sorting
    public static final int SORTING_COLUMN_DEFAULT = -1;

    // Miscellaneous
    public static final String ENGINE_WARNING = "*** Profiler engine warning: "; // NOI18N
    public static final String PLEASE_REPORT_PROBLEM = "*** Please report this problem to feedback@profiler.netbeans.org"; // NOI18N

    // Agent versioning
    public static final int AGENT_VERSION_10_M9 = 1;
    public static final int AGENT_VERSION_10_M10 = 2;
    public static final int AGENT_VERSION_60_M5 = 3;
    public static final int AGENT_VERSION_60_M6 = 4;
    public static final int AGENT_VERSION_60_M7 = 5;
    public static final int AGENT_VERSION_60_M8 = 6;
    public static final int AGENT_VERSION_60_M10 = 7;
    public static final int AGENT_VERSION_60_BETA1 = 8;
    public static final int AGENT_VERSION_67_BETA = 9;
    public static final int AGENT_VERSION_69 = 10;
    public static final int AGENT_VERSION_610_M2 = 11;
    public static final int AGENT_VERSION_71 = 12;
    public static final int AGENT_VERSION_73 = 13;
    public static final int CURRENT_AGENT_VERSION = AGENT_VERSION_73;
}
