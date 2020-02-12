/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.jmx.impl;

import java.awt.event.ActionEvent;
import javax.swing.JMenuItem;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.actions.ActionUtils;
import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ConnectDisconnectAction extends SingleDataSourceAction<JmxApplication> implements Presenter.Popup {
    
    private static ConnectDisconnectAction INSTANCE;
    
    private int currentState = Stateful.STATE_UNKNOWN;
    private boolean currentAutoConnect;
    
    
    public static synchronized ConnectDisconnectAction instance() {
        if (INSTANCE == null) INSTANCE = new ConnectDisconnectAction();
        return INSTANCE;
    }
    
    
    @Override
    protected void actionPerformed(final JmxApplication app, ActionEvent actionEvent) {
        final int state = currentState;
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                if (state == Stateful.STATE_AVAILABLE) app.disconnect();
                else JmxHeartbeat.scheduleImmediately(app);
            }
        });
    }

    @Override
    protected boolean isEnabled(JmxApplication app) {
        return true;
    }
    
    @Override
    public JMenuItem getPopupPresenter() {
        JmxApplication app = ActionUtils.getSelectedDataSource(getScope());
        currentState = app.getState();
        currentAutoConnect = !app.isHeartbeatDisabled();
        
        JMenuItem presenter = new JMenuItem(this);
        if (currentState == Stateful.STATE_AVAILABLE) Mnemonics.setLocalizedText(presenter, NbBundle.getMessage(ConnectDisconnectAction.class, "LBL_Disconnect")); // NOI18N
        else Mnemonics.setLocalizedText(presenter, NbBundle.getMessage(ConnectDisconnectAction.class, "LBL_Connect")); // NOI18N
        presenter.setEnabled(currentState == Stateful.STATE_AVAILABLE || !currentAutoConnect);
        
        return presenter;
    }
    
    
    private ConnectDisconnectAction() {
        super(JmxApplication.class);
    }
    
}
