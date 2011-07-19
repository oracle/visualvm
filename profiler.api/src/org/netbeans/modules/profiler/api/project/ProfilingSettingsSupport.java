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
package org.netbeans.modules.profiler.api.project;

import java.util.Properties;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.spi.project.ProfilingSettingsSupportProvider;
import org.openide.util.Lookup;

/**
 * Support for project-specific information for the Select Profiling Task dialog.
 *
 * @author Jiri Sedlacek
 */
public final class ProfilingSettingsSupport {
    
    private static ProfilingSettingsSupport DEFAULT;
    
    private final ProfilingSettingsSupportProvider provider;
    
    
    /**
     * Returns expected profiling overhead imposed by the provided profiling settings.
     * 
     * @param settings profiling settings
     * @return expected profiling overhead imposed by the provided profiling settings
     */
    public float getProfilingOverhead(ProfilingSettings settings) {
        return provider.getProfilingOverhead(settings);
    }

    /**
     * Returns SettingsCustomizer instance for a project.
     * 
     * @return SettingsCustomizer instance for a project
     */
    public SettingsCustomizer getSettingsCustomizer() {
        return provider.getSettingsCustomizer();
    }

//    public SettingsConfigurator getSettingsConfigurator() {
//        return provider.getSettingsConfigurator();
//    }
    
    public String getProjectOnlyFilterName() {
        return provider.getProjectOnlyFilterName();
    }
    
    public String getProjectSubprojectsFilterName() {
        return provider.getProjectSubprojectsFilterName();
    }
    
    
    private ProfilingSettingsSupport(ProfilingSettingsSupportProvider provider) {
        this.provider = provider;
    }
    
    private static synchronized ProfilingSettingsSupport defaultImpl() {
        if (DEFAULT == null)
            DEFAULT = new ProfilingSettingsSupport(new ProfilingSettingsSupportProvider.Basic(null));
        return DEFAULT;
    }
    

    /**
     * Returns ProfilingSettingsSupport instance for the provided project.
     * 
     * @param project project
     * @return ProfilingSettingsSupport instance for the provided project
     */
    public static ProfilingSettingsSupport get(Lookup.Provider project) {
        ProfilingSettingsSupportProvider provider =
                project != null ? project.getLookup().lookup(ProfilingSettingsSupportProvider.class) : null;
        if (provider == null) return defaultImpl();
        else return new ProfilingSettingsSupport(provider);
    }
    
    
    /**
     * Support for adding custom settings component into the Select Profiling Task dialog.
     */
    public static abstract class SettingsCustomizer {
        
        /**
         * Returns custom settings component to be displayed in the Select Profiling Task dialog.
         * 
         * @param isAttach true if Select Profiling Task dialog is displayed for profiler attach, false for profile project
         * @param isModify true if Select Profiling Task dialog is displayed for modify profiling, false otherwise
         * @return custom settings component to be displayed in the Select Profiling Task dialog
         */
        public abstract JPanel getCustomSettingsPanel(boolean isAttach, boolean isModify);
        
        /**
         * Loads custom settings from project's properties.
         * 
         * @param properties properties
         */
        public abstract void loadCustomSettings(Properties properties);
        
        /**
         * Stores custom settings into project's properties.
         * 
         * @param properties properties
         */
        public abstract void storeCustomSettings(Properties properties);
        
    }
    
}
