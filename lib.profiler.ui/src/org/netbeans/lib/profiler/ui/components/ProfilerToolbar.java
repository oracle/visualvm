/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.swing.GenericToolbar;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerToolbar {
    
    public static ProfilerToolbar create(boolean showSeparator) {
        Provider provider = Lookup.getDefault().lookup(Provider.class);
        return provider != null ? provider.create(showSeparator) :
                                  new Impl(showSeparator);
    }
    
    
    public abstract JComponent getComponent();
    
    
    public Component add(ProfilerToolbar toolbar) { return toolbar.getComponent(); }
    
    public Component add(ProfilerToolbar toolbar, int index) { return toolbar.getComponent(); }
    
    public void remove(ProfilerToolbar toolbar) {}
    
    
    public abstract Component add(Action action);
    
    public abstract Component add(Component component);
    
    public abstract Component add(Component component, int index);
    
    public abstract void addSeparator();
    
    public abstract void addSpace(int width);
    
    public abstract void addFiller();
    
    public abstract void remove(Component component);
    
    public abstract void remove(int index);
    
    public abstract int getComponentCount();
    
    
    protected ProfilerToolbar() {}
    
    
    public static abstract class Provider {
        
        public abstract ProfilerToolbar create(boolean showSeparator);
        
    }
    
    public static class Impl extends ProfilerToolbar {
        
        protected final JComponent component;
        protected final JToolBar toolbar;
        
        protected Impl(boolean showSeparator) {
            toolbar = new GenericToolbar();
            if (UIUtils.isWindowsModernLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
            else if (!UIUtils.isNimbusLookAndFeel() && !UIUtils.isAquaLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
            toolbar.setBorderPainted(false);
            toolbar.setRollover(true);
            toolbar.setFloatable(false);
            
            toolbar.setFocusTraversalPolicyProvider(true);
            toolbar.setFocusTraversalPolicy(new SimpleFocusTraversalPolicy());
            
            if (showSeparator) {
                component = new JPanel(new BorderLayout(0, 0));
                component.setOpaque(false);
                component.add(toolbar, BorderLayout.CENTER);
                component.add(UIUtils.createHorizontalLine(toolbar.getBackground()),
                        BorderLayout.SOUTH);
            } else {
                component = toolbar;
            }
        }
        
        @Override
        public JComponent getComponent() {
            return component;
        }
        
        @Override
        public Component add(ProfilerToolbar toolbar) {
            return add(toolbar, getComponentCount());
        }
    
        @Override
        public Component add(ProfilerToolbar toolbar, int index) {
            JToolBar implToolbar = ((Impl)toolbar).toolbar;
            implToolbar.setBorder(BorderFactory.createEmptyBorder());
            implToolbar.setOpaque(false);
            implToolbar.putClientProperty("Toolbar.noGTKBorder", Boolean.TRUE); // NOI18N
            return add(implToolbar, index);
        }
        
        @Override
        public void remove(ProfilerToolbar toolbar) {
            remove(((Impl)toolbar).toolbar);
        }

        @Override
        public Component add(Action action) {
            Component c = toolbar.add(action);
            toolbar.repaint();
            return c;
        }

        @Override
        public Component add(Component component) {
            Component c = toolbar.add(component);
            toolbar.repaint();
            return c;
        }
        
        @Override
        public Component add(Component component, int index) {
            Component c = toolbar.add(component, index);
            toolbar.repaint();
            return c;
        }

        @Override
        public void addSeparator() {
            toolbar.addSeparator();
            toolbar.repaint();
        }
        
        @Override
        public void addSpace(int width) {
            toolbar.addSeparator(new Dimension(width, 0));
            toolbar.repaint();
        }
        
        @Override
        public void addFiller() {
            Dimension minDim = new Dimension(0, 0);
            Dimension maxDim = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
            toolbar.add(new Box.Filler(minDim, minDim, maxDim));
            toolbar.repaint();
        }
        
        @Override
        public void remove(Component component) {
            toolbar.remove(component);
            toolbar.repaint();
        }
        
        @Override
        public void remove(int index) {
            toolbar.remove(index);
            toolbar.repaint();
        }
        
        @Override
        public int getComponentCount() {
            return toolbar.getComponentCount();
        }
        
    }
    
    
    public static class SimpleFocusTraversalPolicy extends FocusTraversalPolicy {
        
        public Component getComponentAfter(Container aContainer, Component aComponent) {
            List<Component> l = components(topContainer(aContainer));
            int i = l.indexOf(aComponent);
            return i == -1 || i == l.size() - 1 ? null : l.get(i + 1);
        }

        public Component getComponentBefore(Container aContainer, Component aComponent) {
            List<Component> l = components(topContainer(aContainer));
            int i = l.indexOf(aComponent);
            return i == -1 || i == 0 ? null : l.get(i - 1);
        }

        public Component getFirstComponent(Container aContainer) {
            List<Component> l = components(topContainer(aContainer));
            return l.isEmpty() ? null : l.get(0);
        }

        public Component getLastComponent(Container aContainer) {
            List<Component> l = components(topContainer(aContainer));
            return l.isEmpty() ? null : l.get(l.size() - 1);
        }

        public Component getDefaultComponent(Container aContainer) {
            return getFirstComponent(aContainer);
        }

        protected Container topContainer(Container aContainer) {
            while (aContainer.getParent() instanceof JToolBar)
                aContainer = aContainer.getParent();
            return aContainer;
        }

        protected List<Component> components(Container aContainer) {
            List<Component> l = new ArrayList();

            for (int i = 0; i < aContainer.getComponentCount(); i++) {
                Component c = aContainer.getComponent(i);
                if (c instanceof JToolBar || c instanceof JPanel)
                    l.addAll(components((Container)c));
                else if (focusable(c)) l.add(c);
            }

            return l;
        }
        
        protected boolean focusable(Component c) {
            return c.isVisible() && c.isEnabled() && c.isFocusable();
        }
        
    }
    
}
