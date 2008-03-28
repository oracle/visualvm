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

package com.sun.tools.visualvm.modules.jconsole;

import com.sun.tools.jconsole.JConsoleContext;
import com.sun.tools.jconsole.JConsoleContext.ConnectionState;
import com.sun.tools.jconsole.JConsolePlugin;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.modules.jconsole.options.JConsoleSettings;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModelFactory;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.management.remote.JMXServiceURL;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import org.openide.util.Utilities;
import sun.tools.jconsole.JConsole;
import sun.tools.jconsole.LocalVirtualMachine;
import sun.tools.jconsole.MBeansTab;
import sun.tools.jconsole.ProxyClient;
import sun.tools.jconsole.Tab;
import sun.tools.jconsole.VMPanel;

/**
 * @author Leif Samuelsson
 * @author Luis-Miguel Alventosa
 */
class JConsoleView extends DataSourceView {

    private static final String PROPERTY_USERNAME = "prop_username";
    private static final String PROPERTY_PASSWORD = "prop_password";
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/jconsole/ui/resources/jconsole.png"; // NOI18N
    private static final Logger LOGGER = Logger.getLogger(JConsoleView.class.getName());
    
    private Application application;
    private DataViewComponent view;

    public JConsoleView(Application application) {
        super(application, "JConsole Plugins", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
        this.application = application;
        view = createViewComponent();
    }

    @Override
    public DataViewComponent getView() {
        return view;
    }

    private DataViewComponent createViewComponent() {
        JComponent jconsoleView = null;
        JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
        if (jmx.getMBeanServerConnection() == null) {
            JTextArea textArea = new JTextArea("\n\nData not available in " +
                    "this tab because JMX connection to the JMX agent couldn't " +
                    "be established.");
            textArea.setEditable(false);
            jconsoleView = textArea;
        } else {
            try {
                // Enable JConsole debugging
                Field debugField = JConsole.class.getDeclaredField("debug");   // NOI18N
                debugField.setAccessible(true);
                debugField.setBoolean(null, true);

                // Set JConsole plugin path and plugin service
                String pluginPath = JConsoleSettings.getDefault().getPluginsPath();
                boolean availablePlugins = false;
                if (pluginPath != null && !pluginPath.isEmpty()) {
                    Field pluginPathField = JConsole.class.getDeclaredField("pluginPath");   // NOI18N
                    pluginPathField.setAccessible(true);
                    pluginPathField.set(null, pluginPath);
                    Method pathToURLs = JConsole.class.getDeclaredMethod("pathToURLs", String.class);   // NOI18N
                    pathToURLs.setAccessible(true);
                    ClassLoader pluginCL = new URLClassLoader(
                            (URL[]) pathToURLs.invoke(null, pluginPath),
                            JConsole.class.getClassLoader());
                    ServiceLoader<JConsolePlugin> plugins =
                            ServiceLoader.load(JConsolePlugin.class, pluginCL);
                    availablePlugins = plugins.iterator().hasNext();
                    Field pluginServiceField = JConsole.class.getDeclaredField("pluginService");   // NOI18N
                    pluginServiceField.setAccessible(true);
                    pluginServiceField.set(null, plugins);
                }

                if (availablePlugins) {
                    ProxyClient proxyClient = null;
                    Jvm jvm = JvmFactory.getJVMFor(application);
                    JvmstatModel jvmstat = JvmstatModelFactory.getJvmstatFor(application);
                    if (jvmstat != null) { // Use Jvmstat model
                        Storage storage = application.getStorage();
                        String username = storage.getCustomProperty(PROPERTY_USERNAME);
                        String password = storage.getCustomProperty(PROPERTY_PASSWORD);
                        // Create ProxyClient (i.e. create the JMX connection to the JMX agent)
                        if (Application.CURRENT_APPLICATION.equals(application)) {
                            // Monitor self
                            proxyClient = ProxyClient.getProxyClient("localhost", 0, null, null); // NOI18N
                        } else if (application.isLocalApplication()) {
                            // Create a ProxyClient from local pid
                            String connectorAddress = jvmstat.findByName(
                                    "sun.management.JMXConnectorServer.address"); // NOI18N
                            LocalVirtualMachine lvm = new LocalVirtualMachine(
                                    application.getPid(), "Dummy command line",
                                    jvm.isAttachable(), connectorAddress);
                            proxyClient = ProxyClient.getProxyClient(lvm);
                        } else {
                            // Create a ProxyClient for the remote out-of-the-box
                            // JMX management agent using the port and security
                            // related information retrieved through jvmstat.
                            List<String> urls = jvmstat.findByPattern("sun.management.JMXConnectorServer.[0-9]+.address"); // NOI18N
                            if (urls.size() != 0) {
                                List<String> auths = jvmstat.findByPattern("sun.management.JMXConnectorServer.[0-9]+.authenticate"); // NOI18N
                                proxyClient = ProxyClient.getProxyClient(urls.get(0), username, password);
//                                if (username != null && "true".equals(auths.get(0))) {
//                                    supplyCredentials(application, proxyClient);
//                                }
                            } else {
                                // Create a ProxyClient for the remote out-of-the-box
                                // JMX management agent using the port specified in
                                // the -Dcom.sun.management.jmxremote.port=<port>
                                // system property
                                String jvmArgs = jvm.getJvmArgs();
                                StringTokenizer st = new StringTokenizer(jvmArgs);
                                int port = -1;
                                boolean authenticate = false;
                                while (st.hasMoreTokens()) {
                                    String token = st.nextToken();
                                    if (token.startsWith("-Dcom.sun.management.jmxremote.port=")) { // NOI18N
                                        port = Integer.parseInt(token.substring(token.indexOf("=") + 1));
                                    } else if (token.equals("-Dcom.sun.management.jmxremote.authenticate=true")) { // NOI18N
                                        authenticate = true;
                                    }
                                }
                                if (port != -1) {
                                    proxyClient = ProxyClient.getProxyClient(
                                            application.getHost().getHostName(), port, username, password);
//                                    if (username != null && authenticate) {
//                                        supplyCredentials(application, proxyClient);
//                                    }
                                }
                            }
                        }
                    } else { // Use JMX model
                        JMXServiceURL url = jmx.getJMXServiceURL();
                        Storage storage = application.getStorage();
                        String username = storage.getCustomProperty(PROPERTY_USERNAME);
                        String password = storage.getCustomProperty(PROPERTY_PASSWORD);
                        proxyClient = ProxyClient.getProxyClient(url.toString(), username, password);
                    }

                    // Remove static list of VMPanel core tabs before they're instantiated
                    Field tabInfosField = VMPanel.class.getDeclaredField("tabInfos");
                    tabInfosField.setAccessible(true);
                    ArrayList tabInfos = ((ArrayList) tabInfosField.get(null));
                    tabInfos.clear();

                    // Create a VMPanel
                    Constructor vmPanelConstructor = VMPanel.class.getDeclaredConstructors()[0];
                    vmPanelConstructor.setAccessible(true);
                    final VMPanel vmPanel = (VMPanel) vmPanelConstructor.newInstance(
                            new Object[]{ proxyClient, JConsoleSettings.getDefault().getPolling() * 1000 });

                    // Take over handling of connections events, mostly to avoid
                    // activating the SheetDialog (which would require a JConsole object).
                    proxyClient.removePropertyChangeListener(vmPanel);
                    proxyClient.addPropertyChangeListener(new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent e) {
                            if (JConsoleContext.CONNECTION_STATE_PROPERTY.equals(e.getPropertyName())) {
                                ConnectionState newState = (ConnectionState) e.getNewValue();
                                switch (newState) {
                                    case CONNECTED:
                                        vmPanel.propertyChange(e);
                                        break;
                                }
                            }
                        }
                    });
                    vmPanel.connect();

                    jconsoleView = vmPanel;
                } else {
                    JTextArea textArea = new JTextArea(
                            "\n\nJConsole Plugins not available.\n\nThe paths " +
                            "to the jar files of the JConsole plugins to look " +
                            "up can be specified using the \"Plugins Path\" option." +
                            "\n\nChoose \"Tools > Options\" from the main menu, " +
                            "then click on the \"JConsole Plugins\" icon.");
                    textArea.setEditable(false);
                    jconsoleView = textArea;
                }
            } catch (Exception e) {
                LOGGER.throwing(JConsoleView.class.getName(), "createViewComponent", e);
                jconsoleView = new JLabel("\n\nUnexpected error: " + e.getMessage());
            }
        }
        return new DataViewComponent(
                new DataViewComponent.MasterView("JConsole Plugins", null, jconsoleView),
                new DataViewComponent.MasterViewConfiguration(true));
    }
}
