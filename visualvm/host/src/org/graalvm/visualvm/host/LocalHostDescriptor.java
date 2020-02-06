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
import java.awt.Image;
import java.util.Comparator;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * DataSourceDescriptor for Host.LOCALHOST.
 * 
 * @author Jiri Sedlacek
 */
public class LocalHostDescriptor extends DataSourceDescriptor<Host> {

    private static final Image NODE_ICON = ImageUtilities.loadImage(
            "org/graalvm/visualvm/host/resources/localHost.png", true);    // NOI18N

    /**
     * Creates new instance of LocalHostDescriptor.
     */
    public LocalHostDescriptor() {
        super(Host.LOCALHOST, NbBundle.getMessage(LocalHostDescriptor.class, "LBL_Local"),
              NbBundle.getMessage(LocalHostDescriptor.class, "DESCR_Local"), NODE_ICON, 0,
              EXPAND_ON_FIRST_CHILD);  // NOI18N
    }

    
    /**
     * Sets a custom comparator for sorting DataSources within the Host.LOCALHOST.
     * Use setChildrenComparator(null) to restore the default sorting.
     *
     * @param newComparator comparator for sorting DataSources within the Host.LOCALHOST
     */
    public void setChildrenComparator(Comparator<DataSource> newComparator) {
        super.setChildrenComparator(newComparator);
    }

    public Comparator<DataSource> getChildrenComparator() {
        return super.getChildrenComparator();
    }

    public boolean providesProperties() {
        return true;
    }

}
