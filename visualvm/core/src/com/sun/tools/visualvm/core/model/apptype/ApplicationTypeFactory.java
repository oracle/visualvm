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

import com.sun.tools.visualvm.core.application.JvmstatApplication;
import com.sun.tools.visualvm.core.model.ModelFactory;
import com.sun.tools.visualvm.core.model.ModelProvider;
import com.sun.tools.visualvm.core.datasource.Application;

/**
 *
 * @author Tomas Hurka
 */
public class ApplicationTypeFactory extends ModelFactory<ApplicationType,Application> implements ModelProvider<ApplicationType,Application> {

  private static ApplicationTypeFactory appTypeFactory;

  private ApplicationTypeFactory() {
  }

  public static synchronized ApplicationTypeFactory getDefault() {
    if (appTypeFactory == null) {
      appTypeFactory = new ApplicationTypeFactory();
      appTypeFactory.registerFactory(appTypeFactory);
      appTypeFactory.registerFactory(new MainClassApplicationTypeFactory());
      appTypeFactory.registerFactory(new NetBeansApplicationTypeFactory());
      appTypeFactory.registerFactory(new JavaPluginApplicationTypeFactory());
      appTypeFactory.registerFactory(new JavaWebStartApplicationTypeFactory());
    }
    return appTypeFactory;
  }
  
  public static ApplicationType getApplicationTypeFor(Application app) {
    return getDefault().getModel(app);
  }
  
  public ApplicationType createModelFor(Application app) {
    if (app instanceof JvmstatApplication) {
        return new DefaultApplicationType((JvmstatApplication)app);
    }
    return null;
  }
}
