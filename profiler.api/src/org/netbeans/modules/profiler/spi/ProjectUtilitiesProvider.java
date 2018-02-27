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
package org.netbeans.modules.profiler.spi;

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
