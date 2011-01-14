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

package org.netbeans.modules.profiler.ppoints.ui;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.Action;
import org.netbeans.modules.profiler.ppoints.CodeProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.BooleanStateAction;
import org.openide.util.actions.SystemAction;


/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class EnableDisableProfilingPointAction extends SystemAction implements ContextAwareAction {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String ACTION_NAME = NbBundle.getMessage(EnableDisableProfilingPointAction.class,
                                                                  "EnableDisableProfilingPointAction_ActionName"); // NOI18N
                                                                                                                   // -----
    private ContextAwareAction action;

    private class ContextAwareAction extends BooleanStateAction {

        private CodeProfilingPoint profilingPoint;

        ContextAwareAction() {
            setIcon(null);
            putValue("noIconInMenu", Boolean.TRUE); // NOI18N
        }

        void setProfilingPoint(CodeProfilingPoint pp) {
            profilingPoint = pp;
        }

        @Override
        public String getName() {
            return EnableDisableProfilingPointAction.this.getName();
        }

        @Override
        public HelpCtx getHelpCtx() {
            return new HelpCtx(EnableDisableProfilingPointAction.class);
        }

        @Override
        public void actionPerformed(ActionEvent ev) {
            super.actionPerformed(ev);
            profilingPoint.setEnabled(getBooleanState());
        }
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public EnableDisableProfilingPointAction() {
        action = new ContextAwareAction();
        setIcon(null);
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
        setEnabled(false);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(EnableDisableProfilingPointAction.class);
    }

    @Override
    public String getName() {
        return ACTION_NAME;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        if (!ProfilingPointsManager.getDefault().isProfilingSessionInProgress()) {
            Collection<? extends CodeProfilingPoint.Annotation> anns = actionContext.lookupAll(CodeProfilingPoint.Annotation.class);
            if (anns.size() == 1) {
                CodeProfilingPoint pp = anns.iterator().next().profilingPoint();
                
                action.setProfilingPoint(pp);
                action.setBooleanState(pp.isEnabled());
                return action;
            }
        }
        return this;
    }
}
