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

package org.netbeans.modules.profiler.attach.wizard.steps;

import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.attach.wizard.WizardContext;
import org.netbeans.modules.profiler.attach.wizard.functors.ConditionalFunctor;
import org.netbeans.modules.profiler.attach.wizard.functors.TrueConditionalFunctor;
import org.netbeans.modules.profiler.attach.wizard.screen.NullWizardScreen;
import org.netbeans.modules.profiler.attach.wizard.screen.WizardScreen;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ProxyWizardStep implements WizardStep {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ConditionalFunctor condition;
    private WizardContext context;
    private WizardStep nullStep;
    private WizardStep proxiedStep;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ProxyWizardStep(final WizardContext context, final String title) {
        this(context, title, new TrueConditionalFunctor());
    }

    public ProxyWizardStep(final WizardContext context, final String title, final ConditionalFunctor condition) {
        this.nullStep = new NullWizardStep(title);
        this.proxiedStep = nullStep;
        this.context = context;
        this.condition = condition;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public WizardScreen getAttachedScreen() {
        if (canHandle()) {
            return this.proxiedStep.getAttachedScreen();
        } else {
            return new NullWizardScreen();
        }
    }

    public boolean isBegin() {
        return !canHandle() || this.proxiedStep.isBegin();
    }

    public void setCurrent() {
        if (canHandle()) {
            this.proxiedStep.setCurrent();
        }
    }

    public boolean isCurrent() {
        return canHandle() && this.proxiedStep.isCurrent();
    }

    public boolean isEnd() {
        return !canHandle() || this.proxiedStep.isEnd();
    }

    public void setFirst() {
        if (canHandle()) {
            this.proxiedStep.setFirst();
        }
    }

    public boolean isFirst() {
        return canHandle() && this.proxiedStep.isFirst();
    }

    public void setLast() {
        if (canHandle()) {
            this.proxiedStep.setLast();
        }
    }

    public boolean isLast() {
        return canHandle() && this.proxiedStep.isLast();
    }

    public void setNext() {
        if (canHandle()) {
            this.proxiedStep.setNext();
        }
    }

    public void setPrevious() {
        if (canHandle()) {
            this.proxiedStep.setPrevious();
        }
    }

    public int getStepIndex() {
        if (canHandle()) {
            return this.proxiedStep.getStepIndex();
        } else {
            return 0;
        }
    }

    public String getTitle() {
        if (canHandle()) {
            return this.proxiedStep.getTitle();
        } else {
            return ""; // NOI18N
        }
    }

    public void setWizardContext(final WizardContext context) {
        this.context = context;
        this.proxiedStep.setWizardContext(context);
    }

    public void setWizardStep(WizardStep proxiedStep) {
        this.proxiedStep = proxiedStep;
        this.proxiedStep.setWizardContext(this.context);
    }

    public void accept(WizardStepVisitor visitor, WizardContext context, int level) {
        if (canHandle()) {
            visitor.visit(this.proxiedStep, context, level);
        }
    }

    public void addChangeListener(ChangeListener listener) {
        this.proxiedStep.addChangeListener(listener);
    }

    public boolean canBack() {
        return canHandle() && this.proxiedStep.canBack();
    }

    public boolean canFinish() {
        return canHandle() && this.proxiedStep.canFinish();
    }

    public boolean canHandle() {
        return this.condition.evaluate(this.context) && this.proxiedStep.canHandle();
    }

    public boolean canNext() {
        return canHandle() && this.proxiedStep.canNext();
    }

    public boolean onCancel() {
        return this.proxiedStep.onCancel();
    }

    public void onFinish() {
        this.proxiedStep.onFinish();
    }

    public void removeChangeListener(ChangeListener listener) {
        this.proxiedStep.removeChangeListener(listener);
    }

    public void reset() {
        this.proxiedStep = nullStep;
    }
}
