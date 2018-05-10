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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.utils.HeapUtils;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
// O: type of base language objects (JavaScriptDynamicObject, RubyDynamicObject, PythonObject, RObject)
// T: TruffleType or its subclass aggregating O objects
@NbBundle.Messages({
    "TruffleLanguageHeapFragment_Language=Language",
    "TruffleLanguageHeapFragment_InitializingLanguageModel=Initializing {0} Model..."
})
public abstract class TruffleLanguageHeapFragment<O extends TruffleObject, T extends TruffleType<O>> extends HeapFragment {
    
    private final TruffleLanguage<O, T, ? extends TruffleLanguageHeapFragment<O, T>> language;
    
    private long heapSize;
    private long objectsCount;
    private List<T> types;
    private final Object statisticsLock = new Object();
    
    private Progress statisticsProgress;
    private boolean ownProgress;
    private final Object statisticsProgressLock = new Object();
    
    
    protected TruffleLanguageHeapFragment(String ID, String name, String description, TruffleLanguage<O, T, ? extends TruffleLanguageHeapFragment<O, T>> language, Heap heap) {
        super(ID, name, description, heap);
        this.language = language;
    }
    
    
    public static boolean isTruffleHeap(HeapContext context) {
        return context.getFragment() instanceof TruffleLanguageHeapFragment; // NOI18N
    }
    
    
    public long getHeapSize(Progress progress) {
        checkInitialized(progress);
        return heapSize;
    }
    
    public long getObjectsCount(Progress progress) {
        checkInitialized(progress);
        return objectsCount;
    }
    
    public List<T> getTypes(Progress progress) {
        checkInitialized(progress);
        return types;
    }
    
    public T getType(String name, Progress progress) {
        List<T> _types = getTypes(progress);
        for (T type : _types) if (name.equals(type.getName())) return type;
        return null;
    }
    
    
    public abstract Iterator<Instance> getInstancesIterator();
    
    public Iterator<O> getObjectsIterator() {
        return new ObjectsIterator(getInstancesIterator());
    }
    
    
    private void checkInitialized(Progress progress) {
        Progress.Listener progressListener = null;
        
        if (progress != null) {
            synchronized (statisticsProgressLock) {
                if (statisticsProgress != null) {
                    statisticsProgress.addChangeListener(new Progress.Listener() {
                        @Override
                        public void progressChanged(Progress.Event event) {
                            progress.setCurrentStep(event.getCurrentStep());
                        }
                    });
                }
            }
        }
        
        synchronized (statisticsLock) {
            if (types == null) computeStatistics(progress);
        }

        if (progressListener != null) {
            synchronized (statisticsProgressLock) {
                if (statisticsProgress != null) {
                    statisticsProgress.removeChangeListener(progressListener);
                }
            }
        }
    }
    
    private void computeStatistics(Progress progress) {
        if (statisticsProgress == null) {
            synchronized (statisticsProgressLock) {
                if (progress != null) {
                    statisticsProgress = progress;
                } else {
                    ownProgress = true;
                    statisticsProgress = new Progress();
                    statisticsProgress.setupUnknownSteps();
                }
            }
        }
        
        int verIdx = getDescription().indexOf(" ("); // NOI18N
        String langName = verIdx != -1 ? getDescription().substring(0, verIdx) : Bundle.TruffleLanguageHeapFragment_Language();
        ProgressHandle pHandle = ProgressHandle.createHandle(Bundle.TruffleLanguageHeapFragment_InitializingLanguageModel(langName));
        pHandle.setInitialDelay(1000);
        pHandle.start();
        
        TruffleType.TypesComputer<O, T> computer = new TruffleType.TypesComputer(language, heap) {
            @Override
            protected void addingObject(long size, long retained, String type) {
                objectsCount++;
                heapSize += size;
            }
        };
        
        Iterator<O> objects = getObjectsIterator();
        try {
            while (objects.hasNext()) {
                computer.addObject(objects.next());
                if (statisticsProgress != null) statisticsProgress.step();
            }
        } finally {
            if (statisticsProgress != null && ownProgress) statisticsProgress.finish();
            pHandle.finish();
        }
        
        types = computer.getTypes();
    }
    
    
    

    protected final Iterator<Instance> instancesIterator(String javaClassFqn) {
        return new InstancesIterator(HeapUtils.getSubclasses(heap, javaClassFqn));
    }
    
    protected final Iterator<Instance> instancesIterator(String[] javaClassFqns) {
        List classes = new ArrayList();
        for (String fqn : javaClassFqns)
            classes.addAll(HeapUtils.getSubclasses(heap, fqn));
        return new InstancesIterator(classes);
    }    

    
    protected static String fragmentDescription(Instance langID, Heap heap) {
        return DetailsSupport.getDetailsString(langID, heap);
    }

    
    protected static class InstancesIterator implements Iterator<Instance> {
        private final Iterator<JavaClass> classIt;
        private Iterator<Instance> instanceIt;

        public InstancesIterator(Collection<JavaClass> cls) {
            classIt = cls.iterator();
            instanceIt = Collections.EMPTY_LIST.iterator();
        }

        @Override
        public boolean hasNext() {
            if (instanceIt.hasNext()) {
                return true;
            }
            if (!classIt.hasNext()) {
                return false;
            }
            instanceIt = classIt.next().getInstancesIterator();
            return hasNext();
        }

        @Override
        public Instance next() {
            return instanceIt.next();
        }
    }
    
    protected static abstract class ExcludingInstancesIterator implements Iterator<Instance> {
        private final Iterator<Instance> instancesIt;
        private Instance next;

        protected ExcludingInstancesIterator(Iterator<Instance> it) {
            instancesIt = it;
            computeNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Instance next() {
            Instance ret = next;
            computeNext();
            return ret;
        }
        
        private void computeNext() {
            while (instancesIt.hasNext()) {
                next = instancesIt.next();
                if (!exclude(next)) return;
            }
            next = null;
        }
        
        protected abstract boolean exclude(Instance instance);
    }

    protected class ObjectsIterator implements Iterator<O> {
        private final Iterator<Instance> instancesIter;
        
        public ObjectsIterator(Iterator<Instance> iter) {
            instancesIter = iter;
        }

        public ObjectsIterator(Collection<JavaClass> cls) {
            instancesIter = new InstancesIterator(cls);
        }

        @Override
        public boolean hasNext() {
            return instancesIter.hasNext();
        }

        @Override
        public O next() {
            return language.createObject(instancesIter.next());
        }
    }
    
    protected abstract class ExcludingObjectsIterator implements Iterator<O> {
        private final Iterator<O> objectsIt;
        private O next;

        protected ExcludingObjectsIterator(Iterator<O> it) {
            objectsIt = it;
            computeNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public O next() {
            O ret = next;
            computeNext();
            return ret;
        }
        
        private void computeNext() {
            while (objectsIt.hasNext()) {
                next = objectsIt.next();
                if (!exclude(next)) return;
            }
            next = null;
        }
        
        protected abstract boolean exclude(O object);
    }
    
}
