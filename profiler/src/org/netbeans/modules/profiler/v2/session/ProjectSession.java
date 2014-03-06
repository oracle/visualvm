/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.v2.session;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProjectSession {
    
    public static enum State { STARTED, RUNNING, PAUSED, INACTIVE }
    public static enum Type { PROFILE, ATTACH }
    
    
    private final Lookup.Provider project;
    
    private State state;
    private Type type;
    
    private Set<Listener> listeners;
    
    
    public ProjectSession(Lookup.Provider project) {
        this.project = project;
        state = State.INACTIVE;
    }
    
    
    public Lookup.Provider getProject() {
        return project;
    }
    
    protected void setState(State newState) { // not null
        State oldState;
        synchronized (this) {
            oldState = state;
            state = newState;
        }
        if (!oldState.equals(newState)) fireStateChanged(oldState, newState);
    }
    
    public synchronized State getState() {
        return state;
    }
    
    public synchronized boolean inProgress() {
        return state != State.INACTIVE;
    }
    
    public /*synchronized*/ Type getType() {
        return type;
    }
    
    
    public abstract ProfilingSettings getProfilingSettings();
    
    public abstract AttachSettings getAttachSettings();
    
    public abstract void start(ProfilingSettings pSettings, AttachSettings aSettings);
    
    public abstract void modify(ProfilingSettings pSettings);
    
    public abstract void terminate();
    
    
    public boolean equals(Object o) {
        if (!(o instanceof ProjectSession)) return false;
        return Objects.equals(project, ((ProjectSession)o).getProject());
    }
    
    public int hashCode() {
        return Objects.hashCode(project);
    }
    
    
    public String toString() {
        return "ProjectSession for " + ProjectUtilities.getDisplayName(project); // NOI18N
    }
    
    
    public void addListener(Listener listener) {
        synchronized (this) {
            if (listeners == null) listeners = new HashSet();
        }
        
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    
    private void fireStateChanged(State oldState, State newState) {
        synchronized (this) {
            if (listeners == null) return;
        }
        
        Listener[] toNotify;
        synchronized (listeners) {
            toNotify = listeners.toArray(new Listener[listeners.size()]);
        }
        
        for (Listener listener : toNotify)
            listener.stateChanged(oldState, newState);
    }
    
    
    public static interface Listener {
        
        public void stateChanged(State oldState, State newState);
        
    }
    
    public static class Adapter implements Listener {
        
        public void stateChanged(State oldState, State newState) {}
        
    }
    
}
