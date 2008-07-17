/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.killapp;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import java.io.IOException;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tomas Hurka
 */
public final class KillAction extends SingleDataSourceAction<Application> {
    
    public KillAction() {
        super(Application.class);
        putValue(NAME, NbBundle.getMessage(KillAction.class, "CTL_KillAction"));    // NOI18N
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }
    
    protected void actionPerformed(Application app, ActionEvent event) {
        final String[] command;
        
        if (Utilities.isWindows()) {
            command = new String[] {"taskkill","/PID",String.valueOf(app.getPid())};    // NOI18N
        } else if (Utilities.isUnix()) {
            command = new String[] {"kill",String.valueOf(app.getPid())};   // NOI18N
        } else {
            assert false:"strange os";  // NOI18N
            return;
        }
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                try {
                    Runtime.getRuntime().exec(command);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
    }
    
    protected boolean isEnabled(Application application) {
        if (Application.CURRENT_APPLICATION.equals(application)) {
            // don't commit suiside
            return false;
        }
        if (!Host.LOCALHOST.equals(application.getHost())) {
            // we cannot kill remote applications
            return false;
        }
        return true;
    }
    
}
