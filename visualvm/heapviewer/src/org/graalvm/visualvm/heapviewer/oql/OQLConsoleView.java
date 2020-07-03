/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.oql;

import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.net.URL;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToggleButton;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.components.JExtendedSplitPane;
import org.graalvm.visualvm.lib.ui.components.ProfilerToolbar;
import org.graalvm.visualvm.lib.ui.swing.GrayLabel;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.graalvm.visualvm.heapviewer.HeapContext;
import org.graalvm.visualvm.heapviewer.java.ClassNode;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNode;
import org.graalvm.visualvm.heapviewer.java.InstanceNode;
import org.graalvm.visualvm.heapviewer.model.HeapViewerNodeFilter;
import org.graalvm.visualvm.heapviewer.model.Progress;
import org.graalvm.visualvm.heapviewer.model.RootNode;
import org.graalvm.visualvm.heapviewer.options.HeapViewerOptionsCategory;
import org.graalvm.visualvm.heapviewer.ui.HTMLView;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerActions;
import org.graalvm.visualvm.heapviewer.ui.HeapViewerFeature;
import org.graalvm.visualvm.heapviewer.ui.PluggableTreeTableView;
import org.graalvm.visualvm.heapviewer.ui.TreeTableViewColumn;
import org.graalvm.visualvm.heapviewer.utils.HeapUtils;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.netbeans.api.options.OptionsDisplayer;
import org.graalvm.visualvm.lib.profiler.heapwalk.OQLSupport;
import org.graalvm.visualvm.lib.profiler.oql.engine.api.OQLEngine;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "OQLConsoleView_Name=OQL Console",
    "OQLConsoleView_Description=OQL Console",
    "OQLConsoleView_CannotResolveClassMsg=Cannot resolve class",
    "OQLConsoleView_CannotResolveInstanceMsg=Cannot resolve instance",
    "OQLConsoleView_ViewName=Results",
    "OQLConsoleView_OQLQuery=OQL Query:",
    "OQLConsoleView_RunAction=Run",
    "OQLConsoleView_RunActionTooltip=Execute OQL script",
    "OQLConsoleView_CancelAction=Cancel",
    "OQLConsoleView_CancelActionTooltip=Cancel OQL script execution",
    "OQLConsoleView_LoadAction=Load Script",
    "OQLConsoleView_LoadActionTooltip=Load OQL script",
    "OQLConsoleView_SaveAction=Save Script",
    "OQLConsoleView_SaveActionTooltip=Save OQL script",
    "OQLConsoleView_EditAction=Edit Scripts",
    "OQLConsoleView_EditActionTooltip=Edit Custom OQL scripts",
    "OQLConsoleView_ExecutingProgress=Executing...",
    "OQLConsoleView_Results=Results:",
    "OQLConsoleView_ObjectsTooltip=Objects",
    "OQLConsoleView_HTMLTooltip=HTML",
    "OQLConsoleView_Details=Details:",
    "OQLConsoleView_InitializingEngine=<initializing OQL engine...>",
    "OQLConsoleView_EngineNotAvailable=<OQL engine not available>",
    "OQLConsoleView_Packages=Packages",
    "OQLConsoleView_Classes=Classes",
    "OQLConsoleView_Instances=Instances",
    "OQLConsoleView_Aggregation=Aggregation:",
    "OQLConsoleView_ResultsLimit=Results Limit:",
    "OQLConsoleView_SaveOnClosingCaption=OQL Script Not Saved",
    "OQLConsoleView_SaveOnClosingMsg=<html><b>The OQL script has been modified.</b><br><br>Save it before closing the heap viewer?</html>",
    "OQLConsoleView_NoSaveOnCloseBtn=Close Without Saving"
})
public class OQLConsoleView extends HeapViewerFeature {
    
    private static final int RESULTS_LIMIT = Integer.parseInt(System.getProperty("OQLController.limitResults", "100")); // NOI18N
    
    
    private static enum Aggregation {
        PACKAGES (Bundle.OQLConsoleView_Packages(), Icons.getIcon(LanguageIcons.PACKAGE)),
        CLASSES (Bundle.OQLConsoleView_Classes(), Icons.getIcon(LanguageIcons.CLASS)),
        INSTANCES (Bundle.OQLConsoleView_Instances(), Icons.getIcon(LanguageIcons.INSTANCE));
        
        private final String aggregationName;
        private final Icon aggregationIcon;
        private Aggregation(String aggregationName, Icon aggregationIcon) { this.aggregationName = aggregationName; this.aggregationIcon = aggregationIcon; }
        public String toString() { return aggregationName; }
        public Icon getIcon() { return aggregationIcon; }
    }
    
    
    private static final Color SEPARATOR_COLOR = UIManager.getColor("Separator.foreground"); // NOI18N
    
    private final HeapContext context;
    private final HeapViewerActions actions;
    
    private ProfilerToolbar toolbar;
    private ProfilerToolbar objectsToolbar;
    private ProfilerToolbar pluginsToolbar;
    private ProfilerToolbar htmlToolbar;
    private ProfilerToolbar resultsToolbar;
    private ProfilerToolbar progressToolbar;
    
    private JComponent component;
    
    private Action runAction;
    private Action cancelAction;
    private Action loadAction;
    private Action saveAction;
    private Action editAction;
    
    private JLabel progressLabel;
    private JProgressBar progressBar;
    
    private JComboBox limitCombo;
    
    private OQLEditorComponent editor;
    
    private JPanel resultsContainer;
    private HTMLView htmlView;
    private PluggableTreeTableView objectsView;
    
    private JToggleButton rObjects;
    private JToggleButton rHTML;
    
    private Aggregation aggregation = Aggregation.INSTANCES;
    
    private JToggleButton tbPackages;
    private JToggleButton tbClasses;
    private JToggleButton tbInstances;
    
    private OQLQueryExecutor oqlExecutor;
    
    private boolean queryValid;
    
    private OQLSupport.Query currentQuery;
    
    
    public OQLConsoleView(HeapContext context, HeapViewerActions actions) {
        super("java_objects_oql", Bundle.OQLConsoleView_Name(), Bundle.OQLConsoleView_Description(), Icons.getIcon(HeapWalkerIcons.OQL_CONSOLE), 1000); // NOI18N
        
        this.context = context;
        this.actions = actions;
    }
    
    public JComponent getComponent() {
        if (component == null) init();
        return component;
    }

    public ProfilerToolbar getToolbar() {
        if (toolbar == null) init();
        return toolbar;
    }
    
    
    protected void willBeClosed(Runnable viewSelector) {
        if (editor != null && editor.isChanged() && !editor.getScript().isEmpty() && saveAction.isEnabled()) {
            viewSelector.run();
            
            Container c = editor;
            while (c != null) {
                Container p = c.getParent();
                if (p instanceof JTabbedPane) {
                    ((JTabbedPane)p).setSelectedComponent(c);
                } else if (p instanceof TopComponent) {
                    ((TopComponent)p).requestActive();
                    break;
                }
                c = p;
            }
            
            
            JButton saveButton = new JButton(saveAction) {
                public void addActionListener(ActionListener l) {
                    if (l == saveAction) super.addActionListener(l);
                }
                public void removeActionListener(ActionListener l) {
                    if (l == saveAction) super.removeActionListener(l);
                }
            };
            JButton closeButton = new JButton(Bundle.OQLConsoleView_NoSaveOnCloseBtn());
            DialogDescriptor dd = new DialogDescriptor(Bundle.OQLConsoleView_SaveOnClosingMsg(), Bundle.OQLConsoleView_SaveOnClosingCaption(), true, new Object[] { saveButton, closeButton }, saveButton, DialogDescriptor.DEFAULT_ALIGN, null, null);
            dd.setMessageType(NotifyDescriptor.QUESTION_MESSAGE);
            Dialog d = DialogDisplayer.getDefault().createDialog(dd);
            saveAction.putValue("NOTIFIER", new Runnable() { // NOI18N
                public void run() {
                    saveAction.putValue("NOTIFIER", null); // NOI18N
                    d.setVisible(false);
                }
            });
            d.setVisible(true);
        }
    }
    
    @Override
    protected void closed() {
        if (objectsView != null) objectsView.closed();
    }
    
    
    private void init() {
        toolbar = ProfilerToolbar.create(false);
        
        component = new JPanel(new BorderLayout());
        component.setOpaque(true);
        component.setBackground(UIUtils.getProfilerResultsBackground());
        
        JLabel l = new JLabel(Bundle.OQLConsoleView_InitializingEngine(), JLabel.CENTER);
        l.setEnabled(false);
        l.setOpaque(false);
        component.add(l, BorderLayout.CENTER);
        
        VisualVM.getInstance().runTask(new Runnable() {
            @Override
            public void run() {
                Heap heap = context.getFragment().getHeap();
                
                if (OQLEngine.isOQLSupported()) try {
                    final OQLEngine oqlEngine = new OQLEngine(heap);
                    oqlExecutor = new OQLQueryExecutor(oqlEngine) {
                        @Override
                        protected void queryStarted(BoundedRangeModel model) {
                            OQLConsoleView.this.queryStarted(model);
                        }
                        @Override
                        protected void queryFinished(boolean hasObjectsResults, boolean hasHTMLResults, String errorMessage) {
                            OQLConsoleView.this.queryFinished(hasObjectsResults, hasHTMLResults, errorMessage);
                        }
                    };
                    
                    TreeTableViewColumn[] ownColumns = new TreeTableViewColumn[] {
                        new TreeTableViewColumn.Name(heap),
                        new TreeTableViewColumn.Count(heap, false, false),
                        new TreeTableViewColumn.OwnSize(heap, true, true),
                        new TreeTableViewColumn.RetainedSize(heap, true, false)
                    };

                    objectsView = new PluggableTreeTableView("java_objects_oql", context, actions, ownColumns) { // NOI18N
                        protected HeapViewerNode[] computeData(RootNode root, Heap heap, String viewID, HeapViewerNodeFilter viewFilter, List<DataType> dataTypes, List<SortOrder> sortOrders, Progress progress) throws InterruptedException {
                            int aggr = Aggregation.INSTANCES.equals(aggregation) ? 0 :
                                       Aggregation.CLASSES.equals(aggregation) ? 1 : 2;
                            return oqlExecutor.getQueryObjects(root, heap, viewID, viewFilter, dataTypes, sortOrders, progress, aggr);
                        }
                        protected JComponent createComponent() {
                            JComponent comp = super.createComponent();

                            setFilterComponent(FilterUtils.createFilterPanel(this));

                            return comp;
                        }
                    };
                    objectsView.setViewName(Bundle.OQLConsoleView_ViewName());

                    htmlView = new HTMLView("java_objects_oql", context, actions, oqlExecutor.getQueryHTML()) { // NOI18N
                        protected HeapViewerNode nodeForURL(URL url, HeapContext context) {
                            return OQLConsoleView.getNode(url, context);
                        }
                    };
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
//                            toolbar.addSpace(2);
//                            toolbar.addSeparator();
//                            toolbar.addSpace(5);
//
//                            toolbar.add(new GrayLabel(Bundle.OQLConsoleView_OQLQuery()));
//                            toolbar.addSpace(2);

                            runAction = new AbstractAction(Bundle.OQLConsoleView_RunAction(), Icons.getIcon(GeneralIcons.START)) {
                                {
                                    putValue(Action.SHORT_DESCRIPTION, Bundle.OQLConsoleView_RunActionTooltip());
                                }
                                public void actionPerformed(ActionEvent e) {
                                    executeQuery();
                                }
                            };

                            JButton runButton = new JButton(runAction) {
                                public Dimension getPreferredSize() {
                                    Dimension d = super.getPreferredSize();
                                    d.width += 6;
                                    return d;
                                }
                                public Dimension getMinimumSize() {
                                    return getPreferredSize();
                                }
                                public Dimension getMaximumSize() {
                                    return getPreferredSize();
                                }
                            };
                            runButton.putClientProperty("JComponent.sizeVariant", "regular"); // NOI18N

                            cancelAction = new AbstractAction(Bundle.OQLConsoleView_CancelAction(), Icons.getIcon(GeneralIcons.STOP)) {
                                {
                                    putValue(Action.SHORT_DESCRIPTION, Bundle.OQLConsoleView_CancelActionTooltip());
                                }
                                public void actionPerformed(ActionEvent e) {
                                    cancelQuery();
                                }
                            };

                            JButton cancelButton = new JButton(cancelAction);
                            cancelButton.setHideActionText(true);

                            loadAction = new AbstractAction(Bundle.OQLConsoleView_LoadAction(), OQLQueries.ICON_LOAD) {
                                {
                                    putValue(Action.SHORT_DESCRIPTION, Bundle.OQLConsoleView_LoadActionTooltip());
                                }
                                public void actionPerformed(ActionEvent e) {
                                    if (e.getSource() instanceof JComponent) {
                                        JPopupMenu p = new JPopupMenu();
                                        OQLQueries.instance().populateLoadQuery(p, currentQuery, new OQLQueries.Handler() {
                                            protected void querySelected(OQLSupport.Query query) {
                                                currentQuery = query;
                                                if (editor != null) editor.setScript(currentQuery.getScript());
                                            }
                                        });

                                        JComponent c = (JComponent)e.getSource();
                                        if (p.getComponentCount() > 0) {
                                            if (c.getClientProperty("POPUP_LEFT") != null) p.show(c, c.getWidth() + 1, 0); // NOI18N
                                            else p.show(c, 0, c.getHeight() + 1);
                                        }

                                    }
                                }
                            };

                            JButton loadButton = new JButton(loadAction);
                            loadButton.setHideActionText(true);

                            saveAction = new AbstractAction(Bundle.OQLConsoleView_SaveAction(), OQLQueries.ICON_SAVE) {
                                {
                                    putValue(Action.SHORT_DESCRIPTION, Bundle.OQLConsoleView_SaveActionTooltip());
                                }
                                public void actionPerformed(ActionEvent e) {
                                    if (e.getSource() instanceof JComponent) {
                                        JPopupMenu p = new JPopupMenu();
                                        OQLQueries.instance().populateSaveQuery(p, currentQuery, editor.getScript(), new OQLQueries.Handler() {
                                            protected void querySelected(OQLSupport.Query query) {
                                                currentQuery = query;
                                                editor.clearChanged();
                                                
                                                Object notifier = saveAction.getValue("NOTIFIER"); // NOI18N
                                                if (notifier instanceof Runnable) ((Runnable)notifier).run();
                                            }
                                        });

                                        JComponent c = (JComponent)e.getSource();
                                        if (p.getComponentCount() > 0) {
                                            if (c.getClientProperty("POPUP_LEFT") != null) p.show(c, c.getWidth() + 1, 0); // NOI18N
                                            else p.show(c, 0, c.getHeight() + 1);
                                        }

                                    }
                                }
                            };

                            JButton saveButton = new JButton(saveAction);
                            saveButton.setHideActionText(true);

                            editAction = new AbstractAction(Bundle.OQLConsoleView_EditAction(), Icons.getIcon(HeapWalkerIcons.RULES)) {
                                {
                                    putValue(Action.SHORT_DESCRIPTION, Bundle.OQLConsoleView_EditActionTooltip());
                                }
                                public void actionPerformed(ActionEvent e) {
                                    OptionsDisplayer.getDefault().open(HeapViewerOptionsCategory.OPTIONS_HANDLE);
                                }
                            };

                            JButton editButton = new JButton(editAction);
                            editButton.setHideActionText(true);


                            progressToolbar = ProfilerToolbar.create(false);
                            progressToolbar.getComponent().setVisible(false);

                            progressToolbar.addSpace(2);
                            progressToolbar.addSeparator();
                            progressToolbar.addSpace(5);

                            progressLabel = new GrayLabel(Bundle.OQLConsoleView_ExecutingProgress());
                            progressToolbar.add(progressLabel);

                            progressToolbar.addSpace(8);

                            progressBar = new JProgressBar(JProgressBar.HORIZONTAL) {
                                public Dimension getPreferredSize() {
                                    Dimension dim = super.getMinimumSize();
                                    dim.width = 120;
                                    return dim;
                                }
                                public Dimension getMinimumSize() {
                                    return getPreferredSize();
                                }
                                public Dimension getMaximumSize() {
                                    return getPreferredSize();
                                }
                            };
                            progressToolbar.add(progressBar);

//                            toolbar.add(runButton);
//                    //        toolbar.addSpace(2);
//                            toolbar.add(cancelButton);
//
//                            toolbar.addSpace(5);
//
//                            toolbar.add(loadButton);
//                            toolbar.add(saveButton);
//                            toolbar.add(editButton);

                            resultsToolbar = ProfilerToolbar.create(false);

                            resultsToolbar.addSpace(2);
                            resultsToolbar.addSeparator();
                            resultsToolbar.addSpace(5);

                            resultsToolbar.add(new GrayLabel(Bundle.OQLConsoleView_Results()));
                            resultsToolbar.addSpace(3);

                            ButtonGroup resultsBG = new ButtonGroup();

                            rObjects = new JToggleButton(Icons.getIcon(ProfilerIcons.TAB_HOTSPOTS), true) {
                                protected void fireItemStateChanged(ItemEvent e) {
                                    if (e.getStateChange() == ItemEvent.SELECTED) {
                                        if (resultsContainer != null) ((CardLayout)resultsContainer.getLayout()).first(resultsContainer);
                                        if (objectsToolbar != null) objectsToolbar.getComponent().setVisible(true);
                                        if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(true);
                                        if (htmlToolbar != null) htmlToolbar.getComponent().setVisible(false);
                                    }
                                }
                            };
                            rObjects.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                            rObjects.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
                            rObjects.setToolTipText(Bundle.OQLConsoleView_ObjectsTooltip());
                            resultsBG.add(rObjects);
                            resultsToolbar.add(rObjects);

                            rHTML = new JToggleButton(Icons.getIcon(HeapWalkerIcons.PROPERTIES)) {
                                protected void fireItemStateChanged(ItemEvent e) {
                                    if (e.getStateChange() == ItemEvent.SELECTED) {
                                        if (resultsContainer != null) ((CardLayout)resultsContainer.getLayout()).last(resultsContainer);
                                        if (objectsToolbar != null) objectsToolbar.getComponent().setVisible(false);
                                        if (pluginsToolbar != null) pluginsToolbar.getComponent().setVisible(false);
                                        if (htmlToolbar != null) htmlToolbar.getComponent().setVisible(true);
                                    }
                                }
                            };
                            rHTML.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                            rHTML.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
                            rHTML.setToolTipText(Bundle.OQLConsoleView_HTMLTooltip());
                            resultsBG.add(rHTML);
                            resultsToolbar.add(rHTML);
                            
                            objectsToolbar = ProfilerToolbar.create(false);
                            objectsToolbar.addSpace(8);
                            objectsToolbar.add(new GrayLabel(Bundle.OQLConsoleView_Aggregation()));
                            objectsToolbar.addSpace(2);

                            final ButtonGroup aggregationBG = new ButtonGroup();
                            class AggregationButton extends JToggleButton {
                                private final Aggregation aggregation;
                                AggregationButton(Aggregation aggregation, boolean selected) {
                                    super(aggregation.getIcon(), selected);
                                    this.aggregation = aggregation;
                                    setToolTipText(aggregation.toString());
                                    aggregationBG.add(this);
                                }
                                protected void fireItemStateChanged(ItemEvent e) {
                                    // invoked also from constructor: super(aggregation.getIcon(), selected)
                                    // in this case aggregation is still null, ignore the event...
                                    if (e.getStateChange() == ItemEvent.SELECTED && aggregation != null) setAggregation(aggregation);
                                }
                            }

                            tbPackages = new AggregationButton(Aggregation.PACKAGES, Aggregation.PACKAGES.equals(aggregation));
                            tbPackages.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                            tbPackages.putClientProperty("JButton.segmentPosition", "first"); // NOI18N
                            objectsToolbar.add(tbPackages);

                            tbClasses = new AggregationButton(Aggregation.CLASSES, Aggregation.CLASSES.equals(aggregation));
                            tbClasses.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                            tbClasses.putClientProperty("JButton.segmentPosition", "middle"); // NOI18N
                            objectsToolbar.add(tbClasses);

                            tbInstances = new AggregationButton(Aggregation.INSTANCES, Aggregation.INSTANCES.equals(aggregation));
                            tbInstances.putClientProperty("JButton.buttonType", "segmented"); // NOI18N
                            tbInstances.putClientProperty("JButton.segmentPosition", "last"); // NOI18N
                            objectsToolbar.add(tbInstances);
                            
                            resultsToolbar.add(objectsToolbar);

                            if (objectsView.hasPlugins()) {
                                pluginsToolbar = ProfilerToolbar.create(false);
                    //            detailsToolbar.addSpace(2);
                    //            detailsToolbar.addSeparator();
                                pluginsToolbar.addSpace(8);

                                pluginsToolbar.add(new GrayLabel(Bundle.OQLConsoleView_Details()));
                                pluginsToolbar.addSpace(2);

                                pluginsToolbar.add(objectsView.getToolbar());

                                resultsToolbar.add(pluginsToolbar);
                            }
                            
                            htmlToolbar = ProfilerToolbar.create(false);
                            htmlToolbar.getComponent().setVisible(false);
                            htmlToolbar.addSpace(8);
                            htmlToolbar.add(new GrayLabel(Bundle.OQLConsoleView_ResultsLimit()));
                            htmlToolbar.addSpace(3);
                            
                            Set<Integer> limits = new TreeSet();
                            limits.add(10);
                            limits.add(100);
                            limits.add(1000);
//                            limits.add(10000);
                            limits.add(RESULTS_LIMIT);
                            limitCombo = new JComboBox(limits.toArray());
                            limitCombo.setSelectedItem(RESULTS_LIMIT);
                            final Format numberFormat = NumberFormat.getNumberInstance();
                            final ListCellRenderer rendererImpl = limitCombo.getRenderer();
                            ListCellRenderer renderer = new ListCellRenderer() {
                                @Override
                                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                                    return rendererImpl.getListCellRendererComponent(list, numberFormat.format(value), index, isSelected, cellHasFocus);
                                }
                            };
                            limitCombo.setRenderer(renderer);
                            htmlToolbar.add(limitCombo);
                            
                            resultsToolbar.add(htmlToolbar);

                            toolbar.add(resultsToolbar);

                            toolbar.add(progressToolbar);

                            editor = new OQLEditorComponent(oqlEngine) {
                                protected void validityChanged(boolean valid) {
                                    queryValid = valid;
                                    updateUIState();
                                }
                            };

                            resultsContainer = new JPanel(new CardLayout());
                            resultsContainer.add(objectsView.getComponent());
                            resultsContainer.add(new ResultsView(htmlView.getComponent()));

                            JExtendedSplitPane masterSplit = new JExtendedSplitPane(JExtendedSplitPane.VERTICAL_SPLIT, true, resultsContainer, new EditorView(editor));
                            BasicSplitPaneDivider masterDivider = ((BasicSplitPaneUI)masterSplit.getUI()).getDivider();
                            masterDivider.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, SEPARATOR_COLOR));
                            masterDivider.setDividerSize(6);
                            masterSplit.setBorder(BorderFactory.createEmptyBorder());
                            masterSplit.setResizeWeight(0.70d);

                            component.removeAll();
                            component.add(masterSplit, BorderLayout.CENTER);
                            
                            Container parent = component.getParent();
                            if (parent != null) {
                                parent.invalidate();
                                parent.revalidate();
                                parent.repaint();
                            }
                            
                            toolbar.getComponent().repaint();

                            updateUIState();
                        }
                    });
                } catch (Exception e) {
        //            ProfilerLogger.log(e);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            JLabel l = new JLabel(Bundle.OQLConsoleView_EngineNotAvailable(), JLabel.CENTER);
                            l.setEnabled(false);
                            l.setOpaque(false);
                            
                            component.removeAll();
                            component.add(l, BorderLayout.CENTER);
                            
                            Container parent = component.getParent();
                            if (parent != null) {
                                parent.invalidate();
                                parent.revalidate();
                                parent.repaint();
                            }
                        }
                    });
                }
            }
        });
    }
    
    private volatile boolean countVisible1 = true;
    private volatile boolean countVisible2 = false;
    
    private synchronized void setAggregation(Aggregation aggregation) {
        boolean instancesInvolved = Aggregation.INSTANCES.equals(aggregation) ||
                                    Aggregation.INSTANCES.equals(this.aggregation);
        
        this.aggregation = aggregation;
        
        if (instancesInvolved) {
            // TODO: having Count visible for Instances aggregation resets the column width!
            boolean countVisible = objectsView.isColumnVisible(DataType.COUNT);
            if (Aggregation.INSTANCES.equals(aggregation)) {
                countVisible1 = countVisible;
                objectsView.setColumnVisible(DataType.COUNT, countVisible2);
            } else {
                countVisible2 = countVisible;
                objectsView.setColumnVisible(DataType.COUNT, countVisible1);
            }
        }
        
        objectsView.reloadView();
    }
    
    private synchronized Aggregation getAggregation() {
        return aggregation;
    }
    
    
    private void executeQuery() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                oqlExecutor.runQuery(editor.getScript(), true, true, (int)limitCombo.getSelectedItem());
            }
        });
    }
    
    private void cancelQuery() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                oqlExecutor.cancelQuery();
            }
        });
    }
    
    private void queryStarted(final BoundedRangeModel model) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateUIState();
                resultsToolbar.getComponent().setVisible(false);
                progressToolbar.getComponent().setVisible(true);
                progressBar.setModel(model);
                
                objectsView.reloadView();
                htmlView.setText(oqlExecutor.getQueryHTML());
            }
        });
    }

    private void queryFinished(final boolean hasObjectsResults, final boolean hasHTMLResults, final String errorMessage) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateUIState();
                progressToolbar.getComponent().setVisible(false);
                resultsToolbar.getComponent().setVisible(true);
                progressBar.setModel(new DefaultBoundedRangeModel(0, 0, 0, 0));
                
                objectsView.reloadView();
                
                String queryHTML = oqlExecutor.getQueryHTML();
                htmlView.setText(errorMessage != null ? errorMessage : queryHTML);
                
                if (errorMessage != null || (!hasObjectsResults && hasHTMLResults)) {
                    rHTML.setSelected(true);
                }
            }
        });
    }
    
    
    private void updateUIState() {
        int scriptLength = editor.getScript().trim().length();
        
        saveAction.setEnabled(scriptLength > 0);
        
        if (oqlExecutor.isQueryRunning()) {
            runAction.setEnabled(false);
            cancelAction.setEnabled(true);
            loadAction.setEnabled(false);
            editor.setEditable(false);
        } else {
            runAction.setEnabled(scriptLength > 0 && queryValid);
            cancelAction.setEnabled(false);
            loadAction.setEnabled(true);
            editor.setEditable(true);
        }
    }
    
    private static HeapViewerNode getNode(URL url, HeapContext context) {
        String urls = url.toString();
                
        if (HeapUtils.isInstance(urls)) {
            final Instance instance = HeapUtils.instanceFromHtml(urls, context.getFragment().getHeap());
            if (instance != null) return new InstanceNode(instance);
            else ProfilerDialogs.displayError(Bundle.OQLConsoleView_CannotResolveInstanceMsg());
        } else if (HeapUtils.isClass(urls)) {
            JavaClass javaClass = HeapUtils.classFromHtml(urls, context.getFragment().getHeap());
            if (javaClass != null) return new ClassNode(javaClass);
            else ProfilerDialogs.displayError(Bundle.OQLConsoleView_CannotResolveClassMsg());
        }

        return null;
    }
    
    
    private class EditorView extends JPanel {
        
        EditorView(OQLEditorComponent editor) {
            super(new BorderLayout());
            
            editor.clearScrollBorders();
            add(editor, BorderLayout.CENTER);
//            add(new ScrollableContainer(editorContainer), BorderLayout.CENTER);

            JToolBar controls = new JToolBar(JToolBar.VERTICAL);
            controls.setFloatable(false);
            controls.setBorderPainted(false);
            controls.add(runAction);
            controls.add(cancelAction);
            controls.addSeparator();
            controls.add(loadAction).putClientProperty("POPUP_LEFT", Boolean.TRUE); // NOI18N
            controls.add(saveAction).putClientProperty("POPUP_LEFT", Boolean.TRUE); // NOI18N
            controls.add(editAction).putClientProperty("POPUP_LEFT", Boolean.TRUE); // NOI18N
            
            JPanel controlsContainer = new JPanel(new BorderLayout());
            controlsContainer.setOpaque(false);
            controlsContainer.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground")), // NOI18N
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)));
            controlsContainer.add(controls, BorderLayout.CENTER);
            add(controlsContainer, BorderLayout.WEST);
            
            // size to always show Run and Stop buttons
            int h = controls.getComponent(0).getPreferredSize().height;
            h += controls.getComponent(1).getPreferredSize().height + 2;
            setMinimumSize(new Dimension(0, h));
        }
        
    }
    
    
    private class ResultsView extends JPanel {
        
        ResultsView(JComponent results) {
            super(new BorderLayout());
            add(new ScrollableContainer(results), BorderLayout.CENTER);
        }
        
    }
    
}
