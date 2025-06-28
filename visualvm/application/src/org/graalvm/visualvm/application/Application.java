/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Objects;
import org.graalvm.visualvm.core.datasource.StatefulDataSource;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.options.GlobalPreferences;
import org.graalvm.visualvm.core.ui.DataSourceWindowListener;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.host.Host;

/**
 * Abstract implementation of Application.
 * Each application is defined by a Host on which it's running and a unique identificator.
 *
 * @author Jiri Sedlacek
 */
public abstract class Application extends StatefulDataSource {
    
    /**
     * Instance representing actually running VisualVM application.
     */
    public static final Application CURRENT_APPLICATION = ApplicationSupport.getInstance().createCurrentApplication();
    
    /**
     * Process ID of the application is unknown.
     */
    public static final int UNKNOWN_PID = -1;

    private String id;
    private Host host;
    

    /**
     * Creates new instance of Application defined by a Host and unique identificator.
     * 
     * @param host Host on which the application is running.
     * @param id unique identificator of the application.
     */
    public Application(Host host, String id) {
        this(host, id, STATE_AVAILABLE);
    }
    
    /**
     * Creates new instance of Application defined by a Host and unique identificator.
     * 
     * @param host Host on which the application is running.
     * @param id unique identificator of the application.
     * @param state initial state of the application.
     */
    protected Application(Host host, String id, int state) {
        super(state);
        if (host == null) throw new IllegalArgumentException("Host cannot be null");    // NOI18N
        if (id == null) throw new IllegalArgumentException("Application id cannot be null");  // NOI18N
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
     * Returns true if the application is running on {@code Host.LOCALHOST}, false otherwise.
     * 
     * @return true if the application is running on {@code Host.LOCALHOST}, false otherwise.
     */
    public boolean isLocalApplication() {
        return Host.LOCALHOST.equals(getHost());
    }
    
    protected boolean supportsFinishedRemove() {
        return true;
    }
    
    protected boolean handleControlledRemove() {
        if (!canRemoveFinished_Opened()) {
            class ApplicationListener implements DataSourceWindowListener<Application>, DataRemovedListener<Application>, PropertyChangeListener {
                private boolean done;
                
                public void windowClosed(Application application) {
                    synchronized (ApplicationListener.this) {
                        if (!done) {
                            unregister();
                            if (canRemoveFinished_Snapshots())
                                application.getHost().getRepository().removeDataSource(application);
                        }
                    }
                }
                
                public void dataRemoved(Application application) {
                    synchronized (ApplicationListener.this) {
                        if (!done) unregister();
                    }
                }
                
                public void propertyChange(PropertyChangeEvent evt) {
                    if (PROPERTY_STATE.equals(evt.getPropertyName()) &&
                        !Objects.equals(evt.getNewValue(), STATE_UNAVAILABLE))
                            synchronized (ApplicationListener.this) {
                                if (!done) unregister();
                            }
                }
                
                void register() {
                    Application.this.notifyWhenRemoved(this);
                    Application.this.addPropertyChangeListener(PROPERTY_STATE, this);
                    DataSourceWindowManager.sharedInstance().addWindowListener(Application.this, this);
                }
                
                void unregister() {
                    done = true;
                    DataSourceWindowManager.sharedInstance().removeWindowListener(Application.this, this);
                    Application.this.removePropertyChangeListener(this);
                }
            }
            
            new ApplicationListener().register();
            
            return true;
        }
        
        if (!canRemoveFinished_Snapshots()) {
            return true;
        }
        
        return false;
    }
    
    private boolean canRemoveFinished_Opened() {
        return GlobalPreferences.sharedInstance().autoRemoveOpenedFinishedApps() ||
               !DataSourceWindowManager.sharedInstance().isDataSourceOpened(this);
    }
    
    private boolean canRemoveFinished_Snapshots() {
        return GlobalPreferences.sharedInstance().autoRemoveFinishedAppsWithSnapshots() ||
               getRepository().getDataSources().isEmpty();
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
    
    
    // <system_temp>/visualvm_<username>.dat/<application_id>
    @Override
    protected Storage createStorage() {
        File directory = new File(Storage.getTemporaryStorageDirectoryString() +
                                  File.separator + validFileName(getId()));
        return new Storage(directory);
    }

    private static String validFileName(String fileName) {
        char[] fileNameCh = fileName.toCharArray();
        StringBuilder validFileName = new StringBuilder();
        for (char ch : fileNameCh)
            if (Character.isLetterOrDigit(ch)) validFileName.append(ch);
            else validFileName.append('_'); // NOI18N
        return validFileName.toString();
    }

}
