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

package com.sun.tools.visualvm.tools.jmx;

import javax.management.MBeanServerConnection;

/**
 * Cached MBeanServerConnection:
 *
 * This is an object that wraps an existing MBeanServerConnection and adds
 * caching to it, as follows:
 *
 * - The first time an attribute is called in a given MBean, the result is
 *   cached. Every subsequent time getAttribute is called for that attribute
 *   the cached result is returned.
 *
 * - When the {@link CachedMBeanServerConnection.flush()} method is invoked the
 *   attributes cache is flushed. Then any subsequent call to getAttribute will
 *   retrieve all the values for the attributes that are known to the cache.
 *
 * - The attributes cache uses a learning approach and only the attributes
 *   that are in the cache will be retrieved between two subsequent updates.
 *
 * @author Luis-Miguel Alventosa
 */
public interface CachedMBeanServerConnection extends MBeanServerConnection {

    /**
     * Flush all cached values of attributes.
     */
    public void flush();
}
