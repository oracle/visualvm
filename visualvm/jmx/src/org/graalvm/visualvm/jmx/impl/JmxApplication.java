/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jmx.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.datasupport.Utils;
import org.graalvm.visualvm.host.Host;
import org.graalvm.visualvm.jmx.EnvironmentProvider;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModel.ConnectionState;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.tools.jmx.JvmMXBeans;
import org.graalvm.visualvm.tools.jmx.JvmMXBeansFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.remote.JMXServiceURL;
import org.graalvm.visualvm.application.jvm.JvmFactory;

/**
 * This type of application represents an application
 * that is built from a {@link JMXServiceURL}.
 * 
 * @author Luis-Miguel Alventosa
 * @author Jiri Sedlacek
 */
public final class JmxApplication extends Application {
    
    private static final Logger LOGGER = Logger.getLogger(JmxApplication.class.getName());
    
    static final String PROPERTY_DISABLE_HEARTBEAT = "prop_disable_heartbeat"; // NOI18N
    
    private int pid = UNKNOWN_PID;
    private final JMXServiceURL url;
    private final EnvironmentProvider envProvider;
    private final Storage storage;
    // since getting JVM for the first time can take a long time
    // hard reference jvm from application so we are sure that it is not garbage collected
    private Jvm jvm;
    private JmxModel jmxModel;
    private ProxyClient client;
    
    private PropertyChangeListener modelListener;
    
    private final Object connectionLock = new Object();

    // Note: storage may be null, in this case the JmxApplication isn't persistent
    // and creates a temporary storage just like any other regular Application
    public JmxApplication(Host host, JMXServiceURL url, EnvironmentProvider envProvider, Storage storage) {
        super(host, createId(url, envProvider, storage), STATE_UNAVAILABLE);
        this.url = url;
        this.envProvider = envProvider;
        this.storage = storage;
    }


    public JMXServiceURL getJMXServiceURL() {
        return url;
    }

    public EnvironmentProvider getEnvironmentProvider() {
        return envProvider;
    }

    public int getPid() {
        if (pid == UNKNOWN_PID && getState() == Stateful.STATE_AVAILABLE) {
            if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                if (mxbeans != null) {
                    RuntimeMXBean rt = mxbeans.getRuntimeMXBean();
                    if (rt != null) {
                        String name = rt.getName();
                        if (name != null && name.contains("@")) { // NOI18N
                            name = name.substring(0, name.indexOf('@')); // NOI18N
                            pid = Integer.parseInt(name);
                        }
                    }
                }
            }
        }
        return pid;
    }

    private static final String[] HOST_PROPS = {
        "os.arch",
        "os.name",
        "os.version",
        "user.home",
        "user.name"
    };
    public boolean isLocalApplication() {
        if (super.isLocalApplication()) {
            // try to detect tunneled application
            if (getState() == Stateful.STATE_AVAILABLE) {
                if (jmxModel != null && jmxModel.getConnectionState() == ConnectionState.CONNECTED) {
                    JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                    if (mxbeans != null) {
                        RuntimeMXBean rt = mxbeans.getRuntimeMXBean();
                        if (rt != null) {
                            Map<String, String> appProperties = rt.getSystemProperties();
                            if (!matchProps(HOST_PROPS, appProperties)) {
                                return false;
                            }
                            if (!checkHostName(ManagementFactory.getRuntimeMXBean(), rt)) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    public boolean supportsUserRemove() {
        return true;
    }
    
    protected boolean supportsFinishedRemove() {
        return storage == null;
    }
    
    protected Storage createStorage() {
        return storage != null ? storage : super.createStorage();
    }
    
    protected void remove() {
        if (getStorage().directoryExists())
            Utils.delete(getStorage().getDirectory(), true);
    }

    public String toString() {
        return "JmxApplication [id: " + getId() + "]";   // NOI18N
    }

    private static String createId(JMXServiceURL url, EnvironmentProvider envProvider,
                                   Storage storage) {
        // url.toString will always be used
        String urlId = url.toString();
        
        // No envProvider -> return just url.toString()
        if (envProvider == null) return urlId;

        // No environmentID -> return just url.toString()
        String envId = envProvider.getEnvironmentId(storage);
        if (envId == null || "".equals(envId)) return urlId; // NOI18N

        // Defined environmentID -> use 'environmentID-url.toString()'
        // Typically 'username-service:jmx:rmi:///jndi/rmi://hostName:portNum/jmxrmi'
        return envId + "-" + urlId; // NOI18N
    }

    final ProxyClient getProxyClient() {
        synchronized (connectionLock) {
            return client;
        }
    }
    
    
    // Only to be called from JmxHeartbeat
    // Use JmxHeartbeat.scheduleImmediately(JmxApplication) from any other code!
    final boolean tryConnect() {
        synchronized (connectionLock) {
            if (isConnected()) return true;
            
            try {
                ProxyClient newClient = new ProxyClient(this);
                newClient.connect();
                if (newClient.getConnectionState() == ConnectionState.CONNECTED) {
                    client = newClient;

                    setStateImpl(Stateful.STATE_AVAILABLE);

                    jmxModel = JmxModelFactory.getJmxModelFor(this);
                    jvm = JvmFactory.getJVMFor(this);

                    modelListener = new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {
                            if (evt.getNewValue() != ConnectionState.CONNECTED) {
                                synchronized (connectionLock) {
                                    setStateImpl(Stateful.STATE_UNAVAILABLE);
                                }
                            }
                        }
                    };
                    jmxModel.addPropertyChangeListener(modelListener);

                    return true;
                }
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "ProxyClient.connect", ex); // NOI18N
            }

            return false;
        }
    }
    
    final void disconnect() {
        disableHeartbeat();
        
        ProxyClient _client;
        synchronized (connectionLock) {
            if (!isConnected()) return;
            _client = client;
        }
        
        _client.disconnect(); // will invoke modelListener.propertyChange() -> ConnectionState.DISCONNECTED
    }
    
    private boolean isConnected() { // must be called under connectionLock
        return client != null && client.getConnectionState() == ConnectionState.CONNECTED;
    }
    
    
    private void setStateImpl(int newState) { // must be called under connectionLock
        if (newState != Stateful.STATE_AVAILABLE) {
            pid = UNKNOWN_PID;
            jvm = null;
            if (jmxModel != null && modelListener != null) jmxModel.removePropertyChangeListener(modelListener);
            jmxModel = null;
            client = null;
            if (supportsHeartbeat(this)) JmxHeartbeat.scheduleLazily(this);
        }
        
        setState(newState);
    }
    
    
    final void enableHeartbeat() {
        getStorage().clearCustomProperty(PROPERTY_DISABLE_HEARTBEAT);
        if (supportsHeartbeat(this)) {
            synchronized (connectionLock) {
                if (isConnected()) return;
            }
            JmxHeartbeat.scheduleImmediately(this);
        }
    }
    
    final void disableHeartbeat() {
        getStorage().setCustomProperty(PROPERTY_DISABLE_HEARTBEAT, Boolean.TRUE.toString());
    }
    
    final boolean isHeartbeatDisabled() {
        return Boolean.TRUE.toString().equals(getStorage().getCustomProperty(PROPERTY_DISABLE_HEARTBEAT));
    }
    
    
    static boolean supportsHeartbeat(JmxApplication app) {
        return !app.isRemoved() && !app.isHeartbeatDisabled();
    }

    private boolean matchProps(String[] propNames, Map<String, String> appProperties) {
        for (String prop : propNames) {
            String localProp = System.getProperty(prop);
            String appProp = appProperties.get(prop);
            if (!Objects.equals(localProp, appProp)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkHostName(RuntimeMXBean localRuntime, RuntimeMXBean remoteRuntime) {
        String localHost = getHostName(localRuntime.getName());
        String remoteHost = getHostName(remoteRuntime.getName());

        return Objects.equals(localHost, remoteHost);
    }
    
    private String getHostName(String runtimeName) {
        if (runtimeName == null) return null;
        int index = runtimeName.indexOf('@');       // NOI18N

        if (index >= 0) {
            return runtimeName.substring(index+1);
        }
        return null;
    }
}
