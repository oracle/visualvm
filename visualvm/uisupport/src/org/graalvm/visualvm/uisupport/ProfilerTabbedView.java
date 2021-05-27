/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.uisupport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerTabbedView {
    
    // NOTE: LEFT and RIGHT tab placement is not supported by the actual implementation
    // NOTE: SCROLL_TAB_LAYOUT is not supported (doesn't work with minimizeInnerMargin)
    
    public static ProfilerTabbedView createTop(boolean minimizeInnerMargin,
                                               boolean minimizeOuterMargin,
                                               ChangeListener listener) {
        return create(JTabbedPane.TOP, minimizeInnerMargin, minimizeOuterMargin, listener);
    }
    
    public static ProfilerTabbedView createBottom(boolean minimizeInnerMargin,
                                                  boolean minimizeOuterMargin,
                                                  ChangeListener listener) {
        return create(JTabbedPane.BOTTOM, minimizeInnerMargin, minimizeOuterMargin, listener);
    }
    
    private static ProfilerTabbedView create(int tabPlacement, boolean minimizeInnerMargin,
                                             boolean minimizeOuterMargin, ChangeListener listener) {
        Provider provider = Lookup.getDefault().lookup(Provider.class);
        return provider != null ? provider.create(tabPlacement, JTabbedPane.WRAP_TAB_LAYOUT, minimizeInnerMargin, minimizeOuterMargin, listener) :
                                  new Impl(tabPlacement, JTabbedPane.WRAP_TAB_LAYOUT, minimizeInnerMargin, minimizeOuterMargin, listener);
    }
    
    
    public abstract JComponent getComponent();
    
    
    public abstract void addView(String name, Icon icon, String description, JComponent view, boolean closable);
    
    public abstract boolean containsView(JComponent view);
    
//    public abstract void replaceView(JComponent oldView, String name, Icon icon, String description, JComponent newView, boolean closable);
    
    public abstract void updateView(JComponent view, String name, Icon icon, String description);
    
    public abstract void removeView(JComponent view);
    
    public abstract void removeAllViews();
    
    public abstract int getViewsCount();
    
//    public abstract int getViewIndex(JComponent view);
    
    
    public abstract void setViewName(JComponent view, String name);
    
    public abstract String getViewName(JComponent view);
    
    public abstract void setViewEnabled(JComponent view, boolean enabled);
    
    public abstract boolean isViewEnabled(JComponent view);
    
    
    public abstract void selectView(JComponent view);
    
    public abstract void selectView(int index);
    
    public abstract void selectPreviousView();
    
    public abstract void selectNextView();
    
    public abstract JComponent getSelectedView();
    
//    public abstract int getSelectedViewIndex();
    
    
    public abstract void highlightView(JComponent view);
    
    
    public abstract void setFocusMaster(Component focusMaster);
    
    
    public abstract void addViewListener(Listener listener);
    
    public abstract void removeViewListener(Listener listener);
    
    
    public void setShowsTabPopup(boolean showsTabPopup) {}
    
    public boolean getShowsTabPopup() { return true; }
    
    
    protected ProfilerTabbedView() {}
    
    
    public static abstract class Listener {
        
        public void viewAdded(JComponent view) {}
        
        public void viewRemoved(JComponent view) {}
        
    }
    
    
    public static abstract class Provider {
        
        public abstract ProfilerTabbedView create(int tabPlacement, int tabLayoutPolicy,
                                                  boolean minimizeInnerMargin, boolean
                                                  minimizeOuterMargin, ChangeListener listener);
        
    }
    
    
    public static class Impl extends ProfilerTabbedView {
        
        private List<Listener> viewListeners;
        
        private final ChangeListener listener;
        
        private final int tabPlacement;
        private final int tabLayoutPolicy;
        private final boolean minimizeInnerMargin;
        private final boolean minimizeOuterMargin;
        
        private final JComponent component;
        
        private JComponent firstView;
        private String firstName;
        private Icon firstIcon;
        private String firstDescription;
        private boolean firstClosable;
        
        private ProfilerTabbedPane tabs;
        private boolean showsTabPopup = true;
        
        private Component focusMaster;
        
    
        protected Impl(int tabPlacement, int tabLayoutPolicy, boolean minimizeOuterMargin,
                    boolean minimizeInnerMargin, ChangeListener listener) {
            
            this.listener = listener;
            
            this.tabPlacement = tabPlacement;
            this.tabLayoutPolicy = tabLayoutPolicy;
            this.minimizeInnerMargin = minimizeInnerMargin;
            this.minimizeOuterMargin = minimizeOuterMargin;
            
            component = new JPanel(new BorderLayout());
            component.setOpaque(false);
            
            // support for traversing subtabs using Ctrl-Alt-PgDn/PgUp
            component.getActionMap().put("PreviousViewAction", new AbstractAction() { // NOI18N
                public void actionPerformed(ActionEvent e) { selectPreviousView(); }
            });
            component.getActionMap().put("NextViewAction", new AbstractAction() { // NOI18N
                public void actionPerformed(ActionEvent e) { selectNextView(); }
            });
            
            setFocusMaster(null);
            
//            tabs = createTabs(component, tabPlacement, minimizeOuterMargin);
//            component.add(tabs, BorderLayout.CENTER);
        }
        
        
        public void setFocusMaster(Component focusMaster) {
            this.focusMaster = focusMaster == null ? component : focusMaster;
        }
        
        
        public JComponent getComponent() {
            return component;
        }
        
        
        public void addView(String name, Icon icon, String description, JComponent view, boolean closable) {
            if (tabs == null) {
                if (firstView == null) {
                    firstView = view;
                    firstName = name;
                    firstIcon = icon;
                    firstDescription = description;
                    firstClosable = closable;
                    component.add(view, BorderLayout.CENTER);
                    fireChanged();
                } else {
                    component.remove(firstView);
                    tabs = createTabs(tabPlacement, tabLayoutPolicy, minimizeOuterMargin);
                    tabs.setShowsTabPopup(showsTabPopup);
                    tabs.addTab(firstName, firstIcon, createViewport(firstView), firstDescription, firstClosable);
                    tabs.addTab(name, icon, createViewport(view), description, closable);
                    component.add(tabs, BorderLayout.CENTER);
                    firstView = null;
                    firstName = null;
                    firstIcon = null;
                    firstDescription = null;
                }
            } else {
                tabs.addTab(name, icon, createViewport(view), description, closable);
            }
            fireViewAdded(view);
        }
        
        public boolean containsView(JComponent view) {
            if (tabs == null) return Objects.equals(view, firstView);
            TabbedPaneViewport viewport = createViewport(view);
            return tabs.indexOfComponent(viewport) != -1;
        }
        
//        public void replaceView(JComponent oldView, String name, Icon icon, String description, JComponent newView, boolean closable) {
//            if (tabs == null && oldView == firstView) {
//                component.remove(firstView);
//                firstView = newView;
//                firstName = name;
//                firstIcon = icon;
//                firstDescription = description;
//                firstClosable = closable;
//                component.add(newView, BorderLayout.CENTER);
//                component.invalidate();
//                component.revalidate();
//                component.repaint();
//            } else {
//                TabbedPaneViewport oldViewport = TabbedPaneViewport.fromView(oldView);
//                int idx = tabs.indexOfComponent(oldViewport);
//                tabs.setTitleAt(idx, name);
//                tabs.setIconAt(idx, icon);
//                tabs.setToolTipTextAt(idx, description);
//                tabs.setComponentAt(idx, createViewport(newView));
////                tabs.insertTab(name + " ", icon, createViewport(newView, closable), description, idx);
////                TabbedPaneViewport oldViewport = TabbedPaneViewport.fromView(oldView);
////                int idx = tabs.indexOfComponent(oldViewport);
////                tabs.remove(oldViewport);
////                tabs.insertTab(name + " ", icon, createViewport(newView, closable), description, idx);
//            }
//        }
        
        public void updateView(JComponent view, String name, Icon icon, String description) {
            if (tabs == null && view == firstView) {
                firstName = name;
                firstIcon = icon;
                firstDescription = description;
            } else {
                TabbedPaneViewport viewport = TabbedPaneViewport.fromView(view);
                int idx = tabs.indexOfComponent(viewport);
                tabs.setTitleAt(idx, name);
                tabs.setIconAt(idx, icon);
                tabs.setToolTipTextAt(idx, description);
            }
        }

        public void removeView(JComponent view) {
            if (tabs != null) {
//                int viewIndex = tabs.indexOfComponent(view);
//                if (viewIndex == -1) return;
                if (tabs.getTabCount() > 2) {
                    tabs.remove(TabbedPaneViewport.fromView(view));
                } else {
                    tabs.remove(TabbedPaneViewport.fromView(view));
                    TabbedPaneViewport singleViewport = (TabbedPaneViewport)tabs.getComponentAt(0);
                    firstName = tabs.getTitleAt(0);
                    firstIcon = tabs.getIconAt(0);
                    firstDescription = tabs.getToolTipTextAt(0);
                    firstClosable = tabs.isClosableAt(0);
                    firstView = singleViewport.disposeView();
                    component.remove(tabs);
                    component.add(firstView, BorderLayout.CENTER);
                    tabs = null;
                }
            } else if (firstView == view) {
                component.remove(firstView);
                firstView = null;
                firstName = null;
                firstIcon = null;
                firstDescription = null;
                fireChanged();
            }
            fireViewRemoved(view);
        }

        public void removeAllViews() {
            Collection<JComponent> allViews = getAllViews();
            if (allViews.isEmpty()) return;
            
            component.removeAll();
            tabs = null;
            firstView = null;
            firstName = null;
            firstIcon = null;
            firstDescription = null;
            fireChanged();
            
            for (JComponent view : allViews) fireViewRemoved(view);
        }

        public int getViewsCount() {
            return tabs != null ? tabs.getTabCount() : (firstView != null ? 1 : 0);
        }
        
        private int getViewIndex(JComponent view) {
            if (tabs == null) return view == firstView ? 0 : -1;
            else return tabs.indexOfComponent(TabbedPaneViewport.fromView(view));
        }
        
        private Collection<JComponent> getAllViews() {
            if (tabs != null) {
                int tabsCount = tabs.getTabCount();
                List<JComponent> allViews = new ArrayList(tabsCount);
                for (int i = 0; i < tabsCount; i++)
                    allViews.add(((TabbedPaneViewport)tabs.getComponentAt(i)).getView());
                return allViews;
            } else if (firstView != null) {
                return Collections.singletonList(firstView);
            } else {
                return Collections.emptyList();
            }
        }

        
        public final void setViewName(JComponent view, String name) {
            if (tabs == null) firstName = name;
            else tabs.setTitleAt(getViewIndex(view), name);
        }

        public final String getViewName(JComponent view) {
            if (tabs == null) return firstName;
            else return tabs.getTitleAt(getViewIndex(view));
        }
        
        public void setViewEnabled(JComponent view, boolean enabled) {
            if (tabs != null) tabs.setEnabledAt(getViewIndex(view), enabled);
            // TODO: handle single view (no tabs) - introduce firstEnabled?
        }

        public boolean isViewEnabled(JComponent view) {
            if (tabs == null) return view == firstView;
            else return tabs.isEnabledAt(getViewIndex(view));
            // TODO: handle single view (no tabs) - introduce firstEnabled?
        }
        

        public void selectView(JComponent view) {
            if (tabs != null) tabs.setSelectedComponent(TabbedPaneViewport.fromView(view));
        }

        public void selectView(int index) {
            if (tabs != null) tabs.setSelectedIndex(index);
        }

        public void selectPreviousView() {
            if (tabs != null) tabs.setSelectedIndex(UIUtils.getPreviousSubTabIndex(tabs, tabs.getSelectedIndex()));
        }

        public void selectNextView() {
            if (tabs != null) tabs.setSelectedIndex(UIUtils.getNextSubTabIndex(tabs, tabs.getSelectedIndex()));
        }

        public JComponent getSelectedView() {
            if (firstView != null) return firstView;
            if (tabs == null) return null;
            return ((TabbedPaneViewport)tabs.getSelectedComponent()).getView();
        }

//        public int getSelectedViewIndex() {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
        
        
        public void highlightView(JComponent view) {
            highlightTab(createViewport(view));
        }
        
        
        protected final void fireChanged() {
            if (listener != null) listener.stateChanged(new ChangeEvent(this));
        }
        
        
        public void addViewListener(Listener listener) {
            if (viewListeners == null) viewListeners = new ArrayList(3);
            viewListeners.add(listener);
        }
    
        public void removeViewListener(Listener listener) {
            if (viewListeners == null) return;
            viewListeners.remove(listener);
            if (viewListeners.isEmpty()) viewListeners = null;
        }
        
        protected final void fireViewAdded(JComponent view) {
            if (viewListeners != null)
                for (Listener viewListener : viewListeners)
                    viewListener.viewAdded(view);
        }
        
        protected final void fireViewRemoved(JComponent view) {
            if (viewListeners != null)
                for (Listener viewListener : viewListeners)
                    viewListener.viewRemoved(view);
        }
        
        
        public void setShowsTabPopup(boolean showsTabPopup) {
            this.showsTabPopup = showsTabPopup;
            if (tabs != null) tabs.setShowsTabPopup(showsTabPopup);
        }
    
        public boolean getShowsTabPopup() {
            return showsTabPopup;
        }
        
        
        protected final TabbedPaneViewport createViewport(JComponent view) {
            return new TabbedPaneViewport(view) {
                Component getFocusMaster() { return focusMaster; }
                int getTabPlacement() { return tabPlacement; }
                boolean minimizeInnerMargin() { return minimizeInnerMargin; }
            };
        }
        
        protected final ProfilerTabbedPane createTabs(int tabPlacement, int tabLayoutPolicy, boolean minimizeOuterMargin) {
            ProfilerTabbedPane tp = new ProfilerTabbedPane() {
                @Override
                protected void closeTab(Component component) {
                    removeView(((TabbedPaneViewport)component).getView());
                }
            };
//            JTabbedPane tp = TabbedPaneFactory.createCloseButtonTabbedPane();
//            JTabbedPane tp = new JTabbedPane();
            tp.setTabPlacement(tabPlacement);
            tp.setTabLayoutPolicy(tabLayoutPolicy);
            tp.setOpaque(false);
            
            if (minimizeOuterMargin) {
                if (UIUtils.isAquaLookAndFeel()) {
                    tp.setBorder(BorderFactory.createEmptyBorder(-13, -11, 0, -10));
                } else if (UIUtils.isNimbusLookAndFeel()) {
                    if (tabPlacement == JTabbedPane.TOP) tp.setBorder(BorderFactory.createEmptyBorder(-4, 0, 0, 0));
                    else tp.setBorder(BorderFactory.createEmptyBorder(0, 0, -4, 0));
                } else {
                    tp.setBorder(BorderFactory.createEmptyBorder());
                    Insets i = UIManager.getInsets("TabbedPane.contentBorderInsets"); // NOI18N
                    int bottomOffset = 0;
                    if (UIUtils.isMetalLookAndFeel()) {
                        bottomOffset = -i.bottom + 1;
                    } else if (UIUtils.isWindowsLookAndFeel()) {
                        bottomOffset = -i.bottom;
                    }
//                    if (i != null) tp.setBorder(BorderFactory.createEmptyBorder(-i.top, -i.left, -i.bottom, -i.right));
                    if (i != null) tp.setBorder(BorderFactory.createEmptyBorder(-i.top, -i.left, bottomOffset, -i.right));
                }
            }

            // Fix for Issue 115062 (CTRL-PageUp/PageDown should move between snapshot tabs)
            tp.getActionMap().getParent().remove("navigatePageUp"); // NOI18N
            tp.getActionMap().getParent().remove("navigatePageDown"); // NOI18N

            tp.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (listener != null) listener.stateChanged(e);
                }
            });

            return tp;
        }
        
        
        private static final int HIGHLIGHTS_COUNT = 2;
        private static final int HIGHLIGHTS_DURATION = 300;
        private final Map<TabbedPaneViewport, Integer> highlights = new HashMap();
        
        private void highlightTab(TabbedPaneViewport viewport) {
            Integer icount = highlights.get(viewport);
            if (icount == null) {
                // new highlight for the tab
                highlights.put(viewport, HIGHLIGHTS_COUNT);
                highlightTabImpl(viewport);
            } else {
                // tab already being highlighted
                if (icount < HIGHLIGHTS_COUNT) highlights.put(viewport, HIGHLIGHTS_COUNT);
            }
        }
        
        private void highlightTabImpl(final TabbedPaneViewport viewport) {
            if (tabs == null) { highlights.remove(viewport); return; }
            
            Integer icount = highlights.get(viewport);
            if (icount == null) {
                return;
            } else if (icount == 0) {
                { highlights.remove(viewport); return; }
            }
            
            int idx = tabs.indexOfComponent(viewport);
            if (idx == -1) { highlights.remove(viewport); return; }
            
            final Color originalForeground = tabs.getForegroundAt(idx);
            Color highlightForeground = Color.BLUE;
            
            final Icon originalIcon = tabs.getIconAt(idx);
            Image image = ImageUtilities.icon2Image(originalIcon);
            RGBImageFilter filter = new RGBImageFilter() {
                public int filterRGB(int x, int y, int rgb) {
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = (rgb) & 0xFF;
                    int a = (rgb >> 24) & 0xFF;
                    
                    return (a << 24) | ((r / 2 & 0xFF) << 16) |
                           ((g / 2 & 0xFF) << 8) | b;
                }
            };
            ImageProducer prod = new FilteredImageSource(image.getSource(), filter);
            Icon highlightIcon = ImageUtilities.image2Icon(Toolkit.getDefaultToolkit().createImage(prod));
            
            decorateTab(viewport, highlightForeground, highlightIcon);
            
            invokeLater(new Runnable() {
                public void run() {
                    decorateTab(viewport, originalForeground, originalIcon);
                    
                    Integer icount = highlights.get(viewport);
                    if (icount == null) {
                        return;
                    } else {
                        highlights.put(viewport, --icount);
                    }
                
                    invokeLater(new Runnable() {
                        public void run() { highlightTabImpl(viewport); }
                    }, HIGHLIGHTS_DURATION);
                }
            }, HIGHLIGHTS_DURATION);
        }
        
        private void decorateTab(TabbedPaneViewport viewport, Color foreground, Icon icon) {
            if (tabs == null) return;
            
            int idx = tabs.indexOfComponent(viewport);
            if (idx == -1) return;
            
            tabs.setForegroundAt(idx, foreground);
            tabs.setIconAt(idx, icon);
        }
        
        private void invokeLater(final Runnable task, int time) {
            final Timer timer = new Timer(time, null);
            timer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timer.stop();
                    task.run();
                }
            });
            timer.start();
        }
        
        
        private static abstract class TabbedPaneViewport extends JPanel {
            
            private final JComponent content;
            
            private Reference<Component> lastFocusOwner;
            
            TabbedPaneViewport(JComponent view) {
                super(new BorderLayout());
                
                content = view;
                
                setOpaque(false);
                setFocusable(false);
//                setBackground(Color.YELLOW);
                add(view, BorderLayout.CENTER);
                view.putClientProperty("TabbedPaneViewport", this); // NOI18N
                
                final PropertyChangeListener focusListener = new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        Component c = evt.getNewValue() instanceof Component ?
                                (Component)evt.getNewValue() : null;
                        processFocusedComponent(c);
                    }
                    private void processFocusedComponent(Component c) {
                        Component cc = c;
                        while (c != null) {
                            if (c == getFocusMaster()) {
                                lastFocusOwner = new WeakReference(cc);
                                return;
                            }
                            c = c.getParent();
                        }
                    }
                };
                
                addHierarchyListener(new HierarchyListener() {
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                            if (isShowing()) {
                                final Component lastFocus = lastFocusOwner == null ? null : lastFocusOwner.get();
                                if (lastFocus != null) lastFocus.requestFocusInWindow();
                                else content.requestFocusInWindow();
                                
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        if (lastFocus != null) lastFocus.requestFocusInWindow();
                                        else content.requestFocusInWindow();
                                    }
                                });
                                
                                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                                    addPropertyChangeListener("focusOwner", focusListener); // NOI18N
                            } else {
                                KeyboardFocusManager.getCurrentKeyboardFocusManager().
                                    removePropertyChangeListener("focusOwner", focusListener); // NOI18N
                            }
                        }
                    }
                });
            }
            
            
            public boolean equals(Object o) {
                if (o == this) return true;
                if (!(o instanceof TabbedPaneViewport)) return false;
                return content.equals(((TabbedPaneViewport)o).content);
            }

            public int hashCode() {
                return content.hashCode();
            }
            
            
//            boolean isClosable() {
//                return !Boolean.TRUE.equals(getClientProperty(TabbedPaneFactory.NO_CLOSE_BUTTON));
//            }
            
            JComponent getView() {
                return content;
            }
            
            JComponent disposeView() {
                content.putClientProperty("TabbedPaneViewport", null); // NOI18N
                removeAll();
                return content;
            }
            
            static TabbedPaneViewport fromView(JComponent view) {
                return (TabbedPaneViewport)view.getClientProperty("TabbedPaneViewport"); // NOI18N
            }
            
            
            abstract Component getFocusMaster();
            
            abstract int getTabPlacement();
            
            abstract boolean minimizeInnerMargin();
            
            
            public void reshape(int x, int y, int w, int h) {
                if (minimizeInnerMargin()) {
                    Rectangle r = offsetRect();
                    super.reshape(x + r.x, y + r.y, w + r.width, h + r.height);
                } else {
                    super.reshape(x, y, w, h);
                }
            }
            
            private Rectangle offsetRect() {
                Rectangle rect = new Rectangle();
                
                if (UIUtils.isNimbus()) {
                    rect.height = 4;
                    if (getTabPlacement() == JTabbedPane.TOP) rect.y -= rect.height;
                } else if (UIUtils.isGTKLookAndFeel()) {
                    rect.height = 1;
                    if (getTabPlacement() == JTabbedPane.TOP) rect.y -= rect.height++;
                } else {
                    Insets tai = UIManager.getInsets("TabbedPane.tabAreaInsets"); // NOI18N
                    Insets cbi = UIManager.getInsets("TabbedPane.contentBorderInsets"); // NOI18N

                    if (tai != null && cbi != null) {
                        if (getTabPlacement() == JTabbedPane.TOP) {
                            rect.y -= cbi.bottom;
                            rect.height -= rect.y;
                        } else {
                            rect.height = tai.bottom + cbi.bottom - 1;
                        }
                    } else {
                    }
                }
                
                return rect;
            }
            
        }
    
    }
    
}
