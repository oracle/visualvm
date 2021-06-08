/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package net.java.visualvm.modules.glassfish.jmx;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.model.ModelFactory;
import org.graalvm.visualvm.core.model.ModelProvider;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import javax.management.remote.JMXServiceURL;
import net.java.visualvm.modules.glassfish.GlassFishApplicationType;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GFJmxModelFactory extends ModelFactory<JmxModel, Application> implements ModelProvider<JmxModel, Application>{
    private static final String STORAGE_DIRNAME = "jmxapplicationsGF"; // copy from JmxApplicationsSupport ....
    private static final String PROPERTY_USERNAME = "prop_username"; // copy from JmxApplicationsSupport ....
    private static final String PROPERTY_PASSWORD = "prop_password"; // copy from JmxApplicationsSupport ....
    
    private static final Storage STORAGE = new Storage(new File(Storage.getPersistentStorageDirectoryString() + File.separator + STORAGE_DIRNAME));

    private final static GFJmxModelFactory INSTANCE = new GFJmxModelFactory();
    
    private DialogDescriptor credentialsDD = null;
    private JMXDetailsPanel credentialsPanel = null;
    
    public JmxModel createModelFor(Application app) {
        // Local applications will use the default JmxModelFactory
        if (app.isLocalApplication()) {
            return null;
        }
        // Non-GlassFish remote applications will use the default JmxModelFactory too
        ApplicationType at = ApplicationTypeFactory.getApplicationTypeFor(app);
        if (!(at instanceof GlassFishApplicationType)) {
            return null;
        }
        String userName = STORAGE.getCustomProperty(PROPERTY_USERNAME);
        String password = STORAGE.getCustomProperty(PROPERTY_PASSWORD);
        int serverPort = 8686;
        getCredentialsPanel().setPassword(password);
        getCredentialsPanel().setUserName(userName);
        if (DialogDisplayer.getDefault().notify(getDialogDescriptor()) == DialogDescriptor.OK_OPTION) {
            STORAGE.setCustomProperty(PROPERTY_USERNAME, getCredentialsPanel().getUserName());
            STORAGE.setCustomProperty(PROPERTY_PASSWORD, getCredentialsPanel().getPassword());
            serverPort = getCredentialsPanel().getServerPort();
        } else {
            // Revert to default JmxModelFactory on Cancel button
            return null;
        }
        JMXServiceURL serverURL;
        try {
            serverURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" +
                    app.getHost().getHostName() + ":" + serverPort + "/jmxrmi");
        } catch (MalformedURLException e) {
            return null;
        }
        return null;
//        return new JmxModel(new JmxApplication(app.getHost(), serverURL, STORAGE));
    }

    @Override
    public int priority() {
        return 10;
    }
    
    public static void initialize() {
        JmxModelFactory.getDefault().registerProvider(INSTANCE);
    }

    public static void shutdown() {
        JmxModelFactory.getDefault().unregisterProvider(INSTANCE);
    }
    
    private synchronized  DialogDescriptor getDialogDescriptor() {
        if (credentialsDD == null) {
            credentialsDD = new DialogDescriptor(getCredentialsPanel(), "JMX Credentials", true, DialogDescriptor.OK_CANCEL_OPTION, DialogDescriptor.CANCEL_OPTION, null);
        }
        return credentialsDD;
    }
    private synchronized JMXDetailsPanel getCredentialsPanel() {
        if (credentialsPanel == null) {
            credentialsPanel = new JMXDetailsPanel();
            credentialsPanel.addPropertyChangeListener(JMXDetailsPanel.VALIDITY_PROPERTY, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    getDialogDescriptor().setValid(getCredentialsPanel().hasValidCredentials());
                }
            });
        }
        return credentialsPanel;
    }
}
