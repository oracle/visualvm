/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
package org.netbeans.modules.profiler.oql.engine.api.impl;

import org.netbeans.modules.profiler.oql.engine.api.ReferenceChain;
import java.lang.reflect.Method;
import java.util.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.Value;

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

    public Snapshot(Heap heap) {
        this.delegate = heap;
        init();
    }

    private void init() {
        weakReferenceClass = findClass("java.lang.ref.Reference"); // NOI18N
        if (weakReferenceClass == null) {	// JDK 1.1.x
            weakReferenceClass = findClass("sun.misc.Ref"); // NOI18N
            referentFieldIndex = 0;
        } else {
            List flds = weakReferenceClass.getFields();
            int fldsCount = flds.size();

            for (int i = 0; i < fldsCount; i++) {
                if ("referent".equals(((Field) flds.get(i)).getName())) { // NOI18N
                    referentFieldIndex = i;
                    break;
                }
            }
        }
    }
    
    public int getMinimumObjectSize() {
        return 4;
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
            arrDim = className.lastIndexOf("[") + 1; // NOI18N

            className = className.substring(arrDim);
        }
        if (className.length() == 1) {
            if (className.equals("I")) { // NOI18N
                className = "int"; // NOI18N
            } else if (className.equals("J")) { // NOI18N
                className = "long"; // NOI18N
            } else if (className.equals("D")) { // NOI18N
                className = "double"; // NOI18N
            } else if (className.equals("F")) { // NOI18N
                className = "float"; // NOI18N
            } else if (className.equals("B")) { // NOI18N
                className = "byte"; // NOI18N
            } else if (className.equals("S")) { // NOI18N
                className = "short"; // NOI18N
            } else if (className.equals("C")) { // NOI18N
                className = "char"; // NOI18N
            } else if (className.equals("Z")) { // NOI18N
                className = "boolean"; // NOI18N
            }
        }
        if (arrDim > 0 && className.startsWith("L")) { // NOI18N
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
            return delegate.getGCRoot(gcInstance);
        }
        return null;
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
        return new TreeIterator<Instance, JavaClass>(clazz) {

            @Override
            protected Iterator<Instance> getSameLevelIterator(JavaClass popped) {
                return popped.getInstances().iterator();
            }

            @Override
            protected Iterator<JavaClass> getTraversingIterator(JavaClass popped) {
                return includeSubclasses ? popped.getSubClasses().iterator() : Collections.EMPTY_LIST.iterator();
            }
        };
    }

    public Iterator getReferrers(Instance obj) {
        List instances = new ArrayList();
        List references = null;
        references = obj.getReferences();
        if (references != null) {
            for (Iterator iter = references.iterator(); iter.hasNext();) {
                Value val = (Value) iter.next();
                instances.add(val.getDefiningInstance());
            }
        }
        return instances.iterator();
    }

    public Iterator getReferees(Object obj) {
        List instances = new ArrayList();
        List values = null;
        if (obj instanceof Instance) {
            values = ((Instance)obj).getFieldValues();
        } else if (obj instanceof JavaClass) {
            values = ((JavaClass)obj).getStaticFieldValues();
        }
        if (values != null) {
            for (Object value : values) {
                if (value instanceof ObjectFieldValue) {
                    instances.add(((ObjectFieldValue) value).getInstance());
                }
            }
        }
        return instances.iterator();
    }

    public Iterator getReferees(JavaClass clz) {
        List instances = new ArrayList();
        for (Object value : clz.getStaticFieldValues()) {
            if (value instanceof ObjectFieldValue) {
                instances.add(((ObjectFieldValue) value).getInstance());
            }
        }
        return instances.iterator();
    }

    public JavaClass[] getClassesArray() {
        return (JavaClass[]) delegate.getAllClasses().toArray();
    }

    public Iterator getFinalizerObjects() {
        JavaClass clazz = findClass("java.lang.ref.Finalizer"); // NOI18N
        Instance queue = ((ObjectFieldValue) clazz.getValueOfStaticField("queue")).getInstance(); // NOI18N
        ObjectFieldValue headFld = (ObjectFieldValue) queue.getValueOfField("head"); // NOI18N

        List finalizables = new ArrayList();
        if (headFld != null) {
            Instance head = (Instance) headFld.getInstance();
            while (true) {
                ObjectFieldValue referentFld = (ObjectFieldValue) head.getValueOfField("referent"); // NOI18N
                ObjectFieldValue nextFld = (ObjectFieldValue) head.getValueOfField("next"); // NOI18N

                if (nextFld == null || nextFld.getInstance().equals(head)) {
                    break;
                }
                head = (Instance) nextFld.getInstance();
                finalizables.add(referentFld.getInstance());
            }
        }
        return finalizables.iterator();
    }

    public Iterator getRoots() {
        return delegate.getGCRoots().iterator();
    }

    public GCRoot[] getRootsArray() {
        return (GCRoot[]) delegate.getGCRoots().toArray();
    }

    public ReferenceChain[] rootsetReferencesTo(Instance target, boolean includeWeak) {
        Queue<ReferenceChain> fifo = new LinkedList<ReferenceChain>();
        // Must be a fifo to go breadth-first
        Map visited = new HashMap();

        // Objects are added here right after being added to fifo.
        List<ReferenceChain> result = new ArrayList<ReferenceChain>();
        visited.put(target, target);
        fifo.add(new ReferenceChain(target, null));

        ReferenceChain chain = null;
        do {
            chain = (ReferenceChain) fifo.poll();
            if (chain == null) {
                continue;
            }

            Instance curr = chain.getObj();
            if (curr.isGCRoot()) {
                result.add(chain);
            // Even though curr is in the rootset, we want to explore its
            // referers, because they might be more interesting.
            }
            List<Instance> referers = getReferers(curr);
            for (Instance t : referers) {
                if (t != null && !visited.containsKey(t)) {
                    if (includeWeak || !refersOnlyWeaklyTo(t, curr)) {
                        visited.put(t, t);
                        fifo.add(new ReferenceChain(t, chain));
                    }
                }
            }
        } while (chain != null);

        return result.toArray(new ReferenceChain[result.size()]);
    }

    private List<Instance> getReferers(Instance instance) {
        List<Instance> referers = new ArrayList<Instance>();

        for (Object fldObj : instance.getReferences()) {
            if (fldObj instanceof Value) {
                referers.add(((Value) fldObj).getDefiningInstance());
            }
        }
        return referers;
    }

    private boolean refersOnlyWeaklyTo(Instance from, Instance to) {
        if (getWeakReferenceClass() != null) {
            if (isAssignable(getWeakReferenceClass(), from.getJavaClass())) {
                //
                // REMIND:  This introduces a dependency on the JDK
                // 	implementation that is undesirable.
                FieldValue[] flds = (FieldValue[]) from.getFieldValues().toArray();
                for (int i = 0; i < flds.length; i++) {
                    if (i != referentFieldIndex) {
                        if (flds[i] instanceof ObjectFieldValue) {
                            if (((ObjectFieldValue) flds[i]).getInstance() == to) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
        }
        return false;
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
                Class proxy = Class.forName("org.netbeans.lib.profiler.heap.HprofProxy"); // NOI18N
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
