/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.JavaHeapFragment;
import org.graalvm.visualvm.heapviewer.java.ThreadNode;
import org.graalvm.visualvm.heapviewer.java.ThreadNodeRenderer;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.swing.LinkButton;
import org.graalvm.visualvm.heapviewer.ui.HeapView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerNodeAction;
import org.graalvm.visualvm.heapviewer.ui.SummaryView;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ThreadObjectGCRoot;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.graalvm.visualvm.uisupport.SeparatorLine;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaThreadsSummary_Name=OutOfMemoryError Thread",
    "JavaThreadsSummary_Description=OutOfMemoryError Thread",
    "JavaThreadsSummary_Hint=This heap dump has been created automatically on an OutOfMemoryError thrown in this thread:",
    "JavaThreadsSummary_ViewAll=view all",
    "JavaThreadsSummary_NameColumn=Name"
})
class JavaThreadsSummary extends HeapView {
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    private final Collection<HeapViewerNodeAction.Provider> actionProviders;
    
    private JComponent component;
    
    private final Object[][] threadData;
    private boolean keepSelection;
    
    
    private JavaThreadsSummary(Instance oomeInstance, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
        super(Bundle.JavaThreadsSummary_Name(), Bundle.JavaThreadsSummary_Description());
        
        this.context = context;
        this.actions = actions;
        this.actionProviders = actionProviders;
        
        JavaClass vtClass = oomeInstance.getJavaClass().getHeap().getJavaClassByName("java.lang.VirtualThread");    // NOI18N
        String threadName = JavaThreadsProvider.getThreadName(vtClass, oomeInstance);
        threadData = new Object[][] {{ new ThreadNode(threadName, null, true, oomeInstance) }};
    }
    
    
    @Override
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    @Override
    public ProfilerToolbar getToolbar() {
        return null;
    }
    
    
    private void init() {
        component = new JPanel(new GridBagLayout()) {
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.width = 0;
                return dim;
            }

            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width = 0;
                return dim;
            }
        };
        component.setOpaque(false);
        component.setBorder(BorderFactory.createEmptyBorder(12, 5, 0, 5));
        
        JLabel caption = new JLabel(Bundle.JavaThreadsSummary_Name());
        caption.setFont(caption.getFont().deriveFont(Font.BOLD));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 1d;
        c.insets = new Insets(0, 0, 0, 0);
        component.add(caption, c);

        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 5, 0, 0);
        component.add(new JLabel("["), c); // NOI18N

        LinkButton link = new LinkButton(Bundle.JavaThreadsSummary_ViewAll()) {
            @Override
            protected void clicked() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JavaThreadsView threadsView = actions.findFeature(JavaThreadsView.class);
                        if (threadsView != null) {
                            threadsView.configureAllThreads();
                            actions.selectFeature(threadsView);
                        }
                    }
                });
            }
        };
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        component.add(link, c);

        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        component.add(new JLabel("]"), c); // NOI18N

        c = new GridBagConstraints();
        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 1d;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(1, 4, 0, 0);
        component.add(new SeparatorLine(), c);
        
        
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(8, 1, 3, 0);
        component.add(new JLabel(Bundle.JavaThreadsSummary_Hint(), JLabel.LEADING), c);
        
        final TableModel model = new DefaultTableModel(threadData, new Object[] {
                                            Bundle.JavaThreadsSummary_NameColumn() }) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        ProfilerTable thread = createTable(model);
        ProfilerRenderer renderer = new ThreadNodeRenderer(context.getFragment().getHeap());
        thread.setColumnRenderer(0, renderer);
        Dimension dim = thread.getPreferredSize();
        renderer.setValue(threadData[0][0], 0);
        dim.width = renderer.getComponent().getPreferredSize().width + 4;
        thread.setPreferredSize(dim);
        thread.setMinimumSize(dim);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        component.add(thread, c);
    }
    
    
    private ProfilerTable createTable(TableModel model) {
        ProfilerTable t = new SummaryView.SimpleTable(model, 0) {
            public void setBounds(int x, int y, int w, int h) {
                Container parent = getParent();
                if (parent != null) w = Math.min(w, parent.getWidth());
                super.setBounds(x, y, w, h);
            }
            protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                requestFocusInWindow(); // TODO: should be done by ProfilerTable on selectRow(...) in processMouseEvent(...)

                HeapViewerNode node = (HeapViewerNode)value;
                HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions);
                nodeActions.populatePopup(popup);

                if (popup.getComponentCount() > 0) popup.addSeparator();
                popup.add(createCopyMenuItem());
            }
            public void performDefaultAction(ActionEvent e) {
                int row = getSelectedRow();
                if (row == -1) return;

                Object value = getValueForRow(row);
                if (!(value instanceof HeapViewerNode)) return;

                HeapViewerNodeAction.Actions nodeActions =
                        HeapViewerNodeAction.Actions.forNode((HeapViewerNode)value, actionProviders, context, actions);
                nodeActions.performDefaultAction(e);
            }
            protected void popupShowing() {
                keepSelection = true;
            }
            protected void popupHidden() {
                keepSelection = false;
                
                new Timer(100, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (!isFocusOwner()) clearSelection();
                    }
                }) { { setRepeats(false); } }.start();
            }
        };
        t.setRowSelectionAllowed(true);
        t.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (!keepSelection) t.clearSelection();
                else keepSelection = false;
            }
        });
        t.providePopupMenu(true);
        t.setSelectionOnMiddlePress(true);
        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    int row = t.getSelectedRow();
                    if (row == -1) return;

                    Object value = t.getValueForRow(row);
                    if (!(value instanceof HeapViewerNode)) return;

                    HeapViewerNode node = (HeapViewerNode)value;
                    HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions);
                    ActionEvent ae = new ActionEvent(e.getSource(), e.getID(), "middle button", e.getWhen(), e.getModifiers()); // NOI18N
                    nodeActions.performMiddleButtonAction(ae);
                }
            }
        });

        return t;
    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 200)
    public static class Provider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (!JavaHeapFragment.isJavaHeap(context)) return null;
            
            ThreadObjectGCRoot oomeThread = JavaThreadsProvider.getOOMEThread(context.getFragment().getHeap());
            Instance oomeInstance = oomeThread == null ? null : oomeThread.getInstance();
            if (oomeInstance == null) return null;
            
            return new JavaThreadsSummary(oomeInstance, context, actions, actionProviders);
        }
        
    }
    
}
