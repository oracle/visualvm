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

package org.netbeans.modules.profiler.actions;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import javax.swing.*;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;


/**
 * Provisionary action to display internal profiler stats.
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LBL_InternalStatsAction=&Display Internal Statistics",
    "HINT_InternalStatsAction=Display Internal Statistics",
    "CAPTION_InternalStatisticsInstrHotswap=Internal Statistics of Instrumentation and Hotswapping Operations"
})
@ActionID(category="Profile", id="org.netbeans.modules.profiler.actions.InternalStatsAction")
@ActionRegistration(displayName="#LBL_InternalStatsAction")
@ActionReference(path="Menu/Profile/Advanced", position=300, separatorAfter=400)
public final class InternalStatsAction extends ProfilingAwareAction {
    final private static int[] enabledStates = new int[]{Profiler.PROFILING_RUNNING};
            
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public InternalStatsAction() {
        putValue(Action.NAME, Bundle.LBL_InternalStatsAction());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_InternalStatsAction());
        putValue("noIconInMenu", Boolean.TRUE); //NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when an action occurs.
     */
    @Override
    public void performAction() {
        String stats;

        try {
            stats = Profiler.getDefault().getTargetAppRunner().getInternalStats();

            final HTMLTextArea textArea = new HTMLTextArea(stats);
            textArea.getAccessibleContext()
                    .setAccessibleName(Bundle.CAPTION_InternalStatisticsInstrHotswap());

            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout());
            p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            p.add(new JScrollPane(textArea), BorderLayout.CENTER);

            DialogDisplayer.getDefault().createDialog(new DialogDescriptor(p,
                                                              Bundle.CAPTION_InternalStatisticsInstrHotswap(),
                                                              true, new Object[] { DialogDescriptor.CLOSED_OPTION },
                                                              DialogDescriptor.CLOSED_OPTION, DialogDescriptor.BOTTOM_ALIGN,
                                                              null, null)).setVisible(true);
        } catch (ClientUtils.TargetAppOrVMTerminated e) {
             ProfilerDialogs.displayWarning(Bundle.MSG_NotAvailableNow(e.getMessage()));
             ProfilerLogger.log(e.getMessage());
        }
    }

    @Override
    protected int[] enabledStates() {
        return enabledStates;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public String getName() {
        return Bundle.LBL_InternalStatsAction();
    }
    
}
