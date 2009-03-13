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

import com.sun.tools.visualvm.jmx.JmxApplicationException;
import com.sun.tools.visualvm.jmx.JmxEnvironmentFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.management.remote.JMXConnector;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * A factory that is able to create a JMX environment for password-based
 * authentication of jmx connection.
 *
 * The produced enviroment will contain an array of Strings holding username
 * and password needed to authenticate a jmx client.
 *
 * @author mb199851
 */
public class PasswordAuthJmxEnvironmentFactory implements JmxEnvironmentFactory {

    /**
     * Singleton instance.
     */
    private static JmxEnvironmentFactory INSTANCE = new PasswordAuthJmxEnvironmentFactory();

    /**
     * A constructor.
     */
    private PasswordAuthJmxEnvironmentFactory() {
    }

    /**
     * Gets a shared factory instance (singleton).
     *
     * @return an instance of PasswordAuthJmxEnvironmentFactory
     */
    public static JmxEnvironmentFactory getSharedInstance() {
        return INSTANCE;
    }

    /**
     * Creates a jmx environment for password-based authentication of
     * jmx connection.
     *
     * The produced enviroment will hold information about username and
     * password:
     * <code>
     * Map<String, Object> env = new HashMap<String, Object> { JMXConnector.CREDENTIALS,
     *                     new String[]{name, new String(password)}};
     * </code>
     *
     * @param ch an instance of callbackhandler able to retrieve username and password
     * @return a map representing jmx environment
     * @throws com.sun.tools.visualvm.jmx.JmxApplicationException if callbackhandler
     * is null or is not able to retrieve information about user and password
     */
    public Map<String, Object> createJmxEnvironment(CallbackHandler ch) throws JmxApplicationException {
        if (ch == null) {
            throw new JmxApplicationException("CallbackHandler must not be null");
        }

        String defaultUser = System.getProperty("user.name");
        String userPrompt = "username: "; // todo I18N
        NameCallback nameCallback = new NameCallback(userPrompt, defaultUser);

        String pwPrompt = "password: "; // todo I18N
        PasswordCallback pwCallback = new PasswordCallback(pwPrompt, false);

        Callback[] cb = new Callback[]{nameCallback, pwCallback};
        try {
            ch.handle(cb);
        } catch (IOException ex) {
            throw new JmxApplicationException("message here", ex);
        } catch (UnsupportedCallbackException ex) {
            throw new JmxApplicationException("message here", ex);
        }
        String name = nameCallback.getName();
        if (name == null || name.length() == 0) {
            name = nameCallback.getName();
        }
        char[] password = pwCallback.getPassword();
        Map<String, Object> environment = new HashMap<String, Object>(1);
        if (name != null && name.length() != 0 && password != null) {
            environment.put(JMXConnector.CREDENTIALS,
                    new String[]{name, new String(password)});
        }
        return environment;
    }
}
