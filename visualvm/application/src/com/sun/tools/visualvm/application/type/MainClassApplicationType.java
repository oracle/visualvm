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

import com.sun.tools.visualvm.application.Application;
import java.awt.Image;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;


/**
 * ApplicationType which has custom name and icon.
 * Instance of this class is constructed by {@link MainClassApplicationTypeFactory}
 * @author Tomas Hurka
 */
public class MainClassApplicationType extends ApplicationType  {
  String name;
  String description;
  String iconPath;

  MainClassApplicationType(Application app, String n, String d, String path) {
    name = n;
    description = d;
    iconPath = path;
  }

  /**
   * {@inheritDoc}
   */ 
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   */
  public String getVersion() {
    return NbBundle.getMessage(MainClassApplicationType.class, "LBL_Unknown");  // NOI18N
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    return description != null ? description : ""; // NOI18N
  }

  /**
   * {@inheritDoc}
   */
  public Image getIcon() {
    return ImageUtilities.loadImage(iconPath, true);
  }
}
