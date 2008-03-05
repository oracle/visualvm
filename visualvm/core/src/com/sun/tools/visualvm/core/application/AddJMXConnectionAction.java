/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.core.application;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.util.RequestProcessor;

public final class AddJMXConnectionAction extends AbstractAction {
    
    private static AddJMXConnectionAction instance;
    
    public static synchronized AddJMXConnectionAction getInstance() {
        if (instance == null) instance = new AddJMXConnectionAction();
        return instance;
    }
    
    public void actionPerformed(ActionEvent e) {
        final JmxApplicationConfigurator appConfig =
                JmxApplicationConfigurator.addJmxConnection();
        if (appConfig != null) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    JmxApplicationProvider.sharedInstance().createJmxApplication(
                            appConfig.getConnection(), appConfig.getDisplayName());
                }
            });
        }
    }
    
    private AddJMXConnectionAction() {
        putValue(Action.NAME, "Add JMX Connection...");
        putValue(Action.SHORT_DESCRIPTION, "Add JMX Connection");
    }
}
