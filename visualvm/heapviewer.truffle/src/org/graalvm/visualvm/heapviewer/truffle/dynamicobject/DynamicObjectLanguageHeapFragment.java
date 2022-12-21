/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.dynamicobject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguage;
import org.graalvm.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import org.graalvm.visualvm.heapviewer.truffle.TruffleType;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class DynamicObjectLanguageHeapFragment<D extends DynamicObject, T extends TruffleType<D>> extends TruffleLanguageHeapFragment<D, T> {
    
    private final Map<Instance, JavaClass> languageIDsCache;
    
    
    protected DynamicObjectLanguageHeapFragment(String ID, String name, String description, TruffleLanguage<D, T, ? extends TruffleLanguageHeapFragment<D, T>> language, Heap heap) {
        super(ID, name, description, language, heap);
        
        languageIDsCache = new HashMap<>();
    }
    
    
    protected final Iterator<Instance> languageInstancesIterator(String languageID) {
        Iterator<Instance> instIt = HeapUtils.instancesIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));

        return new LanguageInstanceFilterIterator(instIt, languageID);
    }

    protected final Iterator<D> languageObjectsIterator(String languageID) {
        Iterator<D> dynIt = new ObjectsIterator(HeapUtils.getSubclasses(heap, DynamicObject.DYNAMIC_OBJECT_FQN));

        return new LanguageFilterIterator(dynIt, languageID);
    }
    
    
    private JavaClass getLanguageID(Instance shape) {
        JavaClass langID = languageIDsCache.get(shape);
        if (langID == null) {
            langID = DynamicObject.getLanguageIdFromShape(shape);
            languageIDsCache.put(shape, langID);
        }
        return langID;
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
                Instance shape = dobj.getShape();
                if (languageID.equals(DynamicObjectLanguageHeapFragment.this.getLanguageID(shape).getName())) {
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

    
    private class LanguageInstanceFilterIterator implements Iterator<Instance> {
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
                Instance shape = DynamicObject.getShape(inst);
                JavaClass langId = DynamicObjectLanguageHeapFragment.this.getLanguageID(shape);
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
