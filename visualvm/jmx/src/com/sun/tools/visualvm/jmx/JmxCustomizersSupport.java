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

import com.sun.tools.visualvm.jmx.impl.JmxCustomizersSupportImpl;

/**
 * Support for custom JMX connection types.
 *
 * @since 1.2
 * @author Jiri Sedlacek
 */
public final class JmxCustomizersSupport {

    private static JmxCustomizersSupport INSTANCE;

    /**
     * Returns singleton instance of JmxCustomizersSupport.
     *
     * @return singleton instance of JmxCustomizersSupport
     */
    public synchronized static JmxCustomizersSupport getInstance() {
        if (INSTANCE == null) INSTANCE = new JmxCustomizersSupport();
        return INSTANCE;
    }


    /**
     * Registers new JmxConnectionCustomizer.
     *
     * @param customizer JmxConnectionCustomizer to be registered
     */
    public void registerCustomizer(JmxConnectionCustomizer customizer) {
        JmxCustomizersSupportImpl.registerCustomizer(customizer);
    }

    /**
     * Unregisters JmxConnectionCustomizer.
     *
     * @param customizer JmxConnectionCustomizer to unregister
     */
    public void unregisterCustomizer(JmxConnectionCustomizer customizer) {
        JmxCustomizersSupportImpl.unregisterCustomizer(customizer);
    }

}
