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
package org.graalvm.visualvm.lib.profiler.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.profiler.spi.ProjectUtilitiesProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;

/**
 * ProjectUtilities provides profiler with necessary functionality work accessing
 * project oriented data.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public final class ProjectUtilities {

    /**Retrieves the current main project set in the IDE.
     *
     * @return the current main project or null if none
     */
    public static Provider getMainProject() {
        return provider().getMainProject();
    }

    /**
     * Gets a list of currently open projects.
     *
     * @return list of projects currently opened in the IDE's GUI; order not specified
     */
    public static Provider[] getOpenedProjects() {
        return provider().getOpenedProjects();
    }

    /**
     * Get a human-readable display name for the project.
     * May contain spaces, international characters, etc.
     * @param project project
     * @return a display name for the project
     */
    public static String getDisplayName(Lookup.Provider project) {
        return provider().getDisplayName(project);
    }

    /**
     * Gets an associated directory where the project metadata and possibly sources live.
     * In the case of a typical Ant project, this is the top directory, not the
     * project metadata subdirectory.
     * @return a directory
     */
    public static FileObject getProjectDirectory(Lookup.Provider project) {
        return provider().getProjectDirectory(project);
    }

    /**
     * Gets icon for given project.
     * Usually determined by the project type.
     * @param project project
     * @return icon of the project.
     */
    public static Icon getIcon(Provider project) {
        return provider().getIcon(project);
    }
    
    /**
     * Returns true if the provided project has sub-projects.
     * 
     * @param project a project
     * @return true if the provided project has sub-projects, false otherwise
     */
    public static boolean hasSubprojects(Provider project) {
        return provider().hasSubprojects(project);
    }

    /**
     * Computes set of sub-projects of a project
     * @param project a project
     * @param subprojects map of sub-projects
     */
    public static void fetchSubprojects(Provider project, Set<Provider> subprojects) {
        provider().fetchSubprojects(project, subprojects);
    }
    
    /**
     * Find the project, if any, which "owns" the given file.
     * @param fobj the file (generally on disk)
     * @return a project which contains it, or null if there is no known project containing it
     */
    public static Provider getProject(FileObject fobj) {
        return provider().getProject(fobj);
    }
    /**
     * Adds a listener to be notified when set of open projects changes.
     * @param listener listener to be added
     */
    public static void addOpenProjectsListener(ChangeListener listener) {
        provider().addOpenProjectsListener(listener);
    }
    
    /**
     * Removes a listener to be notified when set of open projects changes.
     * @param listener listener to be removed
     */
    public static void removeOpenProjectsListener(ChangeListener listener) {
        provider().removeOpenProjectsListener(listener);
    }

    /**
     * Sorts projects by display name
     * @param projects
     * @return arrays of projects sorted by display name
     */
    public static Provider[] getSortedProjects(Provider[] projects) {
        List<Provider> projectsArray = Arrays.asList(projects);

        Collections.sort(projectsArray,
                new Comparator() {

                    @Override
                    public int compare(Object o1, Object o2) {
                        Provider p1 = (Provider) o1;
                        Provider p2 = (Provider) o2;

                        return getDisplayName(p1).toLowerCase().compareTo(getDisplayName(p2).toLowerCase());
                    }
                });
        projectsArray.toArray(projects);

        return projects;
    }
    
    private static ProjectUtilitiesProvider provider() {
        return Lookup.getDefault().lookup(ProjectUtilitiesProvider.class);
    }
    
}
