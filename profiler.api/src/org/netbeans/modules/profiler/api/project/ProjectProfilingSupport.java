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

import java.util.Properties;
import org.netbeans.lib.profiler.common.SessionSettings;
import org.netbeans.modules.profiler.api.JavaPlatform;
import org.netbeans.modules.profiler.spi.project.ProjectProfilingSupportProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProjectProfilingSupport {
    
    private static ProjectProfilingSupport DEFAULT;
    
    private final ProjectProfilingSupportProvider provider;
    
    
    public boolean isProfilingSupported() {
        return provider.isProfilingSupported();
    }
    
    public boolean isAttachSupported() {
        return provider.isAttachSupported();
    }
    
    public boolean isFileObjectSupported(FileObject fo) {
        return provider.isFileObjectSupported(fo);
    }
    
    public JavaPlatform getProjectJavaPlatform() {
        return provider.getProjectJavaPlatform();
    }
    
    public boolean checkProjectCanBeProfiled(FileObject profiledClassFile) {
        return provider.checkProjectCanBeProfiled(profiledClassFile);
    }
    
    public boolean checkProjectIsModifiedForProfiler() {
        return provider.checkProjectIsModifiedForProfiler();
    }
    
    public void configurePropertiesForProfiling(Properties props, FileObject profiledClassFile) {
        provider.configurePropertiesForProfiling(props, profiledClassFile);
    }
    
    public void setupProjectSessionSettings(SessionSettings ss) {
        provider.setupProjectSessionSettings(ss);
    }
    
    public boolean supportsSettingsOverride() {
        return provider.supportsSettingsOverride();
    }
    
    public boolean supportsUnintegrate() {
        return provider.supportsUnintegrate();
    }
    
    public void unintegrateProfiler() {
        provider.unintegrateProfiler();
    }
    
    public boolean startProfilingSession(FileObject profiledClassFile, boolean isTest, Properties properties) {
        return provider.startProfilingSession(profiledClassFile, isTest, properties);
    }
    
    
    private ProjectProfilingSupport(ProjectProfilingSupportProvider provider) {
        this.provider = provider;
    }
    
    private static synchronized ProjectProfilingSupport defaultImpl() {
        if (DEFAULT == null)
            DEFAULT = new ProjectProfilingSupport(new ProjectProfilingSupportProvider.Basic());
        return DEFAULT;
    }
    
    
    public static ProjectProfilingSupport get(Lookup.Provider project) {
        ProjectProfilingSupportProvider provider =
                project.getLookup().lookup(ProjectProfilingSupportProvider.class);
        if (provider == null) return defaultImpl();
        else return new ProjectProfilingSupport(provider);
    }
    
    
}
