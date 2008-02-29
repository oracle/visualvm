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

package com.sun.tools.visualvm.core.model.apptype;

import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import com.sun.tools.visualvm.core.datasource.Application;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tomas Hurka
 */
public class MainClassApplicationTypeFactory extends AbstractModelProvider<ApplicationType,Application> {
    private static final int CLASS_NAME = 0;
    private static final int NAME = 1;
    private static final int ICON_PATH = 2;

    private String[][] appmatrix = {
        // build tools
        {"org.apache.tools.ant.launch.Launcher","Ant","com/sun/tools/visualvm/core/ui/resources/application.png"},

        // Application servers
        {"com.sun.enterprise.server.PELaunch","GlassFish","com/sun/tools/visualvm/core/ui/resources/apps/GlassFish.png"},
        {"com.sun.enterprise.ee.nodeagent.NodeAgentMain", "GlassFish Node", "com/sun/tools/visualvm/core/ui/resources/apps/GlassFishNode.png"},
        {"org.apache.catalina.startup.Bootstrap","Tomcat","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"org.jboss.Main","JBoss","com/sun/tools/visualvm/core/ui/resources/application.png"},
        
        // JDK tools
        {"sun.tools.jconsole.JConsole","JConsole","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.tools.jps.Jps","Jps","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.tools.jstat.Jstat","Jstat","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.tools.jstatd.Jstatd","Jstatd","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.jvm.hotspot.tools.JStack","JStack","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.tools.jstack.JStack","JStack","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.jvm.hotspot.tools.JMap","JMap","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.tools.jmap.JMap","JMap","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"com.sun.tools.hat.Main","JHat","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.tools.jinfo.JInfo","JInfo","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.jvm.hotspot.jdi.SADebugServer","jsadebugd","com/sun/tools/visualvm/core/ui/resources/application.png"},
        
        // JDK utilitites
        {"sun.tools.jar.Main","Jar","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"com.sun.java.util.jar.pack.Driver","pack200","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"com.sun.tools.javadoc.Main","JavaDoc","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"com.sun.tools.javac.Main","Javac","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"com.sun.tools.javah.Main","Javah","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.tools.javap.Main","Javap","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"sun.security.tools.JarSigner","JarSigner","com/sun/tools/visualvm/core/ui/resources/application.png"},
        {"com.sun.tools.apt.Main","APT","com/sun/tools/visualvm/core/ui/resources/application.png"},
        
        // Java DB
        {"org.apache.derby.drda.NetworkServerControl", "JavaDB", "com/sun/tools/visualvm/core/ui/resources/apps/JavaDB.png"},
    };
    
    Map<String,String[]> map;
    
    protected MainClassApplicationTypeFactory() {
        map = new HashMap();
        for (int i = 0; i < appmatrix.length; i++) {
            String[] appDesc = appmatrix[i];
            
            map.put(appDesc[CLASS_NAME],appDesc);
        }
    }
    
    public ApplicationType createModelFor(Application appl) {
        JVM jvm = JVMFactory.getJVMFor(appl);
            
        if (jvm.isBasicInfoSupported()) {
            String mainClass = jvm.getMainClass();
            if (mainClass != null) {
                return createApplicationTypeFor(appl,jvm,mainClass);
            }
        }
        return null;
    }
    
    public ApplicationType createApplicationTypeFor(Application app, JVM jvm, String mainClass) {
        String[] appDesc = map.get(mainClass);
        if (appDesc != null) {
            return new MainClassApplicationType(app,appDesc[NAME],appDesc[ICON_PATH]);
        }
        return null;
    }
}
