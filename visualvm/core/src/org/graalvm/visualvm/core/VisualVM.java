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
package org.graalvm.visualvm.core;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import org.graalvm.visualvm.core.datasupport.ComparableWeakReference;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class VisualVM {
    
    private static final VisualVM INSTANCE = new VisualVM();
    
    
    private final int TASK_PROCESSOR_THROUGHPUT_DEFAULT = 30;
    private final int TASK_PROCESSOR_THROUGHPUT = Integer.getInteger("org.graalvm.visualvm.core.taskProcessorThroughput", // NOI18N
                                                                     TASK_PROCESSOR_THROUGHPUT_DEFAULT);
    
    
    private final RequestProcessor taskProcessor;
    
    private Collection<ComparableWeakReference<Runnable>> closingHandlers;
    
    
    private VisualVM() {
        taskProcessor = new RequestProcessor("VisualVM Shared RequestProcessor", TASK_PROCESSOR_THROUGHPUT); // NOI18N
    }
    
    
    public static VisualVM getInstance() { return INSTANCE; }
    
    
    public final void runTask(Runnable task) {
        taskProcessor.post(task);
    }
    
    public final void runTask(Runnable task, int timeToWait) {
        taskProcessor.post(task, timeToWait);
    }
    
    
    /**
     * Adds a Runnable instance to be notified when the host VisualVM is closing.
     * Note that the Runnable cannot be explicitly unregistered, it's weakly referenced and will
     * be notified up to once and then unregistered automatically.
     * 
     * @param handler Runnable instance to be notified when the host VisualVM is closing.
     */
    public synchronized final void notifyWhenClosing(Runnable handler) {
        if (closingHandlers == null) closingHandlers = new ArrayList();
        closingHandlers.add(new ComparableWeakReference(handler));
    }
    
    
    synchronized boolean closing() {
        if (closingHandlers != null)
            for (WeakReference<Runnable> handlerR : closingHandlers) {
                Runnable handler = handlerR.get();
                if (handler != null)
                    try { handler.run(); }
                    catch (Exception e) { System.err.println("Exception handling VisualVM.closing(): " + e); } // NOI18N
            }
        
        return true;
    }
    
}
