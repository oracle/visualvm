/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.common;

import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
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

        CPUPreset(int type) {
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
            } else if (type == ProfilingSettings.PROFILE_CPU_JDBC) {
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

        MemoryPreset(int type) {
            setIsPreset(true);
            setProfilingType(type);
            setSettingsName(MEMORY_PRESET_NAME);
        }
    }

    private static final class MonitorPreset extends ProfilingSettings {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        MonitorPreset() {
            setIsPreset(true);
            setProfilingType(ProfilingSettings.PROFILE_MONITOR);
            setSettingsName(MONITOR_PRESET_NAME);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org.graalvm.visualvm.lib.common.Bundle"); // NOI18N
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
