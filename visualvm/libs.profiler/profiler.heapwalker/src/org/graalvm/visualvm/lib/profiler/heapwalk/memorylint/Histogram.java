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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.openide.util.NbBundle;


/**
 *
 * @param T the entry type, which can add additional properties
 * @author nenik
 */
public final class Histogram<T extends Histogram.Entry> {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     *
     */
    public static class Entry<T extends Entry> {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private int count;
        private long size;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Creates an entry with given size and count=1
         * @param size the size this entry represent.
         */
        public Entry(long size) {
            this.count = 1;
            this.size = size;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public final int getCount() {
            return count;
        }

        public final long getSize() {
            return size;
        }

        @Override
        public String toString() {
            return "#:" + getCount() + "/" + getSize() + "B\n"; // NOI18N
        }

        /**
         * A callback for subclasses that should add the additional properties
         * from given source to this entry. The infrastructure takes care of
         * adding base properties. There's no need to call super.add().
         *
         * @param source The Entry to add to this
         */
        protected void add(T source) {
        }

        private void doAdd(T entry) {
            count += ((Entry)entry).count;
            size += ((Entry)entry).size;
            add(entry);
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HashMap<String, T> map = new HashMap<String, T>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     *
     */
    public Histogram() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static final Comparator<Entry> sortByCount() {
        return new Comparator<Entry>() {
                public int compare(Entry o1, Entry o2) {
                    return o2.count - o1.count;
                }
            };
    }

    public static final Comparator<Entry> sortBySize() {
        return new Comparator<Entry>() {
                public int compare(Entry o1, Entry o2) {
                    return Long.compare(o2.size,o1.size);
                }
            };
    }

    public SortedMap<String, T> getResults(final Comparator<Entry> comparator) {
        SortedMap<String, T> sm = new TreeMap<String, T>(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    T t1 = map.get(o1);
                    T t2 = map.get(o2);
                    int delta = comparator.compare(t1, t2);

                    if (delta == 0) {
                        delta = o1.compareTo(o2);
                    }

                    return delta;
                }
            });
        sm.putAll(map);

        return sm;
    }

    public void add(String key, T entry) {
        T current = map.get(key);

        if (current != null) {
            ((Entry)current).doAdd(entry);
        } else {
            map.put(key, entry);
        }
    }

    public String toString(int treshold) {
        StringBuilder result = new StringBuilder();
        long totalSize = 0;
        int totalCount = 0;
        SortedMap<String, T> bySize = getResults(sortBySize());

        for (Map.Entry<String, T> entry : bySize.entrySet()) {
            long size = entry.getValue().getSize();

            if (size > treshold) {
                result.append(entry.getKey()).append(": ").append(entry.getValue()).append("<br>"); // NOI18N
            }

            totalSize += size;
            totalCount += entry.getValue().getCount();
        }

        result.append(NbBundle.getMessage(Histogram.class, "FMT_HistogramSum", totalCount, totalSize));

        return result.toString();
    }
}
