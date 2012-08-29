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

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.modules.profiler.api.ProjectUtilities;
import org.netbeans.modules.profiler.api.project.ProfilingSettingsSupport.SettingsCustomizer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * Provider of support for project-specific information for the Select Profiling Task dialog.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilingSettingsSupportProvider {    
    
    /**
     * Returns expected profiling overhead imposed by the provided profiling settings.
     * 
     * @param settings profiling settings
     * @return expected profiling overhead imposed by the provided profiling settings
     */
    public abstract float getProfilingOverhead(ProfilingSettings settings);
    
    /**
     * Returns SettingsCustomizer instance for a project.
     * 
     * @return SettingsCustomizer instance for a project
     */
    public abstract SettingsCustomizer getSettingsCustomizer();
    
    //    public abstract SettingsConfigurator getSettingsConfigurator();
    
    /**
     * Returns display name of the project-only filter or null if the filter
     * filter is not available for a project.
     * 
     * @return display name of the project-only filter or null
     */
    public abstract String getProjectOnlyFilterName();
    
    /**
     * Returns display name of the project & subprojects filter or null if the
     * filter is not available for a project.
     * 
     * @return display name of the project & subprojects filter or null
     */
    public abstract String getProjectSubprojectsFilterName();
    
    
    public static class Basic extends ProfilingSettingsSupportProvider {
        
        private final Lookup.Provider project;

        @Override
        public float getProfilingOverhead(ProfilingSettings settings) {
            float o = 0.0f;
            int profilingType = settings.getProfilingType();

            if (ProfilingSettings.isMonitorSettings(settings)) {
                //} else if (ProfilingSettings.isAnalyzerSettings(settings)) {
            } else if (ProfilingSettings.isCPUSettings(settings)) {
                if (profilingType == ProfilingSettings.PROFILE_CPU_SAMPLING) {
                    o += 0.05f; // sample app
                } else if (profilingType == ProfilingSettings.PROFILE_CPU_ENTIRE) {
                    o += project == null ? 0.85f : 0.5f; // entire app
                } else if (profilingType == ProfilingSettings.PROFILE_CPU_PART) {
                    o += 0.2f; // part of app
                }

                if (FilterUtils.NONE_FILTER.equals(settings.getSelectedInstrumentationFilter()) &&
                    profilingType != ProfilingSettings.PROFILE_CPU_SAMPLING) {
                    o += 0.5f; // profile all classes
                }
            } else if (ProfilingSettings.isMemorySettings(settings)) {
                if (profilingType == ProfilingSettings.PROFILE_MEMORY_SAMPLING) {
                    o += 0.05f; // sample app
                } else {
                    if (profilingType == ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS) {
                        o += 0.5f; // object allocations
                    } else if (profilingType == ProfilingSettings.PROFILE_MEMORY_LIVENESS) {
                        o += 0.7f; // object liveness
                    }

                    if (settings.getAllocStackTraceLimit() != 0) {
                        o += 0.3f; // record allocation stack traces
                    }
                }
            }

            return Math.min(o, 1);
        }

        @Override
        public SettingsCustomizer getSettingsCustomizer() {
            return null;
        }
        
//        @Override
//        public SettingsConfigurator getSettingsConfigurator() {
//            return null;;
//        }
        
        @Override
        public String getProjectOnlyFilterName() {
            return null;
        }
        
        @Override
        public String getProjectSubprojectsFilterName() {
            return null;
        }
        
        protected final Lookup.Provider getProject() {
            return project;
        }
        
        public Basic(Lookup.Provider project) {
            this.project = project;
        }
        
    }
    
    public static class Default extends Basic {

        @Override
        @NbBundle.Messages({
            "ProfilingSettingsSupportProvider_ProfileProjectClassesString=Profile only project classes"
        })
        public String getProjectOnlyFilterName() {
            return Bundle.ProfilingSettingsSupportProvider_ProfileProjectClassesString();
        }
        
        @Override
        @NbBundle.Messages({
            "ProfilingSettingsSupportProvider_ProfileProjectSubprojectClassesString=Profile project & subprojects classes"
        })
        public String getProjectSubprojectsFilterName() {
            return !ProjectUtilities.hasSubprojects(getProject()) ? null :
                    Bundle.ProfilingSettingsSupportProvider_ProfileProjectSubprojectClassesString();
        }
        
        public Default(Lookup.Provider project) {
            super(project);
        }
        
    }
    
}
