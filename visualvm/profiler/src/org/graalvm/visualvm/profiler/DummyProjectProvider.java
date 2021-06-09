/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiler;

import java.util.Set;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.profiler.spi.ProjectUtilitiesProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;


/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=ProjectUtilitiesProvider.class)
public class DummyProjectProvider extends ProjectUtilitiesProvider {
    
    private static Lookup.Provider[] EMPTY = new Lookup.Provider[0];
    
    public Icon getIcon(Lookup.Provider provider) {
        throw new UnsupportedOperationException();
    }

    public Lookup.Provider getMainProject() {
        return null;
    }

    public String getDisplayName(Lookup.Provider provider) {
        throw new UnsupportedOperationException();
    }

    public FileObject getProjectDirectory(Lookup.Provider provider) {
        throw new UnsupportedOperationException();
    }

    public Lookup.Provider[] getOpenedProjects() {
        return EMPTY;
    }

    public boolean hasSubprojects(Lookup.Provider provider) {
        throw new UnsupportedOperationException();
    }

    public void fetchSubprojects(Lookup.Provider project, Set<Lookup.Provider> subprojects) {
        throw new UnsupportedOperationException();
    }

    public Lookup.Provider getProject(FileObject fileObject) {
        throw new UnsupportedOperationException();
    }

    public void addOpenProjectsListener(ChangeListener changeListener) {
    }

    public void removeOpenProjectsListener(ChangeListener changeListener) {
    }
}
