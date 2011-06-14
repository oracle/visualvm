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
package org.netbeans.modules.profiler.spi.project;

import java.util.Properties;
import org.netbeans.lib.profiler.common.SessionSettings;
import org.netbeans.modules.profiler.api.JavaPlatform;
import org.openide.filesystems.FileObject;

/**
 * Provider of support for profiling projects.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProjectProfilingSupportProvider {
    
    /**
     * Returns true if profiling a project is supported.
     * 
     * @return true if profiling a project is supported, false otherwise
     */
    public abstract boolean isProfilingSupported();
    
    /**
     * Returns true if attaching to a running project is supported.
     * 
     * @return true if attaching to a running project is supported, false otherwise.
     */
    public abstract boolean isAttachSupported();
    
    /**
     * Returns true if profiling the provided file is supported.
     * 
     * @param fo file
     * @return true if profiling the provided file is supported, false otherwise
     */
    public abstract boolean isFileObjectSupported(FileObject fo);
    
    /**
     * Returns the Java platform configured for running the project.
     * 
     * @return Java platform configured for running the project
     */
    public abstract JavaPlatform getProjectJavaPlatform();
    
    /**
     * Returns true if the project is properly set up to be profiled (e.g. main class has a main method).
     * 
     * @param profiledClassFile profiled file or null for profiling the entire project
     * @return true if the project is properly set up to be profiled, false otherwise
     */
    public abstract boolean checkProjectCanBeProfiled(FileObject profiledClassFile);
    
    /**
     * Returns true if the project is configured to be profiled (e.g. build script is customized if needed).
     * 
     * @return true if the project is configured to be profiled, false otherwise
     */
    public abstract boolean checkProjectIsModifiedForProfiler();
    
    /**
     * Configures profiling properties passed to the Ant environment (to be moved to AntProjectSupport?).
     * 
     * @param props properties
     * @param profiledClassFile profiled file or null for profiling the entire project
     */
    public abstract void configurePropertiesForProfiling(Properties props, FileObject profiledClassFile);
    
    /**
     * Configures project-specific session settings.
     * 
     * @param ss session settings
     */
    public abstract void setupProjectSessionSettings(SessionSettings ss);
    
    /**
     * Returns true if profiling settings can be customized by the user (working directory, Java platform etc.)
     * 
     * @return true if profiling settings can be customized by the user, false otherwise
     */
    public abstract boolean supportsSettingsOverride();
    
    /**
     * Returns true if profiler integration can be removed from the project.
     * 
     * @return true if profiler integration can be removed from the project, false otherwise
     */
    public abstract boolean supportsUnintegrate();
    
    /**
     * Removes profiler integration from a project.
     */
    public abstract void unintegrateProfiler();
    
    /**
     * Allows to start a profiling session directly by the ProjectProfilingSupport instance (workaround for Maven projects).
     * 
     * @param profiledClassFile profiled file
     * @param isTest true if profiledClassFile is a test, false otherwise
     * @param properties profiling properties
     * @return true if the ProjectProfilingSupport instance started a profiling session, false otherwise
     */
    public abstract boolean startProfilingSession(FileObject profiledClassFile, boolean isTest, Properties properties);
    
    
    public static class Basic extends ProjectProfilingSupportProvider {

        @Override
        public boolean isProfilingSupported() {
            return false;
        }

        @Override
        public boolean isAttachSupported() {
            return false;
        }

        @Override
        public boolean isFileObjectSupported(FileObject fo) {
            return false;
        }

        @Override
        public JavaPlatform getProjectJavaPlatform() {
            return null;
        }

        @Override
        public boolean checkProjectCanBeProfiled(FileObject profiledClassFile) {
            return true;
        }

        @Override
        public boolean checkProjectIsModifiedForProfiler() {
            return true;
        }

        @Override
        public void configurePropertiesForProfiling(Properties props, FileObject profiledClassFile) {
        }

        @Override
        public void setupProjectSessionSettings(SessionSettings ss) {
        }

        @Override
        public boolean supportsSettingsOverride() {
            return false;
        }

        @Override
        public boolean supportsUnintegrate() {
            return false;
        }

        @Override
        public void unintegrateProfiler() {
        }

        @Override
        public boolean startProfilingSession(FileObject profiledClassFile, boolean isTest, Properties properties) {
            return false;
        }
        
    }
    
}
