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

package com.sun.tools.visualvm.modules.tracer.monitor;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerPackageProvider;
import com.sun.tools.visualvm.modules.tracer.TracerSupport;
import org.openide.modules.ModuleInstall;

/**
 *
 * @author Jiri Sedlacek
 */
class MonitorPackageProvider extends ModuleInstall {

    private Impl provider;


    public synchronized void restored() {
        if (provider == null) provider = new Impl();
        TracerSupport.getInstance().registerPackageProvider(provider);
    }

    public synchronized void uninstalled() {
        if (provider == null) return;
        TracerSupport.getInstance().unregisterPackageProvider(provider);
        provider = null;
    }


    private static class Impl extends TracerPackageProvider<Application> {

        Impl() { super(Application.class); }

        public TracerPackage<Application>[] getPackages(Application application) {
            return new MonitorPackage[] { new MonitorPackage(application) };
        }

    }

}
