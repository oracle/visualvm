/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;

/**
 * Factory which recognizes NetBeans IDE, NetBeans Platform based
 * application and VisualVM itself
 * @author Tomas Hurka
 */
public class NetBeansApplicationTypeFactory extends MainClassApplicationTypeFactory {
    
    private static final String NETBEANS_DIRS = "-Dnetbeans.dirs="; // NOI18N
    private static final String NB_PLATFORM_HOME = "-Dnetbeans.home="; // NOI18N
    private static final String BRANDING_ID = "--branding "; // NOI18N
    private static final String VISUALVM_ID = "visualvm"; // NOI18N
    private static final String MAIN_CLASS = "org.netbeans.Main"; // NOI18N
    private static final Pattern NBCLUSTER_PATTERN = Pattern.compile("nb[0-9]+\\.[0-9]+");    // NOI18N
    private static final String BUILD_CLUSTER = "cluster"; // NOI18N
    private static final String VISUALVM_BUILD_WIN_ID = "\\visualvm\\build\\cluster;"; // NOI18N
    static final String NB_CLUSTER = "nb";    // NOI18N
    static final String PRODUCT_VERSION_PROPERTY="netbeans.productversion";  // NOI18N
    
    private boolean isNetBeans(Jvm jvm, String mainClass) {
        if (MAIN_CLASS.equals(mainClass)) {
            return true;
        }
        if (mainClass == null || mainClass.isEmpty()) {    // there is no main class - detect new NB 7.0 windows launcher
            String args = jvm.getJvmArgs();
            if (args != null && args.contains(NB_PLATFORM_HOME)) {
                return true;
            }
        }
        return false;
    }
    
    protected Set<String> computeClusters(Jvm jvm) {
        String args = jvm.getJvmArgs();
        int clusterIndex = args != null ? args.indexOf(NETBEANS_DIRS) : -1;
        String pathSeparator = jvm.getJavaHome().contains("\\")?";":":";    // NOI18N
        String separator = pathSeparator.equals(":")?"/":"\\";      // NOI18N
        Set<String> clusters = new HashSet<>();
        
        if (clusterIndex > -1) {
            String clustersString=args.substring(clusterIndex);
            int endIndex = clustersString.indexOf(" -");  // NOI18N
            Scanner clusterScanner;
            if (endIndex == -1) {
                endIndex = clustersString.indexOf(" exit");  // NOI18N
            }
            if (endIndex > -1) {
                clustersString = clustersString.substring(0,endIndex);
            }
            clusterScanner = new Scanner(clustersString).useDelimiter(pathSeparator);
            while (clusterScanner.hasNext()) {
                String clusterPath = clusterScanner.next();
                int pathIndex = clusterPath.lastIndexOf(separator);
                if (pathIndex > -1) {
                    clusters.add(clusterPath.substring(pathIndex+1));
                }
            }
        }
        return Collections.unmodifiableSet(clusters);
    }
    
    protected String getBranding(Jvm jvm) {
        String args = jvm.getMainArgs();
        if (args != null) {
            int brandingOffset = args.indexOf(BRANDING_ID);
            
            if (brandingOffset > -1) {
                Scanner sc = new Scanner(args.substring(brandingOffset));
                sc.next(); // skip --branding
                if (sc.hasNext()) {
                    return sc.next();
                }
            }
        }
        return null;
    }
    
    /**
     * Detects NetBeans IDE, NetBeans Platform based
     * application and VisualVM itself. It returns
     * {@link VisualVMApplicationType} for VisualVM,
     * {@link NetBeansApplicationType} for NetBeans 4.0 and newer and
     * {@link NetBeans3xApplicationType} for NetBeans 3.x
     *
     * @return {@link ApplicationType} subclass or <code>null</code> if
     * this application is not NetBeans
     */
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        if (isNetBeans(jvm,mainClass)) {
            String branding = getBranding(jvm);
            if (VISUALVM_ID.equals(branding)) {
                return new VisualVMApplicationType(app);
            }
            Set<String> clusters = computeClusters(jvm);
            
            for (String cluster : clusters) {
                if (NBCLUSTER_PATTERN.matcher(cluster).matches()) {
                    return new NetBeansApplicationType(app,jvm,clusters);
                }
                if (NB_CLUSTER.equals(cluster)) {
                    return new NetBeansApplicationType(app,jvm,clusters);
                }
                if (VISUALVM_ID.equals(cluster)) {
                    return new VisualVMApplicationType(app);
                }
                if (BUILD_CLUSTER.equals(cluster)) {
                    // NetBeans platform application was executed 
                    // directly from IDE or from ant script.
                    // Check if it is VisualVM on Windows - on other platforms
                    // VisualVM is recognized via branding
                    if (jvm.getJvmArgs().contains(VISUALVM_BUILD_WIN_ID)) {
                        return new VisualVMApplicationType(app);
                    }
                }
            }
            if (clusters.isEmpty() && branding == null) {
                return new NetBeans3xApplicationType(app,jvm);
            }
            return new NetBeansBasedApplicationType(app,jvm,clusters,branding);
        }
        return null;
    }
    
}
