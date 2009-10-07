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

import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import java.util.Collection;
import java.util.List;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * This is the MemoryLint engine. It manages the heap dump, available rules,
 * processing and results gathering.
 * Generally, it has no UI, but provides hooks for progress bar and results
 * visualization.
 *
 * @author nenik
 */
public class MemoryLint {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class HierarchicalModel extends DefaultBoundedRangeModel implements ChangeListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        BoundedRangeModel delegate = new DefaultBoundedRangeModel(0, 0, 0, 1);
        int each;
        int step = -1;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        HierarchicalModel(int steps, int each) {
            super(0, 0, 0, steps * each);
            this.each = each;
            delegate.addChangeListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setSteps(int steps) {
            setRangeProperties(0, 0, 0, steps * each, false);
        }

        public void stateChanged(ChangeEvent e) {
            updateValue();
        }

        BoundedRangeModel getNextDelegate() {
            step++;
            delegate.setRangeProperties(0, 0, 0, 1, false);
            updateValue();

            return delegate;
        }

        private void updateValue() {
            int val = getValue();
            setValue((step * each) + ((each * delegate.getValue()) / delegate.getMaximum()));
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private BoundedRangeModel progress;
    private Heap heap;
    private HierarchicalModel globalProgress;
    private StringBuffer results = new StringBuffer();
    private StringHelper stringHelper;
    private volatile boolean interrupted;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemoryLint(Heap heap) {
        this.heap = heap;
        globalProgress = new HierarchicalModel(1, 1000);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public BoundedRangeModel getGlobalProgress() {
        return globalProgress;
    }

    public Heap getHeap() {
        return heap;
    }

    public boolean isInterruped() {
        return interrupted;
    }

    public String getResults() {
        return results.toString();
    }

    /** Perform BFS of incomming references and find shortest one not from SDK
     */
    public String getRootIncommingString(Instance in) {
        String temp = null;

        for (;;) {
            Instance from = in.getNearestGCRootPointer();

            if (from == null) {
                break;
            }

            String rName = getFullName(in, from);

            if (temp == null) {
                temp = "<< " + rName; // there is at least some incoming ref
            }

            if (!rName.startsWith("java.") && !rName.startsWith("javax.")) {
                return rName;
            }

            if (from.isGCRoot()) {
                break;
            }

            in = from;
        }

        return (temp == null) ? "unknown" : temp;
    }

    public StringHelper getStringHelper() {
        if (stringHelper == null) {
            stringHelper = new StringHelper(heap);
        }

        return stringHelper;
    }

    public void appendResults(String s) {
        results.append(s).append('\n');
    }

    public static Collection<Rule> createRules() {
        return RuleRegistry.getRegisteredRules();
    }

    public void interrupt() {
        interrupted = true;
    }

    public void process(Collection<Rule> rules) {
        int count = rules.size();
        globalProgress.setSteps(count);

        for (Rule r : rules) {
            r.prepare(this);
            progress = globalProgress.getNextDelegate();
            results.append(r.resultsHeader());
            r.perform();
            results.append("<hr>");

            if (isInterruped()) {
                break;
            }
        }
    }

    BoundedRangeModel getProgress() {
        return progress;
    }

    private String getFullName(Instance to, Instance from) {
        ObjectFieldValue fv = getInField(to, from);

        if (fv == null) {
            return from.getJavaClass().getName();
        }

        if (fv.getField().isStatic()) {
            return fv.getField().getDeclaringClass().getName() + ";" + fv.getField().getName();
        } else {
            return from.getJavaClass().getName() + ":" + fv.getField().getName();
        }
    }

    private ObjectFieldValue getInField(Instance to, Instance from) {
        List<FieldValue> vals = (List<FieldValue>) from.getFieldValues();

        for (FieldValue fv : vals) {
            if (fv instanceof ObjectFieldValue) {
                if (to.equals(((ObjectFieldValue) fv).getInstance())) {
                    return (ObjectFieldValue) fv;
                }
            }
        }

        if (from.getJavaClass().getName().equals("java.lang.Class")) {
            JavaClass cls = heap.getJavaClassByID(from.getInstanceId());
            vals = (List<FieldValue>) cls.getStaticFieldValues();

            for (FieldValue fv : vals) {
                if (fv instanceof ObjectFieldValue) {
                    if (to.equals(((ObjectFieldValue) fv).getInstance())) {
                        return (ObjectFieldValue) fv;
                    }
                }
            }
        }

        return null;
    }
}
