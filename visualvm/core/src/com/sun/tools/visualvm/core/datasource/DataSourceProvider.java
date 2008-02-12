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

import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import java.util.Set;

/**
 * General definition of a provider providing DataSource instances.
 *
 * @author Jiri Sedlacek
 */
public interface DataSourceProvider <X extends DataSource> {

    /**
     * Adds a listener for DataSoures managed by this provider.
     * 
     * @param listener DataChangeListener to be added,
     * @param scope scope of DataSources for which the listeners registers.
     */
    public <Y extends X> void addDataChangeListener(DataChangeListener<Y> listener, Class<Y> scope);

    /**
     * Removes a listener.
     * 
     * @param listener DataChangeListener to be removed.
     */
    public <Y extends X> void removeDataChangeListener(DataChangeListener<Y> listener);

    /**
     * Returns all DataSource instances managed by this provider.
     * 
     * @return all DataSource instances managed by this provider.
     */
    public Set<X> getDataSources();

    /**
     * Returns all DataSource instances of given scope managed by this provider.
     * 
     * @param scope scope of DataSource types to return.
     * @return all DataSource instances of given scope managed by this provider.
     */
    public <Y extends X> Set<Y> getDataSources(Class<Y> scope);

}