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
import org.netbeans.modules.profiler.heapwalk.memorylint.*;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import org.openide.util.NbBundle;


public class RetainedSetByClass extends Rule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private MemoryLint context;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RetainedSetByClass() {
        super(NbBundle.getMessage(RetainedSetByClass.class, "LBL_RSBC_Name"),
                NbBundle.getMessage(RetainedSetByClass.class, "LBL_RSBC_Desc"));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    @Override
    public String getHTMLDescription() {
        return NbBundle.getMessage(RetainedSetByClass.class, "LBL_RSBC_LongDesc");
    }

    public void perform() {
        Heap heap = context.getHeap();
        @SuppressWarnings("unchecked")
        List<JavaClass> classes = heap.getAllClasses();

        // TODO access to progress
        //        BoundedRangeModel progress = context.getProgress();
        //        progress.setMaximum(classes.size());
        Histogram<Histogram.Entry> hist = new Histogram<Histogram.Entry>();

        for (JavaClass cls : classes) {
            Logger.getLogger(RetainedSetByClass.class.getName()).log(Level.FINE, "Executing rule on class {0}.", cls); // NOI18N
            performClass(cls, hist);

            if (context.isInterruped()) {
                return;
            }

            // TODO access to progress
            //            progress.setValue(progress.getValue()+1);
        }

        summary(hist);
    }

    @Override
    public void prepare(MemoryLint context) {
        this.context = context;
    }

    @Override
    protected JComponent createCustomizer() {
        return null;
    }

    @SuppressWarnings("unchecked")
    private void performClass(JavaClass clz, Histogram<Histogram.Entry> hist) {
        Set<Instance> retained = Utils.getRetainedSet(clz.getInstances(), context.getHeap());
        String name = clz.getName();
        name = Utils.printClass(context, name);

        for (Instance i : retained) {
            hist.add(name, new Histogram.Entry<Histogram.Entry>(i.getSize()));
        }
    }

    private void summary(Histogram h) {
//        context.appendResults("<hr>Histogram of retained size:<br>");
        context.appendResults(h.toString(0));
    }
}
