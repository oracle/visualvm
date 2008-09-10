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

package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Utils;
import com.sun.tools.visualvm.host.impl.HostDescriptorProvider;
import com.sun.tools.visualvm.host.impl.HostProperties;
import com.sun.tools.visualvm.host.impl.HostProvider;
import java.io.File;
import java.net.InetAddress;

/**
 * Support for hosts in VisualVM.
 *
 * @author Jiri Sedlacek
 */
public final class HostsSupport {

    private static final String HOSTS_STORAGE_DIRNAME = "hosts";    // NOI18N
    
    private static final Object hostsStorageDirectoryLock = new Object();
    // @GuardedBy hostsStorageDirectoryLock
    private static File hostsStorageDirectory;
    private static final Object hostsStorageDirectoryStringLock = new Object();
    // @GuardedBy hostsStorageDirectoryStringLock
    private static String hostsStorageDirectoryString;
    
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
     * Creates new host from provided hostname.
     * 
     * @param hostname hostname of the host to be created.
     * @return new host from provided hostname or null if the hostname could not be resolved.
     */
    public Host createHost(String hostname) {
        return hostProvider.createHost(new HostProperties(hostname, hostname), true);
    }
    
    /**
     * Creates new host from provided hostname and display name.
     * 
     * @param hostname hostname of the host to be created.
     * @param displayname displayname of the host to be created.
     * @return new host from provided hostname or null if the hostname could not be resolved.
     */
    public Host createHost(String hostname, String displayname) {
        return hostProvider.createHost(new HostProperties(hostname, displayname), true);
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
    
    
    static String getStorageDirectoryString() {
        synchronized(hostsStorageDirectoryStringLock) {
            if (hostsStorageDirectoryString == null)
                hostsStorageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + HOSTS_STORAGE_DIRNAME;
            return hostsStorageDirectoryString;
        }
    }
    
    /**
     * Returns storage directory for defined hosts.
     * 
     * @return storage directory for defined hosts.
     */
    public static File getStorageDirectory() {
        synchronized(hostsStorageDirectoryLock) {
            if (hostsStorageDirectory == null) {
                String snapshotsStorageString = getStorageDirectoryString();
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
        return new File(getStorageDirectoryString()).isDirectory();
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
