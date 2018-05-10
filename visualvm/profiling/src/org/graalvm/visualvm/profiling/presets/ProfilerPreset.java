/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiling.presets;

import java.util.prefs.Preferences;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerPreset {

    private static final String PROP_NAME = "prof_preset_name"; // NOI18N
    private static final String PROP_SELECTOR = "prof_preset_selector"; // NOI18N
    private static final String PROP_FILTER_MODE_S = "prof_preset_filterMode_s"; // NOI18N
    private static final String PROP_FILTER_S = "prof_preset_filter_s"; // NOI18N
    private static final String PROP_SAMPLING_RATE_S = "prof_preset_samplingRate_s"; // NOI18N
    private static final String PROP_REFRESH_RATE_S = "prof_preset_refreshRate_s"; // NOI18N
    private static final String PROP_SAMPLING_REFRESH_RATE_S = "prof_preset_samplingRefreshRate_s"; // NOI18N
    private static final String PROP_ROOTS_P = "prof_preset_roots_p"; // NOI18N
    private static final String PROP_RUNNABLES_P = "prof_preset_runnables_p"; // NOI18N
    private static final String PROP_FILTER_MODE_P = "prof_preset_filterMode_p"; // NOI18N
    private static final String PROP_FILTER_P = "prof_preset_filter_p"; // NOI18N
    private static final String PROP_MEMORY_MODE_P = "prof_preset_memoryMode_p"; // NOI18N
    private static final String PROP_ALLOC_P = "prof_preset_alloc_p"; // NOI18N
    private static final String PROP_STACKS_P = "prof_preset_stacks_p"; // NOI18N
    private static final String PROP_MEMORY_FILTER_P = "prof_memory_filter_p"; // NOI18N
    private static final String PROP_JDBC_FILTER_P = "prof_jdbc_filter_p"; // NOI18N

    private boolean valid;
    private String name;
    private String selector;
    private boolean filterModeS;
    private String filterS;
    private int samplingRateS;
    private int refreshRateS;
    private int samplingRefreshRateS;
    private String rootsP;
    private boolean runnablesP;
    private boolean filterModeP;
    private String filterP;
    private boolean memoryModeP;
    private int allocP;
    private boolean stacksP;
    private String memoryFilterP;
    private String jdbcFilterP;


    public ProfilerPreset(String name, String selector) {
        this.valid = true;
        this.name = name;
        this.selector = selector;
        this.filterModeS = true;
        this.filterS = ""; // NOI18N
        this.samplingRateS = 100;
        this.refreshRateS = 1000;
        this.samplingRefreshRateS = 1000;
        this.rootsP = ""; // NOI18N
        this.runnablesP = true;
        this.filterModeP = true;
        this.filterP = ""; // NOI18N
        this.memoryModeP = true;
        this.allocP = 10;
        this.stacksP = true;
        this.memoryFilterP = ""; // NOI18N
        this.jdbcFilterP = ""; // NOI18N
    }

    public ProfilerPreset(ProfilerPreset preset) {
        this.valid = preset.valid;
        this.name = preset.name;
        this.selector = preset.selector;
        this.filterModeS = preset.filterModeS;
        this.filterS = preset.filterS;
        this.samplingRateS = preset.samplingRateS;
        this.refreshRateS = preset.refreshRateS;
        this.samplingRefreshRateS = preset.samplingRefreshRateS;
        this.rootsP = preset.rootsP;
        this.runnablesP = preset.runnablesP;
        this.filterModeP = preset.filterModeP;
        this.filterP = preset.filterP;
        this.memoryModeP = preset.memoryModeP;
        this.allocP = preset.allocP;
        this.stacksP = preset.stacksP;
        this.memoryFilterP = preset.memoryFilterP;
        this.jdbcFilterP = preset.jdbcFilterP;
    }

    ProfilerPreset(Preferences prefs, String prefix) {
        valid = true;
        name = prefs.get(prefix + PROP_NAME, NbBundle.getMessage(ProfilerPreset.class, "MSG_Preset")); // NOI18N
        selector = prefs.get(prefix + PROP_SELECTOR, ""); // NOI18N
        filterModeS = prefs.getBoolean(prefix + PROP_FILTER_MODE_S, true);
        filterS = prefs.get(prefix + PROP_FILTER_S, ""); // NOI18N
        samplingRateS = prefs.getInt(prefix + PROP_SAMPLING_RATE_S, 100);
        refreshRateS = prefs.getInt(prefix + PROP_REFRESH_RATE_S, 1000);
        samplingRefreshRateS = prefs.getInt(prefix + PROP_SAMPLING_REFRESH_RATE_S, 1000);
        rootsP = prefs.get(prefix + PROP_ROOTS_P, ""); // NOI18N
        runnablesP = prefs.getBoolean(prefix + PROP_RUNNABLES_P, true);
        filterModeP = prefs.getBoolean(prefix + PROP_FILTER_MODE_P, true);
        filterP = prefs.get(prefix + PROP_FILTER_P, ""); // NOI18N
        memoryModeP = prefs.getBoolean(prefix + PROP_MEMORY_MODE_P, true);
        allocP = prefs.getInt(prefix + PROP_ALLOC_P, 10);
        stacksP = prefs.getBoolean(prefix + PROP_STACKS_P, true);
        memoryFilterP = prefs.get(prefix + PROP_MEMORY_FILTER_P, ""); // NOI18N
        jdbcFilterP = prefs.get(prefix + PROP_JDBC_FILTER_P, ""); // NOI18N
    }
    

    void toPreferences(Preferences prefs, String prefix) {
        prefs.put(prefix + PROP_NAME, name);
        prefs.put(prefix + PROP_SELECTOR, selector);
        prefs.putBoolean(prefix + PROP_FILTER_MODE_S, filterModeS);
        prefs.put(prefix + PROP_FILTER_S, filterS);
        prefs.putInt(prefix + PROP_SAMPLING_RATE_S, samplingRateS);
        prefs.putInt(prefix + PROP_REFRESH_RATE_S, refreshRateS);
        prefs.putInt(prefix + PROP_SAMPLING_REFRESH_RATE_S, samplingRefreshRateS);
        prefs.put(prefix + PROP_ROOTS_P, rootsP);
        prefs.putBoolean(prefix + PROP_RUNNABLES_P, runnablesP);
        prefs.putBoolean(prefix + PROP_FILTER_MODE_P, filterModeP);
        prefs.put(prefix + PROP_FILTER_P, filterP);
        prefs.putBoolean(prefix + PROP_MEMORY_MODE_P, memoryModeP);
        prefs.putInt(prefix + PROP_ALLOC_P, allocP);
        prefs.putBoolean(prefix + PROP_STACKS_P, stacksP);
        prefs.put(prefix + PROP_MEMORY_FILTER_P, memoryFilterP);
        prefs.put(prefix + PROP_JDBC_FILTER_P, jdbcFilterP);
    }


    void setValid(boolean valid) {
        this.valid = valid;
    }
    
    boolean isValid() {
        return valid;
    }
    

    public void setName(String name) {
        if (name != null && !name.isEmpty()) this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getSelector() {
        return selector;
    }

    public void setFilterModeS(boolean filterMode) {
        this.filterModeS = filterMode;
    }

    public boolean getFilterModeS() {
        return filterModeS;
    }

    public void setFilterS(String filter) {
        this.filterS = filter;
    }

    public String getFilterS() {
        return filterS;
    }

    public void setSamplingRateS(int samplingRate) {
        this.samplingRateS = samplingRate;
    }

    public int getSamplingRateS() {
        return samplingRateS;
    }

    public void setRefreshRateS(int refreshRate) {
        this.refreshRateS = refreshRate;
    }

    public int getRefreshRateS() {
        return refreshRateS;
    }

    public void setSamplingRefreshRateS(int samplingRefreshRateS) {
        this.samplingRefreshRateS = samplingRefreshRateS;
    }

    public int getSamplingRefreshRateS() {
        return samplingRefreshRateS;
    }

    public void setRootsP(String roots) {
        this.rootsP = roots;
    }

    public String getRootsP() {
        return rootsP;
    }

    public void setRunnablesP(boolean runnables) {
        this.runnablesP = runnables;
    }

    public boolean getRunnablesP() {
        return runnablesP;
    }

    public void setFilterModeP(boolean filterMode) {
        this.filterModeP = filterMode;
    }

    public boolean getFilterModeP() {
        return filterModeP;
    }

    public void setFilterP(String filter) {
        this.filterP = filter;
    }

    public String getFilterP() {
        return filterP;
    }

    public void setMemoryModeP(boolean memoryMode) {
        this.memoryModeP = memoryMode;
    }

    public boolean getMemoryModeP() {
        return memoryModeP;
    }

    public void setAllocP(int alloc) {
        this.allocP = alloc;
    }

    public int getAllocP() {
        return allocP;
    }

    public void setStacksP(boolean stacks) {
        this.stacksP = stacks;
    }

    public boolean getStacksP() {
        return stacksP;
    }
    
    public void setMemoryFilterP(String filter) {
        this.memoryFilterP = filter;
    }

    public String getMemoryFilterP() {
        return memoryFilterP;
    }
    
    public void setJDBCFilterP(String filter) {
        this.jdbcFilterP = filter;
    }

    public String getJDBCFilterP() {
        return jdbcFilterP;
    }

    public String toString() {
        return getName();
    }
    
    public boolean equals(Object o) {
        return o instanceof ProfilerPreset ? getName().equals(((ProfilerPreset)o).getName()) : false;
    }
    
    public int hashCode() {
        return getName().hashCode();
    }

}
