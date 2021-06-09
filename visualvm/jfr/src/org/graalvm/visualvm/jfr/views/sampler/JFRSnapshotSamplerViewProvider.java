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

package org.graalvm.visualvm.jfr.views.sampler;

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
public final class JFRSnapshotSamplerViewProvider extends JFRViewTabProvider {
    
    static final String EVENT_EXECUTION_SAMPLE = "jdk.ExecutionSample"; // NOI18N
    static final String EVENT_NATIVE_SAMPLE = "jdk.NativeMethodSample"; // NOI18N
    static final String EVENT_THREAD_CPU = "jdk.ThreadCPULoad"; // NOI18N
    static final String EVENT_OBJECT_COUNT = "jdk.ObjectCount"; // NOI18N
    static final String EVENT_THREAD_ALLOCATIONS = "jdk.ThreadAllocationStatistics"; // NOI18N
    
    
    protected JFRViewTab createView(JFRSnapshot jfrSnapshot) {
        return new JFRSnapshotSamplerView(jfrSnapshot);
    }
    
    
//    @ServiceProvider(service=JFREventChecker.class)
//    public static final class CPUSampleChecker extends JFREventChecker {
//        
//        public CPUSampleChecker() {
//            super(checkedTypes());
//        }
//        
//        static String[] checkedTypes() {
//            return new String[] { EVENT_EXECUTION_SAMPLE, EVENT_NATIVE_SAMPLE };
//        }
//        
//    }
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class ThreadCPUChecker extends JFREventChecker {
        
        public ThreadCPUChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] { EVENT_THREAD_CPU };
        }
        
    }    
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class ObjectCountChecker extends JFREventChecker {
        
        public ObjectCountChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] { EVENT_OBJECT_COUNT };
        }
        
    }
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class ThreadAllocationsChecker extends JFREventChecker {
        
        public ThreadAllocationsChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] { EVENT_THREAD_ALLOCATIONS };
        }
        
    }

}
