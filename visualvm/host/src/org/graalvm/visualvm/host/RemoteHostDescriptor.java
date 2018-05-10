/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.host;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import java.awt.Image;
import java.util.Comparator;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * DataSourceDescriptor for remote hosts.
 *
 * @author Jiri Sedlacek
 */
public class RemoteHostDescriptor extends DataSourceDescriptor<Host> {

    private static final Image NODE_ICON = ImageUtilities.loadImage(
            "org/graalvm/visualvm/host/resources/remoteHost.png", true);   // NOI18N

    /**
     * Creates new instance of RemoteHostDescriptor for a given host.
     * 
     * @param host Host for which to create the descriptor.
     */
    public RemoteHostDescriptor(Host host) {
        super(host, resolveName(host, host.getHostName()), NbBundle.getMessage(
              RemoteHostDescriptor.class, "DESCR_Remote"), NODE_ICON, // NOI18N
              resolvePosition(host, POSITION_AT_THE_END, true), EXPAND_ON_FIRST_CHILD);

        setChildrenComparator(DataSourceDescriptorFactory.getDescriptor(
                              Host.LOCALHOST).getChildrenComparator());
    }


    /**
     * Sets a custom comparator for sorting DataSources within a Host.
     * Use setChildrenComparator(null) to restore the default sorting.
     *
     * @param newComparator comparator for sorting DataSources within a Host
     *
     * @since VisualVM 1.3
     */
    public void setChildrenComparator(Comparator<DataSource> newComparator) {
        super.setChildrenComparator(newComparator);
    }

    public boolean supportsRename() {
        return true;
    }

    public boolean providesProperties() {
        return true;
    }
    
}
