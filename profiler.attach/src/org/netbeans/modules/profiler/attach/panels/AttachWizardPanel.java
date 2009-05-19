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

package org.netbeans.modules.profiler.attach.panels;

import org.netbeans.modules.profiler.attach.wizard.AttachWizardContext;
import org.netbeans.modules.profiler.attach.wizard.WizardContext;
import org.netbeans.modules.profiler.attach.wizard.screen.AbstractWizardScreen;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class AttachWizardPanel extends AbstractWizardScreen {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AttachWizardContext temporaryContext;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public final boolean isFinishPanel() {
        return this.canFinish(this.temporaryContext);
    }

    public final boolean canBack(WizardContext context) {
        if (this.temporaryContext == null) return false;

        return canBack(this.temporaryContext);
    }

    public final boolean canFinish(WizardContext context) {
        if (this.temporaryContext == null) return false;
        return canFinish(this.temporaryContext);
    }

    public final boolean canNext(WizardContext context) {
        if (this.temporaryContext == null) return false;
        return canNext(this.temporaryContext);
    }

    public final boolean onCancel(WizardContext context) {
        if (this.temporaryContext == null) return false;
        return onCancel(this.temporaryContext);
    }

    public final void onEnter(WizardContext context) {
        this.temporaryContext = (AttachWizardContext) context;
        onEnter(this.temporaryContext);
    }

    public final void onExit(WizardContext context) {
        if (this.temporaryContext == null) return;
        onExit(this.temporaryContext);
        this.temporaryContext = null;
    }

    public final void onFinish(WizardContext context) {
        if (this.temporaryContext == null) return;
        onFinish(this.temporaryContext);
    }

    public abstract boolean canBack(AttachWizardContext context);

    public abstract boolean canFinish(AttachWizardContext context);

    public abstract boolean canNext(AttachWizardContext context);

    public abstract boolean onCancel(AttachWizardContext context);

    public abstract void onEnter(AttachWizardContext context);

    public abstract void onExit(AttachWizardContext context);

    public abstract void onFinish(AttachWizardContext context);

    protected AttachWizardContext getContext() {
        return this.temporaryContext;
    }

    protected final void onPanelShow(WizardContext ctx) {
        this.temporaryContext = (AttachWizardContext) ctx;
        this.onPanelShow();
    }

    protected abstract void onPanelShow();

    protected void onStoreToContext(WizardContext ctx) {
        this.temporaryContext = (AttachWizardContext) ctx;
    }
}
