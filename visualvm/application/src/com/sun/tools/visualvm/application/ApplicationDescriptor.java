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

package com.sun.tools.visualvm.application;

import com.sun.tools.visualvm.application.type.ApplicationType;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;

/**
 * DataSourceDescriptor for Application.
 *
 * @author Jiri Sedlacek
 */
 public class ApplicationDescriptor extends DataSourceDescriptor<Application> {

    protected ApplicationDescriptor(Application application) {
        this(application, ApplicationTypeFactory.getApplicationTypeFor(application));
    }

    private ApplicationDescriptor(Application application, ApplicationType type) {
        super(application, resolveName(application, type), type.getDescription(),
                type.getIcon(), POSITION_AT_THE_END, EXPAND_ON_FIRST_CHILD);
    }

    private static String resolveName(Application application, ApplicationType type) {
        // Check for persisted displayname (currently only for JmxApplications)
        String persistedName = application.getStorage().getCustomProperty(PROPERTY_NAME);
        if (persistedName != null) return persistedName;

        // Provide generic displayname
        int pid = application.getPid();
        String id = Application.CURRENT_APPLICATION.getPid() == pid ||
        pid == Application.UNKNOWN_PID ? "" : " (pid " + pid + ")"; // NOI18N
        return type.getName() + id;
    }
    
}
