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

package org.graalvm.visualvm.lib.profiler.actions;

import java.awt.event.ActionEvent;
import javax.swing.*;
import org.graalvm.visualvm.lib.profiler.v2.ProfilerSession;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;


/**
 * Action to start profiler attach
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LBL_AttachAction=Attach to &External Process",
    "HINT_AttachAction=Attach to External Process"
})
public final class AttachAction extends AbstractAction {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    final private static class Singleton {
        final private static AttachAction INSTANCE = new AttachAction();
    }

    private AttachAction() {
        putValue(Action.NAME, Bundle.LBL_AttachAction());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_AttachAction());
    }

    public static AttachAction getInstance() {
        return Singleton.INSTANCE;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                ProfilerSession session = ProfilerSession.forContext(Lookup.EMPTY);
                if (session != null) {
                    session.setAttach(true);
                    session.open();
                }
            }
        });
    }
}
