/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.openide.util.NbBundle;


/**
 * An object collector that classifies the objects according to their type.
 * For each logged type, it counts the number of instances and sums their total
 * size.
 *
 * @param T the entry type, which can add additional properties
 * @author nenik
 */
public final class Distribution {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     *
     */
    public static class Entry {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JavaClass type;
        private int count;
        private int size;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Creates an entry with given size and count=1
         * @param size the size this entry represent.
         */
        private Entry(JavaClass cls) {
            this.type = cls;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public final int getCount() {
            return count;
        }

        public final int getSize() {
            return size;
        }

        @Override
        public String toString() {
            return Utils.printClass(null, type.getName()) + ": " + getCount() + "/" + getSize() + "B"; // NOI18N
        }

        private void count(Instance in) {
            assert ((type == null) || type.equals(in.getJavaClass()));
            count++;
            size += in.getSize();
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Entry allEntry = new Entry(null);
    private HashMap<JavaClass, Entry> map = new HashMap<>();
    private Set<Instance> counted = new HashSet<>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     *
     */
    public Distribution() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Set<JavaClass> getClasses() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public boolean isCounted(Instance in) {
        return counted.contains(in);
    }

    public Entry getResults(JavaClass cls) {
        return map.get(cls);
    }

    public void add(Instance in) {
        if (!counted.add(in)) {
            return;
        }

        JavaClass cls = in.getJavaClass();
        Entry en = map.get(cls);

        if (en == null) {
            map.put(cls, en = new Entry(cls));
        }

        en.count(in);
        allEntry.count(in);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(NbBundle.getMessage(Distribution.class, "FMT_DistEntry",
                allEntry.getCount(),
                allEntry.getSize()));

        for (JavaClass key : getClasses()) {
            result.append("  ").append(getResults(key)).append("\n"); // NOI18N
        }

        return result.toString();
    }
}
