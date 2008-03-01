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

package com.sun.tools.visualvm.core.datasource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Abstract implementation of Host.
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractHost extends AbstractDataSource implements Host {

    private final String hostName;
    private InetAddress inetAddress;


    /**
     * Creates new instance of Host defined by hostName.
     * 
     * @param hostName name or IP of the host.
     * @throws java.net.UnknownHostException if host cannot be resolved using provided hostName/IP.
     */
    public AbstractHost(String hostName) throws UnknownHostException {
        this(hostName, InetAddress.getByName(hostName));
    }

    /**
     * Creates new instance of Host defined by hostName, displayName and InetAddress instance for the host.
     * 
     * @param hostName name or IP of the host,
     * @param displayName string which represents this Host instance in UI or null (hostName will be used instead),
     * @param inetAddress InetAddress instance for the host.
     */
    public AbstractHost(String hostName, InetAddress inetAddress) {
        if (hostName == null) throw new IllegalArgumentException("Host name cannot be null");
        if (inetAddress == null) throw new IllegalArgumentException("InetAddress cannot be null");
        
        this.hostName = hostName;
        this.inetAddress = inetAddress;
    }
    
    
    public String getHostName() {
        return hostName;
    }
    
    public final InetAddress getInetAddress() {
        return inetAddress;
    }
    
    public Set<Application> getApplications() {
        return getRepository().getDataSources(Application.class);
    }
    
    
    public int hashCode() {
        if (Host.UNKNOWN_HOST == this) return super.hashCode();
        InetAddress address = getInetAddress();
        if (this == LOCALHOST) return address.hashCode();
        if (address.isAnyLocalAddress()) return LOCALHOST.hashCode();
        else return getInetAddress().hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Host)) return false;
        if (Host.UNKNOWN_HOST == this) return obj == this;
        Host host = (Host)obj;
        InetAddress thisAddress = getInetAddress();
        InetAddress otherAddress = host.getInetAddress();
        if (thisAddress.isAnyLocalAddress() && otherAddress.isAnyLocalAddress()) return true;
        else return thisAddress.equals(otherAddress);
    }

    public String toString() {
        return getHostName() + " [IP: " + getInetAddress().getHostAddress() + "]";
    }

}
