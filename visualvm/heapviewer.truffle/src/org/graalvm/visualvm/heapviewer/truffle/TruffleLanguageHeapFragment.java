/*
 * Copyright (c) 2017, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.HeapFragment;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
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
        
        TruffleType.TypesComputer<O, T> computer = new TruffleType.TypesComputer<O, T>(language, heap) {
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
        return HeapUtils.instancesIterator(HeapUtils.getSubclasses(heap, javaClassFqn));
    }
    
    protected final Iterator<Instance> instancesIterator(String[] javaClassFqns) {
        List<JavaClass> classes = new ArrayList<>();
        for (String fqn : javaClassFqns)
            classes.addAll(HeapUtils.getSubclasses(heap, fqn));
        return HeapUtils.instancesIterator(classes);
    }    

    
    protected static String fragmentDescription(Instance langID) {
        return DetailsSupport.getDetailsString(langID);
    }

    
    protected class ObjectsIterator implements Iterator<O> {
        private final Iterator<Instance> instancesIter;
        
        public ObjectsIterator(Iterator<Instance> iter) {
            instancesIter = iter;
        }

        public ObjectsIterator(Collection<JavaClass> cls) {
            instancesIter = HeapUtils.instancesIterator(cls);
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
    
}
