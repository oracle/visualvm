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

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.openide.filesystems.FileObject;

/**
 * Provider of support for configuring profiling roots and instrumentation filter from a project.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProjectContentsSupportProvider {

    /**
     * Returns array of profiling roots for the defined context.
     *
     * @param profiledClassFile profiled file or null for profiling the entire project
     * @param profileSubprojects true if profiling also project's subprojects, false for profiling just the project
     * @return array of profiling roots for the defined context
     */
    public abstract ClientUtils.SourceCodeSelection[] getProfilingRoots(FileObject profiledClassFile, boolean profileSubprojects);

    /**
     * Returns instrumentation filter for the defined context.
     *
     * @param profileSubprojects true if profiling also project's subprojects, false for profiling just the project
     * @return instrumentation filter for the defined context
     */
    public abstract String getInstrumentationFilter(boolean profileSubprojects);

    /**
     * Resets the ProjectContentsSupport instance after submitting or cancelling the Select Profiling Task dialog.
     */
    public abstract void reset();


//    public static class Basic extends ProjectContentsSupportProvider {
//
//        private static final SourceCodeSelection[] EMPTY_SELECTION = new ClientUtils.SourceCodeSelection[0];
//
//        @Override
//        public SourceCodeSelection[] getProfilingRoots(FileObject profiledClassFile, boolean profileSubprojects) {
//            return EMPTY_SELECTION;
//        }
//
//        @Override
//        public SimpleFilter getInstrumentationFilter(boolean profileSubprojects) {
//            return SimpleFilter.NO_FILTER;
//        }
//
//    }

}
