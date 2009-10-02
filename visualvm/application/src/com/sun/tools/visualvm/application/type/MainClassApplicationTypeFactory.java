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

package com.sun.tools.visualvm.application.type;

import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.application.Application;
import java.util.HashMap;
import java.util.Map;
import org.openide.util.NbBundle;

/**
 * This application type factory recognizes some well-known Java application
 * based on their main class name.
 * 
 * @author Tomas Hurka
 */
public class MainClassApplicationTypeFactory extends AbstractModelProvider<ApplicationType,Application> {
    private static final int CLASS_NAME = 0;
    private static final int NAME = 1;
    private static final int DESCRIPTION = 2;
    private static final int ICON_PATH = 3;

    private String[][] appmatrix = {
        // build tools
        {"org.apache.tools.ant.launch.Launcher","Ant",descr("DESCR_Ant"),"com/sun/tools/visualvm/application/resources/application.png"},  // NOI18N

        // Application servers
        {"com.sun.enterprise.server.PELaunch","GlassFish",descr("DESCR_GlassFish"),"com/sun/tools/visualvm/application/type/resources/GlassFish.png"},   // NOI18N
        {"com.sun.enterprise.glassfish.bootstrap.ASMain","GlassFish",descr("DESCR_GlassFish"),"com/sun/tools/visualvm/application/type/resources/GlassFish.png"},   // NOI18N
        {"com.sun.enterprise.ee.nodeagent.NodeAgentMain", "GlassFish Node", "GlassFish Node", "com/sun/tools/visualvm/application/type/resources/GlassFish.png"}, // NOI18N
        {"org.apache.catalina.startup.Bootstrap","Tomcat",descr("DESCR_Tomcat"),"com/sun/tools/visualvm/application/type/resources/Tomcat.png"},  // NOI18N
        {"org.jboss.Main","JBoss",descr("DESCR_JBoss"),"com/sun/tools/visualvm/application/resources/application.png"},  // NOI18N
        
        // JDK tools
        {"sun.tools.jconsole.JConsole","JConsole",descr("DESCR_JConsole"),"com/sun/tools/visualvm/application/resources/application.png"},  // NOI18N
        {"sun.tools.jps.Jps","Jps",descr("DESCR_Jps"),"com/sun/tools/visualvm/application/resources/application.png"}, // NOI18N
        {"sun.tools.jstat.Jstat","Jstat",descr("DESCR_Jstat"),"com/sun/tools/visualvm/application/resources/application.png"},   // NOI18N
        {"sun.tools.jstatd.Jstatd","Jstatd",descr("DESCR_Jstatd"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"sun.jvm.hotspot.tools.JStack","JStack",descr("DESCR_Jstack"),"com/sun/tools/visualvm/application/resources/application.png"},   // NOI18N
        {"sun.tools.jstack.JStack","JStack",descr("DESCR_Jstack"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"sun.jvm.hotspot.tools.JMap","JMap",descr("DESCR_Jmap"),"com/sun/tools/visualvm/application/resources/application.png"},   // NOI18N
        {"sun.tools.jmap.JMap","JMap",descr("DESCR_Jmap"),"com/sun/tools/visualvm/application/resources/application.png"},  // NOI18N
        {"com.sun.tools.hat.Main","JHat",descr("DESCR_Jhat"),"com/sun/tools/visualvm/application/resources/application.png"},   // NOI18N
        {"sun.tools.jinfo.JInfo","JInfo",descr("DESCR_Jinfo"),"com/sun/tools/visualvm/application/resources/application.png"},   // NOI18N
        {"sun.jvm.hotspot.jdi.SADebugServer","jsadebugd",descr("DESCR_Jsadebugd"),"com/sun/tools/visualvm/application/resources/application.png"},   // NOI18N
        
        // JDK utilitites
        {"sun.tools.jar.Main","Jar",descr("DESCR_Jar"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"com.sun.java.util.jar.pack.Driver","pack200",descr("DESCR_Pack200"),"com/sun/tools/visualvm/application/resources/application.png"}, // NOI18N
        {"com.sun.tools.javadoc.Main","JavaDoc",descr("DESCR_JavaDoc"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"com.sun.tools.javac.Main","Javac",descr("DESCR_Javac"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"com.sun.tools.javah.Main","Javah",descr("DESCR_Javah"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"sun.tools.javap.Main","Javap",descr("DESCR_Javap"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"sun.security.tools.JarSigner","JarSigner",descr("DESCR_JarSigner"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"com.sun.tools.apt.Main","APT",descr("DESCR_Apt"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"sun.applet.Main","Applet Viewer",descr("DESCR_AppletViewer"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"sun.applet.AppletViewer","Applet Viewer",descr("DESCR_AppletViewer"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        
        // Best known JDK demos
        {"FileChooserDemo","FileChooserDemo",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"Font2DTest","Font2DTest",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"java2d.Java2Demo","Java2Demo",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"Metalworks","Metalworks",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"Notepad","Notepad",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"SampleTree","SampleTree",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"Stylepad","Stylepad",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"SwingSet2","SwingSet2",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        {"TableExample","TableExample",descr("DESCR_JdkDemoApp"),"com/sun/tools/visualvm/application/resources/application.png"},    // NOI18N
        
        // Java DB
        {"org.apache.derby.drda.NetworkServerControl", "JavaDB", descr("DESCR_JavaDb"), "com/sun/tools/visualvm/application/type/resources/JavaDB.png"},   // NOI18N
    };
    
    Map<String,String[]> map;
    
    private static String descr(String key) {
        return NbBundle.getMessage(MainClassApplicationTypeFactory.class, key); // NOI18N
    }
    
    protected MainClassApplicationTypeFactory() {
        map = new HashMap();
        for (int i = 0; i < appmatrix.length; i++) {
            String[] appDesc = appmatrix[i];
            
            map.put(appDesc[CLASS_NAME],appDesc);
        }
    }
    
    /**
     * Detects well-known application.
     * @param appl Application
     * @return {@link MainClassApplicationType} instance or <code>null</code>
     * if application is not well-known application
     */
    public ApplicationType createModelFor(Application appl) {
        Jvm jvm = JvmFactory.getJVMFor(appl);
            
        if (jvm.isBasicInfoSupported()) {
            String mainClass = jvm.getMainClass();
            if (mainClass != null) {
                return createApplicationTypeFor(appl,jvm,mainClass);
            }
        }
        return null;
    }
    
    /**
     * Creates ApplicationType for application, jvm and mainClass. Can overriden 
     * by sublasses, which relies on mainClass name
     * @param app Application
     * @param jvm Applications's jvm
     * @param mainClass Application's mainClass
     * @return instance of {@link ApplicationType} or 
     * <CODE>null</CODE> if the app cannot be recognized 
     * by this ApplicationType factory
     */ 
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        String[] appDesc = map.get(mainClass);
        if (appDesc != null) {
            return new MainClassApplicationType(app,appDesc[NAME],appDesc[DESCRIPTION],appDesc[ICON_PATH]);
        }
        return null;
    }
}
