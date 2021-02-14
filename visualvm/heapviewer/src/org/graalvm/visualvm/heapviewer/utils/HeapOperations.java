/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.utils;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.heapviewer.HeapFragment;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapProgress;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapOperations_ComputingReferences=Computing References...",
    "HeapOperations_ComputingGCRoots=Computing GC Roots...",
    "HeapOperations_ComputingRetainedSizes=Computing Retained Sizes..."
})
public final class HeapOperations {
    
    private static Map<Heap, HeapOperations> INSTANCES;
    
    
    private HeapOperations() {}
    
    
    private static synchronized HeapOperations get(Heap heap) {
        if (INSTANCES == null) INSTANCES = new WeakHashMap();
        
        HeapOperations instance = INSTANCES.get(heap);
        if (instance == null) {
            instance = new HeapOperations();
            INSTANCES.put(heap, instance);
        }
        
        return instance;
    }
    
    
    public static void initializeReferences(Heap heap) throws InterruptedException {
        get(heap).initializeReferencesImpl(heap);
    }
    
    public static void initializeGCRoots(Heap heap) throws InterruptedException {
        get(heap).initializeGCRootsImpl(heap);
    }
    
    public static void initializeRetainedSizes(Heap heap) throws InterruptedException {
        get(heap).initializeRetainedSizesImpl(heap);
    }
    
    
    // --- References ----------------------------------------------------------
    
    private volatile boolean referencesInitialized;
    private volatile RequestProcessor.Task referencesComputer;
    
    private void initializeReferencesImpl(Heap heap) throws InterruptedException {
        RequestProcessor.Task _referencesComputer;
        
        synchronized (this) {
            if (referencesInitialized) return;
            
            if (referencesComputer == null) {
                Runnable workerR = new Runnable() {
                    public void run() {
                        ProgressHandle pHandle = null;

                        try {
                            pHandle = ProgressHandle.createHandle(Bundle.HeapOperations_ComputingReferences());
                            pHandle.setInitialDelay(1000);
                            pHandle.start(HeapProgress.PROGRESS_MAX);

                            HeapFragment.setProgress(pHandle, 0);

                            Instance dummy = (Instance)heap.getAllInstancesIterator().next();
                            dummy.getReferences();
                        } finally {
                            if (pHandle != null) pHandle.finish();
                        }

                        synchronized (HeapOperations.this) {
                            referencesInitialized = true;
                            referencesComputer = null;
                        }
                    }
                };
                referencesComputer = new RequestProcessor("References Computer").post(workerR); // NO18N
                _referencesComputer = referencesComputer;
            } else {
                _referencesComputer = referencesComputer;
            }
        }
        
        assert !SwingUtilities.isEventDispatchThread();

        _referencesComputer.waitFinished(0);
    }
    
    
    // --- GC Roots ------------------------------------------------------------
    
    private volatile boolean gcrootsInitialized;
    private volatile RequestProcessor.Task gcrootsComputer;
    
    private void initializeGCRootsImpl(Heap heap) throws InterruptedException {
        initializeReferencesImpl(heap);
        
        RequestProcessor.Task _gcrootsComputer;
        
        synchronized (this) {
            if (gcrootsInitialized) return;
            
            if (gcrootsComputer == null) {
                Runnable workerR = new Runnable() {
                    public void run() {
                        ProgressHandle pHandle = null;

                        try {
                            pHandle = ProgressHandle.createHandle(Bundle.HeapOperations_ComputingGCRoots());
                            pHandle.setInitialDelay(1000);
                            pHandle.start(HeapProgress.PROGRESS_MAX);

                            HeapFragment.setProgress(pHandle, 0);

                            Instance dummy = (Instance)heap.getAllInstancesIterator().next();
                            dummy.getNearestGCRootPointer();
                        } finally {
                            if (pHandle != null) pHandle.finish();
                        }

                        synchronized (HeapOperations.this) {
                            gcrootsInitialized = true;
                            gcrootsComputer = null;
                        }
                    }
                };
                gcrootsComputer = new RequestProcessor("GC Roots Computer").post(workerR); // NO18N
                _gcrootsComputer = gcrootsComputer;
            } else {
                _gcrootsComputer = gcrootsComputer;
            }
        }
        
        assert !SwingUtilities.isEventDispatchThread();

        _gcrootsComputer.waitFinished(0);
    }
    
    // --- Retained Sizes ------------------------------------------------------------
    
    private volatile boolean retainedInitialized;
    private volatile RequestProcessor.Task retainedComputer;
    
    private void initializeRetainedSizesImpl(Heap heap) throws InterruptedException {
        initializeGCRootsImpl(heap);
        
        RequestProcessor.Task _retainedComputer;
        
        synchronized (this) {
            if (retainedInitialized) return;
            
            if (retainedComputer == null) {
                Runnable workerR = new Runnable() {
                    public void run() {
                        ProgressHandle pHandle = null;

                        try {
                            pHandle = ProgressHandle.createHandle(Bundle.HeapOperations_ComputingRetainedSizes());
                            pHandle.setInitialDelay(1000);
                            pHandle.start();

                            HeapFragment.setProgress(pHandle, 0);

                            List<JavaClass> classes = heap.getAllClasses();
                            if (!classes.isEmpty()) classes.get(0).getRetainedSizeByClass();
                        } finally {
                            if (pHandle != null) pHandle.finish();
                        }

                        synchronized (HeapOperations.this) {
                            retainedInitialized = true;
                            retainedComputer = null;
                        }
                    }
                };
                retainedComputer = new RequestProcessor("Retained Sizes Computer").post(workerR); // NO18N
                _retainedComputer = retainedComputer;
            } else {
                _retainedComputer = retainedComputer;
            }
        }
        
        assert !SwingUtilities.isEventDispatchThread();

        _retainedComputer.waitFinished(0);
    }
    
}
