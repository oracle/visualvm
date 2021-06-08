/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package net.java.visualvm.modules.glassfish.datasource;

import com.sun.appserv.management.monitor.ServletMonitor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishServlet extends GlassFishDataSource {
    private static final Image NODE_ICON = ImageUtilities.loadImage("net/java/visualvm/modules/glassfish/resources/servlet_icon.png",
                                                                   true);
    private ServletMonitor monitor;
    private String name;
    
    private final DataSourceDescriptor descriptor = new DataSourceDescriptor(this) {

        @Override
        public Image getIcon() {
            return NODE_ICON;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return null;
        }

//        @Override
//        public int getAutoExpansionPolicy() {
//            return DataSourceDescriptor.EXPAND_NEVER;
//        }
        
    };
    
    public GlassFishServlet(String name, GlassFishWebModule master, ServletMonitor monitor) {
        super(master);
        this.monitor = monitor;
        this.name = name;
    }

    public ServletMonitor getMonitor() {
        return monitor;
    }

    @Override
    public DataSourceDescriptor getDescriptor() {
        return descriptor;
    }

    public String getName() {
        return name;
    }
}
