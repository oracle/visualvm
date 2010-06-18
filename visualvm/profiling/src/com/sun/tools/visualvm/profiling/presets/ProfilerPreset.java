/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.profiling.presets;

import com.sun.tools.visualvm.core.datasource.Storage;

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
        this.stacksP = false;
    }

    ProfilerPreset(ProfilerPreset preset) {
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
    }

    ProfilerPreset(Storage storage) {
        valid = true;
        name = value(storage, PROP_NAME, "Preset"); // NOI18N
        selector = value(storage, PROP_SELECTOR, ""); // NOI18N
        filterModeS = Boolean.parseBoolean(value(storage, PROP_FILTER_MODE_S, "True")); // NOI18N
        filterS = value(storage, PROP_FILTER_S, ""); // NOI18N
        try { samplingRateS = Integer.parseInt(value(storage, PROP_SAMPLING_RATE_S, "100")); } // NOI18N
        catch (NumberFormatException e) { samplingRateS = 100; }
        try { refreshRateS = Integer.parseInt(value(storage, PROP_REFRESH_RATE_S, "1000")); } // NOI18N
        catch (NumberFormatException e) { refreshRateS = 1000; }
        try { samplingRefreshRateS = Integer.parseInt(value(storage, PROP_SAMPLING_REFRESH_RATE_S, "1000")); } // NOI18N
        catch (NumberFormatException e) { samplingRefreshRateS = 1000; }
        rootsP = value(storage, PROP_ROOTS_P, ""); // NOI18N
        runnablesP = Boolean.parseBoolean(value(storage, PROP_RUNNABLES_P, "True")); // NOI18N
        filterModeP = Boolean.parseBoolean(value(storage, PROP_FILTER_MODE_P, "True")); // NOI18N
        filterP = value(storage, PROP_FILTER_P, ""); // NOI18N
        memoryModeP = Boolean.parseBoolean(value(storage, PROP_MEMORY_MODE_P, "True")); // NOI18N
        try { allocP = Integer.parseInt(value(storage, PROP_ALLOC_P, "10")); } // NOI18N
        catch (NumberFormatException e) { allocP = 10; }
        stacksP = Boolean.parseBoolean(value(storage, PROP_STACKS_P, "False")); // NOI18N
    }


    private static String value(Storage storage, String property, String fallback) {
        String value = storage.getCustomProperty(property);
        if (value == null) value = fallback;
        return value;
    }

    void toStorage(Storage storage) {
        String[] properties = new String[] {
            PROP_NAME, PROP_SELECTOR, PROP_FILTER_MODE_S, PROP_FILTER_S,
            PROP_SAMPLING_RATE_S, PROP_REFRESH_RATE_S, PROP_SAMPLING_REFRESH_RATE_S,
            PROP_ROOTS_P, PROP_RUNNABLES_P, PROP_FILTER_MODE_P, PROP_FILTER_P,
            PROP_MEMORY_MODE_P, PROP_ALLOC_P, PROP_STACKS_P
        };
        String[] values = new String[] {
            name, selector, Boolean.toString(filterModeS), filterS,
            Integer.toString(samplingRateS), Integer.toString(refreshRateS),
            Integer.toString(samplingRefreshRateS), rootsP, Boolean.toString(runnablesP),
            Boolean.toString(filterModeP), filterP, Boolean.toString(memoryModeP),
            Integer.toString(allocP), Boolean.toString(stacksP)
        };
        storage.setCustomProperties(properties, values);
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

    public String toString() {
        return getName();
    }

}
