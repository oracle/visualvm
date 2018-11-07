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

    private Set<Evaluator> evaluators;
    private Set<EvaluatorProvider> evaluatorProviders = new HashSet<>();

    private Stack<Boolean> passFlagStack;
    private boolean passingFilter;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of CategoryFilter */
    public CCTResultsFilter() {
        evaluators = new HashSet<>();
        passFlagStack = new Stack<>();
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

        for (EvaluatorProvider evaluatorProvider : evaluatorProviders) {
            evaluators.addAll(evaluatorProvider.getEvaluators());
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

        for (Evaluator evaluator : evaluators) {
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

        for (Evaluator evaluator : evaluators) {
            passingFilter = passingFilter && evaluator.evaluate(node.getMark());
        }
    }
    
    @Override
    public void onBackout(ThreadCPUCCTNode node) {
        if (!passFlagStack.isEmpty()) {
            passingFilter = passFlagStack.pop().booleanValue();
        }
    }
    
    public void onBackout(MarkedCPUCCTNode node) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Leaving a node marked {0}", node.getMark().getId()); // NOI18N
        }

        if (!passFlagStack.isEmpty()) {
            passingFilter = passFlagStack.pop().booleanValue();
        }
    }    

    private void doReset() {
        passingFilter = false;
        passFlagStack.clear();
    }
}
