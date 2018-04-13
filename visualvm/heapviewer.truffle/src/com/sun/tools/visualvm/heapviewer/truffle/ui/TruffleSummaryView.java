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
package com.sun.tools.visualvm.heapviewer.truffle.ui;

import com.sun.tools.visualvm.core.ui.components.SectionSeparator;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.model.DataType;
import com.sun.tools.visualvm.heapviewer.model.HeapViewerNode;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleLanguageHeapFragment;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleObject;
import com.sun.tools.visualvm.heapviewer.truffle.TruffleType;
import com.sun.tools.visualvm.heapviewer.ui.HeapView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerNodeAction;
import com.sun.tools.visualvm.heapviewer.ui.SummaryView;
import com.sun.tools.visualvm.heapviewer.ui.TreeTableViewColumn;
import com.sun.tools.visualvm.uisupport.SeparatorLine;
import com.sun.tools.visualvm.uisupport.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.Component;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import javax.swing.BorderFactory;
import javax.swing.Icon;
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

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleSummaryView_Name=Summary",
    "TruffleSummaryView_Description=Summary",
    "TruffleSummaryView_ComputingProgress=computing...",
    "TruffleSummaryView_NameColumn=Name",
    "TruffleSummaryView_ValueColumn=Value"
})
public class TruffleSummaryView extends HeapViewerFeature {
    
    private final SummaryView summaryView;
    
    
    public TruffleSummaryView(String id, Icon icon, HeapContext context, HeapViewerActions actions) {
        super(id, Bundle.TruffleSummaryView_Name(), Bundle.TruffleSummaryView_Description(), icon, 100);
        
        summaryView = new SummaryView(id, context, actions) {
            @Override
            protected void uiCreated(List<HeapView> views) {
                if (!views.isEmpty()) new RequestProcessor("Truffle Summary Worker").post(new Runnable() { // NOI18N
                    public void run() {
                        for (HeapView view : views)
                            if (view instanceof TruffleSummarySection)
                                ((TruffleSummarySection) view).computeData();
                    }
                });
            }
        };
    }
    

    @Override
    public boolean isDefault() {
        return true;
    }
    
    @Override
    public JComponent getComponent() {
        return summaryView.getComponent();
    }

    @Override
    public ProfilerToolbar getToolbar() {
        return summaryView.getToolbar();
    }
    
    
    // -------------------------------------------------------------------------
    // =========================================================================
    // -------------------------------------------------------------------------
    
    public static abstract class TruffleSummarySection extends HeapView {
        
        public TruffleSummarySection(String name, String description) {
            super(name, description);
        }
        
        protected void computeData() {}
        
    }
    
    
    // -------------------------------------------------------------------------
    // =========================================================================
    // -------------------------------------------------------------------------
    
    @NbBundle.Messages({
        "TruffleOverviewSummary_Name=Overview",
        "TruffleOverviewSummary_Description=Overview",
        "TruffleOverviewSummary_HeapSection=Heap",
        "TruffleOverviewSummary_EnvironmentSection=Environment",
        "TruffleOverviewSummary_SizeItem=Size:",
        "TruffleOverviewSummary_TypesItem=Types:",
        "TruffleOverviewSummary_ObjectsItem=Objects:",
        "TruffleOverviewSummary_LanguageItem=Language:"
    })
    public static abstract class OverviewSection extends TruffleSummarySection {
        
        private final HeapContext context;
        
        private final int heapItemsCount;
        private final int environmentItemsCount;
        
        private JComponent component;
        
        private TruffleOverviewSnippet heapSnippet;
        private TruffleOverviewSnippet environmentSnippet;
        
        
        public OverviewSection(HeapContext context) {
            this(context, 3, 1);
        }
        
        public OverviewSection(HeapContext context, int heapItemsCount, int languageItemsCount) {
            super(Bundle.TruffleOverviewSummary_Name(), Bundle.TruffleOverviewSummary_Description());
            
            this.context = context;
            
            
            this.heapItemsCount = heapItemsCount;
            this.environmentItemsCount = languageItemsCount;
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
        
        
        protected HeapContext getContext() {
            return context;
        }
        
        
        @Override
        protected final void computeData() {
            Object[][] heapData = new Object[heapItemsCount][2];
            computeHeapData(heapData);
            heapSnippet.setData(heapData);
            
            Object[][] environmentData = new Object[environmentItemsCount][2];
            computeEnvironmentData(environmentData);
            environmentSnippet.setData(environmentData);
        }
        
        protected void computeHeapData(Object[][] heapData) {
            TruffleLanguageHeapFragment fragment = (TruffleLanguageHeapFragment)context.getFragment();
            
            NumberFormat numberFormat = NumberFormat.getInstance();
            
            heapData[0][0] = Bundle.TruffleOverviewSummary_SizeItem();
            heapData[0][1] = numberFormat.format(fragment.getHeapSize(null)) + " B";
            
            heapData[1][0] = Bundle.TruffleOverviewSummary_TypesItem();
            heapData[1][1] = numberFormat.format(fragment.getTypes(null).size());
            
            heapData[2][0] = Bundle.TruffleOverviewSummary_ObjectsItem();
            heapData[2][1] = numberFormat.format(fragment.getObjectsCount(null));
        }
        
        protected void computeEnvironmentData(Object[][] environmentData) {
            environmentData[0][0] = Bundle.TruffleOverviewSummary_LanguageItem();
            environmentData[0][1] = getContext().getFragment().getDescription();
        }
        
        
        private void init() {
            heapSnippet = new TruffleOverviewSnippet(Bundle.TruffleOverviewSummary_HeapSection(), heapItemsCount, 0);
            environmentSnippet = new TruffleOverviewSnippet(Bundle.TruffleOverviewSummary_EnvironmentSection(), environmentItemsCount, 1);
            Splitter overviewRow = new Splitter(Splitter.HORIZONTAL_SPLIT, heapSnippet, environmentSnippet);

            component = new JPanel(new VerticalLayout(false)) {
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

            component.add(overviewRow);
        }
        
    }
    
    private static class TruffleOverviewSnippet extends JPanel {
        
        private final int fillerColumn;
        private final SummaryView.SimpleTable table;
        
        TruffleOverviewSnippet(String text, int itemsCount, int fillerColumn) {
            super(new BorderLayout(0, 6));
            
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
            
            add(new SectionSeparator(text), BorderLayout.NORTH);
            
            
            TableModel model = new DefaultTableModel(itemsCount, 2) {
                { setValueAt(Bundle.TruffleSummaryView_ComputingProgress(), 0, 0); }
                public boolean isCellEditable(int row, int column) { return false; }
            };

            ProfilerRenderer renderer = new LabelRenderer() {
                public void setValue(Object o, int i) {
                    super.setValue(o, i);
                    setIcon(o == null || o.toString().isEmpty() ? null : Icons.getIcon(HeapWalkerIcons.PROGRESS));
                }
            };
            
            this.fillerColumn = fillerColumn;

            table = new SummaryView.SimpleTable(model, fillerColumn);
            table.setColumnRenderer(0, renderer, fillerColumn != 0);
            table.setColumnRenderer(1, renderer, fillerColumn != 1);
            
            add(table, BorderLayout.CENTER);
        }
        
        void setData(Object[][] data) {
            final TableModel model = new DefaultTableModel(data, new Object[] { Bundle.TruffleSummaryView_NameColumn(),
                                                                                Bundle.TruffleSummaryView_ValueColumn() }) {
                public boolean isCellEditable(int row, int column) { return false; }
            };
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    table.setModel(model);
                    
                    LabelRenderer r1 = new LabelRenderer();
                    r1.setFont(r1.getFont().deriveFont(Font.BOLD));
                    table.setColumnRenderer(0, r1, fillerColumn != 0);
                    LabelRenderer r2 = new LabelRenderer();
                    r2.setHorizontalAlignment(LabelRenderer.RIGHT);
                    table.setColumnRenderer(1, r2, fillerColumn != 1);
                }
            });
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
        
    }
    
    
    // -------------------------------------------------------------------------
    // =========================================================================
    // -------------------------------------------------------------------------
    
    @NbBundle.Messages({
        "JavaObjectsSummary_Name=Statistics",
        "JavaObjectsSummary_Description=Statistics",
        "JavaObjectsSummary_HeapSection=Heap",
        "JavaObjectsSummary_ClassesInstancesCount=Types by Objects Count",
        "JavaObjectsSummary_ClassesInstancesSize=Types by Objects Size",
        "JavaObjectsSummary_InstancesSize=Objects by Size",
        "JavaObjectsSummary_DominatorsRetainedSize=Dominators by Retained Size",
        "JavaObjectsSummary_ComputeRetainedSizesLbl=Retained sizes must be computed first:",
        "JavaObjectsSummary_ComputeRetainedSizesBtn=Compute Retained Sizes",
        "JavaObjectsSummary_ComputingRetainedSizes=computing retained sizes...",
        "JavaObjectsSummary_ComputingProgress=computing...",
        "JavaObjectsSummary_ViewAll=view all",
        "JavaObjectsSummary_NameColumn=Name",
        "JavaObjectsSummary_ValueColumn=Value"
    })
    public static abstract class ObjectsSection extends TruffleSummarySection {

        private static final int PREVIEW_ITEMS = 5;

        private final HeapContext context;
        private final HeapViewerActions actions;
        private final Collection<HeapViewerNodeAction.Provider> actionProviders;

        private JComponent component;
        
        private ResultsSnippet typesByCount;
        private ResultsSnippet typesBySize;
        private ResultsSnippet objectsBySize;
        private ResultsSnippet dominatorsByRetainedSize;
        
        private boolean retainedAvailable;


        public ObjectsSection(HeapContext context, HeapViewerActions actions, Collection<HeapViewerNodeAction.Provider> actionProviders) {
            super(Bundle.JavaObjectsSummary_Name(), Bundle.JavaObjectsSummary_Description());

            this.context = context;
            this.actions = actions;
            this.actionProviders = actionProviders;
        }
        
        
        protected abstract Runnable typesByCountDisplayer(HeapViewerActions actions);
        
        protected abstract Runnable typesBySizeDisplayer(HeapViewerActions actions);
        
        protected abstract Runnable objectsBySizeDisplayer(HeapViewerActions actions);
        
        protected abstract Runnable dominatorsByRetainedSizeDisplayer(HeapViewerActions actions);
        
        
        protected abstract List<? extends TruffleObject> dominators(TruffleLanguageHeapFragment heapFragment);
        
        
        protected abstract HeapViewerNode typeNode(TruffleType type, Heap heap);
        
        protected abstract ProfilerRenderer typeRenderer(Heap heap);
        
        protected abstract HeapViewerNode objectNode(TruffleObject object, Heap heap);
        
        protected abstract ProfilerRenderer objectRenderer(Heap heap);


        @Override
        public JComponent getComponent() {
            if (component == null) init();
            return component;
        }

        @Override
        public ProfilerToolbar getToolbar() {
            return null;
        }
        
        
        @Override
        protected final void computeData() {
            TruffleLanguageHeapFragment fragment = (TruffleLanguageHeapFragment)context.getFragment();
            Heap heap = fragment.getHeap();
            
            List<TruffleType> allTypes = new ArrayList(fragment.getTypes(null));

            // --- Classes by Instances Count ------------------------------
            Collections.sort(allTypes, new Comparator<TruffleType>() {
                @Override
                public int compare(TruffleType t1, TruffleType t2) {
                    return Integer.compare(t2.getObjectsCount(), t1.getObjectsCount());
                }
            });
            int items = Math.min(PREVIEW_ITEMS, allTypes.size());
            TruffleType[] typesByCountArr = allTypes.subList(0, items).toArray(new TruffleType[0]);
            Object[][] typesByCountData = new Object[typesByCountArr.length][2];
            for (int i = 0; i < typesByCountData.length; i++) {
                typesByCountData[i][0] = typeNode(typesByCountArr[i], heap);
                typesByCountData[i][1] = typesByCountArr[i].getObjectsCount();
            }
            configureSnippet(typesByCount, typesByCountData);


            // --- Classes by Instances Size -------------------------------
            Collections.sort(allTypes, new Comparator<TruffleType>() {
                @Override
                public int compare(TruffleType t1, TruffleType t2) {
                    return Long.compare(t2.getAllObjectsSize(), t1.getAllObjectsSize());
                }
            });
            TruffleType[] typesBySizeArr = allTypes.subList(0, items).toArray(new TruffleType[0]);
            Object[][] typesBySizeData = new Object[typesBySizeArr.length][2];
            for (int i = 0; i < typesBySizeData.length; i++) {
                typesBySizeData[i][0] = typeNode(typesBySizeArr[i], heap);
                typesBySizeData[i][1] = typesBySizeArr[i].getAllObjectsSize();
            }
            configureSnippet(typesBySize, typesBySizeData);


            // --- Instances by Size ---------------------------------------

            items = (int)Math.min(PREVIEW_ITEMS, heap.getSummary().getTotalLiveInstances());
            PriorityQueue<TruffleObject> pqBySize = new PriorityQueue(items, new Comparator<TruffleObject>() {
                @Override
                public int compare(TruffleObject o1, TruffleObject o2) {
                    return Long.compare(o1.getSize(), o2.getSize());
                }
            });
            Iterator<TruffleObject> allObjects = fragment.getObjectsIterator();
            while (allObjects.hasNext()) {
                TruffleObject in = allObjects.next();
                if (pqBySize.size() < items || pqBySize.peek().getSize() < in.getSize()) {
                    if (pqBySize.size() == items) pqBySize.remove();
                    pqBySize.add(in);
                }
            }
            TruffleObject[] objectsBySizeArr = new TruffleObject[pqBySize.size()];
            for (int i = objectsBySizeArr.length - 1; i >= 0; i--)
                objectsBySizeArr[i] = pqBySize.poll();
            Object[][] instancesBySizeData = new Object[objectsBySizeArr.length][2];
            for (int i = 0; i < instancesBySizeData.length; i++) {
                instancesBySizeData[i][0] = objectNode(objectsBySizeArr[i], heap);
                instancesBySizeData[i][1] = objectsBySizeArr[i].getSize();
            }
            configureSnippet(objectsBySize, instancesBySizeData);


            // --- Dominators by Retained Size -----------------------------
            if (retainedAvailable) computeDominators(dominatorsByRetainedSize);
        }


        private void init() {
            Heap heap = context.getFragment().getHeap();
            final ProfilerRenderer typeRenderer = typeRenderer(heap);
            final ProfilerRenderer objectRenderer = objectRenderer(heap);

            TreeTableViewColumn sizeColumn = new TreeTableViewColumn.OwnSize(heap);
            final HideableBarRenderer sizeRenderer = (HideableBarRenderer)sizeColumn.getRenderer();

            TreeTableViewColumn classesByCountColumn = new TreeTableViewColumn.Count(heap);
            final HideableBarRenderer classesByCountRenderer = (HideableBarRenderer)classesByCountColumn.getRenderer();

            typesByCount = new ResultsSnippet(Bundle.JavaObjectsSummary_ClassesInstancesCount(), typesByCountDisplayer(actions)) {
                protected void setupTable(ProfilerTable table) {
                    table.setColumnRenderer(0, typeRenderer);
                    table.setColumnRenderer(1, classesByCountRenderer);
                    table.setDefaultColumnWidth(1, classesByCountRenderer.getNoBarWidth() + 10);
                }
            };

            typesBySize = new ResultsSnippet(Bundle.JavaObjectsSummary_ClassesInstancesSize(), typesBySizeDisplayer(actions)) {
                protected void setupTable(ProfilerTable table) {
                    table.setColumnRenderer(0, typeRenderer);
                    table.setColumnRenderer(1, sizeRenderer);
                    table.setDefaultColumnWidth(1, sizeRenderer.getNoBarWidth() + 10);
                }
            };

            Splitter classesRow = new Splitter(Splitter.HORIZONTAL_SPLIT, typesByCount, typesBySize);

            objectsBySize = new ResultsSnippet(Bundle.JavaObjectsSummary_InstancesSize(), objectsBySizeDisplayer(actions)) {
                protected void setupTable(ProfilerTable table) {
                    table.setColumnRenderer(0, objectRenderer);
                    table.setColumnRenderer(1, sizeRenderer);
                    table.setDefaultColumnWidth(1, sizeRenderer.getNoBarWidth() + 10);
                }
            };

            retainedAvailable = DataType.RETAINED_SIZE.valuesAvailable(heap);
            dominatorsByRetainedSize = new ResultsSnippet(Bundle.JavaObjectsSummary_DominatorsRetainedSize(), dominatorsByRetainedSizeDisplayer(actions)) {
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
                                        computeDominators(rs);
                                        retainedAvailable = true;
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
                    table.setColumnRenderer(0, objectRenderer);

                    TreeTableViewColumn dominatorsByRetainedSizeColumn = new TreeTableViewColumn.RetainedSize(heap);
                    HideableBarRenderer retainedRenderer = (HideableBarRenderer)dominatorsByRetainedSizeColumn.getRenderer();
                    table.setColumnRenderer(1, retainedRenderer);
                    table.setDefaultColumnWidth(1, retainedRenderer.getNoBarWidth() + 10);
                }
                void setRealModel(TableModel model) {
                    if (model.getRowCount() == 0) {
                        DefaultTableModel dmodel = (DefaultTableModel)table.getModel();
                        table.setDefaultRenderer(Object.class, new LabelRenderer());
                        dmodel.setValueAt("<no dominators found>", 0, 0);
                        dmodel.fireTableDataChanged();
                    } else {
                        super.setRealModel(model);
                    }
                }
            };

            Splitter instancesRow = new Splitter(Splitter.HORIZONTAL_SPLIT, objectsBySize, dominatorsByRetainedSize);


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
        }


        private void computeDominators(ResultsSnippet dominatorsByRetainedSize) {
            TruffleLanguageHeapFragment fragment = (TruffleLanguageHeapFragment)context.getFragment();
            Heap heap = fragment.getHeap();
            
            List<? extends TruffleObject> dominators = dominators((TruffleLanguageHeapFragment)context.getFragment());
            Collections.sort(dominators, new Comparator<TruffleObject>() {
                @Override
                public int compare(TruffleObject o1, TruffleObject o2) {
                    return Long.compare(o2.getRetainedSize(), o1.getRetainedSize());
                }
            });
            int items = Math.min(PREVIEW_ITEMS, dominators.size());
            TruffleObject[] dominatorsByRetainedSizeArr = dominators.subList(0, items).toArray(new TruffleObject[0]);
            Object[][] dominatorsByRetainedSizeData = new Object[dominatorsByRetainedSizeArr.length][2];
            for (int i = 0; i < dominatorsByRetainedSizeData.length; i++) {
                dominatorsByRetainedSizeData[i][0] = objectNode(dominatorsByRetainedSizeArr[i], heap);
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
            protected ProfilerTable table;
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

                    Container parent = getParent();
                    if (parent != null) {
                        parent.invalidate();
                        parent.revalidate();
                        parent.repaint();
                    }
                }

                table.setModel(model);
                setupTable(table);
                enableTableEvents(table);

                link.setEnabled(true);
            }

            protected void setupTable(ProfilerTable table) {}

            private ProfilerTable createTable(TableModel model) {
                ProfilerTable t = new SummaryView.SimpleTable(model, 0) {
                    protected void populatePopup(JPopupMenu popup, Object value, Object userValue) {
                        if (!(value instanceof HeapViewerNode)) return;
                        
                        requestFocusInWindow(); // TODO: should be done by ProfilerTable on selectRow(...) in processMouseEvent(...)

                        HeapViewerNode node = (HeapViewerNode)value;
                        HeapViewerNodeAction.Actions nodeActions = HeapViewerNodeAction.Actions.forNode(node, actionProviders, context, actions);
                        nodeActions.populatePopup(popup);

                        if (popup.getComponentCount() > 0) popup.addSeparator();
                        popup.add(createCopyMenuItem());
                    }
                    public void performDefaultAction(ActionEvent e) {
                        if (!getRowSelectionAllowed()) return;
                        
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
                
                return t;
            }
            
            private void enableTableEvents(ProfilerTable t) {
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
            }

        }
    }
    
}
