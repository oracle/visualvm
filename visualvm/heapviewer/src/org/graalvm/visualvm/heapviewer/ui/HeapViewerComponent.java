/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.HeapViewer;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;
import org.graalvm.visualvm.lib.ui.swing.ProfilerPopupMenu;
import org.graalvm.visualvm.lib.ui.swing.StayOpenPopupMenu;
import org.graalvm.visualvm.uisupport.ProfilerTabbedView;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "HeapViewerComponent_LoadingProgress=Opening heap dump...",
    "HeapViewerComponent_Scope=Scope:",
    "HeapViewerComponent_View=View:"
})
public final class HeapViewerComponent extends JPanel {
    
    private final HeapViewer heapViewer;
    private HeapContext[] contexts;
    
    private HeapViewerFeature[][] features;
    
    
    public HeapViewerComponent(HeapViewer heapViewer) {
        super(new BorderLayout());
        setOpaque(false);
        
        this.heapViewer = heapViewer;
        
        add(new JLabel(Bundle.HeapViewerComponent_LoadingProgress(), JLabel.CENTER), BorderLayout.CENTER);
        
        File file = heapViewer.getFile();
        String name = file == null ? "<no heap dump file>" : file.getName(); // NOI18N
        // NOI18N
        new RequestProcessor("HPROF initializer for " + name).post(this::initImpl);
    }
    
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }
    
    private void initImpl() {
        contexts = HeapContext.allContexts(heapViewer);
        
        HeapViewerActions actions = new HeapViewerActions() {
            public HeapViewerFeature findFeature(String id) {
                for (HeapViewerFeature[] featureArr : features)
                    for (HeapViewerFeature feature : featureArr)
                        if (feature.getID().equals(id)) return feature;
                return null;
            }
            public <T extends HeapViewerFeature> T findFeature(Class<T> featureClass) {
                for (HeapViewerFeature[] featureArr : features)
                    for (HeapViewerFeature feature : featureArr)
                        if (feature.getClass() == featureClass) return (T)feature;
                return null;
            }
            public void selectFeature(HeapViewerFeature feature) {
                HeapViewerComponent.this.selectView(mainView);
                mainView.selectFeature(contexts[feature.getScope()], feature);
            }
            public void selectView(HeapView view) {
                HeapViewerComponent.this.selectView(view);
            }
            public void addView(HeapView view, boolean select) {
                HeapViewerComponent.this.addView(view, select, true);
            }
        };
        
        Collection<? extends HeapViewerFeature.Provider> providers = Lookup.getDefault().lookupAll(HeapViewerFeature.Provider.class);
        
        features = new HeapViewerFeature[contexts.length][];
        
        for (int i = 0; i < contexts.length; i++) {
            Set<HeapViewerFeature> featuresS = new TreeSet<>((HeapViewerFeature f1, HeapViewerFeature f2) -> Integer.compare(f1.getPosition(), f2.getPosition()));
            for (HeapViewerFeature.Provider provider : providers) {
                HeapViewerFeature feature = provider.getFeature(contexts[i], actions);
                if (feature != null) {
                    feature.setScope(i);
                    featuresS.add(feature);
                }
            }
            features[i] = featuresS.toArray(new HeapViewerFeature[0]);
        }
        
        SwingUtilities.invokeLater(this::initComponents);
    }

    
//    protected Component defaultFocusOwner() {
//        return currentView == null ? this : currentView.getComponent();
//    }

    // --- UI definition ---------------------------------------------------------
    
    private ProfilerTabbedView views;
    
    private ProfilerToolbar toolbar;
    private ProfilerToolbar viewToolbar;
    
    private HeapView currentView;
    private ProfilerToolbar currentViewToolbar;
    
    private MainView mainView;
    
    private void initComponents() {
        removeAll();
        
        toolbar = ProfilerToolbar.create(true);
        add(toolbar.getComponent(), BorderLayout.NORTH);
        
        // Reserve space for views toolbars
        viewToolbar = ProfilerToolbar.create(false);
        toolbar.add(viewToolbar);
        
        // Add the info action at the end of the toolbar
        toolbar.addFiller();
        toolbar.add(new HeapDumpInfoAction(heapViewer));
        
        
        // Create tabbed view and update the toolbar based on current view
        views = ProfilerTabbedView.createBottom(true, true, new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                final JComponent newComponent = views.getSelectedView();
                if (newComponent != null) viewSelected(getView(newComponent));
            }
        });
        views.addViewListener(new ProfilerTabbedView.Listener() {
            public void viewRemoved(JComponent view) {
                HeapViewerComponent.this.viewClosed(getView(view));
            }
        });
        add(views.getComponent(), BorderLayout.CENTER);
        views.setFocusMaster(this);
        
        // Create the main view
        mainView = new MainView();
        addView(mainView, false, false);
    }
    
    
    private void selectView(HeapView view) {
        // TODO: not selecting features in mainView!
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                views.selectView(view.getComponent());
            }
        });
    }
    
    
    private void addView(final HeapView view, final boolean select, final boolean closable) {
        UIUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                JComponent component = view.getComponent();
                
                if (views.containsView(component)) {
                    if (select) views.selectView(component);
                    else views.highlightView(component);
                } else {
                    component.putClientProperty(HeapView.PROP_KEY, view);
                    views.addView(view.getName(), view.getIcon(), view.getDescription(), component, closable);
                    if (select) views.selectView(component);

                    invalidate();
                    revalidate();
                    doLayout();
                    repaint();
                }
            }
        });
    }
    
    private static HeapView getView(JComponent component) {
        return (HeapView)component.getClientProperty(HeapView.PROP_KEY);
    }
    
    private void viewSelected(HeapView newView) {
        if (currentView == newView) return;
        
        if (currentViewToolbar != null) viewToolbar.remove(currentViewToolbar);
        if (currentView != null) currentView.hidden();
        
        currentView = newView;
        currentView.showing();
        
        currentViewToolbar = currentView.getToolbar();
        if (currentViewToolbar != null) viewToolbar.add(currentViewToolbar);
    }
    
    private void viewClosed(HeapView view) {
        view.closed();
    }
    
    private void updateViewTab(HeapView view) {
        if (views.getViewsCount() > 0) views.updateView(view.getComponent(), view.getName(), view.getIcon(), view.getDescription());
    }
    
    
    public void willBeClosed() {
        if (mainView != null) mainView.willBeClosed(null);
    }
    
    public void closed() {
        if (views != null) views.removeAllViews();
    }
    
    
    private class MainView extends HeapView {
        
        private PopupButton featureChooser;
        
        private ProfilerToolbar toolbar;
        private JComponent component;
        
        private HeapContext selectedContext;
        private HeapViewerFeature selectedFeature;
        
        MainView() {
            super(null, null);
            
            int i = 0;
            while (selectedFeature == null && i < features.length) {
                int j = 0;
                while (selectedFeature == null && j < features[i].length) {
                    if (features[i][j].isDefault()) {
                        selectedContext = contexts[i];
                        selectedFeature = features[i][j];
                    }
                    j++;
                }
                i++;
            }
            if (selectedFeature == null) {
                selectedContext = contexts[0];
                selectedFeature = features[0][0];
            }
        }

        public JComponent getComponent() {
            if (component == null) initUI();
            return component;
        }

        public ProfilerToolbar getToolbar() {
            if (toolbar == null) initUI();
            return toolbar;
        }
        
        
        public String getName() {
            return selectedFeature.getName();
        }

        public String getDescription() {
            return contexts.length == 1 ? selectedFeature.getName() :
                   (selectedContext.getFragment().getName() + ": " + // NOI18N
                   selectedFeature.getName());
        }

        public Icon getIcon() {
            return selectedFeature.getIcon();
        }
        
        
        protected void showing() {
            selectedFeature.showing();
        }
    
        protected void hidden() {
            selectedFeature.hidden();
        }
        
        
        @Override
        protected void willBeClosed(Runnable viewSelector) {
            for (int i = 0; i < features.length; i++) {
                final HeapContext context = contexts[i];
                HeapViewerFeature[] featureArr = features[i];
                for (final HeapViewerFeature feature : featureArr) {
                    Runnable _viewSelector = new Runnable() {
                        public void run() { selectFeature(context, feature); }
                    };
                    feature.willBeClosed(_viewSelector);
                }
            }
        }
        
        @Override
        protected void closed() {
            for (HeapViewerFeature[] featureArr : features)
                    for (HeapViewerFeature feature : featureArr)
                        feature.closed();
        }
        
        
        void selectFeature(HeapContext context, HeapViewerFeature feature) {
            if (selectedFeature != null) selectedFeature.hidden();
            
            selectedContext = context;
            selectedFeature = feature;
            
            selectedFeature.showing();

            featureChooser.setText(getName());
            featureChooser.setToolTipText(getDescription());
            featureChooser.setIcon(getIcon());

            featureChooser.invalidate();
            Container parent = featureChooser.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
    
            component.removeAll();
            component.add(feature.getComponent(), BorderLayout.CENTER);
            component.invalidate();
            parent = component.getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
    
            while (toolbar.getComponentCount() > 1) toolbar.remove(1);
            ProfilerToolbar featureToolbar = feature.getToolbar();
            if (featureToolbar != null) toolbar.add(featureToolbar);

//            viewSelected(this);
            
            updateViewTab(this);
        }
        
        
//        private void updateFocus(final JComponent invoker) {
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    Component focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
//
//                    // Do not change focus for these special cases:
//                    if (focused != null) {
//    //                    if (profilePopupVisible) return; // focus in the Profile popup
//                        if (ProfilerPopup.isInPopup(focused)) return; // focus in a profiler popup
//                        if (HeapViewerComponent.this.isAncestorOf(focused)) return; // focus inside the ProfilerWindow
//                    }
//
//                    SwingUtilities.invokeLater(new Runnable() {
//                        public void run() { invoker.requestFocusInWindow(); }
//                    });
//                }
//            });
//        }
        
        private void displayPopupImpl(final JComponent invoker) {
            final ProfilerPopupMenu popup = new StayOpenPopupMenu() {
//                public void setVisible(boolean visible) {
//                    super.setVisible(visible);
//                    if (!visible) updateFocus(invoker);
//                }
            };
            popup.setLayout(new GridBagLayout());
            if (!UIUtils.isAquaLookAndFeel()) {
                popup.setForceBackground(true);
                Color background = UIUtils.getProfilerResultsBackground();
                popup.setBackground(new Color(background.getRGB())); // JPopupMenu doesn't seem to follow ColorUIResource
                Color foreground = new JLabel().getForeground();
                if (foreground == null) foreground = new JTextArea().getForeground();
                if (foreground == null) foreground = UIUtils.isDarkResultsBackground() ? Color.WHITE : Color.BLACK;
                popup.setForeground(new Color(foreground.getRGB())); // JPopupMenu doesn't seem to follow ColorUIResource
            }

            boolean scopes = contexts.length > 1;
            final boolean[] skipChange = new boolean[1];

            int topl = 8;
            int labl = 8;
            final int left = scopes ? 12 : 4;
            int y = 0;
            GridBagConstraints c;

            class ViewsUpdater {
                int viewsIdx = Integer.MAX_VALUE;
                MenuElement preselect = null;
                void updateViews(final HeapContext context, HeapViewerFeature[] features) {
                    int count = popup.getComponentCount();
                    for (int i = viewsIdx; i < count; i++) popup.remove(viewsIdx);

                    GridBagConstraints c;
                    int y = viewsIdx;

                    for (final HeapViewerFeature feature : features) {
                        c = new GridBagConstraints();
                        c.gridx = 0;
                        c.gridy = y++;
                        c.insets = new Insets(contexts.length == 1 && y == 1 ? 4 : 0, left, 0, 5);
                        c.fill = GridBagConstraints.HORIZONTAL;
                        JMenuItem mi = new JMenuItem(feature.getName(), feature.getIcon()) {
                            protected void fireActionPerformed(ActionEvent e) {
                                selectFeature(context, feature);
                            }
                        };
                        if (selectedFeature == feature) preselect = mi;
                        popup.add(mi, c);
                    }

                    JPanel footer = new JPanel(null);
                    footer.setOpaque(false);
                    c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = y++;
                    c.weightx = 1;
                    c.weighty = 1;
                    c.insets = new Insets(3, 0, 0, 0);
                    c.anchor = GridBagConstraints.NORTHWEST;
                    c.fill = GridBagConstraints.BOTH;
                    popup.add(footer, c);

                    popup.pack();
                }
            }
            final ViewsUpdater updater = new ViewsUpdater();

            if (scopes) {
                JLabel scopeL = new JLabel(Bundle.HeapViewerComponent_Scope(), JLabel.LEADING);
                scopeL.setFont(popup.getFont().deriveFont(Font.BOLD));
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y++;
                c.insets = new Insets(5, labl, 5, 5);
                c.fill = GridBagConstraints.HORIZONTAL;
                popup.add(scopeL, c);    
                
                // Prevent leaking-alike behavior of structures referenced by StayOpenPopupMenu.RadioButtonItem instances
                MultiResolutionImageHack.hackIcon("RadioButtonMenuItem.checkIcon"); // NOI18N

                JMenuItem toSelect = null;
                final ButtonGroup bg = new ButtonGroup();
                for (int i = 0; i < contexts.length; i++) {
                    c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = y++;
                    c.insets = new Insets(0, left, 0, 5);
                    c.fill = GridBagConstraints.HORIZONTAL;
                    final int ii = i;
                    JMenuItem mi = new StayOpenPopupMenu.RadioButtonItem(contexts[i].getFragment().getName()) {
                        protected void fireItemStateChanged(ItemEvent event) {
                            if (isSelected()) {
                                updater.updateViews(contexts[ii], features[ii]);
                                if (!skipChange[0]) selectFeature(contexts[ii], features[ii][0]);
                                else skipChange[0] = false;
                            }
                        }
                    };
                    if (selectedContext == contexts[ii]) toSelect = mi;
                    bg.add(mi);
                    popup.add(mi, c);
                }

                JLabel viewL = new JLabel(Bundle.HeapViewerComponent_View(), JLabel.LEADING);
                viewL.setFont(popup.getFont().deriveFont(Font.BOLD));
                c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = y++;
                c.insets = new Insets(topl, labl, 5, 5);
                c.fill = GridBagConstraints.HORIZONTAL;
                popup.add(viewL, c);

                updater.viewsIdx = y;

                if (toSelect != null) {
                    skipChange[0] = true;
                    toSelect.setSelected(true);
                }
            } else {
                updater.viewsIdx = 0;
                updater.updateViews(contexts[0], features[0]);
            }

            popup.show(invoker, 0, invoker.getHeight());

            if (updater.preselect != null) MenuSelectionManager.defaultManager().setSelectedPath(
                                           new MenuElement[] { popup, updater.preselect });

        }
        
        
        private void initUI() {
            toolbar = ProfilerToolbar.create(false);
            
            featureChooser = new PopupButton() {
                protected void displayPopup() { displayPopupImpl(featureChooser); }
                public int getIconTextGap() { return 5; }
    //            public Dimension getPreferredSize() { Dimension dim = super.getPreferredSize(); dim.width += 10; return dim; }
            };
            toolbar.add(featureChooser);
            
            component = new JPanel(new BorderLayout()) {
                public boolean requestFocusInWindow() {
                    if (getComponentCount() == 0) return super.requestFocusInWindow();
                    else return getComponent(0).requestFocusInWindow();
                }
            };
            component.setOpaque(false);
            
            selectFeature(selectedContext, selectedFeature);
        }
        
    }
    
}
