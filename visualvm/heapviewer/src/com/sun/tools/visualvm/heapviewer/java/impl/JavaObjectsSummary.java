/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.java.impl;

import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.java.ClassNode;
import com.sun.tools.visualvm.heapviewer.java.ClassNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.InstanceNode;
import com.sun.tools.visualvm.heapviewer.java.InstanceNodeRenderer;
import com.sun.tools.visualvm.heapviewer.java.JavaHeapFragment;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.swing.LinkButton;
import com.sun.tools.visualvm.heapviewer.swing.Splitter;
import com.sun.tools.visualvm.heapviewer.ui.HeapView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.SummaryView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import com.sun.tools.visualvm.uisupport.SeparatorLine;
import com.sun.tools.visualvm.uisupport.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.ProfilerTable;
import org.netbeans.lib.profiler.ui.swing.renderer.HideableBarRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.LabelRenderer;
import org.netbeans.lib.profiler.ui.swing.renderer.ProfilerRenderer;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "JavaObjectsSummary_Name=Statistics",
    "JavaObjectsSummary_Description=Statistics",
    "JavaObjectsSummary_HeapSection=Heap",
    "JavaObjectsSummary_ClassesInstancesCount=Classes by Instances Count",
    "JavaObjectsSummary_ClassesInstancesSize=Classes by Instances Size",
    "JavaObjectsSummary_InstancesSize=Instances by Size",
    "JavaObjectsSummary_DominatorsRetainedSize=Dominators by Retained Size",
    "JavaObjectsSummary_ComputeRetainedSizesLbl=Retained sizes must be computed first:",
    "JavaObjectsSummary_ComputeRetainedSizesBtn=Compute Retained Sizes",
    "JavaObjectsSummary_ComputingRetainedSizes=computing retained sizes...",
    "JavaObjectsSummary_ComputingProgress=computing...",
    "JavaObjectsSummary_ViewAll=view all",
    "JavaObjectsSummary_NameColumn=Name",
    "JavaObjectsSummary_ValueColumn=Value"
})
class JavaObjectsSummary extends HeapView {
    
    private static final int PREVIEW_ITEMS = 5;
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    private final Collection<HeapViewerNodeAction.Provider> actionProviders;
    
    private JComponent component;
    
    
    private JavaObjectsSummary(HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
        super(Bundle.JavaObjectsSummary_Name(), Bundle.JavaObjectsSummary_Description());
        
        this.context = context;
        this.actions = actions;
        this.actionProviders = actionProviders;
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
        Heap heap = context.getFragment().getHeap();
        final ClassNodeRenderer classRenderer = new ClassNodeRenderer(heap);
        final InstanceNodeRenderer instanceRenderer = new InstanceNodeRenderer(heap);
        
        TreeTableViewColumn sizeColumn = new TreeTableViewColumn.OwnSize(heap);
        final HideableBarRenderer sizeRenderer = (HideableBarRenderer)sizeColumn.getRenderer();
        
        TreeTableViewColumn classesByCountColumn = new TreeTableViewColumn.Count(heap);
        final HideableBarRenderer classesByCountRenderer = (HideableBarRenderer)classesByCountColumn.getRenderer();
        
        Runnable classesByCountDisplayer = new Runnable() {
            public void run() {
                JavaObjectsView objectsView = actions.findFeature(JavaObjectsView.class);
                if (objectsView != null) {
                    objectsView.configureClassesByInstancesCount();
                    actions.selectFeature(objectsView);
                }
            }
        };
        final ResultsSnippet classesByCount = new ResultsSnippet(Bundle.JavaObjectsSummary_ClassesInstancesCount(), classesByCountDisplayer) {
            protected void setupTable(ProfilerTable table) {
                table.setColumnRenderer(0, classRenderer);
                table.setColumnRenderer(1, classesByCountRenderer);
                table.setDefaultColumnWidth(1, classesByCountRenderer.getNoBarWidth() + 10);
            }
        };
        
        Runnable classesBySizeDisplayer = new Runnable() {
            public void run() {
                JavaObjectsView objectsView = actions.findFeature(JavaObjectsView.class);
                if (objectsView != null) {
                    objectsView.configureClassesByInstancesSize();
                    actions.selectFeature(objectsView);
                }
            }
        };
        final ResultsSnippet classesBySize = new ResultsSnippet(Bundle.JavaObjectsSummary_ClassesInstancesSize(), classesBySizeDisplayer) {
            protected void setupTable(ProfilerTable table) {
                table.setColumnRenderer(0, classRenderer);
                table.setColumnRenderer(1, sizeRenderer);
                table.setDefaultColumnWidth(1, sizeRenderer.getNoBarWidth() + 10);
            }
        };
        
        Splitter classesRow = new Splitter(Splitter.HORIZONTAL_SPLIT, classesByCount, classesBySize);
        
        Runnable instancesBySizeDisplayer = new Runnable() {
            public void run() {
                JavaObjectsView objectsView = actions.findFeature(JavaObjectsView.class);
                if (objectsView != null) {
                    objectsView.configureInstancesBySize();
                    actions.selectFeature(objectsView);
                }
            }
        };
        final ResultsSnippet instancesBySize = new ResultsSnippet(Bundle.JavaObjectsSummary_InstancesSize(), instancesBySizeDisplayer) {
            protected void setupTable(ProfilerTable table) {
                table.setColumnRenderer(0, instanceRenderer);
                table.setColumnRenderer(1, sizeRenderer);
                table.setDefaultColumnWidth(1, sizeRenderer.getNoBarWidth() + 10);
            }
        };
        
        final boolean retainedAvailable = DataType.RETAINED_SIZE.valuesAvailable(heap);
        Runnable dominatorsByRetainedSizeDisplayer = new Runnable() {
            public void run() {
                JavaObjectsView objectsView = actions.findFeature(JavaObjectsView.class);
                if (objectsView != null) {
                    objectsView.configureDominatorsByRetainedSize();
                    actions.selectFeature(objectsView);
                }
            }
        };
        final ResultsSnippet dominatorsByRetainedSize = new ResultsSnippet(Bundle.JavaObjectsSummary_DominatorsRetainedSize(), dominatorsByRetainedSizeDisplayer) {
            private Runnable retainedSizesUpdater;
            protected JComponent createComponent() {
                if (retainedAvailable) {
                    return super.createComponent();
                } else {
                    final JPanel p = new JPanel(new GridBagLayout());
                    p.setOpaque(false);
                    
                    final JLabel l = new JLabel(Bundle.JavaObjectsSummary_ComputeRetainedSizesLbl(), JLabel.LEADING);
                    GridBagConstraints c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = 0;
                    c.fill = GridBagConstraints.NONE;
                    c.anchor = GridBagConstraints.NORTHWEST;
                    c.insets = new Insets(2, 2, 2, 2);
                    p.add(l, c);
                    
                    c = new GridBagConstraints();
                    c.gridx = 1;
                    c.gridy = 0;
                    c.weightx = 1d;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.anchor = GridBagConstraints.NORTHWEST;
                    c.insets = new Insets(0, 0, 0, 0);
                    p.add(UIUtils.createFillerPanel(), c);
                    
                    c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = 1;
                    c.fill = GridBagConstraints.NONE;
                    c.anchor = GridBagConstraints.NORTHWEST;
                    c.insets = new Insets(2, 2, 2, 2);
                    
                    JButton lb = new JButton(Bundle.JavaObjectsSummary_ComputeRetainedSizesBtn()) {
                        protected void fireActionPerformed(ActionEvent e) {
                            if (DataType.RETAINED_SIZE.computeValues(heap, null)) {
                                p.remove(this);
                                l.setText(Bundle.JavaObjectsSummary_ComputingRetainedSizes());
                                l.setIcon(Icons.getIcon(HeapWalkerIcons.PROGRESS));
                                p.invalidate();
                                p.revalidate();
                                p.repaint();
                            }
                        }
                    };
                    
                    final ResultsSnippet rs = this;
                    retainedSizesUpdater = new Runnable() {
                        public void run() {
                            new RequestProcessor("Objects Summary Retained Sizes Worker").post(new Runnable() { // NOI18N
                                public void run() {
                                    computeDominators(heap, rs);
                                    retainedSizesUpdater = null;
                                }
                            });
                        }
                    };
                    DataType.RETAINED_SIZE.notifyWhenAvailable(heap, retainedSizesUpdater);
                    
                    p.add(lb, c);
                    
                    c = new GridBagConstraints();
                    c.gridx = 1;
                    c.gridy = 1;
                    c.weightx = 1d;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.anchor = GridBagConstraints.NORTHWEST;
                    c.insets = new Insets(0, 0, 0, 0);
                    p.add(UIUtils.createFillerPanel(), c);
                    
                    c = new GridBagConstraints();
                    c.gridx = 0;
                    c.gridy = 2;
                    c.weightx = 1d;
                    c.weighty = 1d;
                    c.fill = GridBagConstraints.BOTH;
                    c.anchor = GridBagConstraints.NORTHWEST;
                    c.insets = new Insets(0, 0, 0, 0);
                    p.add(UIUtils.createFillerPanel(), c);
                    
                    return p;
                }
            }
            protected void setupTable(ProfilerTable table) {
                table.setColumnRenderer(0, instanceRenderer);
                
                TreeTableViewColumn dominatorsByRetainedSizeColumn = new TreeTableViewColumn.RetainedSize(heap);
                HideableBarRenderer retainedRenderer = (HideableBarRenderer)dominatorsByRetainedSizeColumn.getRenderer();
                table.setColumnRenderer(1, retainedRenderer);
                table.setDefaultColumnWidth(1, retainedRenderer.getNoBarWidth() + 10);
            }
        };
        
        Splitter instancesRow = new Splitter(Splitter.HORIZONTAL_SPLIT, instancesBySize, dominatorsByRetainedSize);
        
        
        component = new JPanel(new VerticalLayout(false, 5)) {
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.width = 0;
                return dim;
            }

            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.width = 100;
                return dim;
            }
        };
        component.setOpaque(false);
        
        component.add(classesRow);
        component.add(instancesRow);
        
        new RequestProcessor("Objects Summary Worker").post(new Runnable() { // NOI18N
            public void run() {
                Heap heap = context.getFragment().getHeap();
                List<JavaClass> allClasses = new ArrayList(heap.getAllClasses());
                
                // --- Classes by Instances Count ------------------------------
                Collections.sort(allClasses, new Comparator<JavaClass>() {
                    @Override
                    public int compare(JavaClass c1, JavaClass c2) {
                        return Integer.compare(c2.getInstancesCount(), c1.getInstancesCount());
                    }
                });
                int items = Math.min(PREVIEW_ITEMS, allClasses.size());
                JavaClass[] classesByCountArr = allClasses.subList(0, items).toArray(new JavaClass[0]);
                Object[][] classesByCountData = new Object[classesByCountArr.length][2];
                for (int i = 0; i < classesByCountData.length; i++) {
                    classesByCountData[i][0] = new ClassNode(classesByCountArr[i]);
                    classesByCountData[i][1] = classesByCountArr[i].getInstancesCount();
                }
                configureSnippet(classesByCount, classesByCountData);
                
                
                // --- Classes by Instances Size -------------------------------
                Collections.sort(allClasses, new Comparator<JavaClass>() {
                    @Override
                    public int compare(JavaClass c1, JavaClass c2) {
                        return Long.compare(c2.getAllInstancesSize(), c1.getAllInstancesSize());
                    }
                });
                JavaClass[] classesBySizeArr = allClasses.subList(0, items).toArray(new JavaClass[0]);
                Object[][] classesBySizeData = new Object[classesBySizeArr.length][2];
                for (int i = 0; i < classesBySizeData.length; i++) {
                    classesBySizeData[i][0] = new ClassNode(classesBySizeArr[i]);
                    classesBySizeData[i][1] = classesBySizeArr[i].getAllInstancesSize();
                }
                configureSnippet(classesBySize, classesBySizeData);
                
                
                // --- Instances by Size ---------------------------------------
                
                items = (int)Math.min(PREVIEW_ITEMS, heap.getSummary().getTotalLiveInstances());
                PriorityQueue<Instance> pqBySize = new PriorityQueue(items, new Comparator<Instance>() {
                    @Override
                    public int compare(Instance i1, Instance i2) {
                        return Long.compare(i1.getSize(), i2.getSize());
                    }
                });
                Iterator<Instance> allInstances = heap.getAllInstancesIterator();
                while (allInstances.hasNext()) {
                    Instance in = allInstances.next();
                    if (pqBySize.size() < items || pqBySize.peek().getSize() < in.getSize()) {
                        if (pqBySize.size() == items) pqBySize.remove();
                        pqBySize.add(in);
                    }
                }
                Instance[] instancesBySizeArr = new Instance[pqBySize.size()];
                for (int i = instancesBySizeArr.length - 1; i >= 0; i--)
                    instancesBySizeArr[i] = pqBySize.poll();
                Object[][] instancesBySizeData = new Object[instancesBySizeArr.length][2];
                for (int i = 0; i < instancesBySizeData.length; i++) {
                    instancesBySizeData[i][0] = new InstanceNode(instancesBySizeArr[i]);
                    instancesBySizeData[i][1] = instancesBySizeArr[i].getSize();
                }
                configureSnippet(instancesBySize, instancesBySizeData);
                
                
                // --- Dominators by Retained Size -----------------------------
                if (retainedAvailable) computeDominators(heap, dominatorsByRetainedSize);
            }
        });
    }
    
    
    private void computeDominators(Heap heap, ResultsSnippet dominatorsByRetainedSize) {
        List<Instance> dominators = new ArrayList(JavaClassesProvider.getDominatorRoots(heap));
        Collections.sort(dominators, new Comparator<Instance>() {
            @Override
            public int compare(Instance i1, Instance i2) {
                return Long.compare(i2.getRetainedSize(), i1.getRetainedSize());
            }
        });
        int items = Math.min(PREVIEW_ITEMS, dominators.size());
        Instance[] dominatorsByRetainedSizeArr = dominators.subList(0, items).toArray(new Instance[0]);
        Object[][] dominatorsByRetainedSizeData = new Object[dominatorsByRetainedSizeArr.length][2];
        for (int i = 0; i < dominatorsByRetainedSizeData.length; i++) {
            dominatorsByRetainedSizeData[i][0] = new InstanceNode(dominatorsByRetainedSizeArr[i]);
            dominatorsByRetainedSizeData[i][1] = dominatorsByRetainedSizeArr[i].getRetainedSize();
        }
        configureSnippet(dominatorsByRetainedSize, dominatorsByRetainedSizeData);
    }
    
    
    private void configureSnippet(final ResultsSnippet snippet, final Object[][] data) {
        final TableModel model = new DefaultTableModel(data, new Object[] {
                                            Bundle.JavaObjectsSummary_NameColumn(),
                                            Bundle.JavaObjectsSummary_ValueColumn() }) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                snippet.setRealModel(model);
            }
        });
    }
    
    
    private class ResultsSnippet extends JPanel {
        
        private final LinkButton link;
        private ProfilerTable table;
        private boolean keepSelection;
        
        ResultsSnippet(String text, final Runnable allDisplayer) {
            super(new BorderLayout(0, 6));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
            
            JPanel sectionSeparator = new JPanel(new GridBagLayout());
            sectionSeparator.setOpaque(false);
            
            JLabel caption = new JLabel(text);
            caption.setFont(caption.getFont().deriveFont(Font.BOLD));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weighty = 1d;
            sectionSeparator.add(caption, c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.insets = new Insets(0, 5, 0, 0);
            sectionSeparator.add(new JLabel("["), c); // NOI18N
            
            link = new LinkButton(Bundle.JavaObjectsSummary_ViewAll()) {
                @Override
                protected void clicked() {
                    SwingUtilities.invokeLater(allDisplayer);
                }
            };
            link.setEnabled(false);
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 0, 0);
            sectionSeparator.add(link, c);
            
            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 0, 0);
            sectionSeparator.add(new JLabel("]"), c); // NOI18N

            c = new GridBagConstraints();
            c.gridx = 4;
            c.gridy = 0;
            c.weightx = 1d;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(1, 4, 0, 0);
            sectionSeparator.add(new SeparatorLine(), c);
            
            add(sectionSeparator, BorderLayout.NORTH);

            JComponent comp = createComponent();
            
            if (comp instanceof ProfilerTable) table = (ProfilerTable)comp;
            if (comp != null) add(comp, BorderLayout.CENTER);
        }
        
        public Dimension getMinimumSize() {
            Dimension dim = super.getMinimumSize();
            dim.width = 0;
            return dim;
        }
        
        public Dimension getPreferredSize() {
            Dimension dim = super.getPreferredSize();
            dim.width = 100;
            return dim;
        }
        
        protected JComponent createComponent() {
            TableModel model = new DefaultTableModel(PREVIEW_ITEMS, 1) {
                { setValueAt(Bundle.JavaObjectsSummary_ComputingProgress(), 0, 0); }
                public boolean isCellEditable(int row, int column) { return false; }
            };

            ProfilerRenderer renderer = new LabelRenderer() {
                public void setValue(Object o, int i) {
                    super.setValue(o, i);
                    setIcon(o == null || o.toString().isEmpty() ? null : Icons.getIcon(HeapWalkerIcons.PROGRESS));
                }
            };

            ProfilerTable t = createTable(model);
            t.setDefaultRenderer(Object.class, renderer);
            
            return t;
        }
        
        void setRealModel(TableModel model) {
            if (table == null) {
                BorderLayout bl = (BorderLayout)getLayout();
                Component c = bl.getLayoutComponent(BorderLayout.CENTER);
                if (c != null) remove(c);
                
                table = createTable(model);
                add(table, BorderLayout.CENTER);
                
                getParent().invalidate();
                getParent().revalidate();
                getParent().repaint();
            }
            
            table.setModel(model);
            setupTable(table);
            
            link.setEnabled(true);
        }
        
        protected void setupTable(ProfilerTable table) {}
        
        private ProfilerTable createTable(TableModel model) {
            ProfilerTable t = new SummaryView.SimpleTable(model, 0) {
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
        
    }
    
    
    @ServiceProvider(service=SummaryView.ContentProvider.class, position = 300)
    public static class Provider extends SummaryView.ContentProvider {

        @Override
        public HeapView createSummary(String viewID, HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            if (JavaHeapFragment.isJavaHeap(context)) return new JavaObjectsSummary(context, actions, actionProviders);
            return null;
        }
        
    }
    
}
