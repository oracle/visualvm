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
package com.sun.tools.visualvm.truffle.heapwalker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import org.netbeans.modules.profiler.heapwalker.v2.HeapFragment;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class TruffleLanguageHeapFragment extends HeapFragment {

    protected TruffleLanguageHeapFragment(String name, String ID, Instance langInfo, Heap heap) throws IOException {
        super(ID, name, createFragmentDescription(langInfo, heap), heap);
    }

    public Iterator<Instance> getInstancesIterator(String javaClassFqn) {
        return new InstancesIterator(getSubclasses(heap, javaClassFqn));
    }

    public Iterator<DynamicObject> getDynamicObjectsIterator() {
        return new DynamicObjectsIterator(getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));
    }

    private static String createFragmentDescription(Instance langInfo, Heap heap) {
        return DetailsSupport.getDetailsString(langInfo, heap);
    }

    private class InstancesIterator implements Iterator<Instance> {
        Iterator<JavaClass> classIt;
        Iterator<Instance> instanceIt;

        private InstancesIterator(Collection<JavaClass> cls) {
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

    private class DynamicObjectsIterator implements Iterator<DynamicObject> {
        InstancesIterator instancesIter;

        private DynamicObjectsIterator(Collection<JavaClass> cls) {
            instancesIter = new InstancesIterator(cls);
        }

        @Override
        public boolean hasNext() {
            return instancesIter.hasNext();
        }

        @Override
        public DynamicObject next() {
            return new DynamicObject(instancesIter.next());
        }
    }

    private static Collection<JavaClass> getSubclasses(Heap heap, String baseClass) {
        List<JavaClass> subclasses = new ArrayList();

        String escapedClassName = "\\Q" + baseClass + "\\E";
        Collection<JavaClass> jClasses = heap.getJavaClassesByRegExp(escapedClassName);

        for (JavaClass jClass : jClasses) {
            Collection subclassesCol = jClass.getSubClasses();
            subclasses.add(jClass); // instanceof approach rather than subclassof
            subclasses.addAll(subclassesCol);
        }

        return subclasses;
    }

}
