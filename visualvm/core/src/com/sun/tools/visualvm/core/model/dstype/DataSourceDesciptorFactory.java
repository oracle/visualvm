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

package com.sun.tools.visualvm.core.model.dstype;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.model.ModelFactory;
import com.sun.tools.visualvm.core.model.ModelProvider;

/**
 *
 * @author Tomas Hurka
 */
public class DataSourceDesciptorFactory extends ModelFactory<DataSourceDescriptor,DataSource> implements ModelProvider<DataSourceDescriptor,DataSource> {

  private static DataSourceDesciptorFactory dsDescFactory;

  private DataSourceDesciptorFactory() {
  }

  public static synchronized DataSourceDesciptorFactory getDefault() {
    if (dsDescFactory == null) {
      dsDescFactory = new DataSourceDesciptorFactory();
      dsDescFactory.registerFactory(dsDescFactory);
      dsDescFactory.registerFactory(new ApplicationDescriptorFactory());
    }
    return dsDescFactory;
  }
  
  public static DataSourceDescriptor getDataSourceDescriptorFor(DataSource ds) {
    return getDefault().getModel(ds);
  }
  
  public DataSourceDescriptor createModelFor(DataSource ds) {
    return new DefaultDataSourceDescriptor(ds);
  }
}
