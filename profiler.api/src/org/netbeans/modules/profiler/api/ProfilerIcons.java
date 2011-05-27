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
package org.netbeans.modules.profiler.api;

/**
 *
 * @author Jiri Sedlacek
 */
public interface ProfilerIcons extends Icons.Keys {
    
    public static final String NODE_FORWARD = "ProfilerIcons.NodeForward";
    public static final String NODE_REVERSE = "ProfilerIcons.NodeReverse";
    public static final String NODE_LEAF = "ProfilerIcons.NodeLeaf";
    public static final String SNAPSHOT_MEMORY_24 = "ProfilerIcons.SnapshotMemory24";
    public static final String VIEW_THREADS_24 = "ProfilerIcons.ViewThreads24";
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
    
}
