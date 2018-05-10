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

package org.graalvm.visualvm.host.model;

import org.graalvm.visualvm.core.model.ModelFactory;
import org.graalvm.visualvm.core.model.ModelProvider;
import org.graalvm.visualvm.host.Host;

/**
 * The HostOverviewFactory class is a factory class for getting the
 * {@link HostOverview} representation for the {@link Host}.
 *
 * @author Tomas Hurka
 */
public final class HostOverviewFactory extends ModelFactory<HostOverview,Host> implements ModelProvider<HostOverview,Host> {
    
    private static HostOverviewFactory overviewFactory;
    
    private HostOverviewFactory() {
    }
    
    /**
     * Getter for the default version of the HostOverviewFactory.
     * @return instance of {@link HostOverviewFactory}.
     */
    public static synchronized HostOverviewFactory getDefault() {
        if (overviewFactory == null) {
            overviewFactory = new HostOverviewFactory();
            overviewFactory.registerProvider(overviewFactory);
        }
        return overviewFactory;
    }
    
    /**
     * Factory method for obtaining {@link HostOverview} for {@link Host}. Note that there
     * is only one instance of {@link HostOverview} for a concrete application. This {@link HostOverview}
     * instance is cached. This method can return <CODE>null</CODE> if there is no HostOverview
     * available
     * @param host host
     * @return {@link HostOverview} instance or <CODE>null</CODE> if there is no
     * {@link HostOverview}
     */
    public static HostOverview getSystemOverviewFor(Host host) {
        return getDefault().getModel(host);
    }
    
    /**
     * Default {@link ModelProvider} implementation, which creates 
     * HostOverview for localhost. If you want to extend HostOverviewFactory use 
     * {@link HostOverviewFactory#registerProvider(ModelProvider)} to register the new instances
     * of {@link ModelProvider} for the different types of {@link Host}.
     * @param host host
     * @return instance of {@link HostOverview} for localhost
     */
    public HostOverview createModelFor(Host host) {
        if (Host.LOCALHOST.equals(host)) {
            return new LocalHostOverview();
        }
        return null;
    }
}
