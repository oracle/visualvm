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

package org.graalvm.visualvm.sampler.cpu;

import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.profiling.presets.PresetSelector;
import org.graalvm.visualvm.profiling.presets.SamplerCPUPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.common.ProfilingSettings;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class CPUSettingsSupport {
    
    private JPanel container;
    private SamplerCPUPanel panel;
    private PresetSelector selector;
    
    private DataViewComponent.DetailsView detailsView;
    
    
    public DataViewComponent.DetailsView getDetailsView() {
        if (detailsView == null) {
            detailsView = new DataViewComponent.DetailsView(NbBundle.getMessage(
                          CPUSettingsSupport.class, "LBL_Cpu_settings"), null, 10, // NOI18N
                          new ScrollableContainer(createPanel()), null);
        }
        return detailsView;
    }
    
    
    public ProfilingSettings getSettings() { return panel.getSettings(); }

    public int getSamplingRate() { return panel.getSamplingRate(); }
    
    public int getRefreshRate() { return panel.getRefreshRate(); }
    
    public void saveSettings() {
        // NOTE: might save custom configuration here
    }
    
    public abstract boolean presetValid();
    
    public boolean settingsValid() { return panel.settingsValid(); }
    
    public void showSettings(DataViewComponent dvc) {
        dvc.selectDetailsView(getDetailsView());
    }
    
    public abstract PresetSelector createSelector(Runnable presetSynchronizer);
    
    
    public void setEnabled(boolean enabled) {
        if (container != null) container.setEnabled(enabled);
    }
    
    private JPanel createPanel() {
        panel = new SamplerCPUPanel() {
            public void settingsChanged() {
                panel.saveToPreset(selector.customize(presetValid()));
            }
        };
        
        selector = createSelector(new Runnable() {
            public void run() { panel.loadFromPreset(selector.getSelectedPreset()); }
        });
        selector.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        container = new JPanel(new BorderLayout()) {
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                for (Component c : getComponents()) c.setEnabled(enabled);
            }
        };
        container.setOpaque(false);
        container.add(panel, BorderLayout.CENTER);
        container.add(selector, BorderLayout.SOUTH);
        
        return container;
    }

}
