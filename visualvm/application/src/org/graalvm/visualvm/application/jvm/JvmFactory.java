/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.jvm;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.ModelFactory;
import org.graalvm.visualvm.core.model.ModelProvider;

/**
 * The JVMFactory class is a factory class for getting the
 * {@link Jvm} representation  for the {@link Application}.
 * 
 * @author Tomas Hurka
 */
public final class JvmFactory extends ModelFactory<Jvm,Application> implements ModelProvider<Jvm,Application> {

    private static JvmFactory jvmFactory;

    private JvmFactory() {
    }
    
    /**
     * Getter for the default version of the JvmFactory.
     * @return instance of {@link JvmFactory}.
     */
    public static synchronized JvmFactory getDefault() {
        if (jvmFactory == null) {
            jvmFactory = new JvmFactory();
            jvmFactory.registerProvider(jvmFactory);
        }
        return jvmFactory;
    }
    
    /**
     * Factory method for obtaining {@link Jvm} for {@link Application}. Note that there
     * is only one instance of {@link Jvm} for a concrete application. This {@link Jvm}
     * instance is cached.
     * @param app application 
     * @return {@link Jvm} instance which encapsulates application's JVM.
     */
    public static Jvm getJVMFor(Application app) {
        return getDefault().getModel(app);
    }
    
    /**
     * Default {@link ModelProvider} implementation, which creates 
     * dummy {@link Jvm} instances. If you want to extend JvmFactory use 
     * {@link JvmFactory#registerProvider(ModelProvider )} to register the new instances
     * of {@link ModelProvider} for the different types of {@link Application}.
     * @param app application
     * @return dummy instance of {@link Jvm}
     */
    public Jvm createModelFor(Application app) {
        return new DefaultJvm();
    }
}
