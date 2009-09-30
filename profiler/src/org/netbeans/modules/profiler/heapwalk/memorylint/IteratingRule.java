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

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;


/**
 * An iterating rule is a rule that iterates through all instances of single
 * class and does some analysis over each instance. When such a Rule is based
 * on this helper class, the infrastructure can independently monitor
 * the progress and also paralelize the task among available CPUs.
 *
 * Rules can override {@link #prepareRule(MemoryLint)} and {@link #summary()}
 * for preparation and finalization work, and must implement
 * {@link #perform(Instance)} for actual, per-instance analysis.
 *
 * @author nenik
 */
public abstract class IteratingRule extends Rule {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private MemoryLint context;
    private Pattern classNamePattern;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public IteratingRule(String name, String desc, String classNamePattern) {
        super(name, desc);
        setClassNamePattern(classNamePattern);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public final void perform() {
        Heap heap = context.getHeap();
        @SuppressWarnings("unchecked")
        List<JavaClass> classes = heap.getAllClasses();
        List<JavaClass> matching = new ArrayList<JavaClass>();
        int count = 0;

        for (JavaClass cls : classes) {
            if (classNamePattern.matcher(cls.getName()).matches()) {
                matching.add(cls);
                count += cls.getInstancesCount();
            }

            if (context.isInterruped()) {
                return;
            }
        }

        BoundedRangeModel progress = context.getProgress();
        progress.setMaximum((count != 0) ? count : 1);

        for (JavaClass actCls : matching) {
            @SuppressWarnings("unchecked")
            List<Instance> instances = actCls.getInstances();

            for (Instance inst : instances) {
                Logger.getLogger(IteratingRule.class.getName()).log(Level.FINE, "Executing rule on {0} instance", inst); // NOI18N
                perform(inst);
                progress.setValue(progress.getValue() + 1);

                if (context.isInterruped()) {
                    return;
                }
            }
        }

        if (count == 0) {
            progress.setValue(1);
        }

        summary();
    }

    public final void prepare(MemoryLint context) {
        this.context = context;
        prepareRule(context);
    }

    /** Configures the rule to be applied on all instances of classes
     * matching to given pattern.
     */
    protected final void setClassNamePattern(String classNamePattern) {
        this.classNamePattern = Pattern.compile(classNamePattern);
    }

    protected abstract void perform(Instance inst);

    protected final MemoryLint getContext() {
        return context;
    }

    /** Default implementation returns <code>null</code>
     * (no customizer for the rule).
     */
    protected JComponent createCustomizer() {
        return null;
    }

    protected void prepareRule(MemoryLint context) {
    }

    protected void summary() {
    }
}
