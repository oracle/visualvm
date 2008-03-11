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

import com.sun.tools.visualvm.application.jmx.JmxApplication;
import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.jvm.JVM;
import com.sun.tools.visualvm.jvm.JVMFactory;
import java.awt.Image;
import org.openide.util.Utilities;


/**
 *
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
public class DefaultApplicationType extends ApplicationType  {
  String name;
  Application application;

  DefaultApplicationType(Application app) {
    application = app;
  }

  public String getName() {
    if (name == null) {
      JVM jvm = JVMFactory.getJVMFor(application);
      String mainClassName = null;
      if (jvm.isBasicInfoSupported()) {
        mainClassName = jvm.getMainClass();
      }
      String applicationName;
      if (mainClassName != null && mainClassName.length() > 0) {
        applicationName = mainClassName;
      } else if (application instanceof JmxApplication) {
        applicationName = ((JmxApplication) application).getId();
      } else {
        applicationName = "<Unknown>";
      }
      name = applicationName;      
    }
    return name;
  }

  public String getVersion() {
    return "<Unknown>";
  }

  public String getDescription() {
    return "";
  }

  public Image getIcon() {
    String iconPath = "com/sun/tools/visualvm/core/ui/resources/application.png";
    return Utilities.loadImage(iconPath, true);
  }
}
