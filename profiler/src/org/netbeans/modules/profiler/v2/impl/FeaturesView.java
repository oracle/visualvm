/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.profiler.v2.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.v2.ProfilerFeature;

/**
 *
 * @author Jiri Sedlacek
 */
public class FeaturesView extends JPanel {
    
    private JTabbedPane tabs;
    private final Component defaultView;
    
    private final Set<ChangeListener> listeners = new HashSet();
    
    
    public FeaturesView(Component defaultView) {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new BorderLayout(0, 0));
        
        if (defaultView != null) {
            JScrollPane sp = new JScrollPane(defaultView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
            };
            sp.setBorder(null);
            sp.setViewportBorder(null);

            this.defaultView = sp;
            add(this.defaultView, BorderLayout.CENTER);
        } else {
            this.defaultView = null;
        }
    }
    
    
    public final void addFeature(ProfilerFeature feature) {
        if (tabs == null) {
            if (defaultView != null) remove(defaultView);
            tabs = createTabs();
            add(tabs, BorderLayout.CENTER);
        }
        
        JPanel container = createContainer(feature);
        tabs.addTab(feature.getName(), feature.getIcon(), container, null);
        
        doLayout();
        repaint();
        fireViewOrIndexChanged();
    }
    
    private static JPanel createContainer(ProfilerFeature feature) {
        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.putClientProperty(ProfilerFeature.class, feature);
        
        JPanel results = feature.getResultsUI();
        container.add(results, BorderLayout.CENTER);
        
        JPanel settings = feature.getSettingsUI();
        if (settings != null) {
            JPanel pan = new JPanel(new BorderLayout(0, 0)) {
                public void setVisible(boolean visible) {
                    super.setVisible(visible);
                    for (Component c : getComponents()) c.setVisible(visible);
                }
            };
            pan.setOpaque(true);
            pan.setBackground(UIUtils.getProfilerResultsBackground());
            pan.add(settings, BorderLayout.CENTER);
            JSeparator sep = UIUtils.createHorizontalLine(pan.getBackground());
            pan.add(sep, BorderLayout.SOUTH);
            pan.setVisible(settings.isVisible());
            container.add(pan, BorderLayout.NORTH);
        }
        
        return container;
    }
    
    public final void removeFeature(ProfilerFeature feature) {
        if (tabs != null) {
            tabs.remove(feature.getResultsUI());
            doLayout();
            repaint();
            if (tabs.getTabCount() == 0) removeFeatures();
            else fireViewOrIndexChanged();
        }
    }
    
    public final void removeFeatures() {
        removeAll();
        tabs = null;
        if (defaultView != null) add(defaultView, BorderLayout.CENTER);
        doLayout();
        repaint();
        fireViewOrIndexChanged();
    }
    
    
    public final ProfilerFeature getSelectedFeature() {
        if (tabs == null) return null;
        JPanel container = (JPanel)tabs.getSelectedComponent();
        return (ProfilerFeature)container.getClientProperty(ProfilerFeature.class);
    }
    
    public final void selectFeature(ProfilerFeature feature) {
        if (tabs == null) return;
        for (Component c : tabs.getComponents())
            if (((JComponent)c).getClientProperty(ProfilerFeature.class) == feature)
                tabs.setSelectedComponent(c);
    }
    
    public final void selectFeature(int index) {
        if (tabs == null) return;
        tabs.setSelectedIndex(index);
    }
    
    public final void selectPreviousFeature() {
        if (tabs == null) return;
        int index = UIUtils.getPreviousSubTabIndex(tabs, tabs.getSelectedIndex());
        tabs.setSelectedIndex(index);
    }
    
    public final void selectNextFeature() {
        if (tabs == null) return;
        int index = UIUtils.getNextSubTabIndex(tabs, tabs.getSelectedIndex());
        tabs.setSelectedIndex(index);
    }
    
    
    public final void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }
    
    public final void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }
    
    public final void fireViewOrIndexChanged() {
        if (listeners.isEmpty()) return;
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : listeners)
            listener.stateChanged(event);
    }
    
    
    private JTabbedPane createTabs() {
        JTabbedPane tp = new JTabbedPane(JTabbedPane.BOTTOM) {
            protected final void fireStateChanged() {
                super.fireStateChanged();
                fireViewOrIndexChanged();
            }
        };
        tp.setOpaque(false);
        if (UIUtils.isAquaLookAndFeel()) {
            tp.setBorder(BorderFactory.createEmptyBorder(-13, -11, 0, -10));
        } else {
            tp.setBorder(BorderFactory.createEmptyBorder());
            Insets i = UIManager.getInsets("TabbedPane.contentBorderInsets"); // NOI18N
            int bottomOffset = 0;
            if (UIUtils.isMetalLookAndFeel()) {
                bottomOffset = -i.bottom + 1;
            } else if (UIUtils.isWindowsLookAndFeel()) {
                bottomOffset = -i.bottom;
            }
            if (i != null) tp.setBorder(BorderFactory.createEmptyBorder(-i.top, -i.left, bottomOffset, -i.right));
        }
        
        // Fix for Issue 115062 (CTRL-PageUp/PageDown should move between snapshot tabs)
        tp.getActionMap().getParent().remove("navigatePageUp"); // NOI18N
        tp.getActionMap().getParent().remove("navigatePageDown"); // NOI18N
        
        // support for traversing subtabs using Ctrl-Alt-PgDn/PgUp
        getActionMap().put("PreviousViewAction", new AbstractAction() { // NOI18N
            public void actionPerformed(ActionEvent e) { selectPreviousFeature(); }
        });
        getActionMap().put("NextViewAction", new AbstractAction() { // NOI18N
            public void actionPerformed(ActionEvent e) { selectNextFeature(); }
        });
        
        return tp;
    }
    
}
