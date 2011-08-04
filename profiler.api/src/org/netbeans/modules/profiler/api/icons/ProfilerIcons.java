/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api.icons;

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
    
    public static final String ATTACH = "ProfilerIcons.Attach"; // NOI18N
    public static final String ATTACH_24 = "ProfilerIcons.Attach24"; // NOI18N
    public static final String SNAPSHOTS_COMPARE = "ProfilerIcons.SnapshotsCompare"; // NOI18N
    public static final String SNAPSHOT_OPEN = "ProfilerIcons.SnapshotOpen"; // NOI18N
    public static final String SNAPSHOT_TAKE = "ProfilerIcons.SnapshotTake"; // NOI18N
    public static final String PROFILE = "ProfilerIcons.Profile"; // NOI18N
    public static final String PROFILE_24 = "ProfilerIcons.Profile24"; // NOI18N
    public static final String RESET_RESULTS = "ProfilerIcons.ResetResults"; // NOI18N
    public static final String RUN_GC = "ProfilerIcons.RunGC"; // NOI18N
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
    
    public static final String VIEW_LIVE_RESULTS_CPU_32 = "ProfilerIcons.ViewLiveResultsCpu32"; // NOI18N
    public static final String VIEW_LIVE_RESULTS_FRAGMENT_32 = "ProfilerIcons.ViewLiveResultsFragment32"; // NOI18N
    public static final String VIEW_LIVE_RESULTS_MEMORY_32 = "ProfilerIcons.ViewLiveResultsMemory32"; // NOI18N
    public static final String VIEW_THREADS_32 = "ProfilerIcons.ViewThreads32"; // NOI18N
    public static final String VIEW_TELEMETRY_32 = "ProfilerIcons.ViewTelemetry32"; // NOI18N
    
    public static final String CPU = "ProfilerIcons.Cpu"; // NOI18N
    public static final String CPU_32 = "ProfilerIcons.Cpu32"; // NOI18N
    public static final String FRAGMENT = "ProfilerIcons.Fragment"; // NOI18N
    public static final String MEMORY = "ProfilerIcons.Memory"; // NOI18N
    public static final String MEMORY_32 = "ProfilerIcons.Memory32"; // NOI18N
    
    public static final String CUSTOM_32 = "ProfilerIcons.Custom32"; // NOI18N
    public static final String MONITORING = "ProfilerIcons.Monitoring"; // NOI18N
    public static final String MONITORING_32 = "ProfilerIcons.Monitoring32"; // NOI18N
    public static final String STARTUP_32 = "ProfilerIcons.Startup32"; // NOI18N
    
}
