/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.oql.engine.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ReachableObjects {
    private ReachableExcludes excludes;
    private Instance root;
    private Set<Instance> alreadyReached;

    public ReachableObjects(Instance root, final ReachableExcludes excludes) {
        this.root = root;
        this.excludes = excludes;
        alreadyReached = new HashSet();
    }

    public Instance getRoot() {
        return root;
    }

    public Iterator<Instance> getReachables() {
        return new TreeIterator<Instance, Instance>(root) {

            @Override
            protected Iterator<Instance> getSameLevelIterator(Instance popped) {
                Collection<Instance> instances = new ArrayList<Instance>();
                for(Object fv : popped.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        if (excludes == null || !excludes.isExcluded(getFQFieldName(((FieldValue)fv).getField()))) {
                            Instance i = ((ObjectFieldValue)fv).getInstance();
                            if (i != null && !alreadyReached.contains(i)) {
                                instances.add(i);
                                alreadyReached.add(i);
                            }
                        }
                    }
                }
                if (popped instanceof ObjectArrayInstance) {
                    for(Instance i : ((ObjectArrayInstance)popped).getValues()) {
                        if (i != null && !alreadyReached.contains(i)) {
                            instances.add(i);
                            alreadyReached.add(i);
                        }
                    }
                }
                return instances.iterator();
            }

            @Override
            protected Iterator<Instance> getTraversingIterator(Instance popped) {
                Collection<Instance> instances = new ArrayList<Instance>();
                for(Object fv : popped.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        if (excludes == null || !excludes.isExcluded(getFQFieldName(((FieldValue)fv).getField()))) {
                            Instance i = ((ObjectFieldValue)fv).getInstance();
                            if (i != null) {
                                instances.add(i);
                            }
                        }
                    }
                }
                if (popped instanceof ObjectArrayInstance) {
                    for(Instance el : ((ObjectArrayInstance)popped).getValues()) {
                        if (el != null) {
                            instances.add(el);
                        }
                    }
                }
                return instances.iterator();
            }
        };
    }

    public long getTotalSize() {
        return -1;
    }

    private String getFQFieldName(Field fld) {
        return fld.getDeclaringClass().getName() + "." + fld.getName();
    }
}
