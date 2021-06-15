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

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint;

import java.util.ArrayDeque;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import java.util.List;
import java.util.Queue;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.Type;


/**
 * An utility class capable of walking the object graph and counting all
 * found objects.
 *
 * @param T the entry type, which can add additional properties
 * @author nenik
 */
public final class Walker {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    private static final Type OBJECT = new Type() {
        public String getName() { return "object"; }
    };

    private static class ArrayEntryValue implements ObjectFieldValue, Field {
        int idx;
        private Instance src;
        private Instance target;

        ArrayEntryValue(int idx, Instance src, Instance target) {
            this.idx = idx;
            this.src = src;
            this.target = target;
        }

        public Instance getInstance() {
            return target;
        }

        public Field getField() {
            return this;
        }

        public String getValue() {
            return "Instance #" + target.getInstanceId();
        }

        public Instance getDefiningInstance() {
            return src;
        }

        public JavaClass getDeclaringClass() {
            return src.getJavaClass(); // XXX
        }

        public String getName() {
            return "[" + idx + "]";
        }

        public boolean isStatic() {
            return false;
        }

        public Type getType() {
            return OBJECT;
        }
        
    }
    
    public static interface Filter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean accept(ObjectFieldValue val);
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Distribution log = new Distribution();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public Walker() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Distribution getResults() {
        return log;
    }

    public void walk(Instance in) {
        walk(in, null);
    }

    public void walk(Instance in, Filter f) {
        Queue<Instance> q = new ArrayDeque<Instance>();
        q.add(in);

        log.add(in);

        while (!q.isEmpty()) {
            Instance act = q.poll();
            
            if (act instanceof ObjectArrayInstance) {
                List<Instance> out = ((ObjectArrayInstance)act).getValues();
                int i = 0;
                for (Instance target : out) {
                    if (target != null) {
                        if ((f == null || f.accept(new ArrayEntryValue(i, act, target))) && !log.isCounted(target)) {
                            log.add(target);
                            q.add(target);
                        }
                    }
                    i++;
                }
            }

            List<FieldValue> out = act.getFieldValues();

            for (FieldValue fv : out) {
                if (fv instanceof ObjectFieldValue) {
                    ObjectFieldValue ofv = (ObjectFieldValue) fv;

                    if ((f != null) && !f.accept(ofv)) {
                        continue;
                    }

                    Instance target = ofv.getInstance();

                    if ((target != null) && !log.isCounted(target)) {
                        log.add(target);
                        q.add(target);
                    }
                }
            }
        }
    }
}
