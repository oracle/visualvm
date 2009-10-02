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
public class WrongWeakHashMap extends IteratingRule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class WHMRecord {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Instance hm;
        private Instance key;
        private Instance value;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        WHMRecord(Instance hm, Instance key, Instance value) {
            this.hm = hm;
            this.key = key;
            this.value = value;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public String toString() {
            return NbBundle.getMessage(WrongWeakHashMap.class, "FMT_WWMH_Entry",
                    new Object[] {
                        Utils.printClass(getContext(), getContext().getRootIncommingString(hm)),
                        Utils.printInstance(hm),
                        Utils.printInstance(key),
                        Utils.printInstance(value)
                    }
            );
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldHMEKey;
    private FieldAccess fldHMENext;
    private FieldAccess fldHMEValue;
    private FieldAccess fldHMTable;
    private JavaClass clsHM;
    private JavaClass clsHME;
    private Set<WHMRecord> poorWHM = new HashSet<WHMRecord>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public WrongWeakHashMap() {
        super(NbBundle.getMessage(WrongWeakHashMap.class, "LBL_WWMH_Name"),
                NbBundle.getMessage(WrongWeakHashMap.class, "LBL_WWMH_Desc"),
                "java.util.WeakHashMap");
        
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(WrongWeakHashMap.class, "LBL_WWMH_LongDesc");
    }

    protected void perform(Instance hm) {
        scanWeakHashmap(hm);
    }

    @Override
    protected void prepareRule(MemoryLint context) {
        // TODO WeakHashMap might not be present in the dump
        Heap heap = context.getHeap();
        clsHM = heap.getJavaClassByName("java.util.WeakHashMap"); // NOI18N
        clsHME = heap.getJavaClassByName("java.util.WeakHashMap$Entry"); // NOI18N
        fldHMTable = new FieldAccess(clsHM, "table"); // NOI18N

        JavaClass ref = heap.getJavaClassByName("java.lang.ref.Reference"); // NOI18N
        fldHMEKey = new FieldAccess(ref, "referent"); // NOI18N
        fldHMEValue = new FieldAccess(clsHME, "value"); // NOI18N
        fldHMENext = new FieldAccess(clsHME, "next"); // NOI18N
    }

    @Override
    protected void summary() {
        for (WHMRecord whm : poorWHM) {
            getContext().appendResults(whm.toString());
        }
    }

    private void scanWeakHashmap(Instance hm) {
        ObjectArrayInstance table = (ObjectArrayInstance) fldHMTable.getRefValue(hm);

        if (table == null) { // ? 

            return;
        }

        @SuppressWarnings("unchecked")
        List<Instance> tval = table.getValues();

        for (Instance entry : tval) {
            while (entry != null) {
                Instance key = fldHMEKey.getRefValue(entry);

                if (key != null) { // XXX can also scan for weak HM pending cleanup

                    Instance value = fldHMEValue.getRefValue(entry);

                    if (Utils.isReachableFrom(value, key)) {
                        poorWHM.add(new WHMRecord(hm, key, value));

                        return;
                    }
                }

                entry = fldHMENext.getRefValue(entry);
            }
        }

        return;
    }
}
