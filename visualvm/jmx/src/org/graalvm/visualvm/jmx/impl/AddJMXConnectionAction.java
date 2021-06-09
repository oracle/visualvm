/*
 *  Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.jmx.JmxApplicationsSupport;
import org.graalvm.visualvm.jmx.JmxConnectionCustomizer;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.core.VisualVM;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddJMXConnectionAction extends SingleDataSourceAction<Host> {
    
    private static final String ICON_PATH = "org/graalvm/visualvm/jmx/resources/addJmxApplication.png";   // NOI18N
    private static final Image ICON =  ImageUtilities.loadImage(ICON_PATH);
    
    private boolean tracksSelection = false;
    
    private static AddJMXConnectionAction alwaysEnabled;
    private static AddJMXConnectionAction selectionAware;
    
    
    public static synchronized AddJMXConnectionAction alwaysEnabled() {
        if (alwaysEnabled == null) {
            alwaysEnabled = new AddJMXConnectionAction();
            alwaysEnabled.putValue(SMALL_ICON, new ImageIcon(ICON));
            alwaysEnabled.putValue("iconBase", ICON_PATH);  // NOI18N
        }
        return alwaysEnabled;
    }
    
    public static synchronized AddJMXConnectionAction selectionAware() {
        if (selectionAware == null) {
            selectionAware = new AddJMXConnectionAction();
            selectionAware.tracksSelection = true;
        }
        return selectionAware;
    }
    
    
    protected void actionPerformed(Host host, ActionEvent actionEvent) {
        final JmxConnectionConfigurator.Result result = JmxConnectionConfigurator.getResult();
        final JmxConnectionCustomizer.Setup setup = result.getSetup();
        if (setup != null) {
            VisualVM.getInstance().runTask(new Runnable() {
                public void run() {
                    JmxApplication application = (JmxApplication)JmxApplicationsSupport.
                            getInstance().createJmxApplicationInteractive(
                            setup.getConnectionString(), setup.getDisplayName(),
                            setup.getEnvironmentProvider(), setup.isConnectionPersistent(),
                            setup.allowsInsecureConnection(), setup.isConnectImmediately(),
                            setup.isConnectAutomatically());
                    if (application == null) result.cancelled();
                    else result.accepted(application);
                }
            });
        } else {
            result.cancelled();
        }
    }
    
    protected boolean isEnabled(Host host) {
        return host != Host.UNKNOWN_HOST;
    }
    
    protected void updateState(Set<Host> selectedHosts) {
        if (tracksSelection) super.updateState(selectedHosts);
    }
    
    
    private AddJMXConnectionAction() {
        super(Host.class);
        putValue(NAME, NbBundle.getMessage(AddJMXConnectionAction.class, "MSG_Add_JMX_Connection"));    // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(AddJMXConnectionAction.class, "ToolTip_Add_JMX_Connection"));   // NOI18N
    }
}
