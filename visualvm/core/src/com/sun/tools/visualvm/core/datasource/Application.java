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

import com.sun.tools.visualvm.core.application.ApplicationsSupport;
import java.util.Set;

/**
 * DataSource representing an application.
 *
 * @author Jiri Sedlacek
 */
public interface Application extends DataSource {

    /**
     * Instance representing actually running VisualVM application.
     */
    public static final Application CURRENT_APPLICATION = ApplicationsSupport.getInstance().getCurrentApplication();
    
    /**
     * Process ID of the application is unknown.
     */
    public static final int UNKNOWN_PID = -1;

    /**
     * Returns unique Id of this application.
     * 
     * @return unique Id of this application.
     */
    public String getId();

    /**
     * Returns process Id of this application if known.
     * 
     * @return process Id of this application or UNKNOWN_PID.
     */
    public int getPid();

    /**
     * Returns a host instance for this application.
     * 
     * @return host instance for this application.
     */
    public Host getHost();

    /**
     * Returns true if this application is running on the localhost.
     * 
     * @return true if this application is running on the localhost.
     */
    public boolean isLocalApplication();

    /**
     * Returns set of snapshots created for this application.
     * Use com.sun.tools.visualvm.core.snapshot.RegisteredSnapshotCategories
     * to get a list of all registered snapshot types.
     * 
     * @return set of snapshots created for this application.
     */
    public Set<Snapshot> getSnapshots();

}
