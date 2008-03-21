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

import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.Application;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author Tomas Hurka
 */
public class NetBeansApplicationTypeFactory extends MainClassApplicationTypeFactory {

  private static final String NETBEANS_DIRS = "-Dnetbeans.dirs="; // NOI18N
  private static final String BRANDING_ID = "--branding "; // NOI18N
  private static final String VISUALVM_ID = "visualvm";
  private static final String MAIN_CLASS = "org.netbeans.Main";
  private static final Pattern nbcluster_pattern = Pattern.compile("nb[0-9]+\\.[0-9]+");

  protected Set<String> computeClusters(Jvm jvm) {
    String args = jvm.getJvmArgs();
    int clusterIndex = args.indexOf(NETBEANS_DIRS);
    String pathSeparator = jvm.getJavaHome().contains("\\")?";":":";
    String separator = pathSeparator.equals(":")?"/":"\\";    
    Set<String> clusters = new HashSet();
    
    if (clusterIndex > -1) {
      String clustersString=args.substring(clusterIndex);
      int endIndex = clustersString.indexOf(" -");
      Scanner clusterScanner;
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

  public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
    if (MAIN_CLASS.equals(mainClass)) {
      String branding = getBranding(jvm);
      if (VISUALVM_ID.equals(branding)) {
        return new VisualVMApplicationType(app);
      }
      Set<String> clusters = computeClusters(jvm);
      Iterator<String> clIt = clusters.iterator();
      
      while(clIt.hasNext()) {
        String cluster = clIt.next();
        if (nbcluster_pattern.matcher(cluster).matches()) {
          return new NetBeansApplicationType(app,jvm,clusters);
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
