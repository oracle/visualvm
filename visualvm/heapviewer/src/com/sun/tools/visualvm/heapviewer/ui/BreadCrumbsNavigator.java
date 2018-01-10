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

package com.sun.tools.visualvm.heapviewer.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.LanguageIcons;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.model.RootNode;
import com.sun.tools.visualvm.heapviewer.swing.LinkButton;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author JiriSedlacek
 */
@NbBundle.Messages({
    "BreadCrumbsNavigator_Pin=Pin",
    "BreadCrumbsNavigator_ResetPin=Reset Pin",
    "BreadCrumbsNavigator_Class=class",
    "BreadCrumbsNavigator_SelectNode=Select {0}",
    "BreadCrumbsNavigator_ResetPinSelectNode=Reset Pin and Select {0}",
    "BreadCrumbsNavigator_ResetView=Reset View"
})
abstract class BreadCrumbsNavigator {
    
    private static final Icon ICON_SEPARATOR = ImageUtilities.image2Icon(ImageUtilities.loadImage(BreadCrumbsNavigator.class.getPackage().getName().replace('.', '/') + "/separator.png", true));
    private static final Icon ICON_PIN = ImageUtilities.image2Icon(ImageUtilities.loadImage(BreadCrumbsNavigator.class.getPackage().getName().replace('.', '/') + "/pin.png", true));
    private static final Icon ICON_PIN_SELECTED = ImageUtilities.image2Icon(ImageUtilities.loadImage(BreadCrumbsNavigator.class.getPackage().getName().replace('.', '/') + "/pin_selected.png", true));
    
    
    BreadCrumbsNavigator() {
    }
    
    
    private JComponent component;
    
    
    abstract void nodeClicked(HeapViewerNode node);
    
    abstract void nodePinned(HeapViewerNode node);
    
    abstract void openNode(HeapViewerNode node);
    
    
    abstract HeapViewerRenderer getRenderer(HeapViewerNode node);
    
    
    abstract HeapViewerNodeAction.Actions getNodeActions(HeapViewerNode node);
    
    
    Component getComponent() {
        if (component == null) init();
        return component;
    }
    
    
    void setNode(HeapViewerNode node, HeapViewerNode pinnedNode, HeapViewerNode root, String viewName) {
        if (component == null) init();
        setNodeImpl(node, pinnedNode, root, viewName);
    }
    
    private void setNodeImpl(HeapViewerNode node, final HeapViewerNode pinnedNode, HeapViewerNode root, String viewName) {
        component.removeAll();
        
        HeapViewerNode visitedPinnedNode = null;
        
        while (node != null && !(node instanceof RootNode)) {
            component.add(createSeparator(), 0);
            
            final HeapViewerNode nodeF = node;
            
            if (viewName != null || node != root) if (!node.isLeaf()) { // NOTE: may it make sense to pin empty nodes?
                
                component.add(Box.createHorizontalStrut(2), 0);
                
                PinButton pb = new PinButton() {
                    protected void fireActionPerformed(ActionEvent e) {
                        super.fireActionPerformed(e);
                        nodePinned(isSelected() ? nodeF : null);
                    }
                };
                pb.setSelected(node.equals(pinnedNode));
                pb.setToolTipText(pb.isSelected() ? Bundle.BreadCrumbsNavigator_ResetPin() : Bundle.BreadCrumbsNavigator_Pin());
                if (pb.isSelected()) visitedPinnedNode = pinnedNode;
                component.add(pb, 0);
                component.add(Box.createHorizontalStrut(3), 0);
            }
            
            HeapViewerRenderer renderer = getRenderer(node);
            
            LinkButton lb = new LinkButton(renderer.getShortName(), renderer.getIcon()) {
                protected void clicked() {
                    nodeClicked(nodeF);
                }
                protected void middleClicked(MouseEvent e) {
                    HeapViewerNodeAction.Actions nodeActions = getNodeActions(nodeF);
                    ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), "middle button", e.getWhen(), e.getModifiers()); // NOI18N
                    nodeActions.performMiddleButtonAction(ae);
                }
                protected void populatePopup(JPopupMenu popup) {
                    HeapViewerNodeAction.Actions nodeActions = getNodeActions(nodeF);
                    nodeActions.populatePopup(popup);
                }
            };
            boolean beforePinnedNode = visitedPinnedNode != null && visitedPinnedNode != node;
            if (beforePinnedNode) lb.setForeground(UIUtils.getDisabledLineColor());
            lb.setToolTipText(beforePinnedNode ? Bundle.BreadCrumbsNavigator_ResetPinSelectNode(renderer.toString()) : Bundle.BreadCrumbsNavigator_SelectNode(renderer.toString()));
            lb.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e)) openNode(nodeF);
                }
            });

            component.add(lb, 0);
            
            node = (HeapViewerNode)node.getParent();
        }
        
        if (viewName != null) {
            component.add(createSeparator(), 0);

            LinkButton lb = new LinkButton(viewName) {
                protected void clicked() { nodeClicked(null); }
            };
            if (visitedPinnedNode != null) lb.setForeground(UIUtils.getDisabledLineColor());
            lb.setToolTipText(Bundle.BreadCrumbsNavigator_ResetView());
            component.add(lb, 0);
        
            component.add(Box.createHorizontalStrut(4), 0);
        }
        
        component.invalidate();
        component.revalidate();
        component.repaint();
    }
    
    private static JComponent createSeparator() {
        JLabel sepL = new JLabel(ICON_SEPARATOR);
        sepL.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
        return sepL;
    }
    
    
    private void init() {
        final int refHeight = new LinkButton("XXX", Icons.getIcon(LanguageIcons.CLASS)).getPreferredSize().height + 3; // NOI18N
        component = new JPanel(null) {
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.height = refHeight;
                return dim;
            }
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.height = refHeight;
                return dim;
            }
        };
        component.setLayout(new BoxLayout(component, BoxLayout.LINE_AXIS));
        component.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground"))); // NOI18N
    }
    
    
    private static class PinButton extends JToggleButton {
        PinButton() {
            super(ICON_PIN);
            setSelectedIcon(ICON_PIN_SELECTED);
            setRolloverIcon(ICON_PIN_SELECTED);
            setPressedIcon(ICON_PIN_SELECTED);
            setOpaque(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setFocusPainted(false);
            setMargin(new Insets(1, 2, 1, 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }
    
}
