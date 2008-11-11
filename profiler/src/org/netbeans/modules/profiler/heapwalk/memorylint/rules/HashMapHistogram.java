/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.heapwalk.memorylint.rules;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.modules.profiler.heapwalk.memorylint.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openide.util.NbBundle;


@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.heapwalk.memorylint.Rule.class)
public class HashMapHistogram extends IteratingRule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class HashmapEntry extends Histogram.Entry<HashmapEntry> {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        int hmeCount;
        int strCount;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        HashmapEntry(int size, int hmeCount, int strCount) {
            super(size);
            this.hmeCount = hmeCount;
            this.strCount = strCount;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public String toString() {
            return "#:" + getCount() + "/" + getSize() + "B, " + hmeCount + " HMEs, " + strCount + " Strings<br>"; // NOI18N
        }

        @Override
        protected void add(HashmapEntry source) {
            hmeCount += source.hmeCount;
            strCount += source.strCount;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldHMEKey;
    private FieldAccess fldHMENext;
    private FieldAccess fldHMEValue;
    private FieldAccess fldHMTable;
    private FieldAccess fldSValue;
    private Histogram<HashmapEntry> byIncomming;
    private JavaClass clsHM;
    private JavaClass clsHME;
    private JavaClass clsString;
    private Set<Instance> known = new HashSet<Instance>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HashMapHistogram() {
        super(NbBundle.getMessage(HashMapHistogram.class, "LBL_HMH_Name"),
                NbBundle.getMessage(HashMapHistogram.class, "LBL_HMH_Desc"),
                "java.util.HashMap"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(HashMapHistogram.class, "LBL_HMH_LongDesc");
    }

    protected void perform(Instance hm) {
        HashmapEntry he = sizeOfHashmap(hm);
        String incomming = getContext().getRootIncommingString(hm);
        incomming = Utils.printClass(getContext(), incomming);
        byIncomming.add(incomming, he);
    }

    protected @Override void prepareRule(MemoryLint context) {
        Heap heap = context.getHeap();
        clsString = heap.getJavaClassByName("java.lang.String"); // NOI18N
        clsHM = heap.getJavaClassByName("java.util.HashMap"); // NOI18N
        clsHME = heap.getJavaClassByName("java.util.HashMap$Entry"); // NOI18N
        fldSValue = new FieldAccess(clsString, "value"); // NOI18N
        fldHMTable = new FieldAccess(clsHM, "table"); // NOI18N
        fldHMEKey = new FieldAccess(clsHME, "key"); // NOI18N
        fldHMEValue = new FieldAccess(clsHME, "value"); // NOI18N
        fldHMENext = new FieldAccess(clsHME, "next"); // NOI18N
        byIncomming = new Histogram<HashmapEntry>();
    }

    protected @Override void summary() {
        getContext().appendResults(byIncomming.toString(50000));
    }

    private boolean add(Instance inst) {
        if (known.contains(inst)) {
            return false;
        }

        known.add(inst);

        return true;
    }

    private int sizeIfNewString(Instance obj) {
        if (obj == null) {
            return 0;
        }

        if ("java.lang.String".equals(obj.getJavaClass().getName())) { // NOI18N
            if (add(obj)) {
                int sz = obj.getSize();
                Instance arr = fldSValue.getRefValue(obj);

                if ((arr != null) && add(arr)) {
                    sz += arr.getSize();
                }

                return sz;
            }
        }

        return 0;
    }

    private HashmapEntry sizeOfHashmap(Instance hm) {
        ObjectArrayInstance table = (ObjectArrayInstance) fldHMTable.getRefValue(hm);
        int sum = hm.getSize() + table.getSize();
        int hmeCount = 0;
        int strCount = 0;

        List<Instance> tval = table.getValues();

        for (Instance entry : tval) {
            while (entry != null) {
                hmeCount++;
                sum += entry.getSize(); // size of entry

                int sz = sizeIfNewString(fldHMEKey.getRefValue(entry));

                if (sz != 0) {
                    strCount++;
                }

                sum += sz;
                sz = sizeIfNewString(fldHMEValue.getRefValue(entry));

                if (sz != 0) {
                    strCount++;
                }

                sum += sz;
                entry = fldHMENext.getRefValue(entry);
            }
        }

        HashmapEntry hme = new HashmapEntry(sum, hmeCount, strCount);

        return hme;
    }
}
