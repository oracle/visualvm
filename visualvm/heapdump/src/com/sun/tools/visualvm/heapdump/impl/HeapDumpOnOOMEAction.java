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

package com.sun.tools.visualvm.heapdump.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.event.ActionEvent;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
class HeapDumpOnOOMEAction extends SingleDataSourceAction<Application> {
    
    private boolean oomeEnabled;
    
    public static HeapDumpOnOOMEAction create() {
        HeapDumpOnOOMEAction action = new HeapDumpOnOOMEAction();
        action.initialize();
        return action;
    }
    

    protected void actionPerformed(Application application, ActionEvent actionEvent) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        jvm.setDumpOnOOMEnabled(!oomeEnabled);
        updateState(jvm);
    }

    protected boolean isEnabled(Application application) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (!jvm.isDumpOnOOMEnabledSupported()) return false;
        updateState(jvm);
        return true;
    }
    
    
    private void updateState(Jvm jvm) {
        oomeEnabled = jvm.isDumpOnOOMEnabled();
        String actionName = oomeEnabled ? "Disable Heap Dump on OOME" : "Enable Heap Dump on OOME";
        putValue(NAME, actionName);
        putValue(SHORT_DESCRIPTION,actionName);
    }
    
    
    private HeapDumpOnOOMEAction() {
        super(Application.class);
    }

}
