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
package org.netbeans.modules.profiler.api.project;

import java.io.IOException;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.modules.profiler.api.GlobalStorage;
import org.netbeans.modules.profiler.spi.project.ProjectStorageProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Support for storing and retrieving data in context of a project.
 *
 * @author Jiri Sedlacek
 */
public final class ProjectStorage {
    
    /**
     * Returns attach settings for the provided project or null if not available.
     * 
     * @param project project context
     * @return attach settings for the provided project or null if not available
     */
    public static AttachSettings loadAttachSettings(Lookup.Provider project) throws IOException {
        ProjectStorageProvider p = provider();
        if (p != null) return p.loadAttachSettings(project);
        else return null;
    }
    
    /**
     * Saves attach settings in context of the provided project.
     * 
     * @param project project context
     * @param settings attach settings
     */
    public static void saveAttachSettings(Lookup.Provider project, AttachSettings settings) {
        ProjectStorageProvider p = provider();
        if (p != null) p.saveAttachSettings(project, settings);
    }
    
    /**
     * Returns FileObject which can be used as a settings storage for the provided project or null if not available.
     * 
     * @param project project context, for null GlobalStorage.getSettingsFolder(create) will be called
     * @param create if <code>true</code> the storage will be created if not already available
     * @return FileObject which can be used as a settings storage for the provided project or null if not available
     * @throws IOException 
     */
    public static FileObject getSettingsFolder(Lookup.Provider project, boolean create)
            throws IOException {
        if (project == null) return GlobalStorage.getSettingsFolder(create);
        ProjectStorageProvider p = provider();
        if (p != null) return p.getSettingsFolder(project, create);
        else return null;
    }
    
    /**
     * Returns project context for the provided settings storage FileObject or null if not resolvable.
     * 
     * @param settingsFolder settings storage
     * @return  project context for the provided settings storage FileObject or null if not resolvable
     */
    public static Lookup.Provider getProjectFromSettingsFolder(FileObject settingsFolder) {
        ProjectStorageProvider p = provider();
        if (p != null) return p.getProjectFromSettingsFolder(settingsFolder);
        else return null;
    }
    
    private static ProjectStorageProvider provider() {
        return Lookup.getDefault().lookup(ProjectStorageProvider.class);
    }
    
}
