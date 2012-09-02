/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.lib.profiler.common;

import org.netbeans.lib.profiler.global.CommonConstants;
import java.util.ResourceBundle;


/**
 * Factory class to create preset ProfilingSettings
 *
 * @author Jiri Sedlacek
 */
public class ProfilingSettingsPresets {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static final class CPUPreset extends ProfilingSettings {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public CPUPreset(int type) {
            setIsPreset(true);
            setProfilingType(type);
            setSettingsName(CPU_PRESET_NAME);

            setCPUProfilingType(type == ProfilingSettings.PROFILE_CPU_SAMPLING ?
                    CommonConstants.CPU_SAMPLED : CommonConstants.CPU_INSTR_FULL);
            setInstrumentGetterSetterMethods(false);
            setInstrumentEmptyMethods(false);
            setInstrumentMethodInvoke(true);
            setExcludeWaitTime(true);

            if (type == ProfilingSettings.PROFILE_CPU_ENTIRE) {
                setInstrScheme(CommonConstants.INSTRSCHEME_TOTAL);
                //        setInstrumentSpawnedThreads(true);
                setInstrumentSpawnedThreads(false); // Should work better with Marker Methods
            } else if (type == ProfilingSettings.PROFILE_CPU_PART) {
                setInstrScheme(CommonConstants.INSTRSCHEME_LAZY);
                setInstrumentSpawnedThreads(false);
            } else if (type == ProfilingSettings.PROFILE_CPU_SAMPLING) {
                setSamplingFrequency(10);
                setThreadCPUTimerOn(true);
            }
        }
    }

    private static final class MemoryPreset extends ProfilingSettings {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public MemoryPreset(int type) {
            setIsPreset(true);
            setProfilingType(type);
            setSettingsName(MEMORY_PRESET_NAME);
        }
    }

    private static final class MonitorPreset extends ProfilingSettings {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public MonitorPreset() {
            setIsPreset(true);
            setProfilingType(ProfilingSettings.PROFILE_MONITOR);
            setSettingsName(MONITOR_PRESET_NAME);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.Bundle"); // NOI18N
    private static final String MONITOR_PRESET_NAME = bundle.getString("ProfilingSettingsPresets_MonitorPresetName"); // NOI18N
    private static final String CPU_PRESET_NAME = bundle.getString("ProfilingSettingsPresets_CpuPresetName"); // NOI18N
    private static final String MEMORY_PRESET_NAME = bundle.getString("ProfilingSettingsPresets_MemoryPresetName"); // NOI18N
                                                                                                                    // -----

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static ProfilingSettings createCPUPreset() {
        return createCPUPreset(ProfilingSettings.PROFILE_CPU_SAMPLING);
    }

    public static ProfilingSettings createCPUPreset(int type) {
        return new CPUPreset(type);
    }

    public static ProfilingSettings createMemoryPreset() {
        return createMemoryPreset(ProfilingSettings.PROFILE_MEMORY_SAMPLING);
    }

    public static ProfilingSettings createMemoryPreset(int type) {
        return new MemoryPreset(type);
    }

    public static ProfilingSettings createMonitorPreset() {
        return new MonitorPreset();
    }
}
