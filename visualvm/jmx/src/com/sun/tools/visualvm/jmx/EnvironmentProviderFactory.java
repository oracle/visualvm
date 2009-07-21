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

package com.sun.tools.visualvm.jmx;

/**
 * Factory to create EnvironmentProvider instances. The factory itself isn't
 * meant to be publicly accessed, it needs to be registered using
 * JmxEnviromentSupport.registerFactory(EnvironmentProviderFactory). This ensures
 * that the EnvironmentProvider instances can be correctly created for persistent
 * JMX connections.
 *
 * @since 1.2
 * @author Jiri Sedlacek
 */
public abstract class EnvironmentProviderFactory<T extends EnvironmentProvider> {

    private final Class<T> providerClass;


    /**
     * Creates new instance of the factory. Only one instance of the factory
     * needs to be created that will be provided to the
     * JmxEnviromentSupport.registerFactory(EnvironmentProviderFactory) method.
     *
     * @param providerClass class of the EnvironmentProvider this factory creates
     */
    public EnvironmentProviderFactory(Class<T> providerClass) {
        this.providerClass = providerClass;
    }


    /**
     * Returns class of the EnvironmentProvider this factory creates.
     *
     * @return class of the EnvironmentProvider this factory creates
     */
    public final Class<T> getProviderClass() { return providerClass; }

    /**
     * Creates a new instance of EnvironmentProvider using the provided context.
     * The context may be null which means that a provider for loaded persistent
     * JMX connection is being created. The provider will have a possibility to
     * load it's settings later using the
     * EnvironmentProvider.loadPersistentData(Storage) method.
     *
     * @param context Context for the provider or null if not available
     * @return new instance of EnvironmentProvider
     */
    public abstract T createProvider(EnvironmentProvider.Context context);

}
