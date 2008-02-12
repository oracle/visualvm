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

package com.sun.tools.visualvm.core.ui;

import com.sun.tools.visualvm.core.datasource.DataSource;

/**
 * Listener to obtain notification about opening/closing DataSource UIs.
 *
 * @author Jiri Sedlacek
 */
public interface DataSourceUIListener {

    /**
     * Synchronous notification just before an UI of given DataSource will be opened.
     * Handling of this event should be as fast as possible, long-running implementations
     * should handle the event in separate thread. The main aim of this notification is
     * to help the DataSourceViewProvider to lazily initialize all models for the DataSource
     * instance being opened.
     * 
     * @param dataSource DataSource being opened.
     */
    public void uiWillOpen(DataSource dataSource);

    /**
     * Synchronous notification just after an UI of given DataSource has been opened.
     * 
     * @param dataSource opened DataSource.
     */
    public void uiOpened(DataSource dataSource);

    /**
     * Synchronous notification just after an UI of given DataSource has been closed.
     * 
     * @param dataSource closed DataSource.
     */
    public void uiClosed(DataSource dataSource);

}
