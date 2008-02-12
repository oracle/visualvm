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

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;

/**
 * Builder responsible for creating ExplorerNode instances and adding them to the explorer tree.
 *
 * @author Jiri Sedlacek
 */
public interface ExplorerNodeBuilder <A extends DataSource> {

    /**
     * Returns ExplorerNode for given DataSource or null if this builder won't handle the DataSource.
     * Created nodes should be cached or correct ,equals() implementation for new nodes should be ensured
     * as the Builder is asked for the ExplorerNode multiple times during DataSource/ExplorerNode lifecycle.
     * 
     * @param dataSource DataSource to create the ExplorerNode for
     * @return ExplorerNode for given DataSource or null if this builder won't handle the DataSource.
     */
    public ExplorerNode<A> getNodeFor(A dataSource);

}
