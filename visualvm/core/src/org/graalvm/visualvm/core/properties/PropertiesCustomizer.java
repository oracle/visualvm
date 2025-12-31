/*
 * Copyright (c) 2007, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.properties;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.core.ui.components.Spacer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * UI component for presenting user-customizable properties of a DataSource.
 * This class is to be used by DataSource providers supporting defining the
 * initial DataSource properties. Use PropertiesSupport.getCustomizer(Class)
 * method to get an instance of PropertiesCustomizer for a concrete DataSource
 * type.
 *
 * @author Jiri Sedlacek
 */
public final class PropertiesCustomizer<X extends DataSource> extends PropertiesPanel {

    private final X dataSource;
    private final List<List<PropertiesProvider<X>>> groups;
    private final List<PropertiesPanel> panels;
    private final ChangeListener listener = new ChangeListener() {
                      public void stateChanged(ChangeEvent e) { update(); }
                  };

    private JTabbedPane tabbedPane;
    private final Map<Integer, Integer> categories;


    PropertiesCustomizer(X dataSource, Class<X> type) {
        this.dataSource = dataSource;

        groups = createGroups(PropertiesSupport.sharedInstance().getProviders(dataSource, type));
        panels = createPanels(groups, dataSource);

        categories = new HashMap<>();
        
        initComponents();
        update();
        registerListeners();
    }


    /**
     * Invokes PropertiesProvider.propertiesDefined method for every
     * PropertiesProvider supporting the provided DataSource. To be called by
     * DataSource providers when the New DataSource dialog displaying editable
     * properties has been accepted by the user and new DataSource has been
     * created.
     *
     * @param dataSource newly created DataSource
     */
    public void propertiesDefined(X dataSource) {
        unregisterListeners();
        for (int i = 0; i < groups.size(); i++) {
            List<PropertiesProvider<X>> providers = groups.get(i);
            List<PropertiesPanel> categoriesPanels = new ArrayList<>();
            if (providers.size() == 1) {
                categoriesPanels.add(panels.get(i));
            } else {
                MultiPropertiesPanel multiPanel = (MultiPropertiesPanel)panels.get(i);
                categoriesPanels.addAll(multiPanel.getPanels());
            }
            for (int j = 0; j < providers.size(); j++)
                providers.get(j).propertiesDefined(categoriesPanels.get(j), dataSource);
        }
    }

    /**
     * Invokes PropertiesProvider.propertiesCancelled method for every
     * PropertiesProvider supporting the DataSource type defined for this
     * PropertiesCustomizer. To be called by DataSource providers when the New
     * DataSource dialog displaying editable properties has been cancelled and
     * no DataSource has been created.
     */
    public void propertiesCancelled() {
        unregisterListeners();
        for (int i = 0; i < groups.size(); i++) {
            List<PropertiesProvider<X>> providers = groups.get(i);
            List<PropertiesPanel> categoriesPanels = new ArrayList<>();
            if (providers.size() == 1) {
                categoriesPanels.add(panels.get(i));
            } else {
                MultiPropertiesPanel multiPanel = (MultiPropertiesPanel)panels.get(i);
                categoriesPanels.addAll(multiPanel.getPanels());
            }
            for (int j = 0; j < providers.size(); j++)
                providers.get(j).propertiesCancelled(categoriesPanels.get(j), dataSource);
        }
    }
    

    void propertiesChanged() {
        unregisterListeners();
        for (int i = 0; i < groups.size(); i++) {
            List<PropertiesProvider<X>> providers = groups.get(i);
            List<PropertiesPanel> categoriesPanels = new ArrayList<>();
            if (providers.size() == 1) {
                categoriesPanels.add(panels.get(i));
            } else {
                MultiPropertiesPanel multiPanel = (MultiPropertiesPanel)panels.get(i);
                categoriesPanels.addAll(multiPanel.getPanels());
            }
            for (int j = 0; j < providers.size(); j++)
                providers.get(j).propertiesChanged(categoriesPanels.get(j), dataSource);
        }
    }


    void selectCategory(int category) {
        Integer tabIndex = categories.get(category);
        if (tabIndex != null) tabbedPane.setSelectedIndex(tabIndex);
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
        tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);
        for (int i = 0; i < panels.size(); i++) {
            PropertiesPanel panel = panels.get(i);
            PropertiesProvider provider = groups.get(i).get(0);
            ScrollableContainer c = new ScrollableContainer(panel);
            c.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            c.setViewportBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            categories.put(provider.getPropertiesCategory(), tabbedPane.getTabCount());
            tabbedPane.addTab(provider.getPropertiesName(), null, c,
                              provider.getPropertiesDescription());
        }

        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }


    private static <Y extends DataSource> List<List<PropertiesProvider<Y>>>
            createGroups(List<PropertiesProvider<Y>> providers) {
        
        providers.sort(new CategoriesComparator());

        List<List<PropertiesProvider<Y>>> groupedProviders = new ArrayList<>();
        int currentCategory = -1;
        List<PropertiesProvider<Y>> currentGroup = null;
        for (PropertiesProvider<Y> provider : providers) {
            int providerCategory = provider.getPropertiesCategory();
            if (currentGroup == null || providerCategory != currentCategory) {
                currentCategory = providerCategory;
                if (currentGroup != null)
                    Collections.sort(currentGroup, Positionable.COMPARATOR);
                currentGroup = new ArrayList<>();
                groupedProviders.add(currentGroup);
            }
            if (currentGroup != null && currentCategory == providerCategory)
                currentGroup.add(provider);
        }
        if (currentGroup != null)
            Collections.sort(currentGroup, Positionable.COMPARATOR);

        return groupedProviders;
    }

    private static <Y extends DataSource> List<PropertiesPanel>
            createPanels(List<List<PropertiesProvider<Y>>> groups, Y dataSource) {

        List<PropertiesPanel> panels = new ArrayList<>(groups.size());

        for (List<PropertiesProvider<Y>> group : groups)
            if (group.size() == 1)
                panels.add(group.get(0).createPanel(dataSource));
            else
                panels.add(new MultiPropertiesPanel(group, dataSource));

        return panels;
    }


    private static class MultiPropertiesPanel<Y extends DataSource> extends PropertiesPanel {

        private static final Color separatorColor = separatorColor();
        private static final Font separatorFont = separatorFont();

        private final List<PropertiesPanel> panels;
        private final ChangeListener listener = new ChangeListener() {
                          public void stateChanged(ChangeEvent e) { update(); }
                      };


        MultiPropertiesPanel(List<PropertiesProvider<Y>> providers, Y dataSource) {
            panels = new ArrayList<>(providers.size());
            for (PropertiesProvider provider : providers)
                panels.add(provider.createPanel(dataSource));
            
            initComponents(providers);
            update();
        }


        public List<PropertiesPanel> getPanels() {
            return panels;
        }

        
        private void update() {
            boolean valid = true;
            for (PropertiesPanel panel : panels)
                if (!panel.settingsValid()) {
                    valid = false;
                    break;
                }
            setSettingsValid(valid);
        }

        private void initComponents(List<PropertiesProvider<Y>> providers) {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setLayout(new GridBagLayout());

            int currentRow = 0;
            int providerIndex = 1;
            GridBagConstraints constraints;

            for (PropertiesPanel panel : panels) {
                constraints = createConstraints(currentRow++);
                add(panel, constraints);
                panel.addChangeListener(listener);

                if (providerIndex < providers.size()) {
                    PropertiesProvider provider = providers.get(providerIndex++);
                    SectionSeparator separator = new SectionSeparator(provider.getPropertiesName());
                    separator.setForeground(separatorColor);
                    separator.setFont(separatorFont);
                    separator.setToolTipText(provider.getPropertiesDescription());
                    constraints = createConstraints(currentRow++);
                    constraints.insets = new Insets(10, 0, 5, 0);
                    add(separator, constraints);
                }
            }

            constraints = createConstraints(currentRow++);
            constraints.weightx = 1;
            constraints.weighty = 1;
            add(Spacer.create(), constraints);
        }

        private static GridBagConstraints createConstraints(int row) {
            GridBagConstraints c = new GridBagConstraints();

            c.gridx = 0;
            c.gridy = row;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;

            return c;
        }
        
    }

    private static Color separatorColor() {
        Color color = UIManager.getColor("TitledBorder.titleColor"); // NOI18N
        if (color == null) color = new JLabel().getForeground();
        return color;
    }

    private static Font separatorFont() {
        Font font = UIManager.getFont("TitledBorder.font"); // NOI18N
        if (font == null) font = new JLabel().getFont();
        return font;
    }
    
    private static class CategoriesComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            PropertiesProvider p1 = (PropertiesProvider)o1;
            PropertiesProvider p2 = (PropertiesProvider)o2;

            int category1 = p1.getPropertiesCategory();
            int category2 = p2.getPropertiesCategory();

            if (category1 > category2) return 1;
            if (category1 < category2) return -1;
            return p1.getPropertiesName().compareTo(p2.getPropertiesName());
        }

    }

}
