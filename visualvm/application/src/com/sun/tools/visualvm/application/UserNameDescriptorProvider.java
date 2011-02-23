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

package com.sun.tools.visualvm.application;

import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;

/**
 *
 * @author Tomas Hurka
 */
class UserNameDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor, DataSource> {
    private static final String DISPLAY_NAME_PROPERTY = "-Dvisualvm.display.name=";
    
    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (ds instanceof Application) {
            Application application = (Application) ds;
            
            Jvm jvm = JvmFactory.getJVMFor(application);
            
            if (jvm.isBasicInfoSupported()) {
                String args = jvm.getJvmArgs();
                int propIndex = args.indexOf(DISPLAY_NAME_PROPERTY);
                
                if (propIndex != -1) {  // display name propery detected on commandline
                    propIndex += DISPLAY_NAME_PROPERTY.length();
                    int endIndex = args.indexOf(" ",propIndex);
                    String userName;
                    
                    if (endIndex == -1) {
                        userName = args.substring(propIndex);
                    } else {
                        userName = args.substring(propIndex,endIndex);
                    }
                    return new UserNameDescriptor(application, userName);
                }
            }
        }
        return null;
    }
    
    public int priority() {
        return 3;
    }
}
