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

package org.graalvm.visualvm.application.type;

import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.Application;
import java.awt.Image;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import static org.graalvm.visualvm.application.type.NetBeansApplicationTypeFactory.NB_CLUSTER;
import static org.graalvm.visualvm.application.type.NetBeansApplicationTypeFactory.PRODUCT_VERSION_PROPERTY;

/**
 * This {@link ApplicationType} represents NetBeans application from version 4.0.
 * @author Tomas Hurka
 */
public class NetBeansApplicationType extends ApplicationType {
    private static final String BUILD_NUMBER_PROPERTY="netbeans.buildnumber";
    private static final String[] BUILD_NUMBERS={
                                    "201006101454","6.9",
                                    "201007282301","6.9.1",
                                    "201104080000","7.0",
                                    "201107282000","7.0.1"
                                };
    private static final String VERSION_REG="\\d{1,2}\\.\\d{1,2}(\\.\\d{1,2})?";
    Application application;
    String name;
    Set<String> clusters;
    Jvm jvm;
    
    NetBeansApplicationType(Application app,Jvm vm,Set<String> cls) {
        application = app;
        jvm = vm;
        clusters = cls;
    }
    
    /**
     * Returns set of BetBeans' clusters.
     *
     */
    public Set<String> getClusters() {
        return clusters;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "NetBeans IDE " + getVersion();    // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public String getVersion() {
        Iterator<String> clIt = getClusters().iterator();
        while(clIt.hasNext()) {
            String cluster = clIt.next();
            if (cluster.startsWith(NB_CLUSTER)) {
                String ver = cluster.substring(NB_CLUSTER.length());
                
                if (!ver.isEmpty() && Character.isDigit(ver.charAt(0))) {
                    return ver;
                }
            }
            if (cluster.equals(NB_CLUSTER)) {
                //6.9+ does not have version in nb cluster
                // try to use system properties
                if (jvm.isGetSystemPropertiesSupported()) {
                    String ver = getVersionFromSysProps(jvm.getSystemProperties());
                    if (ver != null) {
                        return ver;
                    }
                }
                return "6.9+";  // NOI18N
            }
        }
        return NbBundle.getMessage(NetBeansApplicationType.class, "LBL_Unknown");   // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        if (jvm.isGetSystemPropertiesSupported()) {
            Properties sysProps = jvm.getSystemProperties();
            
            if (sysProps != null) {
                String versionString = sysProps.getProperty(PRODUCT_VERSION_PROPERTY);

                if (versionString != null) {
                    return versionString;
                }
            }
        }
        return NbBundle.getMessage(NetBeansApplicationType.class, "DESCR_NetBeansApplicationType");   // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public Image getIcon() {
        String iconPath = "org/graalvm/visualvm/application/type/resources/NetBeans.png"; // NOI18N
        return ImageUtilities.loadImage(iconPath, true);
    }

    private String getVersionFromSysProps(Properties properties) {
        if (properties == null) return null;
        String versionString = properties.getProperty(PRODUCT_VERSION_PROPERTY);
        
        if (versionString != null) {
            Scanner s = new Scanner(versionString);
            if ("NetBeans".equals(s.next())) {  // NOI18N
                if ("IDE".equals(s.next())) {   // NOI18N
                    return s.next();
                }
            }
            String ver = s.findInLine(VERSION_REG);
            if (ver != null) {
                return ver;
            }
        } else {
            String buildNumber = properties.getProperty(BUILD_NUMBER_PROPERTY);
            for (int i=0; i<BUILD_NUMBERS.length; i+=2) {
                if (BUILD_NUMBERS[i].equals(buildNumber)) {
                    return BUILD_NUMBERS[i+1];
                }
            }
        }
        return null;
    }
}
