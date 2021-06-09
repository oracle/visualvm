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

package org.graalvm.visualvm.tools.jvmstat;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.ModelFactory;

/**
 * The JvmJvmstatModelFactory class is a factory class for getting the
 * {@link JvmJvmstatModel} representation for the {@link Application}.
 * 
 * @author Tomas Hurka
 */
public final class JvmJvmstatModelFactory extends ModelFactory<JvmJvmstatModel, Application> {

    private static JvmJvmstatModelFactory jvmstatModelFactory;

    private JvmJvmstatModelFactory() {
    }

    /**
     * Getter for the default version of the JvmJvmstatModelFactory.
     * @return instance of {@link JvmJvmstatModelFactory}.
     */
    public static synchronized JvmJvmstatModelFactory getDefault() {
        if (jvmstatModelFactory == null) {
            jvmstatModelFactory = new JvmJvmstatModelFactory();
        }
        return jvmstatModelFactory;
    }
    
    /**
     * Factory method for obtaining {@link JvmJvmstatModel} for {@link Application}. Note that there
     * is only one instance of {@link JvmJvmstatModel} for a concrete application. This {@link JvmJvmstatModel}
     * instance is cached. This method can return <CODE>null</CODE> if there is no JvmJvmstatModel
     * available
     * @param app application
     * @return {@link JvmJvmstatModel} instance or <CODE>null</CODE> if there is no
     * {@link JvmJvmstatModel}
     */
    public static JvmJvmstatModel getJvmstatModelFor(Application app) {
        return getDefault().getModel(app);
    }
    
}
