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

import com.sun.tools.visualvm.core.host.HostsSupport;
import java.net.InetAddress;
import java.util.Set;

/**
 * DataSource representing a host.
 *
 * @author Jiri Sedlacek
 */
public interface Host extends DataSource {

    /**
     * Instance representing the localhost.
     */
    public static final Host LOCALHOST = HostsSupport.getInstance().getLocalHost();

    /**
     * Returns host name or IP of the host.
     * This is the string used when adding new host to VisualVM.
     * 
     * @return host name or IP of the host.
     */
    public String getHostName();

    /**
     * Returns an InetAddress instance for this hist.
     * 
     * @return InetAddress instance for this hist.
     */
    public InetAddress getInetAddress();

    /**
     * Returns set of applications known to be running on this host.
     * 
     * @return set of applications known to be running on this host.
     */
    public Set<Application> getApplications();

}
