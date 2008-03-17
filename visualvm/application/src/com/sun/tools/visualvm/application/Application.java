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
 *
 * @author Jiri Sedlacek
 */
public abstract class Application extends DataSource implements Stateful {
    
    /**
     * Instance representing actually running VisualVM application.
     */
    public static final Application CURRENT_APPLICATION = ApplicationsSupport.getInstance().createCurrentApplication();
    
    /**
     * Process ID of the application is unknown.
     */
    public static final int UNKNOWN_PID = -1;

    private String id;
    private int pid;
    private Host host;
    private int state = STATE_AVAILABLE;


    /**
     * Creates new instance of AbstractApplication identified by id running on a Host
     * 
     * @param host host of the application,
     * @param id unique identificator of the application.
     */
    public Application(Host host, String id) {
        this(host, id, UNKNOWN_PID);
    }

    /**
     * Creates new instance of AbstractApplication identified by its process id running on a Host.
     * 
     * @param host host of the application,
     * @param pid process id of the application.
     */
    public Application(Host host, int pid) {
        this(host, host.getHostName() + "-" + pid, pid);
    }

    /**
     * Creates new instance of Abstract application identified by its id and/or process id running on a Host.
     * 
     * @param host host of the application,
     * @param id unique identificator of the application,
     * @param pid process ide of the application or UNKNOWN_PID if the process id is unknown.
     */
    public Application(Host host, String id, int pid) {
        if (host == null) throw new IllegalArgumentException("Host cannot be null");
        if (id == null && pid == UNKNOWN_PID) throw new IllegalArgumentException("Either id or pid must be provided for the application");
        this.host = host;
        this.id = id;
        this.pid = pid;
    }


    public String getId() {
        return id;
    }

    public int getPid() {
        return pid;
    }

    public Host getHost() {
        return host;
    }

    public boolean isLocalApplication() {
        return Host.LOCALHOST.equals(getHost());
    }
    
    
    public int getState() {
        return state;
    }
    
    protected final synchronized void setState(int newState) {
        int oldState = state;
        state = newState;
        getChangeSupport().firePropertyChange(PROPERTY_STATE, oldState, newState);
    }
    
    
    public int hashCode() {
        return getId().hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Application)) return false;
        Application app = (Application) obj;
        return getId().equals(app.getId());
    }
    
    public String toString() {
        return "Application [id: " + getId() + ", pid: " + getPid() + ", host: " + getHost().getHostName() + "]";
    }
    
    
    // <system_temp>/visualvm.dat/<application_id>
    protected Storage createStorage() {
        File directory = new File(Storage.getTemporaryStorageDirectoryString() + File.separator + getId());
        return new Storage(directory);
    }

}
