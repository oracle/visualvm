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
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import com.sun.tools.visualvm.core.model.jvm.JvmstatJVM;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
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

    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/jconsole/ui/resources/jconsole.png"; // NOI18N
    private Application application;
    private DataViewComponent view;

    public JConsoleView(Application application) {
        super("JConsole", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60);
        this.application = application;
        view = createViewComponent();
    }

    @Override
    public DataViewComponent getView() {
        return view;
    }

    private DataViewComponent createViewComponent() {
        JComponent jconsoleView = null;
        try {
            // Enable JConsole debugging
            Field debugField = JConsole.class.getDeclaredField("debug");   // NOI18N
            debugField.setAccessible(true);
            debugField.setBoolean(null, true);

            // Set JConsole plugin path and plugin service
            String pluginPath = System.getProperty("jconsole.plugin.path");   // NOI18N
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
                // Create ProxyClient (i.e. create the JMX connection to the JMX agent)
                JvmstatJVM jvm = (JvmstatJVM) JVMFactory.getJVMFor(application);
                ProxyClient proxyClient = null;
                if (Application.CURRENT_APPLICATION.equals(application)) {
                    // Monitor self
                    proxyClient = ProxyClient.getProxyClient("localhost", 0, null, null);   // NOI18N
                } else if (application.isLocalApplication()) {
                    // Create a ProxyClient from local pid
                    String connectorAddress = jvm.findByName(
                            "sun.management.JMXConnectorServer.address");
                    LocalVirtualMachine lvm = new LocalVirtualMachine(
                            application.getPid(), "Dummy command line",
                            jvm.isAttachable(), connectorAddress);
                    proxyClient = ProxyClient.getProxyClient(lvm);
                } else {
                    // TODO: Remove the following two lines when Connection Dialog is implemented.
                    String username = System.getProperty("jconsole.username");
                    String password = System.getProperty("jconsole.password");
                    // Create a ProxyClient for the remote out-of-the-box
                    // JMX management agent using the port and security
                    // related information retrieved through jvmstat.
                    List<String> urls = jvm.findByPattern(
                            "sun.management.JMXConnectorServer.[0-9]+.address");
                    if (urls.size() != 0) {
                        proxyClient = ProxyClient.getProxyClient(urls.get(0), username, password);
                    // TODO: if security needed show popup connection dialog
                    } else {
                        // Create a ProxyClient for the remote out-of-the-box
                        // JMX management agent using the port specified in
                        // the -Dcom.sun.management.jmxremote.port=<port>
                        // system property
                        String jvmArgs = jvm.getJvmArgs();
                        StringTokenizer st = new StringTokenizer(jvmArgs);
                        int port = -1;
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            if (token.startsWith("-Dcom.sun.management.jmxremote.port=")) {   // NOI18N
                                port = Integer.parseInt(token.substring(token.indexOf("=") + 1));
                                break;
                            }
                        }
                        if (port != -1) {
                            proxyClient = ProxyClient.getProxyClient(
                                    application.getHost().getHostName(), port, username, password);
                        // TODO: if security needed show popup connection dialog
                        }
                    }
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
                        new Object[]{ proxyClient, 4000 });

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
                // TODO: if security needed show popup connection dialog

                jconsoleView = vmPanel;
            } else {
                JTextArea jTextArea = new JTextArea(
                        "\n\nJConsole Plugins not available.\n\nUsage:\n\n" +
                        "$ visualvm -J-Djconsole.plugin.path=<plugin-path>" +
                        "\n\nwhere <plugin-path> specifies the paths " +
                        "to the jar files of the\nJConsole plugins to look up." +
                        " Multiple paths are separated by\nthe path separator " +
                        "character of the platform.");
                jTextArea.setEditable(false);
                jconsoleView = jTextArea;
            }
        } catch (Exception e) {
            e.printStackTrace();
            jconsoleView = new JLabel("\n\nUnexpected error: " + e.getMessage());
        }
        return new DataViewComponent(
                new DataViewComponent.MasterView("JConsole", null, jconsoleView),
                new DataViewComponent.MasterViewConfiguration(true));
    }
}
