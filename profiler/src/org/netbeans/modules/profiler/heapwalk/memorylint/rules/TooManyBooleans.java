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


public class TooManyBooleans extends IteratingRule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    Histogram<Histogram.Entry> booleans = new Histogram<Histogram.Entry>();
    private Heap heap;
    private Instance FALSE;
    private Instance TRUE;
    private StringHelper helper;
    private int count;
    private int total;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TooManyBooleans() {
        super("Too many Boolean", "Checks instances of Boolean other than Boolean.TRUE and Boolean.FALSE", "java.lang.Boolean");
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return "<html><body>Checks if there are more than two instances of <code>Boolean</code> on the heap (only <code>Boolean.TRUE</code> and <code>Boolean.FALSE</code> are necessary).</body></html>";
    }

    protected void perform(Instance in) {
        if (in.equals(TRUE) || in.equals(FALSE)) {
            return;
        }

        count++;
        booleans.add(Utils.printClass(getContext(), getContext().getRootIncommingString(in)), new Histogram.Entry(in.getSize()));
    }

    protected void prepareRule(MemoryLint context) {
        heap = context.getHeap();
        helper = context.getStringHelper();

        JavaClass booleanClass = heap.getJavaClassByName("java.lang.Boolean"); // NOI18N
        TRUE = (Instance) booleanClass.getValueOfStaticField("TRUE"); // NOI18N
        FALSE = (Instance) booleanClass.getValueOfStaticField("FALSE"); // NOI18N
    }

    protected void summary() {
        if (count > 0) {
            getContext()
                .appendResults("There are " + (count + 2) + " Boolean instances wasting " + (count * TRUE.getSize()) + "B<br>");
            getContext().appendResults(booleans.toString(0));
        } else {
            getContext().appendResults("There are not more than two Boolean instances: OK");
        }
    }
}
