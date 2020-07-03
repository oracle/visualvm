/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer;

import org.graalvm.visualvm.heapviewer.ui.HeapViewerComponent;
import org.openide.util.NbBundle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.NotSupportedDisplayer;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.HeapFactory;
import org.graalvm.visualvm.lib.jfluid.heap.HeapProgress;
import org.openide.util.Lookup;


/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapViewer_LoadingDumpMsg=Loading Heap Dump...",
    "HeapViewer_HeapDumpEmpty=heap dump (no content)", // Displays 'Not supported for this heap dump (no content)'
    "HeapViewer_HeapDumpOOME=heap dump (not enough memory)" // Displays 'Not supported for this heap dump (not enough memory)'
})
public final class HeapViewer {

    private final File heapDumpFile;
    private final Lookup.Provider heapDumpProject;
    
    private final List<HeapFragment> heapFragments;
    
    private JComponent component;
    
    
    public HeapViewer(File file) throws IOException {
        assert !SwingUtilities.isEventDispatchThread();
        
        heapDumpFile = file;
        heapDumpProject = null;
        
        heapFragments = computeHeapFragments(heapDumpFile, heapDumpProject, createHeap(heapDumpFile));
    }

    
    public File getFile() {
        return heapDumpFile;
    }

    public Lookup.Provider getProject() {
        return heapDumpProject;
    }
    
    
    public List<HeapFragment> getFragments() {
        return heapFragments;
    }
    
    
    public JComponent getComponent() {
        if (component == null) {
            if (heapFragments == null) component = new NotSupportedDisplayer(Bundle.HeapViewer_HeapDumpOOME());
            else if (heapFragments.isEmpty()) component = new NotSupportedDisplayer(Bundle.HeapViewer_HeapDumpEmpty());
            else component = new HeapViewerComponent(this);
        }
        return component;
    }
    
    
    public void willBeRemoved() {
        if (component instanceof HeapViewerComponent)
            ((HeapViewerComponent)component).willBeClosed();
    }
    
    public void closed() {
        if (component instanceof HeapViewerComponent)
            ((HeapViewerComponent)component).closed();
    }

    
    private static Heap createHeap(File heapFile) throws IOException {
        assert !SwingUtilities.isEventDispatchThread();
        
        ProgressHandle pHandle = null;

        try {
            pHandle = ProgressHandle.createHandle(Bundle.HeapViewer_LoadingDumpMsg());
            pHandle.setInitialDelay(1000);
            pHandle.start(HeapProgress.PROGRESS_MAX*2);
            
            HeapFragment.setProgress(pHandle, 0);
            Heap heap = HeapFactory.createHeap(heapFile);
            
            HeapFragment.setProgress(pHandle, HeapProgress.PROGRESS_MAX);
            heap.getSummary(); // Precompute HeapSummary within the progress

            return heap;
        } catch (OutOfMemoryError e) {
            System.err.println("Out of memory in HeapViewer.createHeap: " + e.getMessage()); // NOI18N
            HeapUtils.handleOOME(false, e);
            return null;
        } finally {
            if (pHandle != null) pHandle.finish();
        }
    }
    
    private static List<HeapFragment> computeHeapFragments(File heapDumpFile, Lookup.Provider heapDumpProject, Heap heap) throws IOException {
        if (heap == null) return null;
        
        Collection<? extends HeapFragment.Provider> providers = Lookup.getDefault().lookupAll(HeapFragment.Provider.class);
        
        List<HeapFragment> fragments = new ArrayList(providers.size());
        for (HeapFragment.Provider provider : providers) {
            HeapFragment fragment = provider.getFragment(heapDumpFile, heapDumpProject, heap);
            if (fragment != null) fragments.add(fragment);
        }
        
        return Collections.unmodifiableList(fragments);
    }
    
}
