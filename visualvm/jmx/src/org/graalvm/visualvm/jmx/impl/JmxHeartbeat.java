/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jmx.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class JmxHeartbeat {
    
    private static final int MAX_HEARTBEAT_THREADS = Integer.getInteger("org.graalvm.visualvm.jmx.JmxHeartbeatThreads", 10); // NOI18N
    
    private static final int LAZY_HEARTBEAT_DELAY = Integer.getInteger("org.graalvm.visualvm.jmx.HeartbeatDelay", 5000); // NOI18N
    private static final int IMMEDIATE_HEARTBEAT_DELAY = Integer.getInteger("org.graalvm.visualvm.jmx.ImmediateHeartbeatDelay", 100); // NOI18N
    
    
    private static final JmxHeartbeat LAZY = new Lazy();
    private static final JmxHeartbeat IMMEDIATE = new Immediate(LAZY);
        
        
    private final String id;
    private final int delay;


    private JmxHeartbeat(String id, int delay) {
        this.id = id;
        this.delay = delay;
    }
    
    
    static void scheduleImmediately(JmxApplication... apps) {
        IMMEDIATE.schedule(apps);
    }
    
    static void scheduleLazily(JmxApplication... apps) {
        LAZY.schedule(apps);
    }


    protected abstract void schedule(JmxApplication... apps);
    
    protected abstract void pingFinished(JmxApplication... unresolved);


    protected final void pingApps(Collection<JmxApplication> applications) {
        int count = applications.size();
        
//        System.err.println(">>> " + id + " Heartbeat for " + count + " targets at " + java.time.LocalTime.now() + ": " + applications);
        
        final AtomicInteger counter = new AtomicInteger(count);
        final Collection<JmxApplication> unresolved = Collections.synchronizedList(new ArrayList());
        RequestProcessor processor = new RequestProcessor("JMX " + id + " Heartbeat Processor", Math.min(count, MAX_HEARTBEAT_THREADS)); // NOI18N
        
        for (final JmxApplication app : applications) {
            processor.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!app.tryConnect()) unresolved.add(app);
                    } finally {
                        if (counter.decrementAndGet() == 0) pingFinished(unresolved.toArray(new JmxApplication[0]));
                    }
                }
            }, delay);
        }
    }


    protected static void cleanupUnavailableApps(Collection<JmxApplication> apps, boolean checkHeartbeat) {
        Iterator<JmxApplication> appsI = apps.iterator();
        while (appsI.hasNext()) {
            JmxApplication app = appsI.next();
            if (app.isRemoved() || app.getState() == Stateful.STATE_AVAILABLE) appsI.remove();
            else if (checkHeartbeat && app.isHeartbeatDisabled()) appsI.remove();
        }
    }
    
    
    private static final class Lazy extends JmxHeartbeat {
        
        private final Collection<JmxApplication> unavailable = new HashSet();
    
        private boolean heartbeatRunning;
        
        
        private Lazy() {
            super("Lazy", LAZY_HEARTBEAT_DELAY); // NOI18N
        }

        
        @Override
        protected void schedule(JmxApplication... apps) {
            Collection<JmxApplication> toPing;
            
            synchronized (unavailable) {
                if (apps != null && apps.length > 0) unavailable.addAll(Arrays.asList(apps));
                cleanupUnavailableApps(unavailable, true);
                
                if (heartbeatRunning || unavailable.isEmpty()) return;
                
                heartbeatRunning = true;
                
                toPing = new ArrayList(unavailable);
                unavailable.clear();
            }
            
            pingApps(toPing);
        }
        
        @Override
        protected void pingFinished(JmxApplication... apps) {
            boolean pendingUnavailable;

            synchronized (unavailable) {
                if (apps.length > 0) unavailable.addAll(Arrays.asList(apps));
                cleanupUnavailableApps(unavailable, true);
                pendingUnavailable = !unavailable.isEmpty();
                heartbeatRunning = false;
            }

            if (pendingUnavailable) schedule();
        }
        
    }
    
    
    private static final class Immediate extends JmxHeartbeat {
        
        private final JmxHeartbeat fallback;
        
        
        private Immediate(JmxHeartbeat fallback) {
            super("Immediate", IMMEDIATE_HEARTBEAT_DELAY); // NOI18N
            
            this.fallback = fallback;
        }
        

        @Override
        protected void schedule(JmxApplication... apps) {
            if (apps == null || apps.length == 0) return;
            
            Collection<JmxApplication> unavailableApps = new ArrayList(Arrays.asList(apps));
            cleanupUnavailableApps(unavailableApps, false);
            if (unavailableApps.isEmpty()) return;
            
            pingApps(unavailableApps);
        }
        
        @Override
        protected void pingFinished(JmxApplication... apps) {
            if (apps.length > 0) fallback.schedule(apps);
        }
        
    }
    
}
