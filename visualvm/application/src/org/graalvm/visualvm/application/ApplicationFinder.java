/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.application;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import javax.swing.Timer;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.host.Host;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ApplicationFinder {
    
    private static final int DEFAULT_TIMEOUT = Integer.getInteger("visualvm.search.process.timeout", 5000); // NOI18N
    
    
    private final int pid;
    private final String id;
    private final Host host;
    private final int timeout;
    
    
    public ApplicationFinder(int pid) {
        this(pid, null);
    }
    
    public ApplicationFinder(String id) {
        this(Application.UNKNOWN_PID, id);
    }
    
    public ApplicationFinder(int pid, String id) {
        this(pid, id, Host.LOCALHOST, DEFAULT_TIMEOUT);
    }
    
    public ApplicationFinder(int pid, String id, Host host, int timeout) {
        this.pid = pid;
        this.id = id;
        this.host = host;
        this.timeout = timeout;
    }
    
    
    public abstract void found(Application application);
    
    public void notFound(int pid, String id) {}
    
    
    public final void find() {
        if (timeout <= 0) findImmediately();
        else new FindLater().find();
    }
    
    
    private void findImmediately() {
        Application application = findInSet(pid, id, host.getRepository().getDataSources(Application.class));
        if (application != null) found(application);
        else notFound(pid, id);
    }
    
    
    private static Application findInSet(int pid, String id, Set<Application> applications) {
        for (Application application : applications) {
            if (pid != Application.UNKNOWN_PID) {
                if (application.getPid() == pid) {
                    return application;
                }
            }
            if (id != null) {
                Jvm jvm = JvmFactory.getJVMFor(application);
                if (jvm.isBasicInfoSupported()) {
                    String args = jvm.getJvmArgs();
                    if (args != null && args.contains(id)) {
                        return application;
                    }
                }
            }
        }
        return null;
    }
    
    
    private class FindLater implements DataChangeListener<Application>, ActionListener {
        
        private volatile boolean removed;
        private final Timer timer;

        
        FindLater() {
            timer = new Timer(timeout, this);
        }
        
        
        synchronized void find() {
            removed = false;
            timer.start();
            host.getRepository().addDataChangeListener(this, Application.class);
        }

        
        public synchronized void dataChanged(DataChangeEvent<Application> event) {
            Set<Application> applications = event.getAdded();
            if (applications.isEmpty()) applications = event.getCurrent();
            Application application = findInSet(pid, id, applications);
            if (application != null) {
                if (!removed) {
                    cleanup();
                    timer.stop();
                }
                found(application);
            }
        }

        public synchronized void actionPerformed(ActionEvent e) {
            if (!removed) {
                cleanup();
                notFound(pid, id);
            }
        }
        
        
        private void cleanup() {
            host.getRepository().removeDataChangeListener(this);
            removed = true;
        }
    }
    
}
