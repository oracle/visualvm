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
import org.netbeans.modules.profiler.attach.wizard.screen.WizardScreen;


/**
 *
 * @author Jaroslav Bachorik
 */
public class SimpleWizardStep implements WizardStep {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int NAVIGATION_STATE_BEGIN = 1;
    private static final int NAVIGATION_STATE_END = 2;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ConditionalFunctor condition;
    private String title;
    private WizardContext context;
    private WizardScreen attachedScreen;
    private boolean currentFlag = false;
    private int navigationState = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SimpleWizardStep(final String title, final WizardScreen screen) {
        this(null, title, screen);
    }

    public SimpleWizardStep(final String title, final WizardScreen screen, final ConditionalFunctor condition) {
        this(null, title, screen, condition);
    }

    public SimpleWizardStep(final WizardContext context, final String title, final WizardScreen screen) {
        this(context, title, screen, new TrueConditionalFunctor());
    }

    public SimpleWizardStep(final WizardContext context, final String title, final WizardScreen screen,
                            final ConditionalFunctor condition) {
        this.context = context;
        this.title = title;
        screen.setTitle(title);
        this.attachedScreen = screen;
        this.condition = condition;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public WizardScreen getAttachedScreen() {
        return this.attachedScreen;
    }

    public boolean isBegin() {
        return (this.navigationState & NAVIGATION_STATE_BEGIN) > 0;
    }

    public void setCurrent() {
        this.currentFlag = true;
    }

    public boolean isCurrent() {
        return this.currentFlag;
    }

    public boolean isEnd() {
        return (this.navigationState & NAVIGATION_STATE_END) > 0;
    }

    public void setFirst() {
        if (this.canHandle()) {
            this.navigationState = 0;
            this.currentFlag = true;

            if (context != null) {
                this.attachedScreen.onEnter(this.context);
            }
        } else {
            this.navigationState = NAVIGATION_STATE_BEGIN + NAVIGATION_STATE_END;
        }
    }

    public boolean isFirst() {
        return true;
    }

    public void setLast() {
        if (this.canHandle()) {
            this.navigationState = 0;
            this.currentFlag = true;

            if (context != null) {
                this.attachedScreen.onEnter(this.context);
            }
        } else {
            this.navigationState = NAVIGATION_STATE_BEGIN + NAVIGATION_STATE_END;
        }
    }

    public boolean isLast() {
        return true;
    }

    public void setNext() {
        if (context != null) {
            this.attachedScreen.onExit(this.context);
        }

        this.navigationState = NAVIGATION_STATE_END;
        this.currentFlag = false;
    }

    public void setPrevious() {
        if (context != null) {
            this.attachedScreen.onExit(this.context);
        }

        this.navigationState = NAVIGATION_STATE_BEGIN;
        this.currentFlag = false;
    }

    public int getStepIndex() {
        return 0;
    }

    public String getTitle() {
        return this.title;
    }

    public void setWizardContext(WizardContext context) {
        this.context = context;
    }

    public void accept(WizardStepVisitor visitor, WizardContext context, int level) {
        if (this.canHandle()) {
            visitor.visit(this, context, level);
        }
    }

    public void addChangeListener(ChangeListener listener) {
        this.attachedScreen.addChangeListener(listener);
    }

    public boolean canBack() {
        return this.attachedScreen.canBack(context);
    }

    public boolean canFinish() {
        return this.attachedScreen.canFinish(context);
    }

    public boolean canHandle() {
        return this.condition.evaluate(context);
    }

    public boolean canNext() {
        return this.attachedScreen.canNext(context);
    }

    public boolean onCancel() {
        return this.attachedScreen.onCancel(this.context);
    }

    public void onFinish() {
        this.attachedScreen.onFinish(this.context);
    }

    public void removeChangeListener(ChangeListener listener) {
        this.attachedScreen.removeChangeListener(listener);
    }
}
