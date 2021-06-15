/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.actions;

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import javax.swing.*;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
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
@ActionID(category="Profile", id="org.graalvm.visualvm.lib.profiler.actions.InternalStatsAction")
//@ActionRegistration(displayName="#LBL_InternalStatsAction")
//@ActionReference(path="Menu/Profile/Advanced", position=300, separatorAfter=400)
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
