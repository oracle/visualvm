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
 *
 * @author Jaroslav Bachorik
 */
final public class ProfilingSettingsManager {
    final private static class Singleton {
        final private static ProfilingSettingsManager INSTANCE = new ProfilingSettingsManager();
    }
    
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

    private ProfilingSettingsManagerProvider impl = null;
    private ProfilingSettingsManager() {
        impl = Lookup.getDefault().lookup(ProfilingSettingsManagerProvider.class);
    }
    
    public static ProfilingSettingsManager getDefault() {
        return Singleton.INSTANCE;
    }
    
    public ProfilingSettings createDuplicateSettings(ProfilingSettings originalSettings, ProfilingSettings[] availableConfigurations) {
        return impl.createDuplicateSettings(originalSettings, availableConfigurations);
    }

    public ProfilingSettings createNewSettings(ProfilingSettings[] availableConfigurations) {
        return impl.createNewSettings(availableConfigurations);
    }

    public  ProfilingSettings createNewSettings(int type, ProfilingSettings[] availableConfigurations) {
        return impl.createNewSettings(type, availableConfigurations);
    }

    public ProfilingSettingsDescriptor getProfilingSettings(Lookup.Provider project) {
        return impl.getProfilingSettings(project);
    }

    public ProfilingSettings renameSettings(ProfilingSettings originalSettings, ProfilingSettings[] availableConfigurations) {
        return impl.renameSettings(originalSettings, availableConfigurations);
    }

    public void storeProfilingSettings(ProfilingSettings[] profilingSettings, ProfilingSettings lastSelectedProfilingSettings, Lookup.Provider project) {
        impl.storeProfilingSettings(profilingSettings, lastSelectedProfilingSettings, project);
    }
}
