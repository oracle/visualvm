/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.v2.ui;

import org.graalvm.visualvm.lib.ui.components.LazyComboBox;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.graalvm.visualvm.lib.profiler.api.ProjectUtilities;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProjectSelector_ExternalProcess=External process"
})
public class ProjectSelector extends LazyComboBox<Lookup.Provider> {

    public static final Lookup.Provider EXTERNAL_PROCESS = new Lookup.Provider() {
        public Lookup getLookup() { return Lookup.EMPTY; }
    };


    public ProjectSelector(Populator populator) {
        super(populator);
        setRenderer(new ProjectNameRenderer());
    }


    public final Lookup.Provider getProject() {
        Lookup.Provider project = (Lookup.Provider)getSelectedItem();
        return project == EXTERNAL_PROCESS ? null : project;
    }

    public final void setProject(Lookup.Provider project) {
        setSelectedItem(project == null ? EXTERNAL_PROCESS : project);
    }

    public void resetProject(Lookup.Provider project) {
        if (getProject() == project) resetModel();
    }


    // --- Projects populator --------------------------------------------------

    public static class Populator extends LazyComboBox.Populator<Lookup.Provider> {

        protected Lookup.Provider initialProject() {
            return null;
        }

        protected Collection<Lookup.Provider> additionalProjects() {
            return Collections.EMPTY_SET;
        }
        
        protected final Lookup.Provider initial() {
            Lookup.Provider initial = initialProject();
            return initial == null ? EXTERNAL_PROCESS : initial;
        }
        
        protected final Lookup.Provider[] populate() {
            Set<Lookup.Provider> s = new HashSet();
            s.addAll(Arrays.asList(ProjectUtilities.getOpenedProjects()));
            s.addAll(additionalProjects());

            List<Lookup.Provider> l = new ArrayList();
            Lookup.Provider[] pa = s.toArray(new Lookup.Provider[0]);
            l.add(EXTERNAL_PROCESS);
            l.addAll(Arrays.asList(ProjectUtilities.getSortedProjects(pa)));
            return l.toArray(new Lookup.Provider[0]);
        }
    }
    
    
    // --- Project renderer ----------------------------------------------------
    
    private static final class ProjectNameRenderer extends DefaultListCellRenderer {
        
        private Font _plainFont;
        private Font _boldFont;
        
        private Renderer _renderer;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel renderer = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (_renderer == null) _renderer = new Renderer();
            _renderer.setComponentOrientation(renderer.getComponentOrientation());
            _renderer.setOpaque(renderer.isOpaque());
            _renderer.setForeground(renderer.getForeground());
            _renderer.setBackground(renderer.getBackground());
            _renderer.setEnabled(renderer.isEnabled());
            _renderer.setBorder(renderer.getBorder());
            
            if (value != EXTERNAL_PROCESS) {
                Lookup.Provider p = (Lookup.Provider)value;
                _renderer.setText(ProjectUtilities.getDisplayName(p));
                _renderer.setIcon(ProjectUtilities.getIcon(p));
                boolean main = ProjectUtilities.getMainProject() == value;
                _renderer.setFontEx(main ? boldFont(renderer) : plainFont(renderer));
            } else {
                _renderer.setText(Bundle.ProjectSelector_ExternalProcess());
                _renderer.setIcon(Icons.getIcon(GeneralIcons.JAVA_PROCESS));
                _renderer.setFontEx(plainFont(renderer));
            }

            return _renderer;
        }
        
        private Font plainFont(JLabel renderer) {
            if (_plainFont == null) _plainFont = renderer.getFont().deriveFont(Font.PLAIN);
            return _plainFont;
        }
        
        private Font boldFont(JLabel renderer) {
            if (_boldFont == null) _boldFont = renderer.getFont().deriveFont(Font.BOLD);
            return _boldFont;
        }
        
        // Default renderer doesn't follow font settings in combo (not popup)
        private static class Renderer extends DefaultListCellRenderer {
            public void setFont(Font font) {}
            public void setFontEx(Font font) { super.setFont(font); }
        }
        
    }
    
}
