/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.modules.tracer.dynamic.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerPackageProvider;
import com.sun.tools.visualvm.modules.tracer.TracerSupport;
import com.sun.tools.visualvm.modules.tracer.dynamic.spi.ApplicationValidator;
import java.util.HashSet;
import java.util.Set;
import javax.swing.ImageIcon;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jaroslav Bachorik
 */
class DynamicPackageProvider extends ModuleInstall {
    final private static ImageIcon defaultIcon = ImageUtilities.loadImageIcon("com/sun/tools/visualvm/modules/tracer/dynamic/resources/default.png", true); // NOI18N
    private Impl provider;


    @Override
    public synchronized void restored() {
        if (provider == null) provider = new Impl();
        TracerSupport.getInstance().registerPackageProvider(provider);
    }

    @Override
    public synchronized void uninstalled() {
        if (provider == null) return;
        TracerSupport.getInstance().unregisterPackageProvider(provider);
        provider = null;
    }


    private static class Impl extends TracerPackageProvider<Application> {

        Impl() { super(Application.class); }

        @Override
        public TracerPackage<Application>[] getPackages(Application application) {
            Set<TracerPackage<Application>> packages = new HashSet<TracerPackage<Application>>();
            FileObject probesRoot = FileUtil.getConfigFile("VisualVM/Tracer/packages"); // NOI18N
            
            int defaultCounter = 1000;
            for(FileObject pkg : probesRoot.getChildren()) {
                Object name = pkg.getAttribute("displayName"); // NOI18N
                Object desc = pkg.getAttribute("desc"); // NOI18N
                Object icon = pkg.getAttribute("icon"); // NOI18N
                Object position = pkg.getAttribute("position"); // NOI18N

                ApplicationValidator validator = (ApplicationValidator)pkg.getAttribute("validator"); // NOI18N
                packages.add(new DynamicPackage(pkg, (String)name, desc != null ? (String)desc : "", icon != null ? ImageUtilities.loadImageIcon((String)icon, true) : defaultIcon, position != null ? ((Integer)position).intValue() : defaultCounter++, isAvailable(validator, application)));
            }
            return (DynamicPackage[])packages.toArray(new DynamicPackage[packages.size()]);
        }

        private static boolean isAvailable(ApplicationValidator validator, Application application) {
            if (validator == null) return true;
            
            if (!application.isLocalApplication() ||
                 application.getState() != Stateful.STATE_AVAILABLE)
            return false;

            return validator.isPackageApplicable(application);
        }

    }

}
