/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.spi;

import java.util.Set;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup.Provider;

/**
 *
 * @author Tomas Hurka
 */
public abstract class ProjectUtilitiesProvider {

     /**
     * Gets icon for given project.
     * Usually determined by the project type.
     * @param project project
     * @return icon of the project.
     */
   public abstract Icon getIcon(Provider project);


    /**Retrieves the current main project set in the IDE.
     *
     * @return the current main project or null if none
     */
    public abstract Provider getMainProject();

    /**
     * Get a human-readable display name for the project.
     * May contain spaces, international characters, etc.
     * @param project project
     * @return a display name for the project
     */
    public abstract String getDisplayName(Provider project);

    /**
     * Gets an associated directory where the project metadata and possibly sources live.
     * In the case of a typical Ant project, this is the top directory, not the
     * project metadata subdirectory.
     * @return a directory
     */
    public abstract FileObject getProjectDirectory(Provider project);

    /**
     * Gets a list of currently open projects.
     *
     * @return list of projects currently opened in the IDE's GUI; order not specified
     */
    public abstract Provider[] getOpenedProjects();

    /**
     * Returns true if the provided project has sub-projects.
     *
     * @param project a project
     * @return true if the provided project has sub-projects, false otherwise
     */
    public abstract boolean hasSubprojects(Provider project);

    /**
     * Computes set of sub-projects of a project
     * @param project a project
     * @param subprojects map of sub-projects
     */
    public abstract void fetchSubprojects(Provider project, Set<Provider> subprojects);

    /**
     * Find the project, if any, which "owns" the given file.
     * @param fobj the file (generally on disk)
     * @return a project which contains it, or null if there is no known project containing it
     */
    public abstract Provider getProject(FileObject fobj);
    
    /**
     * Adds a listener to be notified when set of open projects changes.
     * @param listener listener to be added
     */
    public abstract void addOpenProjectsListener(ChangeListener listener);
    
    /**
     * Removes a listener to be notified when set of open projects changes.
     * @param listener listener to be removed
     */
    public abstract void removeOpenProjectsListener(ChangeListener listener);
}
