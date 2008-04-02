/*
 * Copyright 2004-2007 Sun Microsystems, Inc.  All Rights Reserved.
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
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.management.*;
import javax.swing.event.SwingPropertyChangeSupport;

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
