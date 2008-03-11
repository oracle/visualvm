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

import com.sun.tools.visualvm.jvm.JVM;
import com.sun.tools.visualvm.application.Application;
import java.awt.Image;
import java.util.Iterator;
import java.util.Set;
import org.openide.util.Utilities;


/**
 *
 * @author Tomas Hurka
 */
public class NetBeansApplicationType extends ApplicationType {
  Application application;
  String name;
  Set<String> clusters;
  private static final String NB_CLUSTER = "nb";    // NOI18N

  NetBeansApplicationType(Application app,JVM jvm,Set<String> cls) {
    application = app;
    clusters = cls;
  }

  public Set<String> getClusters() {
    return clusters;
  }

  public String getName() {
    return "NetBeans " + getVersion();    // NOI18N
  }

  public String getVersion() {
    Iterator<String> clIt = getClusters().iterator();
      while(clIt.hasNext()) {
        String cluster = clIt.next();
        if (cluster.startsWith(NB_CLUSTER)) {
          String ver = cluster.substring(NB_CLUSTER.length());
          
          if (ver.length()>0 && Character.isDigit(ver.charAt(0))) {
            return ver;
          }
        } 
      }
    return "<Unknown>";
  }

  public String getDescription() {
    return "";
  }

  public Image getIcon() {
    String iconPath = "com/sun/tools/visualvm/core/ui/resources/apps/NetBeans.png";
    return Utilities.loadImage(iconPath, true);
  }
}
