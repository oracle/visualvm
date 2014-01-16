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

package com.sun.tools.visualvm.tools.sa;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.model.ModelFactory;

/**
 * The SaModelFactory class is a factory class for getting the
 * {@link SaModel} representation for the {@link DataSource}.
 * 
 * @author Tomas Hurka
 */
public final class SaModelFactory extends ModelFactory<SaModel, DataSource> {

    private static SaModelFactory saAgentFactory;

    private SaModelFactory() {
    }

    /**
     * Getter for the default version of the SaModelFactory.
     * @return instance of {@link SaModelFactory}.
     */
    public static synchronized SaModelFactory getDefault() {
        if (saAgentFactory == null) {
            saAgentFactory = new SaModelFactory();
        }
        return saAgentFactory;
    }
    
    /**
     * Factory method for obtaining {@link SaModel} for {@link DataSource}.
     * Note that there is only one instance of {@link SaModel} for a concrete
     * data source. This {@link SaModel} instance is cached. This method can 
     * return <CODE>null</CODE> if there is no AttachModel available
     * @param app application
     * @return {@link SaModel} instance or <CODE>null</CODE> if there is no
     * {@link SaModel}
     */
    public static SaModel getSAAgentFor(DataSource app) {
        return getDefault().getModel(app);
    }
    
}
