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

package net.java.visualvm.modules.glassfish.jmx;

import com.sun.tools.visualvm.core.application.JmxApplication;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasupport.Storage;
import com.sun.tools.visualvm.core.model.ModelFactory;
import com.sun.tools.visualvm.core.model.ModelProvider;
import com.sun.tools.visualvm.core.model.apptype.ApplicationType;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.model.jmx.JmxModel;
import com.sun.tools.visualvm.core.model.jmx.JmxModelFactory;
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
    private static final String STORAGE_DIRNAME = "jmxapplicationsGF"; // copy form JmxApplicationsSupport ....
    private static final String PROPERTY_USERNAME = "prop_username"; // copy form JmxApplicationsSupport ....
    private static final String PROPERTY_PASSWORD = "prop_password"; // copy form JmxApplicationsSupport ....
    
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
        }
        JMXServiceURL serverURL;
        try {
            serverURL = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" +
                    app.getHost().getHostName() + ":" + serverPort + "/jmxrmi");
        } catch (MalformedURLException e) {
            return null;
        }
        return new JmxModel(new JmxApplication(app.getHost(), serverURL, STORAGE));
    }

    @Override
    public int depth() {
        return 10;
    }
    
    public static void initialize() {
        JmxModelFactory.getDefault().registerFactory(INSTANCE);
    }

    public static void shutdown() {
        JmxModelFactory.getDefault().unregisterFactory(INSTANCE);
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
