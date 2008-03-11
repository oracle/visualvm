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

package com.sun.tools.visualvm.application.monitor;

import com.sun.tools.visualvm.core.ui.PluggableViewSupport;

/**
 * A public entrypoint to the Monitor subtab.
 *
 * @author Jiri Sedlacek
 */
public final class MonitorViewSupport {

    private static MonitorViewSupport instance;

    private ApplicationMonitorPluggableView applicationPluggableView;


    /**
     * Returns singleton instance of MonitorViewSupport.
     * 
     * @return singleton instance of MonitorViewSupport.
     */
    public static synchronized MonitorViewSupport getInstance() {
        if (instance == null) instance = new MonitorViewSupport();
        return instance;
    }


    /**
     * Returns PluggableView instance to be used to customize the Monitor view of an application.
     * 
     * @return PluggableView instance to be used to customize the Monitor view of an application.
     */
    public PluggableViewSupport getApplicationPluggableView() {
        return getApplicationMonitorPluggableView();
    }
    
    ApplicationMonitorPluggableView getApplicationMonitorPluggableView() {
        return applicationPluggableView;
    }
    
    
    private MonitorViewSupport() {
        applicationPluggableView = new ApplicationMonitorPluggableView();
        new ApplicationMonitorViewProvider().initialize();
    }

}
