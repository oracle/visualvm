/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.api.icons;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ProfilerIcons extends Icons.Keys {

    public static final String NODE_FORWARD = "ProfilerIcons.NodeForward"; // NOI18N
    public static final String NODE_REVERSE = "ProfilerIcons.NodeReverse"; // NOI18N
    public static final String NODE_LEAF = "ProfilerIcons.NodeLeaf"; // NOI18N
    public static final String SNAPSHOT_MEMORY_32 = "ProfilerIcons.SnapshotMemory32"; // NOI18N
    public static final String THREAD = "ProfilerIcons.Thread"; // NOI18N
    public static final String ALL_THREADS = "ProfilerIcons.AllThreads"; // NOI18N
    public static final String SQL_QUERY = "ProfilerIcons.SqlQuery"; // NOI18N

    public static final String ATTACH = "ProfilerIcons.Attach"; // NOI18N
    public static final String ATTACH_24 = "ProfilerIcons.Attach24"; // NOI18N
    public static final String SNAPSHOTS_COMPARE = "ProfilerIcons.SnapshotsCompare"; // NOI18N
    public static final String SNAPSHOT_OPEN = "ProfilerIcons.SnapshotOpen"; // NOI18N
    public static final String SNAPSHOT_TAKE = "ProfilerIcons.SnapshotTake"; // NOI18N
    public static final String PROFILE = "ProfilerIcons.Profile"; // NOI18N
    public static final String PROFILE_24 = "ProfilerIcons.Profile24"; // NOI18N
    public static final String PROFILE_INACTIVE = "ProfilerIcons.ProfileInactive"; // NOI18N
    public static final String PROFILE_RUNNING = "ProfilerIcons.ProfileRunning"; // NOI18N
    public static final String RESET_RESULTS = "ProfilerIcons.ResetResults"; // NOI18N
    public static final String RUN_GC = "ProfilerIcons.RunGC"; // NOI18N
    public static final String SNAPSHOT_THREADS = "ProfilerIcons.SnapshotThreads"; // NOI18N
    public static final String SNAPSHOT_HEAP = "ProfilerIcons.SnapshotHeap"; // NOI18N
    public static final String CONTROL_PANEL = "ProfilerIcons.ControlPanel"; // NOI18N
    public static final String LIVE_RESULTS = "ProfilerIcons.LiveResults"; // NOI18N
    public static final String MODIFY_PROFILING = "ProfilerIcons.ModifyProfiling"; // NOI18N
    public static final String SHOW_GRAPHS = "ProfilerIcons.ShowGraphs"; // NOI18N
    
    public static final String SNAPSHOT_DO = "ProfilerIcons.SnapshotDO"; // NOI18N
    public static final String SNAPSHOT_DO_32 = "ProfilerIcons.SnapshotDO32"; // NOI18N
    public static final String SNAPSHOT_CPU_DO = "ProfilerIcons.SnapshotCpuDO"; // NOI18N
    public static final String SNAPSHOT_CPU_DO_32 = "ProfilerIcons.SnapshotCpuDO32"; // NOI18N
    public static final String SNAPSHOT_MEMORY_DO = "ProfilerIcons.SnapshotMemoryDO"; // NOI18N
    public static final String SNAPSHOT_MEMORY_DO_32 = "ProfilerIcons.SnapshotMemoryDO32"; // NOI18N
    public static final String SNAPSHOT_FRAGMENT_DO = "ProfilerIcons.SnapshotFragmentDO"; // NOI18N
    public static final String SNAPSHOT_FRAGMENT_DO_32 = "ProfilerIcons.SnapshotFragmentDO32"; // NOI18N
    public static final String TAKE_SNAPSHOT_CPU_32 = "ProfilerIcons.TakeSnapshotCpu32"; // NOI18N
    public static final String TAKE_SNAPSHOT_FRAGMENT_32 = "ProfilerIcons.TakeSnapshotFragment32"; // NOI18N
    public static final String TAKE_SNAPSHOT_MEMORY_32 = "ProfilerIcons.TakeSnapshotMemory32"; // NOI18N
    public static final String TAKE_HEAP_DUMP_32 = "ProfilerIcons.TakeHeapDump32"; // NOI18N
    
    public static final String TAB_BACK_TRACES = "ProfilerIcons.TabBackTraces"; // NOI18N
    public static final String TAB_CALL_TREE = "ProfilerIcons.TabCallTree"; // NOI18N
    public static final String TAB_COMBINED = "ProfilerIcons.TabCombined"; // NOI18N
    public static final String TAB_HOTSPOTS = "ProfilerIcons.TabHotSpots"; // NOI18N
    public static final String TAB_INFO = "ProfilerIcons.TabInfo"; // NOI18N
    public static final String TAB_MEMORY_RESULTS = "ProfilerIcons.TabMemoryResults"; // NOI18N
    public static final String TAB_STACK_TRACES = "ProfilerIcons.TabStackTraces"; // NOI18N
    public static final String TAB_SUBTREE = "ProfilerIcons.TabSubtree"; // NOI18N
    
    public static final String WINDOW_CONTROL_PANEL = "ProfilerIcons.WindowControlPanel"; // NOI18N
    public static final String WINDOW_LIVE_RESULTS = "ProfilerIcons.WindowLiveResults"; // NOI18N
    public static final String WINDOW_TELEMETRY_OVERVIEW = "ProfilerIcons.WindowTelemetryOverview"; // NOI18N
    public static final String WINDOW_TELEMETRY = "ProfilerIcons.WindowTelemetry"; // NOI18N
    public static final String WINDOW_THREADS = "ProfilerIcons.WindowThreads"; // NOI18N
    public static final String WINDOW_LOCKS = "ProfilerIcons.WindowLocks"; // NOI18N
    public static final String WINDOW_SQL = "ProfilerIcons.WindowSql"; // NOI18N
    
    public static final String VIEW_LIVE_RESULTS_CPU_32 = "ProfilerIcons.ViewLiveResultsCpu32"; // NOI18N
    public static final String VIEW_LIVE_RESULTS_FRAGMENT_32 = "ProfilerIcons.ViewLiveResultsFragment32"; // NOI18N
    public static final String VIEW_LIVE_RESULTS_MEMORY_32 = "ProfilerIcons.ViewLiveResultsMemory32"; // NOI18N
    public static final String VIEW_THREADS_32 = "ProfilerIcons.ViewThreads32"; // NOI18N
    public static final String VIEW_TELEMETRY_32 = "ProfilerIcons.ViewTelemetry32"; // NOI18N
    public static final String VIEW_LOCKS_32 = "ProfilerIcons.ViewLocks32"; // NOI18N
    
    public static final String CPU = "ProfilerIcons.Cpu"; // NOI18N
    public static final String CPU_32 = "ProfilerIcons.Cpu32"; // NOI18N
    public static final String FRAGMENT = "ProfilerIcons.Fragment"; // NOI18N
    public static final String MEMORY = "ProfilerIcons.Memory"; // NOI18N
    public static final String MEMORY_32 = "ProfilerIcons.Memory32"; // NOI18N
    public static final String HEAP_DUMP = "ProfilerIcons.HeapDump"; // NOI18N
    
    public static final String CUSTOM_32 = "ProfilerIcons.Custom32"; // NOI18N
    public static final String MONITORING = "ProfilerIcons.Monitoring"; // NOI18N
    public static final String MONITORING_32 = "ProfilerIcons.Monitoring32"; // NOI18N
    public static final String STARTUP_32 = "ProfilerIcons.Startup32"; // NOI18N
    
    public static final String DELTA_RESULTS = "ProfilerIcons.DeltaResults"; // NOI18N
    
}
