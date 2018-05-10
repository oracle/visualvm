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

import org.graalvm.visualvm.jmx.impl.JmxConnectionSupportImpl;

/**
 * Support for customizing JMX connections in VisualVM. An entrypoint for
 * registering custom EnvironmentProviders and JmxConectionCustomizers.
 *
 * @since VisualVM 1.2
 * @author Jiri Sedlacek
 */
public final class JmxConnectionSupport {

    private static JmxConnectionSupport INSTANCE;

    
    /**
     * Returns singleton instance of JmxConnectionSupport.
     * 
     * @return singleton instance of JmxConnectionSupport
     */
    public synchronized static JmxConnectionSupport getInstance() {
        if (INSTANCE == null) INSTANCE = new JmxConnectionSupport();
        return INSTANCE;
    }
    

    /**
     * Registers new EnvironmentProvider.
     * 
     * @param provider EnvironmentProvider to be registered
     */
    public void registerProvider(EnvironmentProvider provider) {
        JmxConnectionSupportImpl.registerProviderImpl(provider);
    }

    /**
     * Unregisters the EnvironmentProvider.
     * 
     * @param provider EnvironmentProvider to unregister
     */
    public void unregisterProvider(EnvironmentProvider provider) {
        JmxConnectionSupportImpl.unregisterProviderImpl(provider);
    }


    /**
     * Registers new JmxConnectionCustomizer.
     *
     * @param customizer JmxConnectionCustomizer to be registered
     */
    public void registerCustomizer(JmxConnectionCustomizer customizer) {
        JmxConnectionSupportImpl.registerCustomizer(customizer);
    }

    /**
     * Unregisters the JmxConnectionCustomizer.
     *
     * @param customizer JmxConnectionCustomizer to unregister
     */
    public void unregisterCustomizer(JmxConnectionCustomizer customizer) {
        JmxConnectionSupportImpl.unregisterCustomizer(customizer);
    }
    
    
    private JmxConnectionSupport() {}

}
