/*
 * Copyright (c) 2017, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.java.impl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNodeRenderer;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaWindowsView_Name=Windows",
    "JavaWindowsView_Description=Application Windows",
    "JavaWindowsView_ComputingWindows=computing windows...",
    "JavaWindowsView_Preview=Preview:",
    "JavaWindowsView_150px=150px",
    "JavaWindowsView_Tooltip150px=Window preview height: 150px",
    "JavaWindowsView_300px=300px",
    "JavaWindowsView_Tooltip300px=Window preview height: 300px",
    "JavaWindowsView_600px=600px",
    "JavaWindowsView_Tooltip600px=Window preview height: 600px",
    "JavaWindowsView_50pc=50%",
    "JavaWindowsView_Tooltip50pc=Window preview size: 50%",
    "JavaWindowsView_100pc=100%",
    "JavaWindowsView_Tooltip100pc=Window preview size: 100%",
    "JavaWindowsView_NoPreview=<no preview>"
})
class JavaWindowsView extends HeapViewerFeature {
    
    private static final String FEATURE_ID = "java_windows"; // NOI18N
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    
    private final Collection<HeapViewerNodeAction.Provider> actionProviders;
    
    private JComponent component;
    private ProfilerToolbar toolbar;
    
    private int height;
    private double scale;
    
    private final int estWindowCount;
    
    
    JavaWindowsView(HeapContext context, HeapViewerActions actions, int estWindowCount) {
        super(FEATURE_ID, Bundle.JavaWindowsView_Name(), Bundle.JavaWindowsView_Description(), Icons.getIcon(HeapWalkerIcons.WINDOW), 400);
        
        this.context = context;
        this.actions = actions;
        
        this.estWindowCount = estWindowCount;
        
        actionProviders = new ArrayList<>();
        for (HeapViewerNodeAction.Provider provider : Lookup.getDefault().lookupAll(HeapViewerNodeAction.Provider.class))
            if (provider.supportsView(context, FEATURE_ID)) actionProviders.add(provider);
    }

    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    @Override
    protected void closed() {
        // TODO: should cancel the preview
    }
    
    
    private void init() {
        component = new JPanel(null);
        component.setLayout(new BorderLayout());
        
        component.setOpaque(false);
        
        JLabel progress = new JLabel(Bundle.JavaWindowsView_ComputingWindows(), JLabel.LEADING);
        progress.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        final int labelHeight = progress.getPreferredSize().height;
        component.add(progress, BorderLayout.NORTH);
        
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        toolbar.add(new GrayLabel(Bundle.JavaWindowsView_Preview()));
        toolbar.addSpace(3);
        
        final ButtonGroup sizes = new ButtonGroup();
        
        toolbar.add(new JToggleButton(Bundle.JavaWindowsView_150px()) {
            {
                sizes.add(this);
                putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                putClientProperty("JButton.segmentPosition", "first"); // NOI18N
                setToolTipText(Bundle.JavaWindowsView_Tooltip150px());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                if (isSelected()) {
                    scale = -1;
                    height = 150;
                    computeWindows();
                }
            }
        });
        toolbar.add(new JToggleButton(Bundle.JavaWindowsView_300px()) {
            {
                sizes.add(this);
                putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
                setToolTipText(Bundle.JavaWindowsView_Tooltip300px());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                if (isSelected()) {
                    scale = -1;
                    height = 300;
                    computeWindows();
                }
            }
        });
        toolbar.add(new JToggleButton(Bundle.JavaWindowsView_600px()) {
            {
                sizes.add(this);
                putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
                setToolTipText(Bundle.JavaWindowsView_Tooltip600px());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                if (isSelected()) {
                    scale = -1;
                    height = 600;
                    computeWindows();
                }
            }
        });
        toolbar.add(new JToggleButton(Bundle.JavaWindowsView_50pc()) {
            {
                sizes.add(this);
                putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
                setToolTipText(Bundle.JavaWindowsView_Tooltip50pc());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                if (isSelected()) {
                    scale = 0.5d;
                    height = -1;
                    computeWindows();
                }
            }
        });
        toolbar.add(new JToggleButton(Bundle.JavaWindowsView_100pc()) {
            {
                sizes.add(this);
                putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                putClientProperty("JButton.segmentPosition", "last"); // NOI18N
                setToolTipText(Bundle.JavaWindowsView_Tooltip100pc());
            }
            protected void fireItemStateChanged(ItemEvent event) {
                if (isSelected()) {
                    scale = 1d;
                    height = -1;
                    computeWindows();
                }
            }
        });
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Enumeration<AbstractButton> buttons = sizes.getElements();
                AbstractButton selected = buttons.nextElement();
                
                int height = component.getHeight();
                int wcount = Math.min(estWindowCount, 3);
                
                if (wcount * (labelHeight + 300) <= height) selected = buttons.nextElement();
                if (wcount * (labelHeight + 600) <= height) selected = buttons.nextElement();
                
                selected.setSelected(true);
            }
        });
    }
    
    private void computeWindows() {
        new RequestProcessor("Heap Windows Processor").post(new Runnable() { // NOI18N
            public void run() {
                final Heap heap = context.getFragment().getHeap();

                final Collection<Instance> windows = new ArrayList<>();
                windows.addAll(getVisibleFrames(heap));
                windows.addAll(getVisibleDialogs(heap));

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JComponent container = new JPanel(null) {
                            {
                                setOpaque(true);
                                setBackground(UIUtils.getProfilerResultsBackground());
                            }
                            public Dimension getMinimumSize() {
                                return getPreferredSize();
                            }
                        };
                        container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));

                        for (Instance window : windows) {
                            InstanceNode windowNode = new InstanceNode(window);
                            
                            ActionsHandler handler = new ActionsHandler(windowNode);
                            
                            WindowPresenter presenter = new WindowPresenter(windowNode, heap);
                            container.add(new MarginContainer(presenter, BorderLayout.WEST, 15, 10, 0, 10));
                            handler.install(presenter);

                            WindowPreview preview = new WindowPreview(window, heap, scale, height);
                            container.add(new MarginContainer(preview, BorderLayout.WEST, 10, 27, 30, 10));
                            handler.install(preview);

                            container.add(new MarginContainer(UIUtils.createHorizontalSeparator(), 0, 15, 0, 10));
                        }

                        container.remove(container.getComponentCount() - 1); // remove the last separator

                        container.add(Box.createVerticalGlue());

                        component.removeAll();
                        component.add(new ScrollableContainer(container), BorderLayout.CENTER);

                        component.invalidate();
                        Container parent = component.getParent();
                        if (parent != null) parent.revalidate();
                        component.repaint();
                    }
                });
            }
        });
    }
    
    
    private class ActionsHandler extends MouseAdapter {
        
        private final InstanceNode windowNode;
        
        
        ActionsHandler(InstanceNode windowNode) {
            this.windowNode = windowNode;
        }
        
        
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) handleClick(e);
            if (SwingUtilities.isMiddleMouseButton(e)) handleMiddleClick(e);
        }
        
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) handleShowPopup(e);
        }
        
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) handleShowPopup(e);
        }
        
        
        void install(Component c) {
            c.addMouseListener(this);
        }
        
        
        private void handleClick(MouseEvent e) {
            HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(windowNode, actionProviders, context, actions);
            ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), "left button", e.getWhen(), e.getModifiers()); // NOI18N
            nodeActions.performDefaultAction(ae);
        }
        
        private void handleMiddleClick(MouseEvent e) {
            HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(windowNode, actionProviders, context, actions);
            ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), "middle button", e.getWhen(), e.getModifiers()); // NOI18N
            nodeActions.performMiddleButtonAction(ae);
        }
        
        private void handleShowPopup(MouseEvent e) {
            JPopupMenu popup = new JPopupMenu();
            
            HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(windowNode, actionProviders, context, actions);
            nodeActions.populatePopup(popup);
            
            if (popup.getComponentCount() > 0) popup.show((Component)e.getSource(), e.getX(), e.getY());
        }
        
    }
    
    
    private static class MarginContainer extends JPanel {
        
        MarginContainer(Component comp, int top, int left, int bottom, int right) {
            this(comp, BorderLayout.CENTER, top, left, bottom, right);
        }
        
        MarginContainer(Component comp, Object position, int top, int left, int bottom, int right) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
            add(comp, position);
        }
        
        public Dimension getMaximumSize() {
            return new Dimension(super.getMaximumSize().width, super.getPreferredSize().height);
        }
        
    }
    
    
    private static class WindowPresenter extends JPanel {
        
        WindowPresenter(InstanceNode windowNode, Heap heap) {
            super(new BorderLayout());
            
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            InstanceNodeRenderer renderer = new InstanceNodeRenderer(heap);
            renderer.setValue(windowNode, -1);
            
            JComponent rendererC = renderer.getComponent();
            rendererC.setOpaque(false);
            add(rendererC, BorderLayout.WEST);
        }
        
    }
    
    
    private static class WindowPreview extends JPanel {
        
        private final Instance windowInstance;
        private final Heap heap;
        
        private final double scale;
        
        private final int prefHeight;
        private int realHeight;
        private int realWidth;
        
        private Image windowImage;
        
        
        WindowPreview(Instance windowInstance, Heap heap, double scale, int prefHeight) {
            super(new BorderLayout());
            
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            this.windowInstance = windowInstance;
            this.heap = heap;
            
            this.scale = scale;
            
            this.prefHeight = prefHeight;
            this.realHeight = prefHeight;
            this.realWidth = -1;
            
            createPreview();
        }
        
        
        public void paint(Graphics g) {
            if (windowImage != null) {
                g.drawImage(windowImage, 0, 0, null);
            } else {
                super.paint(g);
            }
        }
        
        
        public Dimension getPreferredSize() {
            int width = realWidth == -1 ? super.getPreferredSize().width : realWidth;
            return new Dimension(width, realHeight);
        }
        
        
        public Dimension getMinimumSize() {
            return new Dimension(0, realHeight);
        }
        
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, realHeight);
        }
        
        
        private void createPreview() {
            final JComponent windowComponent = DetailsSupport.getDetailsView(windowInstance);
            if (windowComponent != null) {
                final Component progress = windowComponent.getComponent(0);
                
                windowComponent.addContainerListener(new ContainerListener() {
                    private boolean progressRemoved = false;

                    // handle adding the actual preview
                    public void componentAdded(ContainerEvent e) {
                        if (progressRemoved && windowComponent.getComponentCount() == 2) {
                            windowComponent.removeContainerListener(this);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    Component c = windowComponent.getComponent(1);
                                    c.setMinimumSize(c.getPreferredSize());
                                    
                                    c.invalidate();
                                    Container parent = c.getParent();
                                    if (parent != null) parent.revalidate();
                                    c.doLayout();

                                    double ratio;
                                    Dimension winSize = c.getSize();
                                    if (scale > 0 && scale < 1) { // scaled window
                                        ratio = scale;
                                        realHeight = (int)(ratio * winSize.height) + 1;
                                        realWidth = (int)(ratio * winSize.width) + 1;
                                    } else if (scale >= 1 || winSize.height <= prefHeight) { // real window
                                        ratio = 1;
                                        realHeight = winSize.height;
                                        realWidth = winSize.width;
                                    } else { // fixed-height window
                                        ratio = (double)prefHeight / winSize.height;
                                        realHeight = prefHeight;
                                        realWidth = (int)(ratio * winSize.width) + 1;
                                    }
                                    
                                    // WindowPreview.this.createImage correctly renders text, BufferedImage does better interpolation
                                    windowImage = ratio == 1 ? WindowPreview.this.createImage(realWidth, realHeight) :
                                                  new BufferedImage(realWidth, realHeight, BufferedImage.TYPE_INT_ARGB);
                                    
                                    Graphics g = windowImage.getGraphics();
                                    if (g instanceof Graphics2D) {
                                        Graphics2D g2 = (Graphics2D)g;
                                        g2.setTransform(AffineTransform.getScaleInstance(ratio, ratio));
                                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                                    }
                                    c.paint(g);
                                    g.dispose();
                                    
                                    WindowPreview.this.removeAll();
                                    WindowPreview.this.repaint();
                                }
                            });
                        }
                    }

                    // handle removing the progress label
                    public void componentRemoved(ContainerEvent e) {
                        if (e.getChild() == progress) progressRemoved = true;
                    }
                });
                
                add(windowComponent, BorderLayout.WEST);
            } else {
                JLabel noPreview = new JLabel(Bundle.JavaWindowsView_NoPreview(), JLabel.LEADING);
                noPreview.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
                noPreview.setEnabled(false);
                
                Dimension pref = noPreview.getPreferredSize();
                realWidth = pref.width;
                realHeight = pref.height;
                add(noPreview, BorderLayout.CENTER);
            }
        }
        
    }
    
    
    private static Collection<Instance> getVisibleFrames(Heap heap) {
        Collection<JavaClass> framesC = HeapUtils.getSubclasses(heap, "java.awt.Frame"); // NOI18N
        
        Collection<Instance> framesI = new ArrayList<>();
        for (JavaClass frameC : framesC) framesI.addAll(frameC.getInstances());
        
        return onlyVisible(framesI);
    }
    
    private static Collection<Instance> getVisibleDialogs(Heap heap) {
        Collection<JavaClass> dialogsC = HeapUtils.getSubclasses(heap, "java.awt.Dialog"); // NOI18N
        
        Collection<Instance> dialogsI = new ArrayList<>();
        for (JavaClass dialogC : dialogsC) dialogsI.addAll(dialogC.getInstances());
        
        return onlyVisible(dialogsI);
    }
    
    private static Collection<Instance> onlyVisible(Collection<Instance> instances) {
        Iterator<Instance> framesIt = instances.iterator();
        while (framesIt.hasNext())
            if (!DetailsUtils.getBooleanFieldValue(framesIt.next(), "visible", false)) // NOI18N
                framesIt.remove();
        return instances;
    }
    
    
    @ServiceProvider(service=HeapViewerFeature.Provider.class)
    public static class Provider extends HeapViewerFeature.Provider {

        public HeapViewerFeature getFeature(HeapContext context, HeapViewerActions actions) {
            if (!JavaHeapFragment.isJavaHeap(context)) return null;
            
            Heap heap = context.getFragment().getHeap();
            int estWindowsCount = getVisibleFrames(heap).size();
            if (estWindowsCount <= 1) estWindowsCount += getVisibleDialogs(heap).size();
            
            return estWindowsCount == 0 ? null : new JavaWindowsView(context, actions, estWindowsCount);
        }

    }
    
}
