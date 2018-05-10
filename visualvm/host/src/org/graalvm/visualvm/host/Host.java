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

package org.graalvm.visualvm.host;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Stateful;

/**
 * Abstract implementation of Host.
 * Each host is defined by a hostname/ip if resolvable or hostname/ip and InetAddress.
 *
 * @author Jiri Sedlacek
 */
public abstract class Host extends DataSource implements Stateful {
    
    /**
     * Instance representing the localhost.
     */
    public static final Host LOCALHOST = HostsSupport.getInstance().createLocalHost();
    
    /**
     * Instance representing an unknown host (placeholder node in Applications window, hidden if not used).
     */
    public static final Host UNKNOWN_HOST = HostsSupport.getInstance().createUnknownHost();

    private final String hostName;
    private InetAddress inetAddress;
    private int state = STATE_AVAILABLE;


    /**
     * Creates new instance of Host defined by hostName.
     * 
     * @param hostName name or IP of the host.
     * @throws java.net.UnknownHostException if host cannot be resolved using provided hostName/IP.
     */
    public Host(String hostName) throws UnknownHostException {
        this(hostName, InetAddress.getByName(hostName));
    }

    /**
     * Creates new instance of Host defined by hostName and InetAddress instance for the host.
     * 
     * @param hostName name or IP of the host,
     * @param inetAddress InetAddress instance for the host.
     */
    public Host(String hostName, InetAddress inetAddress) {
        if (hostName == null) throw new IllegalArgumentException("Host name cannot be null");   // NOI18N
        if (inetAddress == null) throw new IllegalArgumentException("InetAddress cannot be null");  // NOI18N
        
        this.hostName = hostName;
        this.inetAddress = inetAddress;
    }
    
    
    /**
     * Returns hostname of the host.
     * 
     * @return hostname of the host.
     */
    public String getHostName() {
        return hostName;
    }
    
    /**
     * Returns InetAddress instance of the host.
     * 
     * @return InetAddress instance of the host.
     */
    public final InetAddress getInetAddress() {
        return inetAddress;
    }
    
    public synchronized int getState() {
        return state;
    }
    
    protected final synchronized void setState(int newState) {
        int oldState = state;
        state = newState;
        getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState);
    }
    
    
    public int hashCode() {
        if (Host.UNKNOWN_HOST == this) return super.hashCode();
        InetAddress address = getInetAddress();
        if (this == LOCALHOST) return address.hashCode();
        if (address.isLoopbackAddress()) return LOCALHOST.hashCode();
        else return address.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Host)) return false;
        if (Host.UNKNOWN_HOST == this) return obj == this;
        Host host = (Host)obj;
        InetAddress thisAddress = getInetAddress();
        InetAddress otherAddress = host.getInetAddress();
        if (thisAddress.isLoopbackAddress() && otherAddress.isLoopbackAddress()) return true;
        return thisAddress.equals(otherAddress);
    }

    public String toString() {
        return getHostName() + " [IP: " + getInetAddress().getHostAddress() + "]";  // NOI18N
    }

}
