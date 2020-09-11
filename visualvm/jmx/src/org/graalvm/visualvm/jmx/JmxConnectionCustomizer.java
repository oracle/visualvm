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

package org.graalvm.visualvm.jmx;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.properties.PropertiesPanel;
import org.graalvm.visualvm.core.properties.PropertiesProvider;
import org.graalvm.visualvm.jmx.impl.JmxPropertiesProvider;

/**
 * Provider of a special JMX connection type. By registering the customizer
 * using JmxApplicationsSupport.registerConnectionCustomizer() a new connection
 * type is added to the Add JMX Connection dialog.
 * 
 * If the hidesDefault flag is set for the customizer the default JMX connection
 * type provided by VisualVM is not displayed. This is useful when the custom
 * connection type extends the default one by adding new settings and it's not
 * desired to present both types to the user.
 *
 * @author Jiri Sedlacek
 */
public abstract class JmxConnectionCustomizer extends PropertiesProvider<Application> {

    private final boolean hidesDefault;


    /**
     * Creates new instance of the JmxConnectionCustomizer. Typically only one
     * instance of the customizer is needed, use
     * JmxConnectionSupport.registerCustomizer to register the instance.
     *
     * @param customizerName name of the customizer to be displayed in the UI
     * @param customizerDescription optional description of the customizer, may be null
     * @param customizerPosition preferred position of this customizer in UI
     * @param hidesDefault true if the default connection type should be hidden by this customizer, false otherwise
     */
    public JmxConnectionCustomizer(String customizerName, String customizerDescription,
                                   int customizerPosition, boolean hidesDefault) {
        super(customizerName, customizerDescription,
              JmxPropertiesProvider.CATEGORY_JMX_CONNECTION, customizerPosition);
        if (customizerName == null)
            throw new IllegalArgumentException("customizerName cannot be null"); // NOI18N
        this.hidesDefault = hidesDefault;
    }

    /**
     * Returns an unique String identifying the JmxConnectionCustomizer. The return
     * value is used for persistency purposes and must be constant for customizers
     * customizing persistent JMX connections. Default implementation returns
     * this.getClass().getName().
     *
     * @return unique String identifying the JmxConnectionCustomizer
     */
    public String getId() { return getClass().getName(); }

    /**
     * Returns the Setup defining the JMX connection to be created.
     * 
     * @param customizerPanel PropertiesPanel with the user-defined settings
     * @return Setup defining the JMX connection to be created
     */
    public abstract Setup getConnectionSetup(PropertiesPanel customizerPanel);

    /**
     * Returns true if the JmxConnectionCustomizer works as a PropertiesProvider
     * for the provided Application, false otherwise.
     *
     * @param application Application for which to provide the properties (never null)
     * @return true if the JmxConnectionCustomizer works as a PropertiesProvider for the provided Application, false otherwise
     */
    public boolean providesProperties(Application application) { return true; }


    /**
     * Returns true if the default connection type should be hidden by this customizer, false otherwise.
     *
     * @return true if the default connection type should be hidden by this customizer, false otherwise
     */
    public final boolean hidesDefault() { return hidesDefault; }


    /**
     * Default implementation of the PropertiesProvider.supportsDataSource method,
     * cannot be further overridden. JmxConnectionCustomizer always supports providing
     * initial properties for a JMX application being created. Use the providesProperties
     * method to control whether to provide a properties category for an existing
     * application or not.
     * 
     * @param application Application for which to provide the properites
     * @return true for null Application, providesProperties(application) result otherwise
     */
    public final boolean supportsDataSource(Application application) {
        return application == null ? true : providesProperties(application);
    }

    public void propertiesDefined(PropertiesPanel panel, Application application) {};

    public void propertiesChanged(PropertiesPanel panel, Application application) {};

    public void propertiesCancelled(PropertiesPanel panel, Application application) {};


    public final String toString() { return getPropertiesName(); }


    /**
     * Setup based on the user-provided settings in the Panel defining the JMX
     * connection to be created.
     *
     * @author Jiri Sedlacek
     */
    public static final class Setup {

        private final String connectionString;
        private final String displayName;
        private final EnvironmentProvider environmentProvider;
        private final boolean persistentConnection;
        private final boolean allowsInsecureConnection;
        private final boolean connectImmediately;
        private final boolean connectAutomatically;


        /**
         * Creates new instance of Setup.
         *
         * @param connectionString connection string for the JMX connection
         * @param displayName display name of the JMX connection or null
         * @param environmentProvider EnvironmentProvider for the JMX connection
         * @param persistentConnection true if the connection should be persisted for another VisualVM sessions, false otherwise
         */
        public Setup(String connectionString, String displayName,
                     EnvironmentProvider environmentProvider,
                     boolean persistentConnection) {
            this(connectionString, displayName, environmentProvider,
                 persistentConnection, false);
        }
        
        /**
         * Creates new instance of Setup.
         *
         * @param connectionString connection string for the JMX connection
         * @param displayName display name of the JMX connection or null
         * @param environmentProvider EnvironmentProvider for the JMX connection
         * @param persistentConnection true if the connection should be persisted for another VisualVM sessions, false otherwise
         * @param allowsInsecureConnection true if SSL is not required for the connection, false otherwise
         */
        public Setup(String connectionString, String displayName,
                     EnvironmentProvider environmentProvider,
                     boolean persistentConnection, boolean allowsInsecureConnection) {
            this(connectionString, displayName, environmentProvider, persistentConnection, allowsInsecureConnection, true, true);
        }
        
        /**
         * Creates new instance of Setup.
         *
         * @param connectionString connection string for the JMX connection
         * @param displayName display name of the JMX connection or null
         * @param environmentProvider EnvironmentProvider for the JMX connection
         * @param persistentConnection true if the connection should be persisted for another VisualVM sessions, false otherwise
         * @param allowsInsecureConnection true if SSL is not required for the connection, false otherwise
         * @param connectImmediately true if the JMX connection should be attempted immediately after submitting, false otherwise
         * @param connectAutomatically true if the JMX connection should be made automatically whenever the target application is available, false otherwise
         */
        public Setup(String connectionString, String displayName,
                     EnvironmentProvider environmentProvider,
                     boolean persistentConnection, boolean allowsInsecureConnection,
                     boolean connectImmediately, boolean connectAutomatically) {
            if (connectionString == null)
                throw new IllegalArgumentException("connectionString cannot be null"); // NOI18N
            if (environmentProvider == null)
                throw new IllegalArgumentException("environmentProvider cannot be null"); // NOI18N

            this.connectionString = connectionString;
            this.displayName = displayName;
            this.environmentProvider = environmentProvider;
            this.persistentConnection = persistentConnection;
            this.allowsInsecureConnection = allowsInsecureConnection;
            this.connectImmediately = connectImmediately;
            this.connectAutomatically = connectAutomatically;
        }


        /**
         * Returns the JMX connection string defining the connection to be created.
         *
         * @return JMX connection string defining the connection to be created
         */
        public String getConnectionString() { return connectionString; }

        /**
         * Returns the display name of the JMX connection to be created.
         *
         * @return display name of the JMX connection to be created
         */
        public String getDisplayName() { return displayName; }

        /**
         * Returns the EnvironmentProvider for the JMX connection to be created or null.
         *
         * @return EnvironmentProvider for the JMX connection to be created or null
         */
        public EnvironmentProvider getEnvironmentProvider() { return environmentProvider; }

        /**
         * Returns true if the JMX connection to be created should be restored for another VisualVM sessions, false otherwise
         *
         * @return true if the JMX connection to be created should be restored for another VisualVM sessions, false otherwise
         */
        public boolean isConnectionPersistent() { return persistentConnection; }
        
        /**
         * Returns true if SSL is not required for the connection.
         *
         * @return true if SSL is not required for the connection, false otherwise
         */
        public boolean allowsInsecureConnection() { return allowsInsecureConnection; }
        
        /**
         * Returns true if the JMX connection should be attempted immediately after submitting.
         *
         * @return true if the JMX connection should be attempted immediately after submitting, false otherwise
         */
        public boolean isConnectImmediately() { return connectImmediately; }
        
        /**
         * Returns true if the JMX connection should be made automatically whenever the target application is available.
         *
         * @return true if the JMX connection should be made automatically whenever the target application is available, false otherwise
         */
        public boolean isConnectAutomatically() { return connectAutomatically; }
        
    }

}
