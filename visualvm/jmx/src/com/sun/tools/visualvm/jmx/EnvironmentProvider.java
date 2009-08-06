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

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.Storage;
import java.util.Map;

/**
 * Provider for the JMX environment for each JMX connection. The provider instance
 * should preferably be created using JmxEnvironmentSupport (which accesses the
 * appropriate registered EnvironmentProviderFactory) and not directly by a public
 * constructor. This ensures that the EnvironmentProviderFactory will be correctly
 * registered to create provider instances for persistent JMX connections.
 *
 * @since VisualVM 1.2
 * @author Jiri Sedlacek
 */
public abstract class EnvironmentProvider {

    /**
     * Protected constructor to not allow creating new instances of the provider
     * directly. Preferably only the JmxEnvironmentSupport (which accesses the
     * appropriate registered EnvironmentProviderFactory) should create the
     * provider instances.
     */
    protected EnvironmentProvider() {}

    /**
     * Returns the JMX environment for the provided Application. Use
     * application.getStorage() to access the Storage where persistent data
     * were previously stored using the savePersistentData(Storage) method.
     *
     * @param application Application for which to create the JMX environment
     * @return JMX environment for the provided Application
     */
    public abstract Map<String, ?> getEnvironment(Application application);


    /**
     * Returns an unique identificator of the provided environment which helps
     * to differentiate JMX connections defined by the same connection string
     * but having different JMX environments. For example, the CredentialsProvider
     * returns the username if defined.
     *
     * Note: the provided Storage may be null which means that the provider
     * provides the environment for not persistent JMX connection.
     *
     * @param storage Storage or null if the JMX connection is not persistent
     * @return unique identificator of the provided environment
     */
    public String getEnvironmentId(Storage storage) { return ""; } // NOI18N
    /**
     * Gives the EnvironmentProvider a possibility to save it's settings into
     * a Storage. This method is called by the framework as soon as the Storage
     * is available. The provider may save the data to the provided Storage or
     * it can use it's own approach to persist the data.
     *
     * @param storage Storage to store the provider's settings
     */
    public void savePersistentData(Storage storage) {}

    /**
     * Gives the EnvironmentProvider a possibility to load saved settings from
     * a Storage. This method is called by the framework as soon as the Storage
     * of a restored persistent JMX connection is available. The provider may
     * load the data from the provided Storage or it can use it's own approach
     * to retrieve the persisted data.
     *
     * Note: typically this method is not needed as the provider can access the
     * Storage directly in the getEnvironment(Application) method using
     * application.getStorage().
     *
     * @param storage Storage containing the provider's settings
     */
    public void loadPersistentData(Storage storage) {}


    /**
     * A context required to create the EnvironmentProvider. It's provided to
     * the JmxEnvironmentSupport to create an instance of the EnvironmentProvider.
     */
    public static abstract class Context {}

}
