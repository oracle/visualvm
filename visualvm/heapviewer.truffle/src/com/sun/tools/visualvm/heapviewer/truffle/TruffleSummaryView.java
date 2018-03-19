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
package com.sun.tools.visualvm.heapviewer.truffle;

import com.sun.tools.visualvm.core.ui.components.SectionSeparator;
import com.sun.tools.visualvm.heapviewer.HeapContext;
import com.sun.tools.visualvm.heapviewer.ui.HeapView;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerActions;
import com.sun.tools.visualvm.heapviewer.ui.HeapViewerFeature;
import com.sun.tools.visualvm.heapviewer.ui.SummaryView;
import com.sun.tools.visualvm.uisupport.VerticalLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
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
    
    
    public static abstract class TruffleSummarySection extends HeapView {
        
        public TruffleSummarySection(String name, String description) {
            super(name, description);
        }
        
        protected void computeData() {}
        
    }
    
    
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
            // Should be displayed after heapData, but is much faster to compute
            Object[][] environmentData = new Object[environmentItemsCount][2];
            computeEnvironmentData(environmentData);
            environmentSnippet.setData(environmentData);
            
            Object[][] heapData = new Object[heapItemsCount][2];
            computeHeapData(heapData);
            heapSnippet.setData(heapData);
        }
        
        
//        protected abstract Iterator getObjectsIterator();
//        
//        protected abstract String getType(Object object, Map<Object, String> typesCache);
//        
//        protected abstract long updateObjectsSize(Object object, long objectsSize);
//        
//        protected long getObjectsSize(long objectsSize, long objectsCount) { return objectsSize; };
        
        protected void computeHeapData(Object[][] heapData) {
//            long objectsCount = 0;
//            long objectsSize = 0;
//            
//            Map<Object, String> typesCache = new HashMap();
//            Set<String> types = new HashSet();
//            
//            Iterator objectsI = getObjectsIterator();
//
//            while (objectsI.hasNext()) {
//                Object object = objectsI.next();
//                
//                objectsCount++;
//                objectsSize = updateObjectsSize(object, objectsSize);
//                
//                String type = getType(object, typesCache);
//                types.add(type);
//            }

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
            environmentSnippet = new TruffleOverviewSnippet(Bundle.TruffleOverviewSummary_EnvironmentSection(), environmentItemsCount, 0);
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
            
            
            TableModel model = new DefaultTableModel(itemsCount, 1) {
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
            table.setDefaultRenderer(Object.class, renderer);
            
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
    
}
