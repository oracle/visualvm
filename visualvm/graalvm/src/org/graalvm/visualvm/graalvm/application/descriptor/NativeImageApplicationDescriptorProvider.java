/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.application.descriptor;

import java.util.Properties;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.model.AbstractModelProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public class NativeImageApplicationDescriptorProvider extends
        AbstractModelProvider<DataSourceDescriptor, DataSource> {

    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (ds instanceof Application) {
            Jvm jvm = JvmFactory.getJVMFor((Application)ds);
            if (jvm.isGetSystemPropertiesSupported()) {
                Properties prop = jvm.getSystemProperties();
                if ("Substrate VM".equals(prop.getProperty("java.vm.name"))) // NOI18N
                    return new NativeImageApplicationDescriptor((Application)ds);
            }
        }
        return null;
    }
    
    public int priority() {
        return 10;
    }
}
