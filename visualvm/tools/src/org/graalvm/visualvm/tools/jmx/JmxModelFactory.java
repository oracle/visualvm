/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.tools.jmx;


import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.ModelFactory;

/**
 * The {@code JmxModelFactory} class is a factory class for getting
 * the {@link JmxModel} representation for the {@link Application}.
 *
 * @author Luis-Miguel Alventosa
 */
public final class JmxModelFactory extends ModelFactory<JmxModel, Application> {

    private static JmxModelFactory factory;

    private JmxModelFactory() {
    }

    /**
     * Getter for the default version of the {@link JmxModelFactory}.
     * 
     * @return an instance of {@link JmxModelFactory}.
     */
    public static synchronized JmxModelFactory getDefault() {
        if (factory == null) {
            factory = new JmxModelFactory();
        }
        return factory;
    }

    /**
     * Factory method for obtaining the {@link JmxModel} for the given
     * {@link Application}. Note that there is only one instance of
     * {@link JmxModel} for application instance. This {@link JmxModel}
     * instance is cached.
     * 
     * @param app application.
     * 
     * @return a {@link JmxModel} instance which encapsulates the
     * application's JMX model.
     */
    public static JmxModel getJmxModelFor(Application app) {
        return getDefault().getModel(app);
    }

}
