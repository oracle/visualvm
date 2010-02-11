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

package com.sun.tools.visualvm.modules.tracer.impl;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;
import org.openide.modules.ModuleInstall;

/**
 *
 * @author Jiri Sedlacek
 */
final class TracerViewProvider extends ModuleInstall {

    private Impl provider;


    public synchronized void restored() {
        if (provider == null) provider = new Impl();
        DataSourceViewsManager.sharedInstance().addViewProvider(
                provider, DataSource.class);
    }

    public synchronized void uninstalled() {
        if (provider == null) return;
        DataSourceViewsManager.sharedInstance().removeViewProvider(provider);
        provider = null;
    }


    private static final class Impl extends DataSourceViewProvider<DataSource> {

        protected boolean supportsViewFor(DataSource dataSource) {
            return TracerSupportImpl.getInstance().hasPackages(dataSource);
        }

        protected DataSourceView createView(DataSource dataSource) {
            TracerModel model = new TracerModel(dataSource);
            TracerController controller = new TracerController(model);
            return new TracerView(model, controller);
        }

    }

}
