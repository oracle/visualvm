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
package org.graalvm.visualvm.lib.profiler.api.project;

import org.graalvm.visualvm.lib.common.SessionSettings;
import org.graalvm.visualvm.lib.profiler.api.JavaPlatform;
import org.graalvm.visualvm.lib.profiler.spi.project.ProjectProfilingSupportProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Support for profiling projects.
 *
 * @author Jiri Sedlacek
 */
public final class ProjectProfilingSupport {

    private static ProjectProfilingSupport DEFAULT;

    private final ProjectProfilingSupportProvider provider;


    /**
     * Returns true if profiling a project is supported.
     *
     * @return true if profiling a project is supported, false otherwise
     */
    public boolean isProfilingSupported() {
        return provider.isProfilingSupported();
    }

    /**
     * Returns true if attaching to a running project is supported.
     *
     * @return true if attaching to a running project is supported, false otherwise.
     */
    public boolean isAttachSupported() {
        return provider.isAttachSupported();
    }

    /**
     * Returns true if profiling the provided file is supported.
     *
     * @param fo file
     * @return true if profiling the provided file is supported, false otherwise
     */
    public boolean isFileObjectSupported(FileObject fo) {
        return provider.isFileObjectSupported(fo);
    }

    /**
     * Returns true if Profiling Points can be processed by this project.
     *
     * @return true if Profiling Points can be processed by this project, false otherwise.
     */
    public boolean areProfilingPointsSupported() {
        return provider.areProfilingPointsSupported();
    }

    /**
     * Returns the Java platform configured for running the project.
     *
     * @return Java platform configured for running the project
     */
    public JavaPlatform getProjectJavaPlatform() {
        return provider.getProjectJavaPlatform();
    }
    
    /**
     * Returns true if the project is configured and properly set up to be profiled (e.g. profiler is integrated with the project, main class has a main method etc.).
     * 
     * @param profiledClassFile profiled file or null for profiling the entire project
     * @return true if the project is configured and properly set up to be profiled, false otherwise
     */
    public boolean checkProjectCanBeProfiled(FileObject profiledClassFile) {
        return provider.checkProjectCanBeProfiled(profiledClassFile);
    }
    
    /**
     * Configures project-specific session settings.
     * 
     * @param ss session settings
     */
    public void setupProjectSessionSettings(SessionSettings ss) {
        provider.setupProjectSessionSettings(ss);
    }
    
    /**
     * Allows to start a profiling session directly by the ProjectProfilingSupport instance.
     * 
     * @param profiledClassFile profiled file
     * @param isTest true if profiledClassFile is a test, false otherwise
     * @return true if the ProjectProfilingSupport instance started a profiling session, false otherwise
     */
    public boolean startProfilingSession(FileObject profiledClassFile, boolean isTest) {
        return provider.startProfilingSession(profiledClassFile, isTest);
    }
    
    
    private ProjectProfilingSupport(ProjectProfilingSupportProvider provider) {
        this.provider = provider;
    }
    
    private static synchronized ProjectProfilingSupport defaultImpl() {
        if (DEFAULT == null)
            DEFAULT = new ProjectProfilingSupport(new ProjectProfilingSupportProvider.Basic());
        return DEFAULT;
    }
    
    
    /**
     * Returns ProjectProfilingSupport instance for the provided project.
     * 
     * @param project project
     * @return ProjectProfilingSupport instance for the provided project
     */
    public static ProjectProfilingSupport get(Lookup.Provider project) {
        ProjectProfilingSupportProvider provider =
                project != null ? project.getLookup().lookup(ProjectProfilingSupportProvider.class) : null;
        if (provider == null) return defaultImpl();
        else return new ProjectProfilingSupport(provider);
    }
    
    
}
