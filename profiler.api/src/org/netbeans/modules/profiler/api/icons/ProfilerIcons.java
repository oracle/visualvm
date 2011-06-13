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
    
    public static final String NODE_FORWARD = "ProfilerIcons.NodeForward";
    public static final String NODE_REVERSE = "ProfilerIcons.NodeReverse";
    public static final String NODE_LEAF = "ProfilerIcons.NodeLeaf";
    public static final String SNAPSHOT_MEMORY_32 = "ProfilerIcons.SnapshotMemory32";
    public static final String THREAD = "ProfilerIcons.Thread";
    public static final String ALL_THREADS = "ProfilerIcons.AllThreads";
    
    public static final String ATTACH = "ProfilerIcons.Attach";
    public static final String ATTACH_24 = "ProfilerIcons.Attach24";
    public static final String SNAPSHOTS_COMPARE = "ProfilerIcons.SnapshotsCompare";
    public static final String SNAPSHOT_OPEN = "ProfilerIcons.SnapshotOpen";
    public static final String SNAPSHOT_TAKE = "ProfilerIcons.SnapshotTake";
    public static final String PROFILE = "ProfilerIcons.Profile";
    public static final String PROFILE_24 = "ProfilerIcons.Profile24";
    public static final String RESET_RESULTS = "ProfilerIcons.ResetResults";
    public static final String RUN_GC = "ProfilerIcons.RunGC";
    public static final String SNAPSHOT_HEAP = "ProfilerIcons.SnapshotHeap";
    public static final String CONTROL_PANEL = "ProfilerIcons.ControlPanel";
    public static final String LIVE_RESULTS = "ProfilerIcons.LiveResults";
    public static final String MODIFY_PROFILING = "ProfilerIcons.ModifyProfiling";
    public static final String SHOW_GRAPHS = "ProfilerIcons.ShowGraphs";
    
    public static final String SNAPSHOT_DO = "ProfilerIcons.SnapshotDO";
    public static final String SNAPSHOT_DO_32 = "ProfilerIcons.SnapshotDO32";
    public static final String SNAPSHOT_CPU_DO = "ProfilerIcons.SnapshotCpuDO";
    public static final String SNAPSHOT_CPU_DO_32 = "ProfilerIcons.SnapshotCpuDO32";
    public static final String SNAPSHOT_MEMORY_DO = "ProfilerIcons.SnapshotMemoryDO";
    public static final String SNAPSHOT_MEMORY_DO_32 = "ProfilerIcons.SnapshotMemoryDO32";
    public static final String SNAPSHOT_FRAGMENT_DO = "ProfilerIcons.SnapshotFragmentDO";
    public static final String SNAPSHOT_FRAGMENT_DO_32 = "ProfilerIcons.SnapshotFragmentDO32";
    public static final String TAKE_SNAPSHOT_CPU_32 = "ProfilerIcons.TakeSnapshotCpu32";
    public static final String TAKE_SNAPSHOT_FRAGMENT_32 = "ProfilerIcons.TakeSnapshotFragment32";
    public static final String TAKE_SNAPSHOT_MEMORY_32 = "ProfilerIcons.TakeSnapshotMemory32";
    
    public static final String TAB_BACK_TRACES = "ProfilerIcons.TabBackTraces";
    public static final String TAB_CALL_TREE = "ProfilerIcons.TabCallTree";
    public static final String TAB_COMBINED = "ProfilerIcons.TabCombined";
    public static final String TAB_HOTSPOTS = "ProfilerIcons.TabHotSpots";
    public static final String TAB_INFO = "ProfilerIcons.TabInfo";
    public static final String TAB_MEMORY_RESULTS = "ProfilerIcons.TabMemoryResults";
    public static final String TAB_STACK_TRACES = "ProfilerIcons.TabStackTraces";
    public static final String TAB_SUBTREE = "ProfilerIcons.TabSubtree";
    
    public static final String WINDOW_CONTROL_PANEL = "ProfilerIcons.WindowControlPanel";
    public static final String WINDOW_LIVE_RESULTS = "ProfilerIcons.WindowLiveResults";
    public static final String WINDOW_TELEMETRY_OVERVIEW = "ProfilerIcons.WindowTelemetryOverview";
    public static final String WINDOW_TELEMETRY = "ProfilerIcons.WindowTelemetry";
    public static final String WINDOW_THREADS = "ProfilerIcons.WindowThreads";
    
    public static final String VIEW_LIVE_RESULTS_CPU_32 = "ProfilerIcons.ViewLiveResultsCpu32";
    public static final String VIEW_LIVE_RESULTS_FRAGMENT_32 = "ProfilerIcons.ViewLiveResultsFragment32";
    public static final String VIEW_LIVE_RESULTS_MEMORY_32 = "ProfilerIcons.ViewLiveResultsMemory32";
    public static final String VIEW_THREADS_32 = "ProfilerIcons.ViewThreads32";
    public static final String VIEW_TELEMETRY_32 = "ProfilerIcons.ViewTelemetry32";
    
    public static final String CPU = "ProfilerIcons.Cpu";
    public static final String CPU_32 = "ProfilerIcons.Cpu32";
    public static final String FRAGMENT = "ProfilerIcons.Fragment";
    public static final String MEMORY = "ProfilerIcons.Memory";
    public static final String MEMORY_32 = "ProfilerIcons.Memory32";
    
    public static final String CUSTOM_32 = "ProfilerIcons.Custom32";
    public static final String MONITORING = "ProfilerIcons.Monitoring";
    public static final String MONITORING_32 = "ProfilerIcons.Monitoring32";
    public static final String STARTUP_32 = "ProfilerIcons.Startup32";
    
}
