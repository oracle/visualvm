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

package com.sun.tools.visualvm.core.datasource;

import java.util.Set;

/**
 * General definition of a container managing and providing DataSource instances.
 *
 * @author Jiri Sedlacek
 */
public interface DataSourceContainer<X extends DataSource> extends DataSourceProvider<X> {

    /**
     * Adds new DataSource instance to the container.
     * 
     * @param added DataSource to be added.
     */
    public <Y extends X> void addDataSource(Y added);

    /**
     * Adds new DataSource instances to the container.
     * 
     * @param added set of DataSource instances to be added.
     */
    public <Y extends X> void addDataSources(Set<Y> added);

    /**
     * Removes a DataSource from the container.
     * 
     * @param removed DataSource to be removed.
     */
    public <Y extends X> void removeDataSource(Y removed);

    /**
     * Removes the DataSource instances from the container.
     * 
     * @param removed set of DataSource instances to be removed.
     */
    public <Y extends X> void removeDataSources(Set<Y> removed);

}
