/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.core.properties;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Jiri Sedlacek
 */
public final class PropertiesCustomizer<X extends DataSource> extends PropertiesPanel {

    private final X dataSource;
    private final List<PropertiesProvider<X>> providers;
    private final List<PropertiesPanel> panels;
    private final ChangeListener listener = new ChangeListener() {
                      public void stateChanged(ChangeEvent e) { update(); }
                  };


    PropertiesCustomizer(X dataSource, Class<X> type) {
        this.dataSource = dataSource;

        providers = PropertiesSupport.sharedInstance().getProviders(dataSource, type);
        panels = new ArrayList();
        for (PropertiesProvider provider : providers)
            panels.add(provider.createPanel(dataSource));
        
        initComponents();
        update();
        registerListeners();
    }


    public boolean hasProperties() {
        return !providers.isEmpty();
    }

    public void settingsDefined(X dataSource) {
        unregisterListeners();
        for (int i = 0; i < providers.size(); i++) {
            PropertiesProvider<X> provider = providers.get(i);
            PropertiesPanel panel = panels.get(i);
            provider.propertiesDefined(panel, dataSource);
        }
    }

    void settingsChanged() {
        unregisterListeners();
        for (int i = 0; i < providers.size(); i++) {
            PropertiesProvider<X> provider = providers.get(i);
            PropertiesPanel panel = panels.get(i);
            provider.propertiesChanged(panel, dataSource);
        }
    }

    public void settingsCancelled() {
        unregisterListeners();
        for (int i = 0; i < providers.size(); i++) {
            PropertiesProvider<X> provider = providers.get(i);
            PropertiesPanel panel = panels.get(i);
            provider.propertiesCancelled(panel, dataSource);
        }
    }


    private void registerListeners() {
        for (PropertiesPanel panel : panels)
            panel.addChangeListener(listener);
    }

    private void unregisterListeners() {
        for (PropertiesPanel panel : panels)
            panel.removeChangeListener(listener);
    }

    private void update() {
        for (PropertiesPanel panel : panels)
            if (!panel.settingsValid()) {
                setSettingsValid(false);
                return;
            }
        setSettingsValid(true);
    }

    private void initComponents() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);
        for (int i = 0; i < providers.size(); i++) {
            PropertiesProvider provider = providers.get(i);
            PropertiesPanel panel = panels.get(i);
            ScrollableContainer c = new ScrollableContainer(panel);
            c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            c.setViewportBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            tabbedPane.addTab(provider.getPropertiesName(), null, c,
                              provider.getPropertiesDescription());
        }

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

}
