/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.graalvm.visualvm.lib.profiler.v2.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.graalvm.visualvm.lib.ui.results.ColoredFilter;
import org.graalvm.visualvm.lib.ui.results.PackageColorer;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;
import org.graalvm.visualvm.lib.ui.swing.ProfilerPopup;
import org.graalvm.visualvm.lib.ui.swing.TextArea;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "FilterSelector_outgoingCalls=Outgoing calls filter:",
    "FilterSelector_noFilter=No filter",
    "FilterSelector_excludeCoreJava=Exclude core Java classes",
    "FilterSelector_excludeCustom=Exclude defined classes",
    "FilterSelector_includeCustom=Include defined classes",
    "FilterSelector_excludeCustomEx=Exclude defined classes:",
    "FilterSelector_includeCustomEx=Include defined classes:",
    "FilterSelector_filterHint=org.mypackage.**\norg.mypackage.*\norg.mypackage.MyClass",
    "FilterSelector_filterTooltip=<html>Include/exclude profiling outgoing calls from these classes or packages:<br><br>"
            + "<code>&nbsp;org.mypackage.**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package and subpackages<br>"
            + "<code>&nbsp;org.mypackage.*&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package<br>"
            + "<code>&nbsp;org.mypackage.MyClass&nbsp;&nbsp;</code>single class<br></html>",
    "FilterSelector_insertFilter=Insert Defined Filter"
})
public abstract class FilterSelector {
    
    public static enum FilterName {
        NO_FILTER,
        EXCLUDE_JAVA_FILTER,
        EXCLUDE_CUSTOM_FILTER,
        INCLUDE_CUSTOM_FILTER;
        
        public String toString() {
            switch(this) {
                case NO_FILTER:             return Bundle.FilterSelector_noFilter();
                case EXCLUDE_JAVA_FILTER:   return Bundle.FilterSelector_excludeCoreJava();
                case EXCLUDE_CUSTOM_FILTER: return Bundle.FilterSelector_excludeCustom();
                case INCLUDE_CUSTOM_FILTER: return Bundle.FilterSelector_includeCustom();
                default:                    throw new IllegalArgumentException();
            }
        }
    }
    
    
    public void show(Component invoker, FilterName filterName, String filterValue) {
        UI ui = new UI(filterName, filterValue);
        ui.show(invoker);
    }
    
    
    protected abstract void filterChanged(FilterName filterName, String filterValue);
    
    
    private class UI {
        
        private JRadioButton noFilterChoice;
        private JRadioButton javaClassesChoice;
        private JRadioButton excludeCustomChoice;
        private JRadioButton includeCustomChoice;
        private TextArea customClasses;
        private PopupButton insertFilter;
        
        private JPanel panel;
        
        UI(FilterName filterName, String filterValue) {
            populatePopup(filterName, filterValue);
        }
        
        void show(Component invoker) {
            int resizeMode = ProfilerPopup.RESIZE_LEFT | ProfilerPopup.RESIZE_BOTTOM;
            ProfilerPopup.createRelative(invoker, panel, SwingConstants.SOUTH_EAST, resizeMode).show();
        }
        
        private void populatePopup(FilterName filterName, String filterValue) {
            JPanel content = new JPanel(new BorderLayout());
            
            JLabel hint = new JLabel(Bundle.FilterSelector_outgoingCalls(), JLabel.LEADING);
            hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
            content.add(hint, BorderLayout.NORTH);
            
            ButtonGroup bg = new ButtonGroup() {
                public void setSelected(ButtonModel m, boolean b) {
                    super.setSelected(m, b);
                    if (b && m.isSelected()) filterChanged(true);
                }
            };
            
            JPanel filters = new JPanel(new GridLayout(3, 1));
            
            noFilterChoice = new JRadioButton(Bundle.FilterSelector_noFilter(),
                                FilterName.NO_FILTER.equals(filterName));
            bg.add(noFilterChoice);
            JPanel noFilter = new JPanel(null);
            noFilter.setLayout(new BoxLayout(noFilter, BoxLayout.LINE_AXIS));
            noFilter.add(noFilterChoice);
            filters.add(noFilter);
            
            javaClassesChoice = new JRadioButton(Bundle.FilterSelector_excludeCoreJava(),
                                FilterName.EXCLUDE_JAVA_FILTER.equals(filterName));
            bg.add(javaClassesChoice);
            
            JLabel javaClassesHint = new JLabel("(java.*, javax.*, sun.*, com.sun.*, etc.)", JLabel.LEADING);
            javaClassesHint.setFont(javaClassesHint.getFont().deriveFont(javaClassesHint.getFont().getSize2D() - 1));
            javaClassesHint.setEnabled(false);
            
            JPanel javaFilters = new JPanel(null);
            javaFilters.setLayout(new BoxLayout(javaFilters, BoxLayout.LINE_AXIS));
            javaFilters.add(javaClassesChoice);
            javaFilters.add(createStrut(javaClassesChoice, 5, false));
            javaFilters.add(javaClassesHint);
            filters.add(javaFilters);
            
            excludeCustomChoice = new JRadioButton(Bundle.FilterSelector_excludeCustomEx(),
                                  FilterName.EXCLUDE_CUSTOM_FILTER.equals(filterName));
            bg.add(excludeCustomChoice);
            
            includeCustomChoice = new JRadioButton(Bundle.FilterSelector_includeCustomEx(),
                                  FilterName.INCLUDE_CUSTOM_FILTER.equals(filterName));
            bg.add(includeCustomChoice);
            
            JPanel customFilters = new JPanel(null);
            customFilters.setLayout(new BoxLayout(customFilters, BoxLayout.LINE_AXIS));
            customFilters.add(excludeCustomChoice);
            customFilters.add(createStrut(excludeCustomChoice, 8, false));
            customFilters.add(includeCustomChoice);
            filters.add(customFilters);
            
            if (PackageColorer.hasRegisteredColors()) {
                insertFilter = new PopupButton(Icons.getIcon(GeneralIcons.FILTER)) {
                    {
                        setToolTipText(Bundle.FilterSelector_insertFilter());
                    }
                    protected void populatePopup(JPopupMenu popup) {
                        for (final ColoredFilter color : PackageColorer.getRegisteredColors()) {
                            if (color.getValue().trim().isEmpty()) continue;
                            Icon icon = color.getColor() == null ? null : color.getIcon(12, 12);
                            popup.add(new JMenuItem(color.getName(), icon) {
                                protected void fireActionPerformed(ActionEvent event) {
                                    StringBuilder added = new StringBuilder();
                                    for (String f : color.getValues()) {
                                        if (added.length() > 0) added.append(", "); // NOI18N
                                        added.append(f);
                                        if (f.endsWith(".")) added.append("**"); // NOI18N
                                    }
                                    
                                    String current = customClasses.showsHint() ? "" : customClasses.getText(); // NOI18N
                                    if (!current.isEmpty()) current += "\n"; // NOI18N
                                    current += added.toString();
                                    
                                    customClasses.requestFocusInWindow();
                                    customClasses.setText(current);
                                }
                            });
                        }
                    }
                    public Dimension getPreferredSize() {
                        Dimension dim = super.getPreferredSize();
                        dim.width -= 2;
                        dim.height -= 2;
                        return dim;
                    }
                };
                customFilters.add(Box.createHorizontalGlue());
                customFilters.add(insertFilter);
            }
            
            JPanel filtersOut = new JPanel(new GridBagLayout());
            GridBagConstraints c;
            int y = 0;
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(3, 0, 0, 0);
            filtersOut.add(filters, c);
            
            customClasses = new TextArea() {
                protected void changed() {
                    filterChanged(true);
                }
                public Point getToolTipLocation(MouseEvent event) {
                    Component scroll = getParent().getParent();
                    return SwingUtilities.convertPoint(scroll, 0, scroll.getHeight(), this);
                }
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                    if (insertFilter != null) insertFilter.setEnabled(enabled);
                }
            };
            customClasses.setFont(new Font("Monospaced", Font.PLAIN, customClasses.getFont().getSize())); // NOI18N
            customClasses.setRows(0);
            customClasses.setColumns(0);
            JScrollPane customClassesScroll = new JScrollPane(customClasses);
            Dimension d = customClassesScroll.getPreferredSize();
            customClasses.setRows(3);
            customClasses.setColumns(56);
            Dimension _d = customClasses.getPreferredScrollableViewportSize();
            d.width += _d.width;
            d.height += _d.height;
            customClassesScroll.setPreferredSize(d);
            customClassesScroll.setMinimumSize(d);
            customClasses.setText(filterValue);
            customClasses.setHint(Bundle.FilterSelector_filterHint());
            customClasses.setToolTipText(Bundle.FilterSelector_filterTooltip());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.weighty = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(3, 20, 0, 0);
            filtersOut.add(customClassesScroll, c);
            
            filterChanged(false);
            
            content.add(filtersOut, BorderLayout.CENTER);
            
            panel = content;
        }
        
        private void filterChanged(boolean fire) {
            customClasses.setEnabled(excludeCustomChoice.isSelected() ||
                                     includeCustomChoice.isSelected());
            
            if (!fire) return;
            
            String filterValue = customClasses.showsHint() ? "" : customClasses.getText().trim(); // NOI18N
            
            if (noFilterChoice.isSelected()) {
                FilterSelector.this.filterChanged(FilterName.NO_FILTER, filterValue);
            } else if (javaClassesChoice.isSelected()) {
                FilterSelector.this.filterChanged(FilterName.EXCLUDE_JAVA_FILTER, filterValue);
            } else if (excludeCustomChoice.isSelected()) {
                FilterSelector.this.filterChanged(FilterName.EXCLUDE_CUSTOM_FILTER, filterValue);
            } else if (includeCustomChoice.isSelected()) {
                FilterSelector.this.filterChanged(FilterName.INCLUDE_CUSTOM_FILTER, filterValue);
            }
        }
        
    }
    
    
    private static Component createStrut(JComponent c, int width, boolean before) {
        Border b = c.getBorder();
        Insets i = b != null ? b.getBorderInsets(c) : null;
        int w = i == null ? width : Math.max(width - (before ? i.left : i.right), 0);
        return Box.createHorizontalStrut(w);
    }
    
}
