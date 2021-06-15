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
package org.graalvm.visualvm.lib.profiler.spi.java;

import java.util.Collection;
import java.util.Set;
import org.graalvm.visualvm.lib.profiler.api.java.ProfilerTypeUtils;
import org.graalvm.visualvm.lib.profiler.api.java.SourceClassInfo;
import org.graalvm.visualvm.lib.profiler.api.java.SourcePackageInfo;

/**
 * An SPI for {@linkplain ProfilerTypeUtils} functionality
 * @author Jaroslav Bachorik
 */
public abstract class ProfilerTypeUtilsProvider {
    /**
     *
     * @param className A fully qualified class name
     * @return Returns a resolved class or NULL
     */
    abstract public SourceClassInfo resolveClass(String className);

    /**
     * @return Returns a list of all main classes present in the project
     */
    abstract public Collection<SourceClassInfo> getMainClasses();

    /**
     *
     * @param subprojects A flag indicating whether subprojects should be taken into account
     * @param scope A {@linkplain SourcePackageInfo.Scope} - SOURCE or DEPENDENCIES
     * @return Returns a list of project's packages
     */
    abstract public Collection<SourcePackageInfo> getPackages(boolean subprojects, SourcePackageInfo.Scope scope);

    /**
     * Case insensitive regexp class search
     * @param pattern Class pattern as a regular expression
     * @param scope A {@linkplain SourcePackageInfo.Scope} - SOURCE or DEPENDENCIES
     * @return Returns a collection of classes matching the given pattern
     */
    public Collection<SourceClassInfo> findClasses(String pattern, Set<SourcePackageInfo.Scope> scope) {
        throw new UnsupportedOperationException();
    }
}
