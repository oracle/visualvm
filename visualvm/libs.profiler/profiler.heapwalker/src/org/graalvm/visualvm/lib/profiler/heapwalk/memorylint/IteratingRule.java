/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.profiler.heapwalk.memorylint;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.BoundedRangeModel;
import javax.swing.JComponent;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;


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
        List<JavaClass> matching = new ArrayList<>();
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
