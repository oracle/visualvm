/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.graalvm.visualvm.lib.profiler.oql.engine.api.impl;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.GCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.Value;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.ReferenceChain;

import static org.graalvm.visualvm.lib.jfluid.utils.VMUtils.*;
import org.openide.util.Enumerations;

/**
 *
 * @author      Jaroslav Bachorik
 */
/**
 * A helper class for OQL engine allowing easy access to the underlying
 * heapwalker model
 */
public class Snapshot {
    private final Heap delegate;
    private JavaClass weakReferenceClass;
    private int referentFieldIndex;
    private ReachableExcludes reachableExcludes;
    final private OQLEngine engine;
    
    public Snapshot(Heap heap, OQLEngine engine) {
        this.delegate = heap;
        this.engine = engine;
        init();
    }

    private void init() {
        weakReferenceClass = findClass("java.lang.ref.Reference"); // NOI18N
        if (weakReferenceClass == null) {	// JDK 1.1.x
            weakReferenceClass = findClass("sun.misc.Ref"); // NOI18N
            referentFieldIndex = 0;
        } else {
            List<Field> flds = weakReferenceClass.getFields();
            int fldsCount = flds.size();

            for (int i = 0; i < fldsCount; i++) {
                if ("referent".equals(flds.get(i).getName())) { // NOI18N
                    referentFieldIndex = i;
                    break;
                }
            }
        }
    }

    public JavaClass findClass(String name) {
        try {
            long classId;
            if (name.startsWith("0x")) {
                classId = Long.parseLong(name.substring(2), 16);
            } else {
                classId = Long.parseLong(name);
            }
            return delegate.getJavaClassByID(classId);
        } catch (NumberFormatException e) {}
        return delegate.getJavaClassByName(preprocessClassName(name));
    }

    private String preprocessClassName(String className) {
        int arrDim = 0;
        if (className.startsWith("[")) { // NOI18N
            arrDim = className.lastIndexOf('[') + 1; // NOI18N

            className = className.substring(arrDim);
        }
        if (className.length() == 1) {
            if (className.equals(INT_CODE)) {
                className = INT_STRING;
            } else if (className.equals(LONG_CODE)) {
                className = LONG_STRING;
            } else if (className.equals(DOUBLE_CODE)) {
                className = DOUBLE_STRING;
            } else if (className.equals(FLOAT_CODE)) {
                className = FLOAT_STRING;
            } else if (className.equals(BYTE_CODE)) {
                className = BYTE_STRING;
            } else if (className.equals(SHORT_CODE)) {
                className = SHORT_STRING;
            } else if (className.equals(CHAR_CODE)) {
                className = CHAR_STRING;
            } else if (className.equals(BOOLEAN_CODE)) {
                className = BOOLEAN_STRING;
            }
        }
        if (arrDim > 0 && className.charAt(0) == REFERENCE) {   // class name
            className = className.substring(1);
        }
        StringBuilder sb = new StringBuilder(className);
        for (int i = 0; i < arrDim; i++) {
            sb.append("[]"); // NOI18N
        }

        return sb.toString();
    }

    public Instance findThing(long objectId) {
        return delegate.getInstanceByID(objectId);
    }

    public GCRoot findRoot(Instance object) {
        Instance gcInstance = object;
        do {
            gcInstance = gcInstance.getNearestGCRootPointer();
        } while (!gcInstance.isGCRoot());
        if (gcInstance != null) {
            Collection<GCRoot> roots = delegate.getGCRoots(gcInstance);
            if (!roots.isEmpty()) {
                // TODO getGCRoot() now returns Collection
                return roots.iterator().next();
            }
        }
        return null;
    }

    public int distanceToGCRoot(Instance object) {
        Instance gcInstance = object;
        int distance = 0;
        do {
            gcInstance = gcInstance.getNearestGCRootPointer();
            if (gcInstance == null) {
                return 0;
            }
            distance++;
        } while (!gcInstance.isGCRoot());
        return distance;
    }

    public Enumeration concat(Enumeration en1, Enumeration en2) {
        return Enumerations.concat(en1, en2);
    }

    /**
     * Return an Iterator of all of the classes in this snapshot.
     **/
    public Iterator getClasses() {
        return delegate.getAllClasses().iterator();
    }

    public Iterator getClassNames(String regex)  {
        final Iterator delegated = delegate.getJavaClassesByRegExp(regex).iterator();
        return new Iterator() {

            public boolean hasNext() {
                return delegated.hasNext();
            }

            public Object next() {
                return ((JavaClass)delegated.next()).getName();
            }

            public void remove() {
                delegated.remove();
            }
        };

    }

    public Iterator getInstances(final JavaClass clazz, final boolean includeSubclasses) {
        // special case for all subclasses of java.lang.Object
        if (includeSubclasses && clazz.getSuperClass() == null) {
            return delegate.getAllInstancesIterator();
        }
        return new TreeIterator<Instance, JavaClass>(clazz) {

            @Override
            protected Iterator<Instance> getSameLevelIterator(JavaClass popped) {
                return popped.getInstancesIterator();
            }

            @Override
            protected Iterator<JavaClass> getTraversingIterator(JavaClass popped) {
                return includeSubclasses ? popped.getSubClasses().iterator() : Collections.EMPTY_LIST.iterator();
            }
        };
    }

    public Iterator getReferrers(Object obj, boolean includeWeak) {
        List instances = new ArrayList();
        List references = new ArrayList();
        
        if (obj instanceof Instance) {
            references.addAll(((Instance)obj).getReferences());
        } else if (obj instanceof JavaClass) {
            references.addAll(((JavaClass)obj).getInstances());
            references.add(((JavaClass)obj).getClassLoader());
        }
        if (!references.isEmpty()) {
            for (Object o : references) {
                if (o instanceof Value) {
                    Value val = (Value) o;
                    Instance inst = val.getDefiningInstance();
                    if (includeWeak || !isWeakRef(inst)) {
                        instances.add(inst);
                    }
                } else if (o instanceof Instance) {
                    if (includeWeak || !isWeakRef((Instance)o)) {
                        instances.add(o);
                    }
                }
            }
        }
        return instances.iterator();
    }

    public Iterator getReferees(Object obj, boolean includeWeak) {
        List instances = new ArrayList();
        List values = new ArrayList();
        
        if (obj instanceof Instance) {
            Instance o = (Instance)obj;
            values.addAll(o.getFieldValues());
        }
        if (obj instanceof JavaClass) {
            values.addAll(((JavaClass)obj).getStaticFieldValues());
        }
        if (obj instanceof ObjectArrayInstance) {
            ObjectArrayInstance oarr = (ObjectArrayInstance)obj;
            values.addAll(oarr.getValues());
        }
        if (!values.isEmpty()) {
            for (Object value : values) {
                if (value instanceof ObjectFieldValue && ((ObjectFieldValue) value).getInstance() != null) {
                    Instance inst = ((ObjectFieldValue) value).getInstance();
                    if (includeWeak || !isWeakRef(inst)) {
                        if (inst.getJavaClass().getName().equals("java.lang.Class")) {
                            JavaClass jc = delegate.getJavaClassByID(inst.getInstanceId());
                            if (jc != null) {
                                instances.add(jc);
                            } else {
                                instances.add(inst);
                            }
                        } else {
                            instances.add(inst);
                        }
                    }
                } else if (value instanceof Instance) {
                    if (includeWeak || !isWeakRef((Instance)value)) {
                        instances.add(value);
                    }
                }
            }
        }
        return instances.iterator();
    }

    public Iterator getFinalizerObjects() {
        JavaClass clazz = findClass("java.lang.ref.Finalizer"); // NOI18N
        Instance queue = (Instance) clazz.getValueOfStaticField("queue"); // NOI18N
        Instance head = (Instance) queue.getValueOfField("head"); // NOI18N

        List finalizables = new ArrayList();
        if (head != null) {
            while (true) {
                Instance referent = (Instance) head.getValueOfField("referent"); // NOI18N
                Instance next = (Instance) head.getValueOfField("next"); // NOI18N

                finalizables.add(referent);
                if (next == null || next.equals(head)) {
                    break;
                }
                head = next;
            }
        }
        return finalizables.iterator();
    }

    public Iterator getRoots() {
        return delegate.getGCRoots().iterator();
    }
    
    private Set getRootsInstances() {
        Set<Object> roots = new HashSet<Object>();
        for(GCRoot root : delegate.getGCRoots()) {
            Instance inst = root.getInstance();
            if (inst.getJavaClass().getName().equals("java.lang.Class")) {
                JavaClass jc = delegate.getJavaClassByID(inst.getInstanceId());
                if (jc != null) {
                    roots.add(jc);
                } else {
                    roots.add(inst);
                }
            } else {
                roots.add(inst);
            }
        }
        return roots;
    }

    public GCRoot[] getRootsArray() {
        Collection<GCRoot> rootList = delegate.getGCRoots();
        return rootList.toArray(new GCRoot[0]);
    }
   
    public ReferenceChain[] rootsetReferencesTo(Instance target, boolean includeWeak) {
        class State {
            private Iterator<Instance> iterator;
            private ReferenceChain path;
            private AtomicLong hits = new AtomicLong(0);

            public State(ReferenceChain path, Iterator<Instance> iterator) {
                this.iterator = iterator;
                this.path = path;
            }
        }
        Deque<State> stack = new ArrayDeque<State>();
        Set ignored = new HashSet();
        
        List<ReferenceChain> result = new ArrayList<ReferenceChain>();
        
        Iterator toInspect = getRootsInstances().iterator();
        ReferenceChain path = null;
        State s = new State(path, toInspect);
        
        do {
            if (path != null && path.getObj().equals(target)) {
                result.add(path);
                s.hits.incrementAndGet();
            } else {
                while(!engine.isCancelled() && toInspect.hasNext()) {
                    Object node = toInspect.next();
                    if (path != null && path.contains(node)) continue;
                    if (ignored.contains(node)) continue;

                    stack.push(s);
                    path = new ReferenceChain(delegate, node, path);
                    toInspect = getReferees(node, includeWeak);
                    s = new State(path, toInspect);
                }
                if (path != null && path.getObj().equals(target)) {
                    result.add(path);
                    s.hits.incrementAndGet();
                }
            }
            State s1 = stack.poll();
            if (s1 == null) break;
            s1.hits.addAndGet(s.hits.get());
            if (s.hits.get() == 0L && path != null) {
                ignored.add(path.getObj());
            }
            s = s1;
            path = s.path;
            toInspect = s.iterator;
        } while (!engine.isCancelled());

        return result.toArray(new ReferenceChain[0]);
    }

    private boolean isAssignable(JavaClass from, JavaClass to) {
        if (from == to) {
            return true;
        } else if (from == null) {
            return false;
        } else {
            return isAssignable(from.getSuperClass(), to);
        // Trivial tail recursion:  I have faith in javac.
        }
    }
    
    private boolean isWeakRef(Instance inst) {
        return weakReferenceClass != null && isAssignable(inst.getJavaClass(), weakReferenceClass);
    }

    public JavaClass getWeakReferenceClass() {
        return weakReferenceClass;
    }

    public int getReferentFieldIndex() {
        return referentFieldIndex;
    }

    public void setReachableExcludes(ReachableExcludes e) {
        reachableExcludes = e;
    }

    public ReachableExcludes getReachableExcludes() {
        return reachableExcludes;
    }

    public String valueString(Instance instance) {
        if (instance == null) return null;
        try {
            if (instance.getJavaClass().getName().equals(String.class.getName())) {
                Class proxy = Class.forName("org.graalvm.visualvm.lib.jfluid.heap.HprofProxy"); // NOI18N
                Method method = proxy.getDeclaredMethod("getString", Instance.class); // NOI18N
                method.setAccessible(true);
                return (String) method.invoke(proxy, instance);
            } else if (instance.getJavaClass().getName().equals("char[]")) { // NOI18N
                Method method = instance.getClass().getDeclaredMethod("getChars", int.class, int.class);
                method.setAccessible(true);
                char[] chars = (char[])method.invoke(instance, 0, ((PrimitiveArrayInstance)instance).getLength());
                if (chars != null) {
                    return new String(chars);
                } else {
                    return "*null*"; // NOI18N
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Snapshot.class.getName()).log(Level.WARNING, "Error getting toString() value of an instance dump", ex); // NO18N
        }
        return instance.toString();
    }
}
