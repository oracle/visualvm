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
package org.graalvm.visualvm.lib.profiler.spi.project;

import org.graalvm.visualvm.lib.common.SessionSettings;
import org.graalvm.visualvm.lib.profiler.api.JavaPlatform;
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
     * Returns true if Profiling Points can be processed by this project.
     *
     * @return true if Profiling Points can be processed by this project, false otherwise.
     */
    public abstract boolean areProfilingPointsSupported();

    /**
     * Returns the Java platform configured for running the project.
     *
     * @return Java platform configured for running the project
     */
    public abstract JavaPlatform getProjectJavaPlatform();

    /**
     * Returns true if the project is configured and properly set up to be profiled (e.g. main class has a main method etc.).
     *
     * @param profiledClassFile profiled file or null for profiling the entire project
     * @return true if the project is configured and properly set up to be profiled, false otherwise
     */
    public abstract boolean checkProjectCanBeProfiled(FileObject profiledClassFile);

    /**
     * Configures project-specific session settings.
     * 
     * @param ss session settings
     */
    public abstract void setupProjectSessionSettings(SessionSettings ss);
    
    /**
     * Allows to start a profiling session directly by the ProjectProfilingSupport instance.
     * 
     * @param profiledClassFile profiled file
     * @param isTest true if profiledClassFile is a test, false otherwise
     * @return true if the ProjectProfilingSupport instance started a profiling session, false otherwise
     */
    public abstract boolean startProfilingSession(FileObject profiledClassFile, boolean isTest);
    
    
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
        public boolean areProfilingPointsSupported() {
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
        public void setupProjectSessionSettings(SessionSettings ss) {
        }

        @Override
        public boolean startProfilingSession(FileObject profiledClassFile, boolean isTest) {
            return false;
        }
        
    }
    
}
