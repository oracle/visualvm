/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.jvmstat.application;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.host.Host;
import sun.jvmstat.monitor.HostIdentifier;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class JvmstatApplication extends Application {   
    
    final private int pid;
    final HostIdentifier hostId;
    // since getting JVM for the first time can take a long time
    // hard reference jvm from application so we are sure that it is not garbage collected
    Jvm jvm;

    JvmstatApplication(Host host, HostIdentifier hostId, String id, int pid) {
        super(host, id);
        this.pid = pid;
        this.hostId = hostId;
    }
    
    
    public int getPid() {
        return pid;
    }
    
    
    public boolean supportsUserRemove() {
        return getState() == Stateful.STATE_UNAVAILABLE;
    }
    
    protected boolean handleControlledRemove() {
        return super.handleControlledRemove();
    }
    

    protected void remove() {
        super.remove();
        JvmstatApplicationProvider.sharedInstance().removeFromMap(this);
    }
    
    
    void setStateImpl(int newState) {
        setState(newState);
    }

    HostIdentifier getHostIdentifier() {
       return hostId; 
    }
}
