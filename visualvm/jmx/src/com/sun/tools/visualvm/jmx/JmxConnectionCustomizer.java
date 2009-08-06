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

import com.sun.tools.visualvm.core.datasupport.Positionable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
 * @since VisualVM 1.2
 * @author Jiri Sedlacek
 */
public abstract class JmxConnectionCustomizer implements Positionable {
    
    private final String customizerName;
    private final String customizerDescription;
    private final int preferredPosition;
    private final boolean hidesDefault;


    /**
     * Creates new instance of the JmxConnectionCustomizer. Typically only one
     * instance of the customizer is needed, use
     * JmxConnectionSupport.registerCustomizer to register the instance.
     *
     * @param customizerName name of the customizer to be displayed in the UI
     * @param customizerName optional description of the customizer, may be null
     * @param preferredPosition preferred position of this customizer in UI
     * @param hidesDefault true if the default connection type should be hidden by this customizer, false otherwise
     */
    public JmxConnectionCustomizer(String customizerName, String customizerDescription,
                                   int preferredPosition, boolean hidesDefault) {
        if (customizerName == null)
            throw new IllegalArgumentException("customizerName cannot be null");
        
        this.customizerName = customizerName;
        this.customizerDescription = customizerDescription;
        this.preferredPosition = preferredPosition;
        this.hidesDefault = hidesDefault;
    }

    /**
     * Returns an UI component used as a customizer for the new JMX connection.
     * A new Panel instance should be created for each method invocation.
     * Providing some default values and/or hints is always a good idea!
     *
     * @return UI component used as a customizer for the new JMX connection
     */
    public abstract Panel createPanel();

    /**
     * Returns the Setup defining the JMX connection to be created.
     * 
     * @param customizerPanel Panel with the user-defined settings
     * @return Setup defining the JMX connection to be created
     */
    public abstract Setup getConnectionSetup(Panel customizerPanel);


    /**
     * Returns name of the customizer.
     *
     * @return name of the customizer
     */
    public final String getCustomizerName() { return customizerName; }

    /**
     * Returns optional description of the customizer, may be null.
     *
     * @return description of the customizer or null
     */
    public final String getCustomizerDescription() { return customizerDescription; }

    /**
     * Returns preferred position of the customizer in UI.
     *
     * @return preferred position of the customizer in UI
     */
    public final int getPreferredPosition() { return preferredPosition; }

    /**
     * Returns true if the default connection type should be hidden by this customizer, false otherwise.
     *
     * @return true if the default connection type should be hidden by this customizer, false otherwise
     */
    public final boolean hidesDefault() { return hidesDefault; }

    public final String toString() { return getCustomizerName(); }


    /**
     * UI component presented to the user to set up the JMX connection. Provides
     * a validity notification support to correctly handle current state of the
     * user-provided data - valid or invalid.
     */
    public static class Panel extends JPanel {

        private boolean settingsValid = true;
        private List<ChangeListener> listeners = new ArrayList();


        /**
         * Returns true if settings defined by this Panel are valid.
         * To be called in EDT.
         *
         * @return true if settings defined by this Panel are valid
         */
        public final boolean settingsValid() {
            return settingsValid;
        }

        /**
         * Add a ChangeListener. Use settingsValid() method to read the state.
         * To be called in EDT.
         *
         * @param listener ChangeListener
         */
        public final void addChangeListener(ChangeListener listener) {
            if (!listeners.contains(listener)) listeners.add(listener);
        }

        /**
         * Remove a ChangeListener. To be called in EDT.
         * @param listener ChangeListener
         */
        public final void removeChangeListener(ChangeListener listener) {
            listeners.remove(listener);
        }


        /**
         * Notifies the Panel that validity of the user-provided data changed.
         * To be called in EDT.
         * 
         * @param valid
         */
        protected final void setSettingsValid(boolean valid) {
            if (settingsValid != valid) {
                settingsValid = valid;
                fireStateChanged();
            }
        }


        private void fireStateChanged() {
            for (ChangeListener listener : listeners)
                listener.stateChanged(new ChangeEvent(this));
        }

    }


    /**
     * Setup based on the user-provided settings in the Panel defining the JMX
     * connection to be created.
     */
    public static final class Setup {

        private final String connectionString;
        private final String displayName;
        private final EnvironmentProvider environmentProvider;
        private final boolean persistentConnection;


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
            if (connectionString == null)
                throw new IllegalArgumentException("connectionString cannot be null"); // NOI18N
            if (environmentProvider == null)
                throw new IllegalArgumentException("environmentProvider cannot be null"); // NOI18N

            this.connectionString = connectionString;
            this.displayName = displayName;
            this.environmentProvider = environmentProvider;
            this.persistentConnection = persistentConnection;
        }


        public String getConnectionString() { return connectionString; }
        public String getDisplayName() { return displayName; }
        public EnvironmentProvider getEnvironmentProvider() { return environmentProvider; }
        public boolean isConnectionPersistent() { return persistentConnection; }
        
    }

}
