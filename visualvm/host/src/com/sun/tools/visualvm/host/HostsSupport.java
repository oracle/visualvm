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

package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.host.impl.HostDescriptorProvider;
import com.sun.tools.visualvm.host.impl.HostProperties;
import com.sun.tools.visualvm.host.impl.HostProvider;
import com.sun.tools.visualvm.host.impl.HostsSupportImpl;
import java.io.File;
import java.net.InetAddress;

/**
 * Support for hosts in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class HostsSupport {
    
    private static final Object hostsStorageDirectoryLock = new Object();
    // @GuardedBy hostsStorageDirectoryLock
    private static File hostsStorageDirectory;
    
    private static HostsSupport instance;

    private final HostProvider hostProvider = new HostProvider();


    /**
     * Returns singleton instance of HostsSupport.
     * 
     * @return singleton instance of HostsSupport.
     */
    public static synchronized HostsSupport getInstance() {
        if (instance == null) instance = new HostsSupport();
        return instance;
    }
    

    /**
     * Creates new host from provided hostname. Displays a popup dialog if wrong
     * hostname is provided or the host has already been defined.
     * 
     * @param hostname hostname of the host to be created.
     * @return new host from provided hostname or null if the hostname could not be resolved.
     */
    public Host createHost(String hostname) {
        return createHost(new HostProperties(hostname, hostname, null), true, true);
    }
    
    /**
     * Creates new host from provided hostname and display name. Displays a popup
     * dialog if wrong hostname is provided or the host has already been defined.
     * 
     * @param hostname hostname of the host to be created.
     * @param displayname displayname of the host to be created.
     * @return new host from provided hostname or null if the hostname could not be resolved.
     */
    public Host createHost(String hostname, String displayname) {
        return createHost(new HostProperties(hostname, displayname, null), true, true);
    }

    /**
     * Returns an existing Host instance or creates a new Host if needed.
     *
     * @param hostname hostname of the host to be created
     * @param interactive true if any failure should be visually presented to the user, false otherwise.
     * @return an existing or a newly created Host
     *
     * @since VisualVM 1.1.1
     */
    public Host getOrCreateHost(String hostname, boolean interactive) {
        return createHost(new HostProperties(hostname, hostname, null), false, interactive);
    }

    Host createHost(HostProperties properties, boolean createOnly, boolean interactive) {
        return hostProvider.createHost(properties, createOnly, interactive);
    }
    
    /**
     * Returns already known Host instance with the same InetAddress or null.
     * 
     * @param inetAddress InetAddess to search.
     * @return already known Host instance with the same InetAddress or null.
     */
    public Host getHostByAddress(InetAddress inetAddress) {
        return hostProvider.getHostByAddress(inetAddress);
    }
    
    /**
     * Returns storage directory for defined hosts.
     * 
     * @return storage directory for defined hosts.
     */
    public static File getStorageDirectory() {
        synchronized(hostsStorageDirectoryLock) {
            if (hostsStorageDirectory == null) {
                String snapshotsStorageString = HostsSupportImpl.getStorageDirectoryString();
                hostsStorageDirectory = new File(snapshotsStorageString);
                if (hostsStorageDirectory.exists() && hostsStorageDirectory.isFile())
                    throw new IllegalStateException("Cannot create hosts storage directory " + snapshotsStorageString + ", file in the way");   // NOI18N
                if (hostsStorageDirectory.exists() && (!hostsStorageDirectory.canRead() || !hostsStorageDirectory.canWrite()))
                    throw new IllegalStateException("Cannot access hosts storage directory " + snapshotsStorageString + ", read&write permission required");    // NOI18N
                if (!Utils.prepareDirectory(hostsStorageDirectory))
                    throw new IllegalStateException("Cannot create hosts storage directory " + snapshotsStorageString); // NOI18N
            }
            return hostsStorageDirectory;
        }
    }
    
    /**
     * Returns true if the storage directory for defined hosts already exists, false otherwise.
     * 
     * @return true if the storage directory for defined hosts already exists, false otherwise.
     */
    public static boolean storageDirectoryExists() {
        return new File(HostsSupportImpl.getStorageDirectoryString()).isDirectory();
    }
    
    
    Host createLocalHost() {
        return hostProvider.createLocalHost();
    }
    
    Host createUnknownHost() {
        return hostProvider.createUnknownHost();
    }


    private HostsSupport() {
        DataSourceDescriptorFactory.getDefault().registerProvider(new HostDescriptorProvider());
        
        RemoteHostsContainer container = RemoteHostsContainer.sharedInstance();
        DataSource.ROOT.getRepository().addDataSource(container);
        
        hostProvider.initialize();
    }

}
