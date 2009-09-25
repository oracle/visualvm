/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.profiler.heapwalk.memorylint;

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
        private int size;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Creates an entry with given size and count=1
         * @param size the size this entry represent.
         */
        public Entry(int size) {
            this.count = 1;
            this.size = size;
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
                    return o2.size - o1.size;
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
        StringBuffer result = new StringBuffer();
        int totalSize = 0;
        int totalCount = 0;
        SortedMap<String, T> bySize = getResults(sortBySize());

        for (Map.Entry<String, T> entry : bySize.entrySet()) {
            int size = entry.getValue().getSize();

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
