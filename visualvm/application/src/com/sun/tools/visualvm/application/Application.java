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

package com.sun.tools.visualvm.application;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.host.Host;
import java.io.File;

/**
 * Abstract implementation of Application.
 * Each application is defined by a Host on which it's running and an unique identificator.
 *
 * @author Jiri Sedlacek
 */
public abstract class Application extends DataSource implements Stateful {
    
    /**
     * Instance representing actually running VisualVM application.
     */
    public static final Application CURRENT_APPLICATION = ApplicationSupport.getInstance().createCurrentApplication();
    
    /**
     * Process ID of the application is unknown.
     */
    public static final int UNKNOWN_PID = -1;

    private String id;
    private int pid;
    private Host host;
    private int state = STATE_AVAILABLE;
    

    /**
     * Creates new instance of Application defined by a Host and unique identificator.
     * 
     * @param host Host on which the application is running.
     * @param id unique identificator of the application.
     */
    public Application(Host host, String id) {
        if (host == null) throw new IllegalArgumentException("Host cannot be null");    // NOI18N
        if (id == null && pid == UNKNOWN_PID) throw new IllegalArgumentException("Either id or pid must be provided for the application");  // NOI18N
        this.host = host;
        this.id = id;
    }


    /**
     * Returns unique identificator of the application.
     * 
     * @return unique identificator of the application.
     */
    public final String getId() {
        return id;
    }

    /**
     * Returns process id of the application or Application.UNKNOWN_PID.
     * 
     * @return process id of the application or Application.UNKNOWN_PID.
     */
    public int getPid() {
        return UNKNOWN_PID;
    }

    /**
     * Returns Host on which the application is running.
     * 
     * @return Host on which the application is running.
     */
    public final Host getHost() {
        return host;
    }

    /**
     * Returns true if the application is running on Host.LOCALHOST, false otherwise.
     * 
     * @return true if the application is running on Host.LOCALHOST, false otherwise.
     */
    public final boolean isLocalApplication() {
        return Host.LOCALHOST.equals(getHost());
    }
    
    
    public final synchronized int getState() {
        return state;
    }
    
    protected final synchronized void setState(final int newState) {
        final int oldState = state;
        state = newState;
        if (DataSource.EVENT_QUEUE.isRequestProcessorThread())
            getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState);
        else DataSource.EVENT_QUEUE.post(new Runnable() {
            public void run() { getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState); };
        });
    }
    
    @Override
    public final int hashCode() {
        return getId().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof Application)) return false;
        Application app = (Application) obj;
        return getId().equals(app.getId());
    }
    
    @Override
    public String toString() {
        return "Application [id: " + getId() + ", pid: " + getPid() + ", host: " + getHost().getHostName() + "]";   // NOI18N
    }
    
    
    // <system_temp>/visualvm.dat/<application_id>
    @Override
    protected Storage createStorage() {
        File directory = new File(Storage.getTemporaryStorageDirectoryString() + File.separator + getId());
        return new Storage(directory);
    }

}
