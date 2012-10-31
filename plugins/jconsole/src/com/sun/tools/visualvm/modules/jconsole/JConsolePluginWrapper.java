/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.jconsole;

import com.sun.tools.jconsole.JConsoleContext;
import static com.sun.tools.jconsole.JConsoleContext.*;
import com.sun.tools.jconsole.JConsoleContext.ConnectionState;
import com.sun.tools.jconsole.JConsolePlugin;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.ui.components.Spacer;
import com.sun.tools.visualvm.modules.jconsole.options.JConsoleSettings;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import javax.management.MBeanServerConnection;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.SwingPropertyChangeSupport;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

class JConsolePluginWrapper {

    private static final Logger LOGGER = Logger.getLogger(JConsolePluginWrapper.class.getName());
    private ServiceLoader<JConsolePlugin> pluginService;
    private JComponent jconsoleView;
    private VMPanel vmPanel;

    JConsolePluginWrapper(Application application) {
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null || jmxModel.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
            JTextArea textArea = new JTextArea();
            textArea.setBorder(BorderFactory.createEmptyBorder(25, 9, 9, 9));
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setText(NbBundle.getMessage(JConsolePluginWrapper.class, "JMX_Not_Available")); // NOI18N
            jconsoleView = textArea;
        } else {
            boolean availablePlugins = getPlugins().iterator().hasNext();
            if (availablePlugins) {
                vmPanel = new VMPanel(application, this, new ProxyClient(jmxModel));
                vmPanel.connect();
                JPanel panel = new JPanel(new BorderLayout());
                panel.setOpaque(false);
                panel.add(new JLabel(" "), BorderLayout.NORTH); // NOI18N
                panel.add(vmPanel, BorderLayout.CENTER);
                jconsoleView = panel;
            } else {
                GridBagConstraints c;

                JPanel hintPanel = new JPanel(new GridBagLayout());
                hintPanel.setOpaque(false);
                hintPanel.setBorder(BorderFactory.createEmptyBorder(25, 9, 9, 9));

                JLabel hintLabel = new JLabel(NbBundle.getMessage(
                        JConsolePluginWrapper.class, "NoPluginInstalled")); // NOI18N
                hintLabel.setFont(hintLabel.getFont().deriveFont(Font.BOLD));
                c = new GridBagConstraints();
                c.gridy = 0;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.NONE;
                c.insets = new Insets(0, 0, 0, 0);
                hintPanel.add(hintLabel, c);

                JTextArea hintArea = new JTextArea();
                hintArea.setEnabled(false);
                hintArea.setEditable(false);
                hintArea.setLineWrap(true);
                hintArea.setWrapStyleWord(true);
                hintArea.setDisabledTextColor(hintArea.getForeground());
                hintArea.setOpaque(false);
                hintArea.setText(NbBundle.getMessage(
                        JConsolePluginWrapper.class, "InstallPluginHint")); // NOI18N
                c = new GridBagConstraints();
                c.gridy = 1;
                c.weightx = 1;
                c.anchor = GridBagConstraints.WEST;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.insets = new Insets(5, 0, 0, 0);
                hintPanel.add(hintArea, c);

                JButton optionsButton = new JButton() {
                    protected void fireActionPerformed(ActionEvent event) {
                        OptionsDisplayer.getDefault().open("JConsoleOptions"); // NOI18N
                    }
                };
                Mnemonics.setLocalizedText(optionsButton, NbBundle.getMessage(
                        JConsolePluginWrapper.class, "ConfigurePlugins")); // NOI18N
                c = new GridBagConstraints();
                c.gridy = 2;
                c.anchor = GridBagConstraints.EAST;
                c.fill = GridBagConstraints.NONE;
                c.insets = new Insets(10, 0, 0, 0);
                hintPanel.add(optionsButton, c);

                c = new GridBagConstraints();
                c.gridy = 3;
                c.weighty = 1;
                c.anchor = GridBagConstraints.NORTHWEST;
                c.fill = GridBagConstraints.BOTH;
                c.gridwidth = GridBagConstraints.REMAINDER;
                hintPanel.add(Spacer.create(), c);

                jconsoleView = hintPanel;
            }
        }
    }

    JComponent getView() {
        return jconsoleView;
    }
    void releasePlugins() {
        if (vmPanel != null) {
            vmPanel.disconnect();
        }
    }

    // Return a list of newly instantiated JConsolePlugin objects
    synchronized List<JConsolePlugin> getPlugins() {
        if (pluginService == null) {
            String pluginPath = JConsoleSettings.getDefault().getPluginsPath();
            // First time loading and initializing the plugins
            initPluginService(pluginPath == null ? "" : pluginPath); // NOI18N
        } else {
            // Reload the plugin so that new instances will be created
            pluginService.reload();
        }
        List<JConsolePlugin> plugins = new ArrayList<JConsolePlugin>();
        for (JConsolePlugin p : pluginService) {
            plugins.add(p);
        }
        return plugins;
    }

    private void initPluginService(String pluginPath) {
        if (pluginPath.length() > 0) {
            try {
                ClassLoader pluginCL = new URLClassLoader(
                        pathToURLs(pluginPath),
                        JConsolePluginWrapper.class.getClassLoader());
                ServiceLoader<JConsolePlugin> plugins =
                        ServiceLoader.load(JConsolePlugin.class, pluginCL);
                // Validate all plugins
                for (JConsolePlugin p : plugins) {
                    LOGGER.finer("JConsole plugin " + p.getClass().getName() + " loaded."); // NOI18N
                }
                pluginService = plugins;
            } catch (ServiceConfigurationError e) {
                // Error occurs during initialization of plugin
                LOGGER.warning("Fail to load JConsole plugin: " + e.getMessage()); // NOI18N
                LOGGER.throwing(JConsolePluginWrapper.class.getName(), "initPluginService", e); // NOI18N
            } catch (MalformedURLException e) {
                LOGGER.warning("Invalid JConsole plugin path: " + e.getMessage()); // NOI18N
                LOGGER.throwing(JConsolePluginWrapper.class.getName(), "initPluginService", e); // NOI18N
            }
        }
        if (pluginService == null) {
            initEmptyPlugin();
        }
    }

    private void initEmptyPlugin() {
        ClassLoader pluginCL = new URLClassLoader(new URL[0], JConsolePluginWrapper.class.getClassLoader());
        pluginService = ServiceLoader.load(JConsolePlugin.class, pluginCL);
    }

    /**
     * Utility method for converting a search path string to an array
     * of directory and JAR file URLs.
     *
     * @param path the search path string
     * @return the resulting array of directory and JAR file URLs
     */
    private static URL[] pathToURLs(String path) throws MalformedURLException {
        String[] names = path.split(File.pathSeparator);
        URL[] urls = new URL[names.length + 1];
        urls[0] = JConsolePluginWrapper.class.getProtectionDomain().getCodeSource().getLocation();
        int count = 1;
        for (String f : names) {
            URL url = fileToURL(new File(f));
            urls[count++] = url;
        }
        return urls;
    }

    /**
     * Returns the directory or JAR file URL corresponding to the specified
     * local file name.
     *
     * @param file the File object
     * @return the resulting directory or JAR file URL, or null if unknown
     */
    private static URL fileToURL(File file) throws MalformedURLException {
        String name;
        try {
            name = file.getCanonicalPath();
        } catch (IOException e) {
            name = file.getAbsolutePath();
        }
        name = name.replace(File.separatorChar, '/');
        if (!name.startsWith("/")) { // NOI18N
            name = "/" + name; // NOI18N
        }
        // If the file does not exist, then assume that it's a directory
        if (!file.isFile()) {
            name = name + "/"; // NOI18N
        }
        return new URL("file", "", name); // NOI18N
    }

    class ProxyClient implements JConsoleContext, PropertyChangeListener {

        private ConnectionState connectionState = ConnectionState.DISCONNECTED;

        // The SwingPropertyChangeSupport will fire events on the EDT
        private SwingPropertyChangeSupport propertyChangeSupport =
                new SwingPropertyChangeSupport(this, true);
        private volatile boolean isDead = true;
        private JmxModel jmxModel = null;
        private MBeanServerConnection server = null;

        ProxyClient(JmxModel jmxModel) {
            this.jmxModel = jmxModel;
        }

        private void setConnectionState(ConnectionState state) {
            ConnectionState oldState = this.connectionState;
            this.connectionState = state;
            propertyChangeSupport.firePropertyChange(CONNECTION_STATE_PROPERTY,
                    oldState, state);
        }

        public ConnectionState getConnectionState() {
            return this.connectionState;
        }

        void connect() {
            setConnectionState(ConnectionState.CONNECTING);
            try {
                tryConnect();
                setConnectionState(ConnectionState.CONNECTED);
            } catch (Exception e) {
                e.printStackTrace();
                setConnectionState(ConnectionState.DISCONNECTED);
            }
        }

        private void tryConnect() throws IOException {
            jmxModel.addPropertyChangeListener(this);
            this.server = jmxModel.getMBeanServerConnection();
            this.isDead = false;
        }

        public MBeanServerConnection getMBeanServerConnection() {
            return server;
        }

        synchronized void disconnect() {
            jmxModel.removePropertyChangeListener(this);
            // Set connection state to DISCONNECTED
            if (!isDead) {
                isDead = true;
                setConnectionState(ConnectionState.DISCONNECTED);
            }
        }

        boolean isDead() {
            return isDead;
        }

        boolean isConnected() {
            return !isDead();
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            propertyChangeSupport.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            String prop = evt.getPropertyName();
            if (CONNECTION_STATE_PROPERTY.equals(prop)) {
                com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState newState = (com.sun.tools.visualvm.tools.jmx.JmxModel.ConnectionState) evt.getNewValue();
                setConnectionState(ConnectionState.valueOf(newState.name()));
            }
        }
    }

    class VMPanel extends JTabbedPane implements PropertyChangeListener {

        private Application application;
        private ProxyClient proxyClient;
        private Timer timer;
        private int updateInterval = JConsoleSettings.getDefault().getPolling() * 1000;
        private boolean wasConnected = false;

        // Each VMPanel has its own instance of the JConsolePlugin.
        // A map of JConsolePlugin to the previous SwingWorker.
        private Map<JConsolePlugin, SwingWorker<?, ?>> plugins = null;
        private boolean pluginTabsAdded = false;

        VMPanel(Application application, JConsolePluginWrapper wrapper, ProxyClient proxyClient) {
            this.application = application;
            this.proxyClient = proxyClient;
            plugins = new LinkedHashMap<JConsolePlugin, SwingWorker<?, ?>>();
            for (JConsolePlugin p : wrapper.getPlugins()) {
                p.setContext(proxyClient);
                plugins.put(p, null);
            }
            // Start listening to connection state events
            //
            proxyClient.addPropertyChangeListener(this);
        }

        boolean isConnected() {
            return proxyClient.isConnected();
        }

        // Call on EDT
        void connect() {
            if (isConnected()) {
                // Create plugin tabs if not done
                createPluginTabs();
                // Start/Restart update timer on connect/reconnect
                startUpdateTimer();
            } else {
                proxyClient.connect();
            }
        }

        // Call on EDT
        void disconnect() {
            // Disconnect
            proxyClient.disconnect();
            // Dispose JConsole plugins
            disposePlugins(plugins.keySet());
            // Cancel pending update tasks
            //
            if (timer != null) {
                timer.stop();
            }
            // Stop listening to connection state events
            //
            proxyClient.removePropertyChangeListener(this);
        }

        // Called on EDT
        public void propertyChange(PropertyChangeEvent ev) {
            String prop = ev.getPropertyName();
            if (CONNECTION_STATE_PROPERTY.equals(prop)) {
                ConnectionState newState = (ConnectionState) ev.getNewValue();
                switch (newState) {
                    case CONNECTED:
                        // Create tabs if not done
                        createPluginTabs();
                        repaint();
                        // Start/Restart update timer on connect/reconnect
                        startUpdateTimer();
                        break;
                    case DISCONNECTED:
                        disconnect();
                        break;
                }
            }
        }

        private void startUpdateTimer() {
            if (timer != null) {
                timer.stop();
            }
            timer = new Timer(updateInterval, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {
                        public void run() {
                            update();
                        }
                    });
                }
            });
            timer.setCoalesce(true);
            timer.setInitialDelay(0);
            timer.start();
        }

        // Note: This method is called on a TimerTask thread. Any GUI manipulation
        // must be performed with invokeLater() or invokeAndWait().
        private Object lockObject = new Object();

        private void update() {
            synchronized (lockObject) {
                if (!isConnected()) {
                    if (wasConnected) {
                        disconnect();
                    }
                    wasConnected = false;
                    return;
                } else {
                    wasConnected = true;
                }
                // Plugin GUI update
                for (JConsolePlugin p : plugins.keySet()) {
                    SwingWorker<?, ?> sw = p.newSwingWorker();
                    SwingWorker<?, ?> prevSW = plugins.get(p);
                    // Schedule SwingWorker to run only if the previous
                    // SwingWorker has finished its task and it hasn't started.
                    if (prevSW == null || prevSW.isDone()) {
                        if (sw == null || sw.getState() == SwingWorker.StateValue.PENDING) {
                            plugins.put(p, sw);
                            if (sw != null) {
                                RequestProcessor.getDefault().post(sw);
                            }
                        }
                    }
                }
            }
        }

        private void createPluginTabs() {
            // Add plugin tabs if not done
            if (!pluginTabsAdded) {
                Set<JConsolePlugin> failedPlugins = new HashSet<JConsolePlugin>();
                for (JConsolePlugin p : plugins.keySet()) {
                    try {
                        Map<String, JPanel> tabs = p.getTabs();
                        for (Map.Entry<String, JPanel> e : tabs.entrySet()) {
                            addTab(e.getKey(), e.getValue());
                        }
                    } catch (Throwable t) {
                        // Error occurs during plugin tab creation.
                        failedPlugins.add(p);
                        LOGGER.warning("JConsole plugin " + p.getClass().getName() + " removed: Failed to create JConsole plugin tabs."); // NOI18N
                        LOGGER.throwing(VMPanel.class.getName(), "createPluginTabs", t); // NOI18N
                    }
                }
                // Remove plugins that failed to return the plugin tabs
                for (JConsolePlugin p : failedPlugins) {
                    plugins.remove(p);
                }
                disposePlugins(failedPlugins);
                pluginTabsAdded = true;
            }
        }

        private void disposePlugins(Set<JConsolePlugin> pluginSet) {
            for (JConsolePlugin p : pluginSet) {
                try {
                    p.dispose();
                } catch (Throwable t) {
                    // Best effort, ignore if plugin fails to cleanup itself.
                }
            }
        }
    }
}
