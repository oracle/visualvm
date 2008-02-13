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

package com.sun.tools.visualvm.core.model.dsdescr;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jiri Sedlacek
 */
// TODO: rewrite according to current core.model* approaches
public final class DataSourceDescriptorFactory {
    
    private static final Map<DataSource, DataSourceDescriptor> mapping = new HashMap();
    
    
    public static synchronized DataSourceDescriptor getDescriptor(DataSource dataSource) {
        DataSourceDescriptor descriptor = mapping.get(dataSource);
        if (descriptor == null) {
            if (dataSource instanceof Application) descriptor = new ApplicationDescriptor((Application)dataSource);
            else descriptor = new AbstractDataSourceDescriptor(dataSource) {};
            mapping.put(dataSource, descriptor);
        }
        return descriptor;
    }

}
