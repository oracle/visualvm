
/*
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/, and in the file LICENSE.html in the
 * doc directory.
 * 
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun. Portions created by Bill Foote and others
 * at Javasoft/Sun are Copyright (C) 1997-2004. All Rights Reserved.
 * 
 * In addition to the formal license, I ask that you don't
 * change the history or donations files without permission.
 * 
 */
package org.netbeans.modules.profiler.heapwalk.oql.model;

import java.util.*;
import org.netbeans.lib.profiler.heap.ArrayItemValue;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.lib.profiler.heap.Value;

/**
 *
 * @version     1.26, 10/08/98 [jhat @(#)Snapshot.java	1.16 06/10/27]
 * @author      Bill Foote
 */
/**
 * Represents a snapshot of the Java objects in the VM at one instant.
 * This is the top-level "model" object read out of a single .hprof or .bod
 * file.
 */
public class Snapshot {

    public static long SMALL_ID_MASK = 0x0FFFFFFFFL;
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private final Heap delegate;
    private JavaClass weakReferenceClass;
    private int referentFieldIndex;
    private ReachableExcludes reachableExcludes;

    public Snapshot(Heap heap) {
        this.delegate = heap;
        init();
    }

    private void init() {
        weakReferenceClass = findClass("java.lang.ref.Reference");
        if (weakReferenceClass == null) {	// JDK 1.1.x
            weakReferenceClass = findClass("sun.misc.Ref");
            referentFieldIndex = 0;
        } else {
            List flds = weakReferenceClass.getFields();
            int fldsCount = flds.size();

            for (int i = 0; i < fldsCount; i++) {
                if ("referent".equals(((Field) flds.get(i)).getName())) {
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
        return delegate.getJavaClassByName(preprocessClassName(name));
    }

    private String preprocessClassName(String className) {
        int arrDim = 0;
        if (className.startsWith("[")) {
            arrDim = className.lastIndexOf("[") + 1;

            className = className.substring(arrDim);
        }
        if (className.length() == 1) {
            if (className.equals("I")) {
                className = "int";
            } else if (className.equals("J")) {
                className = "long";
            } else if (className.equals("D")) {
                className = "double";
            } else if (className.equals("F")) {
                className = "float";
            } else if (className.equals("B")) {
                className = "byte";
            } else if (className.equals("S")) {
                className = "short";
            } else if (className.equals("C")) {
                className = "char";
            } else if (className.equals("Z")) {
                className = "boolean";
            }
        }
        if (arrDim > 0 && className.startsWith("L")) {
            className = className.substring(1);
        }
        StringBuilder sb = new StringBuilder(className);
        for (int i = 0; i < arrDim; i++) {
            sb.append("[]");
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

    public Iterator getInstances(final JavaClass clazz, final boolean includeSubclasses) {
        return new Iterator() {

            private Deque<JavaClass> toInspect = new ArrayDeque<JavaClass>();
            private JavaClass popped = null;
            private Iterator inspecting = null;


            {
                toInspect.push(clazz);
            }

            public boolean hasNext() {
                setupIterator();

                return inspecting != null && inspecting.hasNext();
            }

            public Object next() {
                setupIterator();

                if (inspecting == null || !inspecting.hasNext()) {
                    throw new NoSuchElementException();
                }

                Object retVal = inspecting.next();
                return retVal;
            }

            private void setupIterator() {
                while (!toInspect.isEmpty() && (inspecting == null || !inspecting.hasNext())) {
                    popped = toInspect.poll();
                    if (popped != null) {
                        inspecting = popped.getInstances().iterator();
                        if (includeSubclasses) {
                            for (Object subclass : popped.getSubClasses()) {
                                if (!toInspect.contains(subclass)) {
                                    toInspect.offer(((JavaClass) subclass));
                                }
                            }
                        }
                    } else {
                        inspecting = null;
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator getReferrers(Instance obj) {
        List instances = new ArrayList();
        for (Iterator iter = obj.getReferences().iterator(); iter.hasNext();) {
            Value val = (Value) iter.next();
            instances.add(val.getDefiningInstance());
        }
        return instances.iterator();
    }

    public Iterator getReferees(Instance obj) {
        List instances = new ArrayList();
        for (Object value : obj.getFieldValues()) {
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
//        Vector obj;
//        if (finalizablesCache != null &&
//            (obj = finalizablesCache.get()) != null) {
//            return obj.elements();
//        }

        JavaClass clazz = findClass("java.lang.ref.Finalizer");
        Instance queue = ((ObjectFieldValue) clazz.getValueOfStaticField("queue")).getInstance();
        ObjectFieldValue headFld = (ObjectFieldValue) queue.getValueOfField("head");

        List finalizables = new ArrayList();
        if (headFld != null) {
            Instance head = (Instance) headFld.getInstance();
            while (true) {
                ObjectFieldValue referentFld = (ObjectFieldValue) head.getValueOfField("referent");
                ObjectFieldValue nextFld = (ObjectFieldValue) head.getValueOfField("next");

                if (nextFld == null || nextFld.getInstance().equals(head)) {
                    break;
                }
                head = (Instance) nextFld.getInstance();
                finalizables.add(referentFld.getInstance());
            }
        }
//        finalizablesCache = new SoftReference<List>(finalizables);
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
            if (curr.getNearestGCRootPointer() != null) {
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

//        ReferenceChain[] realResult = new ReferenceChain[result.size()];
//        for (int i = 0; i < result.size(); i++) {
//            realResult[i] = (ReferenceChain) result.elementAt(i);
//        }
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
//
//    public boolean getUnresolvedObjectsOK() {
//        return unresolvedObjectsOK;
//    }
//
//    public void setUnresolvedObjectsOK(boolean v) {
//        unresolvedObjectsOK = v;
//    }

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

//    // package privates
//    void addReferenceFromRoot(Root r, JavaHeapObject obj) {
//        Root root = rootsMap.get(obj);
//        if (root == null) {
//            rootsMap.put(obj, r);
//        } else {
//            rootsMap.put(obj, root.mostInteresting(r));
//        }
//    }
//
//    Root getRoot(JavaHeapObject obj) {
//        return rootsMap.get(obj);
//    }
//
//    JavaClass getJavaLangClass() {
//        return javaLangClass;
//    }
//
//    JavaClass getJavaLangString() {
//        return javaLangString;
//    }
//
//    JavaClass getJavaLangClassLoader() {
//        return javaLangClassLoader;
//    }
//
//    JavaClass getOtherArrayType() {
//        if (otherArrayType == null) {
//            synchronized (this) {
//                if (otherArrayType == null) {
//                    addFakeClass(new JavaClass("[<other>", 0, 0, 0, 0,
//                            EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY,
//                            0));
//                    otherArrayType = findClass("[<other>");
//                }
//            }
//        }
//        return otherArrayType;
//    }
//
//    JavaClass getArrayClass(String elementSignature) {
//        JavaClass clazz;
//        synchronized (classes) {
//            clazz = findClass("[" + elementSignature);
//            if (clazz == null) {
//                clazz = new JavaClass("[" + elementSignature, 0, 0, 0, 0,
//                        EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
//                addFakeClass(clazz);
//            // This is needed because the JDK only creates Class structures
//            // for array element types, not the arrays themselves.  For
//            // analysis, though, we need to pretend that there's a
//            // JavaClass for the array type, too.
//            }
//        }
//        return clazz;
//    }
//
//    ReadBuffer getReadBuffer() {
//        return readBuf;
//    }
//
//    void setNew(JavaHeapObject obj, boolean isNew) {
//        initNewObjects();
//        if (isNew) {
//            newObjects.put(obj, Boolean.TRUE);
//        }
//    }
//
//    boolean isNew(JavaHeapObject obj) {
//        if (newObjects != null) {
//            return newObjects.get(obj) != null;
//        } else {
//            return false;
//        }
//    }
//
//    // Internals only below this point
//    private Number makeId(long id) {
//        if (identifierSize == 4) {
//            return new Integer((int) id);
//        } else {
//            return new Long(id);
//        }
//    }
//
//    private void putInClassesMap(JavaClass c) {
//        String name = c.getName();
//        if (classes.containsKey(name)) {
//            // more than one class can have the same name
//            // if so, create a unique name by appending
//            // - and id string to it.
//            name += "-" + c.getIdString();
//        }
//        classes.put(c.getName(), c);
//    }
//
//    private void addFakeClass(JavaClass c) {
//        putInClassesMap(c);
//        c.resolve(this);
//    }
//
//    private void addFakeClass(Number id, JavaClass c) {
//        fakeClasses.put(id, c);
//        addFakeClass(c);
//    }
//
//    private synchronized void initNewObjects() {
//        if (newObjects == null) {
//            synchronized (this) {
//                if (newObjects == null) {
//                    newObjects = new HashMap<JavaHeapObject, Boolean>();
//                }
//            }
//        }
//    }
//
//    private synchronized void initSiteTraces() {
//        if (siteTraces == null) {
//            synchronized (this) {
//                if (siteTraces == null) {
//                    siteTraces = new HashMap<JavaHeapObject, StackTrace>();
//                }
//            }
//        }
//    }
}
