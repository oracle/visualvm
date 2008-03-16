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
import com.sun.tools.visualvm.host.impl.HostActionsProvider;
import com.sun.tools.visualvm.host.impl.HostDescriptorProvider;
import com.sun.tools.visualvm.host.impl.HostProperties;
import com.sun.tools.visualvm.host.impl.HostProvider;
import java.io.File;
import java.net.InetAddress;

/**
 *
 * @author Jiri Sedlacek
 */
public final class HostsSupport {

    private static final String HOSTS_STORAGE_DIRNAME = "hosts";
    
    private static File hostsStorageDirectory;
    private static String hostsStorageDirectoryString;
    
    private static HostsSupport instance;

    private final HostProvider hostProvider = new HostProvider();


    public static synchronized HostsSupport getInstance() {
        if (instance == null) instance = new HostsSupport();
        return instance;
    }
    

    public Host createHost(String hostname) {
        return getHostProvider().createHost(new HostProperties(hostname, hostname), true);
    }
    
    public Host getHostByAddress(InetAddress inetAddress) {
        return getHostProvider().getHostByAddress(inetAddress);
    }

    public HostProvider getHostProvider() {
        return hostProvider;
    }
    
    
    public static String getStorageDirectoryString() {
        if (hostsStorageDirectoryString == null)
            hostsStorageDirectoryString = Storage.getPersistentStorageDirectoryString() + File.separator + HOSTS_STORAGE_DIRNAME;
        return hostsStorageDirectoryString;
    }
    
    public static File getStorageDirectory() {
        if (hostsStorageDirectory == null) {
            String snapshotsStorageString = getStorageDirectoryString();
            hostsStorageDirectory = new File(snapshotsStorageString);
            if (hostsStorageDirectory.exists() && hostsStorageDirectory.isFile())
                throw new IllegalStateException("Cannot create hosts storage directory " + snapshotsStorageString + ", file in the way");
            if (hostsStorageDirectory.exists() && (!hostsStorageDirectory.canRead() || !hostsStorageDirectory.canWrite()))
                throw new IllegalStateException("Cannot access hosts storage directory " + snapshotsStorageString + ", read&write permission required");
            if (!Utils.prepareDirectory(hostsStorageDirectory))
                throw new IllegalStateException("Cannot create hosts storage directory " + snapshotsStorageString);
        }
        return hostsStorageDirectory;
    }
    
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
        DataSourceDescriptorFactory.getDefault().registerFactory(new HostDescriptorProvider());
        
        RemoteHostsContainer container = RemoteHostsContainer.sharedInstance();
        DataSource.ROOT.getRepository().addDataSource(container);
        
        hostProvider.initialize();

        new HostActionsProvider().initialize();
    }

}
