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
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.modules.profiler.heapwalk.memorylint.*;
import java.util.HashMap;
import java.util.Map;
import org.openide.util.NbBundle;


@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.heapwalk.memorylint.Rule.class)
public class OverallocatedString extends IteratingRule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldCount;
    private FieldAccess fldOffset;
    private FieldAccess fldValue;
    private JavaClass clsString;
    private Map<Instance, Integer> covered = new HashMap<Instance, Integer>();
    private int total;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public OverallocatedString() {
        super(NbBundle.getMessage(OverallocatedString.class, "LBL_OverStr_Name"),
                NbBundle.getMessage(OverallocatedString.class, "LBL_OverStr_Desc"),
                "java.lang.String"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(OverallocatedString.class, "LBL_OverStr_LongDesc");
    }

    protected void perform(Instance in) {
        int off = fldOffset.getIntValue(in);
        int cnt = fldCount.getIntValue(in);
        PrimitiveArrayInstance arrValue = (PrimitiveArrayInstance) fldValue.getRefValue(in);

        if (arrValue == null) {
            return; // empty
        }

        if ((off > 0) || (arrValue.getLength() > cnt)) {
            if (covered.containsKey(arrValue)) {
                // simplification - don't track shared char arrays
                total -= covered.remove(arrValue);
            } else {
                int waste = (2 * off) + (2 * (arrValue.getLength() - (cnt + off)));
                covered.put(arrValue, waste);
                total += waste;
            }
        }
    }

    protected void prepareRule(MemoryLint context) {
        Heap heap = context.getHeap();
        clsString = heap.getJavaClassByName("java.lang.String"); // NOI18N
        fldOffset = new FieldAccess(clsString, "offset"); // NOI18N
        fldCount = new FieldAccess(clsString, "count"); // NOI18N
        fldValue = new FieldAccess(clsString, "value"); // NOI18N
    }

    @Override
    protected void summary() {
        getContext().appendResults(NbBundle.getMessage(OverallocatedString.class, "FMT_OverStr_Result", total));

        Histogram<Histogram.Entry> h = new Histogram<Histogram.Entry>();

        for (Map.Entry<Instance, Integer> e : covered.entrySet()) {
            String incomming = getContext().getRootIncommingString(e.getKey());
            incomming = Utils.printClass(getContext(), incomming);
            h.add(incomming, new Histogram.Entry(e.getValue()));
        }

        getContext().appendResults(h.toString(5000));
    }
}
