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
package org.netbeans.modules.profiler.v2.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import org.netbeans.lib.profiler.ui.swing.ProfilerPopupFactory;
import org.netbeans.lib.profiler.ui.swing.TextArea;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "FilterSelector_outgoingCalls=Outgoing calls filter:",
    "FilterSelector_excludeCoreJava=Exclude core Java classes",
    "FilterSelector_excludeCustom=Exclude defined classes",
    "FilterSelector_includeCustom=Include defined classes",
    "FilterSelector_excludeCustomEx=Exclude defined classes:",
    "FilterSelector_includeCustomEx=Include defined classes:",
    "FilterSelector_filterHint=org.mypackage.**\norg.mypackage.*\norg.mypackage.MyClass",
    "FilterSelector_filterTooltip=<html>Include/exclude profiling outgoing calls from these classes or packages:<br><br>"
            + "<code>&nbsp;org.mypackage.**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package and subpackages<br>"
            + "<code>&nbsp;org.mypackage.*&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</code>all classes in package<br>"
            + "<code>&nbsp;org.mypackage.MyClass&nbsp;&nbsp;</code>single class<br></html>"
})
public abstract class FilterSelector {
    
    public static enum FilterName {
        EXCLUDE_JAVA_FILTER,
        EXCLUDE_CUSTOM_FILTER,
        INCLUDE_CUSTOM_FILTER;
        
        public String toString() {
            switch(this) {
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
        
        private JRadioButton javaClassesChoice;
        private JRadioButton excludeCustomChoice;
        private JRadioButton includeCustomChoice;
        private TextArea customClasses;
        
        private JPanel panel;
        
        UI(FilterName filterName, String filterValue) {
            populatePopup(filterName, filterValue);
        }
        
        void show(Component invoker) {
            ProfilerPopupFactory.getPopup(invoker, panel, invoker.getWidth() - panel.getPreferredSize().width, invoker.getHeight()).show();
        }
        
        private void populatePopup(FilterName filterName, String filterValue) {
            JPanel content = new JPanel(new BorderLayout());
            content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            
            JLabel hint = new JLabel(Bundle.FilterSelector_outgoingCalls(), JLabel.LEADING);
            hint.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
            content.add(hint, BorderLayout.NORTH);
            
            JPanel filters = new JPanel(new GridBagLayout());
            ButtonGroup bg = new ButtonGroup() {
                public void setSelected(ButtonModel m, boolean b) {
                    super.setSelected(m, b);
                    if (b && m.isSelected()) filterChanged(true);
                }
            };
            GridBagConstraints c;
            int y = 0;
            
            javaClassesChoice = new JRadioButton(Bundle.FilterSelector_excludeCoreJava(),
                                FilterName.EXCLUDE_JAVA_FILTER.equals(filterName));
            bg.add(javaClassesChoice);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(3, 0, 0, 0);
            filters.add(javaClassesChoice, c);
            
            JLabel javaClassesHint = new JLabel("(java.*, javax.*, sun.*, com.sun.*, etc.)", JLabel.LEADING);
            javaClassesHint.setFont(javaClassesHint.getFont().deriveFont(javaClassesHint.getFont().getSize2D() - 1));
            javaClassesHint.setEnabled(false);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(3, 5, 0, 0);
            filters.add(javaClassesHint, c);
            
            excludeCustomChoice = new JRadioButton(Bundle.FilterSelector_excludeCustomEx(),
                                  FilterName.EXCLUDE_CUSTOM_FILTER.equals(filterName));
            bg.add(excludeCustomChoice);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(3, 0, 0, 0);
            filters.add(excludeCustomChoice, c);
            
            includeCustomChoice = new JRadioButton(Bundle.FilterSelector_includeCustomEx(),
                                  FilterName.INCLUDE_CUSTOM_FILTER.equals(filterName));
            bg.add(includeCustomChoice);
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(3, 0, 0, 0);
            filters.add(includeCustomChoice, c);
            
            customClasses = new TextArea() {
                protected void changed() {
                    filterChanged(true);
                }
                public Point getToolTipLocation(MouseEvent event) {
                    return new Point(-1, getHeight() + 2);
                }
            };
            customClasses.setFont(new Font("Monospaced", Font.PLAIN, customClasses.getFont().getSize())); // NOI18N
            customClasses.setRows(0);
            customClasses.setColumns(0);
            JScrollPane customClassesScroll = new JScrollPane(customClasses);
            Dimension d = customClassesScroll.getPreferredSize();
            customClasses.setRows(3);
            customClasses.setColumns(50);
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
            filters.add(customClassesScroll, c);
            
            filterChanged(false);
            
            content.add(filters, BorderLayout.CENTER);
            
            panel = content;
        }
        
        private void filterChanged(boolean fire) {
            customClasses.setEnabled(excludeCustomChoice.isSelected() ||
                                     includeCustomChoice.isSelected());
            
            if (!fire) return;
            
            String filterValue = customClasses.showsHint() ? "" : customClasses.getText().trim(); // NOI18N
            
            if (javaClassesChoice.isSelected()) {
                FilterSelector.this.filterChanged(FilterName.EXCLUDE_JAVA_FILTER, filterValue);
            } else if (excludeCustomChoice.isSelected()) {
                FilterSelector.this.filterChanged(FilterName.EXCLUDE_CUSTOM_FILTER, filterValue);
            } else if (includeCustomChoice.isSelected()) {
                FilterSelector.this.filterChanged(FilterName.INCLUDE_CUSTOM_FILTER, filterValue);
            }
        }
        
    }
    
}
