/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.spi.ProjectUtilitiesProvider;
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
