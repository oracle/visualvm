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

package com.sun.tools.visualvm.application.type;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.model.ModelFactory;
import com.sun.tools.visualvm.core.model.ModelProvider;

/**
 * The ApplicationTypeFactory class is a factory class for getting the
 * {@link ApplicationType} representation for the {@link Application}.
 *
 * @author Tomas Hurka
 * @author Luis-Miguel Alventosa
 */
public final class ApplicationTypeFactory extends ModelFactory<ApplicationType,Application> implements ModelProvider<ApplicationType,Application> {
    
    private static ApplicationTypeFactory appTypeFactory;
    
    private ApplicationTypeFactory() {
    }
    
    /**
     * Getter for the default version of the ApplicationTypeFactory.
     * @return instance of {@link ApplicationTypeFactory}.
     */
    public static synchronized ApplicationTypeFactory getDefault() {
        if (appTypeFactory == null) {
            appTypeFactory = new ApplicationTypeFactory();
            appTypeFactory.registerProvider(appTypeFactory);
            appTypeFactory.registerProvider(new MainClassApplicationTypeFactory());
            appTypeFactory.registerProvider(new NetBeansApplicationTypeFactory());
            appTypeFactory.registerProvider(new JavaPluginApplicationTypeFactory());
            appTypeFactory.registerProvider(new JavaWebStartApplicationTypeFactory());
            appTypeFactory.registerProvider(new JDeveloperApplicationTypeFactory());
            appTypeFactory.registerProvider(new MavenApplicationTypeFactory());
            appTypeFactory.registerProvider(new IntellijApplicationTypeFactory());
            appTypeFactory.registerProvider(new EclipseApplicationTypeFactory());
            appTypeFactory.registerProvider(new GraalVMApplicationTypeFactory());
        }
        return appTypeFactory;
    }
    
    /**
     * Factory method for obtaining {@link ApplicationType} for {@link Application}. Note that there
     * is only one instance of {@link ApplicationType} for a concrete application. This {@link ApplicationType}
     * instance is cached.
     * @param app application 
     * @return {@link ApplicationType} instance which describes application type.
     */
    public static ApplicationType getApplicationTypeFor(Application app) {
        return getDefault().getModel(app);
    }
    
    /**
     * Default {@link ApplicationType} implementation, which creates 
     * generic {@link ApplicationType} instances. If you want to extend ApplicationTypeFactory use 
     * {@link ApplicationTypeFactory#registerProvider(ModelProvider )} to register the new instances
     * of {@link ModelProvider} for the different types of {@link Application}.
     * @param app application
     * @return generic instance of {@link ApplicationType}
     */
    public ApplicationType createModelFor(Application app) {
        return new DefaultApplicationType(app);
    }
}
