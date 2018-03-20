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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.heapviewer.HeapFragment;
import com.sun.tools.visualvm.heapviewer.model.Progress;
import com.sun.tools.visualvm.heapviewer.utils.HeapUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
// TODO: TruffleObject should be introduced - superclass for DynamicObject, PythonObject, RObject
//
// O: type of base language objects (DynamicObject, PythonObject, RObject)
// T: TruffleType or its subclass aggregating O objects
public abstract class TruffleLanguageHeapFragment<O extends Object, T extends TruffleType<O>> extends HeapFragment {
    
    private long heapSize;
    private long objectsCount;
    private List<T> types;
    private final Object statisticsLock = new Object();
    
    private Progress statisticsProgress;
    private boolean ownProgress;
    private final Object statisticsProgressLock = new Object();
    
    
    protected TruffleLanguageHeapFragment(String ID, String name, String description, Heap heap) throws IOException {
        super(ID, name, description, heap);
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
    
    
    protected abstract O createObject(Instance instance);
    
    protected abstract T createTruffleType(String name);
    
    
    protected abstract Iterator<Instance> getInstancesIterator();
    
    protected Iterator<O> getObjectsIterator() {
        return new ObjectsIterator(getInstancesIterator());
    }
    
    
    protected abstract long getObjectSize(O object);
    
    protected abstract long getObjectRetainedSize(O object);
    
    protected abstract String getObjectType(O object);
    
    
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
        
        Map<String, T> cache = new HashMap();
        Iterator<O> objects = getObjectsIterator();
        
        while (objects.hasNext()) {
            O object = objects.next();
            
            if (statisticsProgress != null) statisticsProgress.step();
            objectsCount++;
            
            long objectSize = getObjectSize(object);
            heapSize += objectSize;
            
            long objectRetainedSize = getObjectRetainedSize(object);
            
            String typeName = getObjectType(object);
            T type = cache.get(typeName);
            if (type == null) {
                type = createTruffleType(typeName);
                cache.put(typeName, type);
            }
            type.addObject(object, objectSize, objectRetainedSize);
        }
        
        if (statisticsProgress != null && ownProgress) statisticsProgress.finish();
        
        types = Collections.unmodifiableList(new ArrayList(cache.values()));
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
//
//    public Iterator<Instance> getLanguageInstancesIterator(String languageID) {
//        Iterator<Instance> instIt = new InstancesIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));
//        
//        return new LanguageInstanceFilterIterator(instIt, languageID);
//    }
//
//    public Iterator<O> getObjectsIterator() {
//        return new ObjectsIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));
//    }
//
//    protected Iterator<O> getObjectsIterator(String languageID) {
//        Iterator<O> dynIt = new ObjectsIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));
//
//        return new LanguageFilterIterator(dynIt, languageID);
//    }
    

    
    protected static String fragmentDescription(Instance langID, Heap heap) {
        return DetailsSupport.getDetailsString(langID, heap);
    }

    
    protected static class InstancesIterator implements Iterator<Instance> {
        private final Iterator<JavaClass> classIt;
        private Iterator<Instance> instanceIt;

        protected InstancesIterator(Collection<JavaClass> cls) {
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
        
        protected ObjectsIterator(Iterator<Instance> iter) {
            instancesIter = iter;
        }

        protected ObjectsIterator(Collection<JavaClass> cls) {
            instancesIter = new InstancesIterator(cls);
        }

        @Override
        public boolean hasNext() {
            return instancesIter.hasNext();
        }

        @Override
        public O next() {
            return createObject(instancesIter.next());
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
    
    
    public static abstract class DynamicObjectBased<D extends DynamicObject, T extends TruffleType<D>> extends TruffleLanguageHeapFragment<D, T> {
        
        protected DynamicObjectBased(String ID, String name, String description, Heap heap) throws IOException {
            super(ID, name, description, heap);
        }
        
        
        protected final Iterator<Instance> languageInstancesIterator(String languageID) {
            Iterator<Instance> instIt = new InstancesIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));

            return new LanguageInstanceFilterIterator(instIt, languageID);
        }
        
        protected final Iterator<D> languageObjectsIterator(String languageID) {
            Iterator<D> dynIt = new ObjectsIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));

            return new LanguageFilterIterator(dynIt, languageID);
        }
        
        
        private class LanguageFilterIterator implements Iterator<D> {
            private final String languageID;
            private final Iterator<D> objIterator;
            private D next;

            private LanguageFilterIterator(Iterator<D> oit, String langID) {
                objIterator = oit;
                languageID = langID;
            }

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                while (objIterator.hasNext()) {
                    D dobj = objIterator.next();
                    if (languageID.equals(dobj.getLanguageId().getName())) {
                        next = dobj;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public D next() {
                if (hasNext()) {
                    D dobj = next;
                    next = null;
                    return dobj;
                }
                throw new NoSuchElementException();
            }
        }

        private static class LanguageInstanceFilterIterator implements Iterator<Instance> {
            private final String languageID;
            private final Iterator<Instance> instancesIterator;
            private Instance next;

            private LanguageInstanceFilterIterator(Iterator<Instance> instIt, String langID) {
                instancesIterator = instIt;
                languageID = langID;
            }

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                while (instancesIterator.hasNext()) {
                    Instance inst = instancesIterator.next();
                    JavaClass langId = DynamicObject.getLanguageId(inst);
                    if (langId != null && languageID.equals(langId.getName())) {
                        next = inst;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Instance next() {
                if (hasNext()) {
                    Instance inst = next;
                    next = null;
                    return inst;
                }
                throw new NoSuchElementException();
            }
        }
        
    }

}
