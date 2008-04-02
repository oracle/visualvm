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

import com.sun.tools.jconsole.JConsolePlugin;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.jconsole.options.JConsoleSettings;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import org.openide.util.NbBundle;

class JConsolePluginWrapper {

    private static final Logger LOGGER = Logger.getLogger(JConsolePluginWrapper.class.getName());
    private ServiceLoader<JConsolePlugin> pluginService;
    private JComponent jconsoleView;

    JConsolePluginWrapper(Application application) {
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel.getMBeanServerConnection() == null) {
            JTextArea textArea = new JTextArea(NbBundle.getMessage(JConsolePluginWrapper.class, "JMX_Not_Available"));
            textArea.setEditable(false);
            jconsoleView = textArea;
        } else {
            boolean availablePlugins = getPlugins().iterator().hasNext();
            if (availablePlugins) {
                VMPanel vmPanel = new VMPanel(application, this, new ProxyClient(jmxModel));
                vmPanel.connect();
                jconsoleView = vmPanel;
            } else {
                JTextArea textArea = new JTextArea(NbBundle.getMessage(JConsolePluginWrapper.class, "PluginPath_Not_Available"));
                textArea.setEditable(false);
                jconsoleView = textArea;
            }
        }
    }

    JComponent getView() {
        return jconsoleView;
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
                    LOGGER.finer("Plugin " + p.getClass() + " loaded."); // NOI18N
                }
                pluginService = plugins;
            } catch (ServiceConfigurationError e) {
                // Error occurs during initialization of plugin
                
                LOGGER.finer("Warning: Fail to load plugin: " + e.getMessage()); // NOI18N
                LOGGER.throwing(JConsolePluginWrapper.class.getName(), "initPluginService", e); // NOI18N
            } catch (MalformedURLException e) {
                LOGGER.finer("Warning: Invalid plugin path: " + e.getMessage()); // NOI18N
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
}
