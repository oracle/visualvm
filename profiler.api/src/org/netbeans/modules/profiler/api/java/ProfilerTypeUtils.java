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
package org.netbeans.modules.profiler.api.java;

import java.util.Collection;
import java.util.Collections;
import org.netbeans.modules.profiler.spi.java.ProfilerTypeUtilsProvider;
import org.openide.util.Lookup;

/**
 * Java types related profiler utility methods
 * 
 * @author Jaroslav Bachorik
 */
final public class ProfilerTypeUtils {
    private static ProfilerTypeUtilsProvider getProvider(Lookup.Provider project) {
        return project.getLookup().lookup(ProfilerTypeUtilsProvider.class);
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
}
