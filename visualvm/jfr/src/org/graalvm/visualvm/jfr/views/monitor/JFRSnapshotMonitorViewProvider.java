/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.monitor;

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
public class JFRSnapshotMonitorViewProvider extends JFRViewTabProvider {
    
    static final String EVENT_CPU_LOAD = "jdk.CPULoad"; // NOI18N
    static final String EVENT_HEAP_SUMMARY = "jdk.GCHeapSummary"; // NOI18N
    static final String EVENT_PERMGEN_SUMMARY = "http://www.oracle.com/hotspot/jvm/vm/gc/heap/perm_gen_summary"; // NOI18N
    static final String EVENT_METASPACE_SUMMARY = "jdk.MetaspaceSummary"; // NOI18N
    static final String EVENT_CLASS_LOADING = "jdk.ClassLoadingStatistics"; // NOI18N
    static final String EVENT_JAVA_THREAD = "jdk.JavaThreadStatistics"; // NOI18N
    
    
    @Override
    protected JFRViewTab createView(JFRSnapshot jfrSnapshot) {
        return new JFRSnapshotMonitorView(jfrSnapshot);
    }
    
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class PermGenChecker extends JFREventChecker {
        
        public PermGenChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] {
                EVENT_PERMGEN_SUMMARY
            };
        }
        
    }
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class MetaspaceChecker extends JFREventChecker {
        
        public MetaspaceChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] {
                EVENT_METASPACE_SUMMARY
            };
        }
        
    }
    
    @ServiceProvider(service=JFREventChecker.class)
    public static final class EventChecker extends JFREventChecker {
        
        public EventChecker() {
            super(checkedTypes());
        }
        
        static String[] checkedTypes() {
            return new String[] {
                EVENT_CPU_LOAD, EVENT_HEAP_SUMMARY, EVENT_PERMGEN_SUMMARY, EVENT_METASPACE_SUMMARY, EVENT_CLASS_LOADING, EVENT_JAVA_THREAD
            };
        }
        
    }

}
