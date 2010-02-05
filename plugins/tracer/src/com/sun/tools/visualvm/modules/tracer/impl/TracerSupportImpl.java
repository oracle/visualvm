/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.impl;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerPackageProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TracerSupportImpl {

    private static TracerSupportImpl INSTANCE;

    private final Set<TracerPackageProvider> providers;


    public static synchronized TracerSupportImpl getInstance() {
        if (INSTANCE == null) INSTANCE = new TracerSupportImpl();
        return INSTANCE;
    }


    public synchronized void registerPackageProvider(TracerPackageProvider provider) {
        providers.add(provider);
    }

    public synchronized void unregisterPackageProvider(TracerPackageProvider provider) {
        providers.remove(provider);
    }


    public synchronized List<TracerPackage> getPackages(DataSource dataSource) {
        List<TracerPackage> packages = new ArrayList();
        for (TracerPackageProvider provider : providers)
            if (provider.getScope().isInstance(dataSource))
                packages.add(provider.getPackage(dataSource));
        Collections.sort(packages, Positionable.COMPARATOR);
        return packages;
    }


    private TracerSupportImpl() {
        providers = new HashSet();
    }

}
