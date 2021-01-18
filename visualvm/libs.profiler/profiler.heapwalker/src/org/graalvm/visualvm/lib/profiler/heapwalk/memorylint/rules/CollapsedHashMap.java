/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.rules;

import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.openide.util.NbBundle;

@NbBundle.Messages({
    "LBL_CHM_Name=Collapsed (Weak)HashMaps",
    "LBL_CHM_Desc=HashMaps populated by entries with poorly distributed hashcode",
    "LBL_CHM_LongDesc=<html><body>This rule checks for (<code>Weak</code>)<code>" +
    "HashMap</code>s that have bad distribution of entries among allocated buckets," +
    "like in the illustration: <br><img src='res/wrongmap.png'><br> This can be " +
    "caused by bad implementation of <code>hashcode()</code> or " +
    "<code>equals()</code> methods of the objects used as map keys</body></html>",
    "FMT_CHM_Record={0}: {1} {2,number} entries are allocated to " +
    "{3,choice,1#'<b>one bucket</b>'|2#{3,number,integer} buckets}"
})
//@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.heapwalk.memorylint.Rule.class)
public class CollapsedHashMap extends IteratingRule {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class HMRecord {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Instance hm;
        private int size;
        private int slots;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        HMRecord(Instance hm, int size, int slots) {
            this.hm = hm;
            this.size = size;
            this.slots = slots;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        @Override
        public String toString() {
            boolean reallyBad = slots == 1;
            return Bundle.FMT_CHM_Record(
                        Utils.printClass(getContext(), getContext().getRootIncomingString(hm)),
                        Utils.printInstance(hm),
                        size,
                        slots
                    );
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldHMSize;
    private FieldAccess fldHMTable;
    private FieldAccess fldWHMSize;
    private FieldAccess fldWHMTable;
    private JavaClass clsHM;
    private JavaClass clsWHM;
    private Set<HMRecord> poorHM = new HashSet<HMRecord>();

    /** Threshold for count of chained entries to raise the warning */
    private float ratio = 1.5f;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CollapsedHashMap() {
        super(Bundle.LBL_CHM_Name(),
                Bundle.LBL_CHM_Desc(),
                "java.util.HashMap|java.util.WeakHashMap"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public String getHTMLDescription() {
        return Bundle.LBL_CHM_LongDesc();
    }

    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    @Override
    protected void perform(Instance hm) {
        if (clsHM.equals(hm.getJavaClass())) {
            scanHashmap(hm, fldHMSize, fldHMTable);
        } else {
            scanHashmap(hm, fldWHMSize, fldWHMTable);
        }
    }

    @Override
    protected void prepareRule(MemoryLint context) {
        Heap heap = context.getHeap();
        clsHM = heap.getJavaClassByName("java.util.HashMap"); // NOI18N
        fldHMTable = new FieldAccess(clsHM, "table"); // NOI18N
        fldHMSize = new FieldAccess(clsHM, "size"); // NOI18N
        clsWHM = heap.getJavaClassByName("java.util.WeakHashMap"); // NOI18N
        fldWHMTable = new FieldAccess(clsWHM, "table"); // NOI18N
        fldWHMSize = new FieldAccess(clsWHM, "size"); // NOI18N
    }

    @Override
    protected void summary() {
        for (HMRecord hm : poorHM) {
            getContext().appendResults(hm.toString() + "<br>"); // NOI18N
        }
    }

    private void scanHashmap(Instance hm, FieldAccess sizeAccess, FieldAccess tableAccess) {
        int size = sizeAccess.getIntValue(hm);

        if (size < 5) {
            return; // not really significant
        }

        ObjectArrayInstance table = (ObjectArrayInstance) tableAccess.getRefValue(hm);

        if (table != null) {
            int slots = 0;
            @SuppressWarnings("unchecked")
            List<Instance> tval = table.getValues();

            for (Instance entry : tval) {
                if (entry != null) {
                    slots++;
                }
            }

            if (slots > 0 && (size / slots) > ratio) {
                poorHM.add(new HMRecord(hm, size, slots));
            }
        }

        return;
    }
}
