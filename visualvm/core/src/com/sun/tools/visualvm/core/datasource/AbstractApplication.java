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

import java.io.File;
import java.util.Set;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractApplication extends AbstractDataSource implements Application {

    private String id;
    private int pid;
    private Host host;


    public AbstractApplication(Host host, String id) {
        this(host, id, UNKNOWN_PID);
    }

    public AbstractApplication(Host host, int pid) {
        this(host, host.getHostName() + "-" + pid, pid);
    }

    public AbstractApplication(Host host, String id, int pid) {
        if (host == null) throw new IllegalArgumentException("Host cannot be null");
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
    
    public Set<Snapshot> getSnapshots() {
        return getRepository().getDataSources(Snapshot.class);
    }
    
    public File getStorage() {
        File storage = super.getStorage();
        File applicationStorage = new File(storage, getId());
        if (!applicationStorage.exists() && !applicationStorage.mkdir()) throw new IllegalStateException("Cannot create storage directory for " + toString());
        return applicationStorage;
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

}
