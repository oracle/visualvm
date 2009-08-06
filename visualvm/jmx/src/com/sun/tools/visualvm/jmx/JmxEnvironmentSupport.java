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

import com.sun.tools.visualvm.jmx.impl.JmxEnvironmentSupportImpl;

/**
 * Support for customizing JMX environment map in VisualVM. An entrypoint for
 * registering custom EnvironmentProviderFactories and creating EnvironmentProvider
 * instances.
 *
 * @since VisualVM 1.2
 * @author Jiri Sedlacek
 */
public final class JmxEnvironmentSupport {

    private static JmxEnvironmentSupport INSTANCE;

    
    /**
     * Returns singleton instance of JmxEnvironmentSupport.
     * 
     * @return singleton instance of JmxEnvironmentSupport
     */
    public synchronized static JmxEnvironmentSupport getInstance() {
        if (INSTANCE == null) INSTANCE = new JmxEnvironmentSupport();
        return INSTANCE;
    }
    

    /**
     * Registers an EnvironmentProviderFactory using the provided name.
     * 
     * @param category EnvironmentProviderFactory to be registered
     */
    public void registerFactory(EnvironmentProviderFactory factory) {
        JmxEnvironmentSupportImpl.registerFactoryImpl(factory);
    }

    /**
     * Unregisters an EnvironmentProviderFactory registered using the provided name.
     * 
     * @param category EnvironmentProviderFactory to unregister
     */
    public void unregisterFactory(EnvironmentProviderFactory factory) {
        JmxEnvironmentSupportImpl.unregisterFactoryImpl(factory);
    }

    /**
     * Creates new instance of EnvironmentProvider of the provided type using the
     * provided context.
     *
     * @param <T> type of the EnvironmentProvider to be created
     * @param cls class of the EnvironmentProvider to be created
     * @param ctx context for the EnvironmentProvider to be created
     * @return new instance of EnvironmentProvider
     */
    public <T extends EnvironmentProvider> T createProvider(Class<T> type,
                                             EnvironmentProvider.Context ctx) {
        return (T)JmxEnvironmentSupportImpl.createProviderImpl(type.getName(), ctx);
    }

    /**
     * Creates new instance of the CredentialsProvider which customizes the JMX
     * environment by providing defined username and password. If the saveCredentials
     * flag is set the credentials are stored in the Application's storage and
     * eventually persisted between VisualVM sessions, otherwise the credentials
     * are only stored in the CredentialsProvider instance and cleared after
     * finishing the current VisualVM session.
     *
     * @param username username for the JMX connection
     * @param password password for the JMX connection
     * @param saveCredentials flag to control persistence of the credentials
     * @return new instance of the CredentialsProvider
     */
    public EnvironmentProvider createCredentialsProvider(String username, char[] password,
                                                         boolean saveCredentials) {
        CredentialsProvider.Context ctx =
                new CredentialsProvider.Context(username, password,
                                                saveCredentials);
        return createProvider(CredentialsProvider.class, ctx);
    }
    
    
    private JmxEnvironmentSupport() {}

}
