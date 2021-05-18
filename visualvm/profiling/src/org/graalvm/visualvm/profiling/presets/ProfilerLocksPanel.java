/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class ProfilerLocksPanel extends JPanel {

    private boolean internalChange;

    public ProfilerLocksPanel() {
        this(false);
    }

    ProfilerLocksPanel(boolean mnemonics) {
        initComponents(mnemonics);
    }

    public ProfilingSettings getSettings() {
        ProfilingSettings settings = ProfilerIDESettings.getInstance().createDefaultProfilingSettings();
        settings.setProfilingType(ProfilingSettings.PROFILE_MONITOR);
        settings.setLockContentionMonitoringEnabled(true);
        return settings;
    }

    public boolean settingsValid() {
        return true;
    }

    public void loadFromPreset(ProfilerPreset preset) {
        if (preset == null) {
            return;
        }

        internalChange = true;
        internalChange = false;
    }

    public void saveToPreset(ProfilerPreset preset) {
        if (preset == null) {
            return;
        }
    }

    public abstract void settingsChanged();

    private void syncUI() {
        if (internalChange) {
            return;
        }
        settingsChanged();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component c : getComponents()) {
            c.setEnabled(enabled);
        }
    }

    private void initComponents(boolean mnemonics) {
        setOpaque(false);
        setLayout(new GridBagLayout());

    }
}
