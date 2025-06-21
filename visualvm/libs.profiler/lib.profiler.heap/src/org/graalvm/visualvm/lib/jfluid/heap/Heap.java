/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;


/**
 * This is top-level interface representing one instance of heap dump.
 * @author Tomas Hurka
 */
public interface Heap {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * computes List of all {@link JavaClass} instances in this heap.
     * The classes are ordered according to the position in the dump file.
     * <br>
     * Speed: slow for the first time, subsequent invocations are fast.
     * @return list of all {@link JavaClass} in the heap.
     */
    List<JavaClass> getAllClasses();

    /**
     * computes List of N biggest {@link Instance}-s in this heap.
     * The instances are ordered according to their retained size.
     * <br>
     * Speed: slow for the first time, subsequent invocations are normal.
     * @param number size of the returned List
     * @return list of N biggest {@link Instance}.
     */
    List<Instance> getBiggestObjectsByRetainedSize(int number);

    /**
     * returns list of {@link GCRoot} for {@link Instance}.
     * <br>
     * Speed: normal for first invocation, fast for subsequent
     * @param instance {@link Instance} whose associated list of {@link GCRoot} is to be returned.
     * @return list of {@link GCRoot} for corresponding instance or empty list if instance is not GC root.
     */
    Collection<GCRoot> getGCRoots(Instance instance);

    /**
     * returns list of all GC roots.
     * <br>
     * Speed: normal for first invocation, fast for subsequent
     * @return list of {@link GCRoot} instances representing all GC roots.
     */
    Collection<GCRoot> getGCRoots();

    /**
     * computes {@link Instance} for instanceId.
     * <br>
     * Speed: fast
     * @param instanceId unique ID of {@link Instance}
     * @return return <CODE>null</CODE> if there is no {@link Instance} with instanceId, otherwise
     * corresponding {@link Instance} is returned so that
     * <CODE>heap.getInstanceByID(instanceId).getInstanceId() == instanceId</CODE>
     */
    Instance getInstanceByID(long instanceId);

    /**
     * computes {@link JavaClass} for javaclassId.
     * <br>
     * Speed: fast
     * @param javaclassId unique ID of {@link JavaClass}
     * @return return <CODE>null</CODE> if there is no java class with javaclassId, otherwise corresponding {@link JavaClass}
     * is returned so that <CODE>heap.getJavaClassByID(javaclassId).getJavaClassId() == javaclassId</CODE>
     */
    JavaClass getJavaClassByID(long javaclassId);

    /**
     * computes {@link JavaClass} for fully qualified name.
     * <br>
     * Speed: slow
     * @param fqn fully qualified name of the java class.
     * @return return <CODE>null</CODE> if there is no class with fqn name, otherwise corresponding {@link JavaClass}
     * is returned so that <CODE>heap.getJavaClassByName(fqn).getName().equals(fqn)</CODE>
     */
    JavaClass getJavaClassByName(String fqn);

    /**
     * computes collection of {@link JavaClass} filtered by regular expression.
     * <br>
     * Speed: slow
     * @param regexp regular expression for java class name.
     * @return return collection of {@link JavaClass} instances, which names satisfy the regexp expression. This
     * collection is empty if no class matches the regular expression
     */
    Collection<JavaClass> getJavaClassesByRegExp(String regexp);

    /**
     * returns an iterator over the {@link Instance}es in the whole heap. There are no
     * guarantees concerning the order in which the {@link Instance}es are returned.
     * <br>
     * Speed: fast
     *
     * @return an <tt>Iterator</tt> over the {@link Instance}es in this heap
     */
    public Iterator<Instance> getAllInstancesIterator();
    
    /**
     * returns optional summary information of the heap.
     * If this information is not available in the dump,
     * some data (like number of instances) are computed
     * from the dump itself.
     * <br>
     * Speed: fast if the summary is available in dump, slow if
     * summary needs to be computed from dump.
     * @return {@link HeapSummary} of the heap
     */
    HeapSummary getSummary();

    /**
     * Determines the system properties of the {@link Heap}. It returns {@link Properties} with the same
     * content as if {@link System#getProperties()} was invoked in JVM, where this heap dump was taken.
     * <br>
     * Speed: slow
     * @return the system properties or <CODE>null</CODE> if the system properties cannot be computed from
     * this {@link Heap}
     */
    Properties getSystemProperties();

    boolean isRetainedSizeComputed();
    boolean isRetainedSizeByClassComputed();
}
