/*
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


package net.java.visualvm.modules.glassfish;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.application.type.MainClassApplicationTypeFactory;



/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishApplicationTypeFactory extends MainClassApplicationTypeFactory {
    private final static GlassFishApplicationTypeFactory INSTANCE = new GlassFishApplicationTypeFactory();
    
    private GlassFishApplicationTypeFactory() {}
    
    public static void initialize() {
        ApplicationTypeFactory.getDefault().registerProvider(INSTANCE);
    }
    
    public static void shutdown() {
        ApplicationTypeFactory.getDefault().unregisterProvider(INSTANCE);
    }

    @Override
    public ApplicationType createModelFor(Application app) {
        Jvm jvm = JvmFactory.getJVMFor(app);
        if (!jvm.isBasicInfoSupported()) return null;
        if (jvm.getMainClass() != null) return super.createModelFor(app);
        if (jvm.isGetSystemPropertiesSupported() && !jvm.getJvmArgs().contains("felix.fileinstall.dir")) {
            if (jvm.getSystemProperties().get("com.sun.aas.instanceName") != null) {
                return new GlassFishInstanceType(app, jvm);
            }
        }
        return null;
    }
    
    @Override
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        if (!jvm.getJvmArgs().contains("felix.fileinstall.dir")) {
            if ("com.sun.enterprise.server.PELaunch".equals(mainClass)) {
                return new GlassFishInstanceType(app, jvm);
            } else if ("com.sun.enterprise.ee.nodeagent.NodeAgentMain".equals(mainClass)) {
                return new GlassFishNodeType(jvm, app.getPid());
            }
        }
        return null;
    }
}
