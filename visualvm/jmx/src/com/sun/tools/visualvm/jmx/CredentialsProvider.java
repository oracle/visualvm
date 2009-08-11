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
import com.sun.tools.visualvm.core.datasupport.Utils;
import java.util.HashMap;
import java.util.Map;
import javax.management.remote.JMXConnector;

/**
 * EnvironmentProvider adding the JMXConnector.CREDENTIALS property to the JMX
 * environment map. The provider can either store the username and password in
 * it's instance variables (won't be restored for another VisualVM session) or
 * can save it into a persistent Storage (will be restored when persistent JMX
 * connection is restored for another VisualVM session).
 *
 * Note that if the credentials provided by this provider are incorrect a dialog
 * requesting new credentials will be displayed by the framework. If the
 * user-provided credentials are correct they will override the credentials
 * defined by this provider. The user-provided credentials are never persisted.
 *
 * @since VisualVM 1.2
 * @author Jiri Sedlacek
 */
public abstract class CredentialsProvider extends EnvironmentProvider {

    private static final String PROPERTY_USERNAME = "prop_credentials_username"; // NOI18N
    private static final String PROPERTY_PASSWORD = "prop_credentials_password"; // NOI18N

    private static Persistent PERSISTENT_PROVIDER;


    static synchronized Persistent persistent() {
        if (PERSISTENT_PROVIDER == null) PERSISTENT_PROVIDER = new Persistent();
        return PERSISTENT_PROVIDER;
    }


    /**
     * Returns an unique String identifying the CredentialsProvider. Must be
     * overriden to return a different identificator when subclassing the
     * CredentialsProvider.
     *
     * @return unique String identifying the CredentialsProvider
     */
    public String getId() {
        return CredentialsProvider.class.getName();
    }


    public static class Custom extends CredentialsProvider {

        private final String username;
        private final char[] password;
        private final boolean persistent;


        public Custom(String username, char[] password, boolean persistent) {
            this.username = username;
            this.password = encodePassword(password);
            this.persistent = persistent;
        }


        public Map<String, ?> getEnvironment(Application application) {
            return createMap(username, password != null ? new String(password) : null);
        }

        public String getEnvironmentId(Storage storage) {
            if (username != null) return username;
            return super.getEnvironmentId(storage);
        }

        public void saveEnvironment(Storage storage) {
            if (!persistent) return;
            storage.setCustomProperty(PROPERTY_USERNAME, username);
            storage.setCustomProperty(PROPERTY_PASSWORD, new String(password));
        }

    }


    public static class Persistent extends CredentialsProvider {

        public Map<String, ?> getEnvironment(Application application) {
            Storage storage = application.getStorage();
            String username = storage.getCustomProperty(PROPERTY_USERNAME);
            String password = storage.getCustomProperty(PROPERTY_PASSWORD);
            return createMap(username, password);
        }

        public String getEnvironmentId(Storage storage) {
            if (storage != null) {
                String username = storage.getCustomProperty(PROPERTY_USERNAME);
                if (username != null) return username;
            }
            return super.getEnvironmentId(storage);
        }

    }


    // --- Private implementation ----------------------------------------------

    private static Map<String, ?> createMap(String username, String password) {
        Map map = new HashMap();

        if (username != null || password != null)
            map.put(JMXConnector.CREDENTIALS,
                    new String[] { username, decodePassword(password) });

        return map;
    }

    private static char[] encodePassword(char[] password) {
        if (password == null) return null;
        return Utils.encodePassword(new String(password)).toCharArray();
    }

    private static String decodePassword(String password) {
        if (password == null) return null;
        return Utils.decodePassword(password);
    }

}
