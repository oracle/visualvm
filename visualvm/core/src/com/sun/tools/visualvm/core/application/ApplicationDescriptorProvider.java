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

package com.sun.tools.visualvm.core.application;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.apptype.ApplicationType;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.model.dsdescr.*;
import java.awt.Image;

/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 * @author Luis-Miguel Alventosa
 */
class ApplicationDescriptorProvider extends
        AbstractModelProvider<DataSourceDescriptor, DataSource> {

    ApplicationDescriptorProvider() {
    }

    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (ds instanceof Application) {
            return new ApplicationDescriptor((Application) ds);
        }
        return null;
    }

    private static class ApplicationDescriptor
            extends DataSourceDescriptor<Application> {

        private final ApplicationType appType;
        private final int pid;

        ApplicationDescriptor(Application app) {
            super(app, null, null, null, POSITION_AT_THE_END, EXPAND_ON_FIRST_CHILD);
            appType = ApplicationTypeFactory.getApplicationTypeFor(app);
            pid = app.getPid();
        }

        @Override
        public String getName() {
            if (supportsRename() && super.getName() != null) {
                return super.getName();
            }
            String id = Application.CURRENT_APPLICATION.getPid() == pid ||
                    pid == Application.UNKNOWN_PID ? "" : " (pid " + pid + ")";
            return appType.getName() + id;
        }

        @Override
        public String getDescription() {
            return appType.getDescription();
        }

        @Override
        public Image getIcon() {
            return appType.getIcon();
        }

        @Override
        protected boolean supportsRename() {
            return (getDataSource() instanceof JmxApplication);
        }
    }
}
