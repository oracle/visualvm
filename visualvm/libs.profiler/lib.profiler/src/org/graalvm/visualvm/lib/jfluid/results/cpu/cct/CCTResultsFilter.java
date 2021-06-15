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

package org.graalvm.visualvm.lib.jfluid.results.cpu.cct;

import java.util.Collection;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MarkedCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.ThreadCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNodeProcessor;


/**
 *
 * @author Jaroslav Bachorik
 */
public final class CCTResultsFilter extends RuntimeCCTNodeProcessor.PluginAdapter {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Evaluator {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        boolean evaluate(Mark mark);
    }

    public static interface EvaluatorProvider {
        Set/*<Evaluator>*/ getEvaluators();
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(CCTResultsFilter.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Set /*<Evaluator>*/ evaluators = null;
    private Set evaluatorProviders = new HashSet();

    private Stack passFlagStack;
    private boolean passingFilter;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of CategoryFilter */
    public CCTResultsFilter() {
        evaluators = new HashSet /*<Evaluator>*/();
        passFlagStack = new Stack();
        doReset();
    }

    public void setEvaluators(Collection evaluatorProviders) {
        this.evaluatorProviders.clear();
        this.evaluatorProviders.addAll(evaluatorProviders);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public synchronized boolean passesFilter() {
        return passingFilter;
    }

    @Override
    public void onStart() {
        evaluators.clear();
        
        for(Iterator iter = evaluatorProviders.iterator();iter.hasNext();) {
            evaluators.addAll(((EvaluatorProvider)iter.next()).getEvaluators());
        }
    }

    @Override
    public void onStop() {
        evaluators.clear();
    }

    public void reset() {
        doReset();
    }

    @Override
    public void onNode(ThreadCPUCCTNode node) {
        LOGGER.finest("visiting thread node");
        passFlagStack.push(Boolean.valueOf(passingFilter));
        passingFilter = true;

        for (Iterator iter = evaluators.iterator(); iter.hasNext();) {
            Evaluator evaluator = (Evaluator) iter.next();
            passingFilter = passingFilter && evaluator.evaluate(Mark.DEFAULT);
        }

        LOGGER.log(Level.FINEST, "Evaluator result: {0}", passingFilter);
    }
    
    @Override
    public void onNode(MarkedCPUCCTNode node) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Entering a node marked {0}", node.getMark().getId()); // NOI18N
        }

        passFlagStack.push(Boolean.valueOf(passingFilter));
        passingFilter = true;

        for (Iterator iter = evaluators.iterator(); iter.hasNext();) {
            Evaluator evaluator = (Evaluator) iter.next();
            passingFilter = passingFilter && evaluator.evaluate(node.getMark());
        }
    }
    
    @Override
    public void onBackout(ThreadCPUCCTNode node) {
        if (!passFlagStack.isEmpty()) {
            passingFilter = ((Boolean) passFlagStack.pop()).booleanValue();
        }
    }
    
    public void onBackout(MarkedCPUCCTNode node) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Leaving a node marked {0}", node.getMark().getId()); // NOI18N
        }

        if (!passFlagStack.isEmpty()) {
            passingFilter = ((Boolean) passFlagStack.pop()).booleanValue();
        }
    }    

    private void doReset() {
        passingFilter = false;
        passFlagStack.clear();
    }
}
