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

import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
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
    private HashMap<JavaClass, Entry> map = new HashMap<JavaClass, Entry>();
    private Set<Instance> counted = new HashSet<Instance>();

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
        StringBuffer result = new StringBuffer();

        result.append(NbBundle.getMessage(Distribution.class, "FMT_DistEntry",
                allEntry.getCount(),
                allEntry.getSize()));

        for (JavaClass key : getClasses()) {
            result.append("  " + getResults(key) + "\n"); // NOI18N
        }

        return result.toString();
    }
}
