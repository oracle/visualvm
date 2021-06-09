/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.gc;

import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFREventChecker;
import org.graalvm.visualvm.jfr.view.JFRViewTab;
import org.graalvm.visualvm.jfr.view.JFRViewTabProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=JFRViewTabProvider.class)
public final class JFRSnapshotGcViewProvider extends JFRViewTabProvider {
    
    static final String EVENT_GARBAGE_COLLECTION = "jdk.GarbageCollection"; // NOI18N
    static final String EVENT_GC_CONFIGURATION = "jdk.GCConfiguration"; // NOI18N
    static final String EVENT_HEAP_CONFIGURATION = "jdk.GCHeapConfiguration"; // NOI18N
    static final String EVENT_YOUNG_GEN_CONFIGURATION = "jdk.YoungGenerationConfiguration"; // NOI18N
    static final String EVENT_SURVIVOR_CONFIGURATION = "jdk.GCSurvivorConfiguration"; // NOI18N
    static final String EVENT_TLAB_CONFIGURATION = "jdk.GCTLABConfiguration"; // NOI18N
    
    static final String PREFIX_GCPHASE = "jdk.GCPhase"; // NOI18
    static final String EVENT_GCPHASE_CONCURRENT = "jdk.GCPhaseConcurrent"; // NOI18
    static final String EVENT_GCPHASE_PARALLEL = "jdk.GCPhaseParallel"; // NOI18
    static final String EVENT_GCPHASE_PAUSE = "jdk.GCPhasePause"; // NOI18N
    static final String EVENT_GCPHASE_PAUSE_LVL1 = "jdk.GCPhasePauseLevel1"; // NOI18N
    static final String EVENT_GCPHASE_PAUSE_LVL2 = "jdk.GCPhasePauseLevel2"; // NOI18N
    static final String EVENT_GCPHASE_PAUSE_LVL3 = "jdk.GCPhasePauseLevel3"; // NOI18N
    static final String EVENT_GCPHASE_PAUSE_LVL4 = "jdk.GCPhasePauseLevel4"; // NOI18N
    
    
    protected JFRViewTab createView(JFRSnapshot jfrSnapshot) {
        return new JFRSnapshotGcView(jfrSnapshot);
    }
    
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class EventChecker extends JFREventChecker {
        
        public EventChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] {
                EVENT_GARBAGE_COLLECTION,
                EVENT_GC_CONFIGURATION,
                EVENT_YOUNG_GEN_CONFIGURATION,
                EVENT_SURVIVOR_CONFIGURATION,
                EVENT_TLAB_CONFIGURATION
            };
        }
        
    }
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class EventChecker_Phases extends JFREventChecker {
        
        public EventChecker_Phases() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] {
                EVENT_GCPHASE_CONCURRENT,
                EVENT_GCPHASE_PARALLEL,
                EVENT_GCPHASE_PAUSE,
                EVENT_GCPHASE_PAUSE_LVL1,
                EVENT_GCPHASE_PAUSE_LVL2,
                EVENT_GCPHASE_PAUSE_LVL3,
                EVENT_GCPHASE_PAUSE_LVL4
            };
        }
        
    }
    
}
