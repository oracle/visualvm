/*
 * Copyright 2004-2006 Sun Microsystems, Inc.  All Rights Reserved.
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

import static com.sun.tools.jconsole.JConsoleContext.ConnectionState.*;
import com.sun.tools.jconsole.JConsolePlugin;
import com.sun.tools.visualvm.application.Application;
import static com.sun.tools.visualvm.modules.jconsole.ProxyClient.*;
import com.sun.tools.visualvm.modules.jconsole.options.JConsoleSettings;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

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
        for (JConsolePlugin p : plugins.keySet()) {
            p.dispose();
        }
        // Cancel pending update tasks
        //
        if (timer != null) {
            timer.cancel();
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
            timer.cancel();
        }
        TimerTask timerTask = new TimerTask() {
            public void run() {
                update();
            }
        };
        String timerName = "Timer-" + application.getId(); // NOI18N
        timer = new Timer(timerName, true);
        timer.schedule(timerTask, 0, updateInterval);
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
                            sw.execute();
                        }
                    }
                }
            }
        }
    }

    private void createPluginTabs() {
        // Add plugin tabs if not done
        if (!pluginTabsAdded) {
            for (JConsolePlugin p : plugins.keySet()) {
                Map<String, JPanel> tabs = p.getTabs();
                for (Map.Entry<String, JPanel> e : tabs.entrySet()) {
                    addTab(e.getKey(), e.getValue());
                }
            }
            pluginTabsAdded = true;
        }
    }
}
