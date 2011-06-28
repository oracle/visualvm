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
package org.netbeans.modules.profiler.api;

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.spi.ProfilingSettingsManagerProvider;
import org.openide.util.Lookup;

/**
 * API for managing profiling settings
 * @author Jaroslav Bachorik
 */
final public class ProfilingSettingsManager {
    
    /**
     * Profiling settings wrapper
     * @author Jiri Sedlacek
     */
    final public static class ProfilingSettingsDescriptor {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private ProfilingSettings lastSelectedProfilingSettings;
        private ProfilingSettings[] profilingSettings;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProfilingSettingsDescriptor(ProfilingSettings[] profilingSettings, ProfilingSettings lastSelectedProfilingSettings) {
            this.profilingSettings = profilingSettings;
            this.lastSelectedProfilingSettings = lastSelectedProfilingSettings;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public ProfilingSettings getLastSelectedProfilingSettings() {
            return lastSelectedProfilingSettings;
        }

        public ProfilingSettings[] getProfilingSettings() {
            return profilingSettings;
        }
    }

    private static ProfilingSettingsManagerProvider getProvider() {
        return Lookup.getDefault().lookup(ProfilingSettingsManagerProvider.class);
    }
    
    /**
     * Duplicates the provided settings and stores them in the array of available configurations
     * @param originalSettings The original settings to be duplicated
     * @param availableConfigurations The array of all available configurations to store the duplicated settings to
     * @return Returns the duplicated {@linkplain ProfilingSettings} instance
     */
    public static ProfilingSettings createDuplicateSettings(ProfilingSettings originalSettings, ProfilingSettings[] availableConfigurations) {
        return getProvider().createDuplicateSettings(originalSettings, availableConfigurations);
    }

    /**
     * Creates a new default instance of {@linkplain ProfilingSettings} and stores it in the array of available configurations
     * @param availableConfigurations The array of available configurations to store the newly created settings into
     * @return Returns a new instance of {@linkplain ProfilingSettings}
     */
    public static ProfilingSettings createNewSettings(ProfilingSettings[] availableConfigurations) {
        return getProvider().createNewSettings(availableConfigurations);
    }

    /**
     * Creates a new instance of {@linkplain ProfilingSettings} for certain session type and stores it in the array of available configurations
     * @param type Any of the following values: <ul>
     * <li>{@linkplain ProfilingSettings#PROFILE_CPU_ENTIRE}</li>
     * <li>{@linkplain ProfilingSettings#PROFILE_CPU_PART}</li>
     * <li>{@linkplain ProfilingSettings#PROFILE_MEMORY_ALLOCATIONS}</li>
     * <li>{@linkplain ProfilingSettings#PROFILE_MEMORY_LIVENESS}</li>
     * <li>{@linkplain ProfilingSettings#PROFILE_MONITOR}</li>
     * <li>{@linkplain ProfilingSettings#PROFILE_CPU_STOPWATCH}</li>
     * </ul>
     * @param availableConfigurations The array of available configurations to store the newly created settings into
     * @return Returns a new instance of {@linkplain ProfilingSettings}
     */
    public static ProfilingSettings createNewSettings(int type, ProfilingSettings[] availableConfigurations) {
        return getProvider().createNewSettings(type, availableConfigurations);
    }

    /**
     * Retrieves the effective profiling settings from the given project
     * @param project The project to retrieve the settings for
     * @return Returns {@linkplain ProfilingSettingsDescriptor} instance wrapping the effective profiling settings
     */
    public static ProfilingSettingsDescriptor getProfilingSettings(Lookup.Provider project) {
        return getProvider().getProfilingSettings(project);
    }

    /**
     * Renames the given {@linkplain ProfilingSettings} and stores the changes in the array of available configurations
     * @param originalSettings The settings to rename
     * @param availableConfigurations The array of available configurations to store the newly created settings into
     * @return Returns a renamed instance of {@linkplain ProfilingSettings}
     */
    public static ProfilingSettings renameSettings(ProfilingSettings originalSettings, ProfilingSettings[] availableConfigurations) {
        return getProvider().renameSettings(originalSettings, availableConfigurations);
    }

    /**
     * Stores the given profiling settings alongside the defining project
     * @param profilingSettings The collection of all available profiling settings
     * @param activeProfilingSettings The active profiling settings
     * @param project The project to store the settings for
     */
    public static void storeProfilingSettings(ProfilingSettings[] profilingSettings, ProfilingSettings activeProfilingSettings, Lookup.Provider project) {
        getProvider().storeProfilingSettings(profilingSettings, activeProfilingSettings, project);
    }
}
