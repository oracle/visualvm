/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.graalvm.visualvm.profiling.presets.ProfilingOptionsSectionProvider;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service = ProfilingOptionsSectionProvider.class)
@NbBundle.Messages({
    "CAP_SectionName=Sampler Initialization",
    "BTN_InitializeAutomatically=&Initialize Sampler automatically when displayed"
})
public final class SamplerInitialization extends ProfilingOptionsSectionProvider {
    
    public static final String PROP_INITIALIZE_AUTOMATICALLY = "SamplerInitialization.initializeAutomatically"; // NOI18N
    
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    
    private final Preferences prefs;
    
    private JCheckBox initializeAutomatically;
    
    private Runnable changedToAutomatic;
    
    public SamplerInitialization() {
        prefs = NbPreferences.forModule(SamplerInitialization.class);
    }
    
    public static SamplerInitialization getInstance() {
        return Lookup.getDefault().lookup(SamplerInitialization.class);
    }

    public String getSectionName() {
        return Bundle.CAP_SectionName();
    }

    public Component getSection() {
        initializeAutomatically = new JCheckBox(null, null, isAutomatic());
        Mnemonics.setLocalizedText(initializeAutomatically, Bundle.BTN_InitializeAutomatically());
        
        initializeAutomatically.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                changed();
            }
        });
        
        JPanel container = new JPanel(new BorderLayout());
        container.add(initializeAutomatically, BorderLayout.WEST);
        
        return container;
    }
    
    protected void load() {
        initializeAutomatically.setSelected(isAutomatic());
    }

    protected void store() {
        if (initializeAutomatically != null) {
            setAutomatic(initializeAutomatically.isSelected());
        }
    }

    protected void closed() {
        changedToAutomatic = null;
    }
    
    boolean isAutomatic() {
        return prefs.getBoolean(PROP_INITIALIZE_AUTOMATICALLY, true);
    }
    
    private void setAutomatic(boolean automatic) {
        boolean orig = isAutomatic();
        if (orig != automatic) {
            prefs.putBoolean(PROP_INITIALIZE_AUTOMATICALLY, automatic);
            changeSupport.firePropertyChange(PROP_INITIALIZE_AUTOMATICALLY, orig, automatic);
            if (automatic && changedToAutomatic != null) changedToAutomatic.run();
        }
    }
    
    void runIfChangedToAutomatic(Runnable changedToAutomatic) {
        this.changedToAutomatic = changedToAutomatic;
    }
    
    public void addChangeListener(String property, PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(property, listener);
    }
    
    public void removeChangeListener(String property, PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(property, listener);
    }
    
}
