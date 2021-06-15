/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.api.java;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.graalvm.visualvm.lib.profiler.spi.java.ProfilerTypeUtilsProvider;
import org.openide.util.Lookup;

/**
 * Java types related profiler utility methods
 *
 * @author Jaroslav Bachorik
 */
final public class ProfilerTypeUtils {
    private static ProfilerTypeUtilsProvider getProvider(Lookup.Provider project) {
        return project != null ? project.getLookup().lookup(ProfilerTypeUtilsProvider.class) : Lookup.getDefault().lookup(ProfilerTypeUtilsProvider.class);
    }

    /**
     * Resolves a class given its FQN
     * @param className The class FQN
     * @param project A project to resolve the class in
     * @return Returns a resolved {@linkplain SourceClassInfo} or null
     */
    public static SourceClassInfo resolveClass(String className, Lookup.Provider project) {
        ProfilerTypeUtilsProvider p = getProvider(project);
        return p != null ? p.resolveClass(className) : null;
    }

    /**
     * @param project A project to get the main classes for
     * @return Returns a list of all main classes present in the project
     */
    public static Collection<SourceClassInfo> getMainClasses(Lookup.Provider project) {
        ProfilerTypeUtilsProvider p = getProvider(project);

        return p != null ? p.getMainClasses() : Collections.EMPTY_LIST;
    }

    /**
     * Retrieves project's packages
     * @param subprojects Flag indicating whether subprojects should be taken into account
     * @param scope A {@linkplain SourcePackageInfo.Scope} - SOURCE or DEPENDENCIES
     * @param project A project to get the packages for
     * @return Returns a list of project's packages
     */
    public static Collection<SourcePackageInfo> getPackages(boolean subprojects, SourcePackageInfo.Scope scope, Lookup.Provider project) {
        ProfilerTypeUtilsProvider p = getProvider(project);
        
        return p != null ? p.getPackages(subprojects, scope) : Collections.EMPTY_LIST;
    }
    
    /**
     * Case insensitive regexp class search
     * @param pattern Class pattern as a regular expression
     * @param scope A {@linkplain SourcePackageInfo.Scope} - SOURCE or DEPENDENCIES
     * @param project A project to get the packages for
     * @return Returns a collection of classes matching the given pattern
     */
    public static Collection<SourceClassInfo> findClasses(String pattern, Set<SourcePackageInfo.Scope> scope, Lookup.Provider project) {
        ProfilerTypeUtilsProvider p = getProvider(project);
        
        return p != null ? p.findClasses(pattern, scope) : Collections.EMPTY_LIST;
    }
}
