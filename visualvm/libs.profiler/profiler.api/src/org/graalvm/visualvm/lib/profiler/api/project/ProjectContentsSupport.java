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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.profiler.spi.project.ProjectContentsSupportProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 * Support for configuring profiling roots and instrumentation filter from a project.
 *
 * @author Jiri Sedlacek
 */
public final class ProjectContentsSupport {

    private static final ClientUtils.SourceCodeSelection[] EMPTY_SELECTION = new ClientUtils.SourceCodeSelection[0];
    private static ProjectContentsSupport DEFAULT;

    private final Collection<? extends ProjectContentsSupportProvider> providers;


    /**
     * Returns array of profiling roots for the defined context.
     *
     * @param profiledClassFile profiled file or null for profiling the entire project
     * @param profileSubprojects true if profiling also project's subprojects, false for profiling just the project
     * @return array of profiling roots for the defined context
     */
    public ClientUtils.SourceCodeSelection[] getProfilingRoots(FileObject profiledClassFile,
                                                               boolean profileSubprojects) {
        if (providers == null) {
            return EMPTY_SELECTION;
        } else {
            Set<ClientUtils.SourceCodeSelection> allRoots = new HashSet<>();
            for (ProjectContentsSupportProvider provider : providers) {
                ClientUtils.SourceCodeSelection[] roots = provider.getProfilingRoots(profiledClassFile, profileSubprojects);
                if (roots != null && roots.length > 0) allRoots.addAll(Arrays.asList(roots));
            }
            return allRoots.toArray(new ClientUtils.SourceCodeSelection[0]);
        }
    }

    /**
     * Returns instrumentation filter for the defined context.
     * 
     * @param profileSubprojects true if profiling also project's subprojects, false for profiling just the project
     * @return instrumentation filter for the defined context
     */
    public String getInstrumentationFilter(boolean profileSubprojects) {
        if (providers == null) {
            return ""; // NOI18N
        } else {
            StringBuilder buffer = new StringBuilder();
            for( ProjectContentsSupportProvider provider : providers) {
                String filter = provider.getInstrumentationFilter(profileSubprojects);
                if (filter != null && !filter.isEmpty()) {
                    buffer.append(filter).append(" "); // NOI18N
                }
            }
            return buffer.toString().trim();
        }
    }
    
    /**
     * Resets the ProjectContentsSupport instance after submitting or cancelling the Select Profiling Task dialog.
     */
    public void reset() {
        if (providers != null)
            for (ProjectContentsSupportProvider provider : providers) 
                provider.reset();
    }
    
    
    private ProjectContentsSupport(Collection<? extends ProjectContentsSupportProvider> providers) {
        this.providers = providers;
    }
    
    private static synchronized ProjectContentsSupport defaultImpl() {
        if (DEFAULT == null)
            DEFAULT = new ProjectContentsSupport(null);
        return DEFAULT;
    }
    

    /**
     * Returns ProjectContentsSupport instance for the provided project.
     * 
     * @param project project
     * @return ProjectContentsSupport instance for the provided project
     */
    public static ProjectContentsSupport get(Lookup.Provider project) {
        Collection<? extends ProjectContentsSupportProvider> providers =
                project != null ? project.getLookup().lookupAll(ProjectContentsSupportProvider.class) : null;
        if (providers == null || providers.isEmpty()) return defaultImpl();
        else return new ProjectContentsSupport(providers);
    }
    
}
