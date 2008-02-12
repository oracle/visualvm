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

import com.sun.tools.visualvm.core.datasource.Application;
import java.awt.Image;
import org.openide.util.Utilities;


/**
 *
 * @author Tomas Hurka
 */
public class VisualVMApplicationType extends ApplicationType {
  Application application;
  String name;
  boolean itself;

  VisualVMApplicationType(Application app) {
    application = app;
    itself = Application.CURRENT_APPLICATION.equals(application);
  }

  public String getName() {
    if (name == null) {
      String applicationName = "VisualVM";
      if (itself) {
        name = applicationName;
      } else {
        name = applicationName + " (pid " + application.getPid() + ")";
      }
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
