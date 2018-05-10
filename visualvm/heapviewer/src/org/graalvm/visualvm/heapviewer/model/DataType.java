/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.model;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public class DataType<T> {
    
    public static final String NO_VALUE_STRING = new String();
    public static final String UNSUPPORTED_VALUE_STRING = new String();
    public static final String NOT_AVAILABLE_VALUE_STRING = new String();
    
    public static final Integer NO_VALUE_INTEGER = new Integer(Integer.MIN_VALUE + 10);
    public static final Integer UNSUPPORTED_VALUE_INTEGER = new Integer(Integer.MIN_VALUE + 11);
    public static final Integer NOT_AVAILABLE_VALUE_INTEGER = new Integer(Integer.MIN_VALUE + 12);
    
    public static final Long NO_VALUE_LONG = new Long(Long.MIN_VALUE + 10);
    public static final Long UNSUPPORTED_VALUE_LONG = new Long(Long.MIN_VALUE + 11);
    public static final Long NOT_AVAILABLE_VALUE_LONG = new Long(Long.MIN_VALUE + 12);
    
    public static final DataType<String> NAME = new DataType<String>(String.class, NO_VALUE_STRING, UNSUPPORTED_VALUE_STRING);
    public static final DataType<Integer> COUNT = new DataType<Integer>(Integer.class, NO_VALUE_INTEGER, UNSUPPORTED_VALUE_INTEGER);
    public static final DataType<Long> OWN_SIZE = new DataType<Long>(Long.class, NO_VALUE_LONG, UNSUPPORTED_VALUE_LONG);
    
    public static final DataType<Long> RETAINED_SIZE = new RetainedSize();
    
    public static final DataType<String> LOGICAL_VALUE = new DataType<String>(String.class, NO_VALUE_STRING, UNSUPPORTED_VALUE_STRING);
    
    public static final DataType<Long> OBJECT_ID = new DataType<Long>(Long.class, NO_VALUE_LONG, UNSUPPORTED_VALUE_LONG);
    
    public static final DataType<JavaClass> CLASS = new DataType<JavaClass>(JavaClass.class, null, null);
    public static final DataType<Instance> INSTANCE = new DataType<Instance>(Instance.class, null, null);
    
    public static final DataType<HeapViewerNode> LOOP = new DataType<HeapViewerNode>(HeapViewerNode.class, null, null);
    public static final DataType<HeapViewerNode> LOOP_ORIGIN = new DataType<HeapViewerNode>(HeapViewerNode.class, null, null);
    
    
    static final Set<DataType> DEFAULT_TYPES = new HashSet(Arrays.asList(
        NAME, COUNT, OWN_SIZE, RETAINED_SIZE, LOGICAL_VALUE, OBJECT_ID,
        CLASS, INSTANCE, LOOP, LOOP_ORIGIN
    ));
    
    
    private final Class<T> type;
    private final T noValue;
    private final T unsupportedValue;

    
    public DataType(Class<T> type, T noValue, T unsupportedValue) {
        this.type = type;
        this.noValue = noValue;
        this.unsupportedValue = unsupportedValue;
    }


    public Class<T> getType() { return type; }

    public T getNoValue() { return noValue; }

    public T getUnsupportedValue() { return unsupportedValue; }
    
    
    public boolean valuesAvailable(Heap heap) { return true; }
    
    public boolean computeValues(Heap heap, Runnable whenComputed) { return true; }
    
    public void computeValuesImmediately(Heap heap) {}
    
    public void notifyWhenAvailable(Heap heap, Runnable target) {}
    
    public T getNotAvailableValue() { return null; }
    
    
    public static abstract class Lazy<T> extends DataType<T> {
        
        private final T notAvailableValue;
        private Map<Heap, Set<WeakReference<Runnable>>> notifyTargets;
        
        public Lazy(Class<T> type, T noValue, T unsupportedValue, T notAvailableValue) {
            super(type, noValue, unsupportedValue);
            this.notAvailableValue = notAvailableValue;
        }
        
        public abstract boolean valuesAvailable(Heap heap);
    
        public abstract boolean computeValues(Heap heap, Runnable whenComputed);
        
        public abstract void computeValuesImmediately(Heap heap);
        
        protected void valuesComputed(Heap heap, Runnable whenComputed) {
            if (whenComputed != null) whenComputed.run();
            
            if (notifyTargets != null) {
                Set<WeakReference<Runnable>> targetRefs = notifyTargets.remove(heap);
                if (targetRefs != null) {
                    for (WeakReference<Runnable> targetRef : targetRefs) {
                        Runnable target = targetRef.get();
                        if (target != null) target.run();
                    }
                    targetRefs.clear();
                }
            }
        }
        
        public void notifyWhenAvailable(Heap heap, Runnable target) {
            if (notifyTargets == null) notifyTargets = new WeakHashMap();
            Set<WeakReference<Runnable>> targetRefs = notifyTargets.get(heap);
            if (targetRefs == null) {
                targetRefs = new HashSet();
                notifyTargets.put(heap, targetRefs);
            }
            targetRefs.add(new WeakReference(target));
        }

        public T getNotAvailableValue() { return notAvailableValue; }
        
    }
    
    
    public static abstract class ValueProvider {
    
        public abstract boolean supportsView(Heap heap, String viewID);

        public abstract <T> T getValue(HeapViewerNode node, DataType<T> type, Heap heap);

    }
    
    
    @NbBundle.Messages({
        "RetainedSize_ComputeRetainedMsg=<html><b>Retained sizes will be computed.</b><br><br>For large heap dumps this operation can take a significant<br>amount of time. Do you want to continue?</html>",
        "RetainedSize_ComputeRetainedCaption=Compute Retained Sizes",
        "RetainedSize_ComputingRetainedMsg=Computing retained sizes..."
    })
    private static class RetainedSize extends Lazy<Long> {
        
        private volatile boolean computing;
        
        private RetainedSize() {
            super(Long.class, NO_VALUE_LONG, UNSUPPORTED_VALUE_LONG, NOT_AVAILABLE_VALUE_LONG);
        }
        
        public boolean valuesAvailable(Heap heap) {
            return heap.isRetainedSizeComputed() && heap.isRetainedSizeByClassComputed();
        }
        
        public boolean computeValues(final Heap heap, Runnable whenComputed) {
            if (computing) return true;
            
            if (!ProfilerDialogs.displayConfirmationDNSA(Bundle.RetainedSize_ComputeRetainedMsg(), 
                                                         Bundle.RetainedSize_ComputeRetainedCaption(),
                                                         null, "HeapFragmentWalker.computeRetainedSizes", false)) // NOI18N
                return false;
            
            computing = true;
            new RequestProcessor("Retained Sizes Computer").post(new Runnable() { // NOI18N
                public void run() { computeValuesImmediately(heap, whenComputed); }
            });
            return true;
        }
        
        public void computeValuesImmediately(Heap heap) {
            if (computing) return;
            
            computing = true;
            computeValuesImmediately(heap, null);
        }
        
        public void computeValuesImmediately(Heap heap, final Runnable whenComputed) {
            List<JavaClass> classes = heap.getAllClasses();
            if (classes.size() > 0) {
                ProgressHandle pd = ProgressHandle.createHandle(Bundle.RetainedSize_ComputingRetainedMsg());
                pd.start();
                classes.get(0).getRetainedSizeByClass();
                pd.finish();
            }
            
            computing = false;
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { valuesComputed(heap, whenComputed); }
            });
        }
        
    }
    
}
