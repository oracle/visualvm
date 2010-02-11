/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer;

import com.sun.tools.visualvm.core.datasource.DataSource;

/**
 * Provider of TracerPackage for a DataSource type.
 *
 * @param <X> any DataSource type
 * @author Jiri Sedlacek
 */
public abstract class TracerPackageProvider<X extends DataSource> {

    private final Class<X> scope;


    /**
     * Creates new instance of TracerPackageProvider with defined scope.
     *
     * @param scope scope of the provider
     */
    public TracerPackageProvider(Class<X> scope) { this.scope = scope; }


    /**
     * Returns scope of the provider.
     *
     * @return scope of the provider
     */
    public final Class<X> getScope() { return scope; }
    

    /**
     * Returns TracerPackage for the provided DataSource.
     *
     * @return TracerPackage for the provided DataSource
     */
    public abstract TracerPackage<X> getPackage(X dataSource);

}
