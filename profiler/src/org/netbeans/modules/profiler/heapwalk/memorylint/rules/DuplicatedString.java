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
import org.netbeans.modules.profiler.heapwalk.memorylint.*;
import java.util.HashMap;
import org.openide.util.NbBundle;


@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.heapwalk.memorylint.Rule.class)
public class DuplicatedString extends IteratingRule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private FieldAccess fldValue;
    private HashMap<String, Integer> map = new HashMap<String, Integer>();
    private Histogram<Histogram.Entry> dupSources = new Histogram<Histogram.Entry>();
    private StringHelper helper;
    private int total;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DuplicatedString() {
        super(NbBundle.getMessage(DuplicatedString.class, "LBL_DupStr_Name"),
                NbBundle.getMessage(DuplicatedString.class, "LBL_DupStr_Desc"),
                "java.lang.String"); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(DuplicatedString.class, "LBL_DupStr_LongDesc");
    }

    protected void perform(Instance in) {
        String str = helper.decodeString(in);
        Integer val = map.get(str);

        if (val != null) { // already known, histogram the rest.

            int strSize = in.getSize();
            Instance arr = fldValue.getRefValue(in);

            if (arr != null) {
                strSize += ((str.length() * 2) + 14); // XXX aproximation
            }

            String incomming = getContext().getRootIncommingString(in);
            incomming = Utils.printClass(getContext(), incomming);
            dupSources.add(incomming, new Histogram.Entry(strSize));
            total += strSize;
        }

        val = (val == null) ? 1 : (val + 1);
        map.put(str, val);
    }

    protected @Override void prepareRule(MemoryLint context) {
        Heap heap = context.getHeap();
        helper = context.getStringHelper();

        JavaClass clsString = heap.getJavaClassByName("java.lang.String"); // NOI18N
        fldValue = new FieldAccess(clsString, "value"); // NOI18N
    }

    protected @Override void summary() {
        getContext().appendResults(NbBundle.getMessage(DuplicatedString.class, "FMT_DupStr_Result", total));
        getContext().appendResults(dupSources.toString(50000));
    }
}
