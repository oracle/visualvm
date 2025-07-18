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
package org.graalvm.visualvm.lib.ui.components;

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
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.swing.InvisibleToolbar;
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
            toolbar = new InvisibleToolbar();
            if (UIUtils.isWindowsModernLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
            else if (UIUtils.isMetalLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 1, 2));
            else if (UIUtils.isNimbusLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(-1, 0, -2, 0));
            else if (UIUtils.isGTKLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));
            else if (!UIUtils.isAquaLookAndFeel())
                toolbar.setBorder(BorderFactory.createEmptyBorder(1, 2, 0, 2));
            toolbar.setBorderPainted(false);
            toolbar.setRollover(true);
            toolbar.setFloatable(false);
            
            toolbar.setFocusTraversalPolicyProvider(true);
            toolbar.setFocusTraversalPolicy(new SimpleFocusTraversalPolicy());
            
            if (showSeparator) {
                component = new JPanel(new BorderLayout(0, 0));
                component.setOpaque(false);
                component.add(toolbar, BorderLayout.CENTER);
                component.add(UIUtils.createHorizontalLine(), BorderLayout.SOUTH);
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
            tweakComponent(c);
            toolbar.repaint();
            return c;
        }

        @Override
        public Component add(Component component) {
            Component c = toolbar.add(component);
            tweakComponent(c);
            toolbar.repaint();
            return c;
        }
        
        @Override
        public Component add(Component component, int index) {
            Component c = toolbar.add(component, index);
            tweakComponent(c);
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
        
        
        private void tweakComponent(Component c) {
            if (c instanceof JComponent) ((JComponent)c).setOpaque(false);
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
            List<Component> l = new ArrayList<>();

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
