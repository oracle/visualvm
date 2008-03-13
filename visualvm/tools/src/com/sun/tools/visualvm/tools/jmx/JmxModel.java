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

package com.sun.tools.visualvm.tools.jmx;

import com.sun.tools.visualvm.core.model.Model;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import javax.management.MBeanServerConnection;
import javax.swing.event.SwingPropertyChangeSupport;


/**
 * This class encapsulates the JMX functionality of the target Java application.
 *
 * Call {@link JmxModelFactory.getJmxModelFor()} to get an instance of the
 * {@link JmxModel} class.
 *
 * Usually this class will be used as follows:
 *
 * <pre>
 * JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
 * MBeanServerConnection mbsc = jmx.getMBeanServerConnection();
 * if (mbsc != null) {
 *    // Invoke JMX operations...
 * }
 * </pre>
 *
 * {@link JmxModel.getCachedMBeanServerConnection()} should be called
 * if you want to get a {@link CachedMBeanServerConnection} instead of
 * a plain MBeanServerConnection.
 *
 * In case the JMX connection is not established yet, you could register
 * a listener on the {@code JmxModel} for ConnectionState property changes.
 * The JmxModel notifies any PropertyChangeListeners about the ConnectionState
 * property change to CONNECTED and DISCONNECTED. The JmxModel instance will
 * be the source for any generated events.
 *
 * Polling for the ConnectionState is also possible by calling
 * {@link JmxModel.getConnectionState()}.
 *
 * @author Luis-Miguel Alventosa
 */
public abstract class JmxModel extends Model {

    protected SwingPropertyChangeSupport propertyChangeSupport =
            new SwingPropertyChangeSupport(this, true);
    /**
     * The {@link ConnectionState ConnectionState} bound property name.
     */
    public static String CONNECTION_STATE_PROPERTY = "connectionState";

    /**
     * Values for the {@linkplain #CONNECTION_STATE_PROPERTY
     * <i>ConnectionState</i>} bound property.
     */
    public enum ConnectionState {

        /**
         * The connection has been successfully established.
         */
        CONNECTED,
        /**
         * No connection present.
         */
        DISCONNECTED,
        /**
         * The connection is being attempted.
         */
        CONNECTING
    }

    /**
     * Add a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If {@code listener} is {@code null}, no exception is thrown and
     * no action is taken.
     *
     * @param listener the {@code PropertyChangeListener} to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    private void addWeakPropertyChangeListener(PropertyChangeListener listener) {
        if (!(listener instanceof WeakPCL)) {
            listener = new WeakPCL(listener);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * from the listener list. This
     * removes a {@code PropertyChangeListener} that was registered for all
     * properties. If {@code listener} was added more than once to the same
     * event source, it will be notified one less time after being removed. If
     * {@code listener} is {@code null}, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener the {@code PropertyChangeListener} to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (!(listener instanceof WeakPCL)) {
            // Search for the WeakPCL holding this listener (if any)
            for (PropertyChangeListener pcl : propertyChangeSupport.getPropertyChangeListeners()) {
                if (pcl instanceof WeakPCL && ((WeakPCL) pcl).get() == listener) {
                    listener = pcl;
                    break;
                }
            }
        }
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Returns the current connection state.
     *
     * @return the current connection state.
     */
    public abstract ConnectionState getConnectionState();

    /**
     * Returns the {@link MBeanServerConnection MBeanServerConnection} for the
     * connection to an application. The returned {@code MBeanServerConnection}
     * object becomes invalid when the connection state is changed to the
     * {@link ConnectionState#DISCONNECTED DISCONNECTED} state.
     *
     * @return the {@code MBeanServerConnection} for the
     * connection to an application. It returns {@code null}
     * if the JMX connection couldn't be established.
     */
    public abstract MBeanServerConnection getMBeanServerConnection();

    /**
     * Returns the {@link CachedMBeanServerConnection cached MBeanServerConnection}
     * for the connection to an application. The returned {@code CachedMBeanServerConnection}
     * object becomes invalid when the connection state is changed to the
     * {@link ConnectionState#DISCONNECTED DISCONNECTED} state.
     *
     * @return the {@code CachedMBeanServerConnection} for the
     * connection to an application. It returns {@code null}
     * if the JMX connection couldn't be established.
     */
    public abstract CachedMBeanServerConnection getCachedMBeanServerConnection();

    /**
     * The PropertyChangeListener is handled via a WeakReference
     * so as not to pin down the listener.
     */
    private class WeakPCL extends WeakReference<PropertyChangeListener>
            implements PropertyChangeListener {

        WeakPCL(PropertyChangeListener referent) {
            super(referent);
        }

        public void propertyChange(PropertyChangeEvent pce) {
            PropertyChangeListener pcl = get();

            if (pcl == null) {
                // The referent listener was GC'ed, we're no longer
                // interested in PropertyChanges, remove the listener.
                dispose();
            } else {
                pcl.propertyChange(pce);
            }
        }

        private void dispose() {
            JmxModel.this.removePropertyChangeListener(this);
        }
    }

}
