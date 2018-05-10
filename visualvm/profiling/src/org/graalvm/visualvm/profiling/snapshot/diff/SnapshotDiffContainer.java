/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.profiling.snapshot.diff;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.profiling.snapshot.ProfilerSnapshot;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.AllocMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.LivenessMemoryResultsSnapshot;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsDiff;
import org.netbeans.lib.profiler.results.memory.SampledMemoryResultsSnapshot;
import org.netbeans.modules.profiler.LoadedSnapshot;

/**
 *
 * @author Jiri Sedlacek
 */
final class SnapshotDiffContainer extends DataSource {
    
    private ResultsSnapshot diffSnapshot;
    private ProfilerSnapshot snapshot1;
    private ProfilerSnapshot snapshot2;
    
    public SnapshotDiffContainer(ProfilerSnapshot ps1, ProfilerSnapshot ps2, DataSource master) {
        super(master);
        diffSnapshot = createDiff(ps1, ps2);
        if (diffSnapshot == null) throw new UnsupportedOperationException(
                                  "Unable to create diff from " + ps1 + " and " + ps2); // NOI18N
        snapshot1 = ps1;
        snapshot2 = ps2;
    }
    
    
    public ResultsSnapshot getDiff() {
        return diffSnapshot;
    }
    
    public ProfilerSnapshot getSnapshot1() {
        return snapshot1;
    }
    
    public ProfilerSnapshot getSnapshot2() {
        return snapshot2;
    }
    
    
    private static ResultsSnapshot createDiff(ProfilerSnapshot ps1, ProfilerSnapshot ps2) {
        LoadedSnapshot s1 = ps1.getLoadedSnapshot();
        LoadedSnapshot s2 = ps2.getLoadedSnapshot();
        
        if (s1.getSnapshot() instanceof AllocMemoryResultsSnapshot &&
            s2.getSnapshot() instanceof AllocMemoryResultsSnapshot)
            return new AllocMemoryResultsDiff((AllocMemoryResultsSnapshot) s1.getSnapshot(),
                                              (AllocMemoryResultsSnapshot) s2.getSnapshot());
        else if (s1.getSnapshot() instanceof LivenessMemoryResultsSnapshot &&
                 s2.getSnapshot() instanceof LivenessMemoryResultsSnapshot)
            return new LivenessMemoryResultsDiff((LivenessMemoryResultsSnapshot) s1.getSnapshot(),
                                                 (LivenessMemoryResultsSnapshot) s2.getSnapshot());
        else if (s1.getSnapshot() instanceof SampledMemoryResultsSnapshot &&
                 s2.getSnapshot() instanceof SampledMemoryResultsSnapshot)
            return new SampledMemoryResultsDiff((SampledMemoryResultsSnapshot) s1.getSnapshot(),
                                                 (SampledMemoryResultsSnapshot) s2.getSnapshot());
        return null;
    }

}
