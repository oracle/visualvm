/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.core.explorer;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Utils;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class ExplorerModelBuilder implements DataChangeListener<DataSource> {
    
    private static final RequestProcessor queue = new RequestProcessor("Explorer Builder Processor");   // NOI18N
    
    private static ExplorerModelBuilder instance;
    
    private final ExplorerNode explorerRoot;
    private final DefaultTreeModel explorerModel;
    
    private final Map<DataSource, ExplorerNode> nodes = new HashMap();
    private final Map<DataSource, PropertyChangeListener> visibilityListeners = new HashMap();
    private final Map<DataSourceDescriptor, PropertyChangeListener> descriptorListeners = new HashMap();

    private static final ExplorerNodesComparator RELATIVE_COMPARATOR =
            new ExplorerNodesComparator(new RelativePositionComparator());

    
    public static synchronized ExplorerModelBuilder getInstance() {
        if (instance == null) instance = new ExplorerModelBuilder();
        return instance;
    }
    
    
    DefaultTreeModel getModel() {
        return explorerModel;
    }
    
    ExplorerNode getNodeFor(DataSource dataSource) {
        return nodes.get(dataSource);
    }

        
    public void dataChanged(final DataChangeEvent event) {
        queue.post(new Runnable() {
            public void run() {
                Set<DataSource> removed = event.getRemoved();
                Set<DataSource> added = event.getAdded();

                if (!removed.isEmpty()) processRemovedDataSources(removed);
                if (!added.isEmpty()) processAddedDataSources(added);
            }
        });
    }
    
    private void processAddedDataSources(Set<DataSource> added) {
        for (DataSource dataSource : added) installVisibilityListener(dataSource);
        processIndependentAddedDataSources(Utils.getIndependentDataSources(added));
    }
    
    private void processIndependentAddedDataSources(Set<DataSource> added) {
        Set<DataSource> addedDisplayable = new HashSet();

        for (DataSource dataSource : added) {
            if (isDisplayed(dataSource) && dataSource != DataSource.ROOT) return;
            if (isDisplayable(dataSource)) addedDisplayable.add(dataSource);
        }

        if (!addedDisplayable.isEmpty()) processAddedDisplayableDataSources(addedDisplayable);
    }
    
    private void processRemovedDataSources(Set<DataSource> removed) {
        for (DataSource dataSource : removed) uninstallVisibilityListener(dataSource);
        processIndependentRemovedDataSources(Utils.getIndependentDataSources(removed));
    }
    
    private void processIndependentRemovedDataSources(Set<DataSource> removed) {
        Set<DataSource> removedDisplayed = new HashSet();
        
        for (DataSource dataSource : removed)
            if (isDisplayed(dataSource)) removedDisplayed.add(dataSource);
        
        if (!removedDisplayed.isEmpty()) processRemovedDisplayedDataSources(removedDisplayed);
    }
    
    private void processAddedDisplayableDataSources(Set<DataSource> addedDisplayable) {
        final List<ExplorerNode> addedNodes = new ArrayList();
        final ProgressHandle[] pHandle = new ProgressHandle[1];

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                pHandle[0] = ProgressHandleFactory.createHandle(NbBundle.getMessage(ExplorerModelBuilder.class, "LBL_Computing_description"));

                pHandle[0].setInitialDelay(5000);
                pHandle[0].start();
            }
        });

        try {
            for (DataSource dataSource : addedDisplayable) {
                if (dataSource != DataSource.ROOT) {
                    final ExplorerNode node = new ExplorerNode(dataSource);
                    addedNodes.add(node);
                    DataSourceDescriptor descriptor = DataSourceDescriptorFactory.getDescriptor(dataSource);
                    PropertyChangeListener descriptorListener = new PropertyChangeListener() {
                        public void propertyChange(final PropertyChangeEvent evt) {
                            queue.post(new Runnable() {
                                public void run() { updateNode(node, evt); }
                            });
                        }
                    };
                    descriptor.addPropertyChangeListener(descriptorListener);
                    descriptorListeners.put(descriptor, descriptorListener);
                    updateNode(node, descriptor);
                }
            }
        } finally {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { pHandle[0].finish(); }
            });
        }

        Collections.sort(addedNodes, RELATIVE_COMPARATOR);

        try { SwingUtilities.invokeAndWait(new Runnable() {
            public void run() { addNodes(addedNodes); }
        }); } catch (Exception e) {}
        
        Set<DataSource> addedChildren = new HashSet();
        for (DataSource dataSource : addedDisplayable)
            addedChildren.addAll(dataSource.getRepository().getDataSources());
        if (!addedChildren.isEmpty()) processIndependentAddedDataSources(addedChildren);
    }
    
    private void processRemovedDisplayedDataSources(Set<DataSource> removedDisplayed) {
        Set<DataSource> removedChildren = new HashSet();
        for (DataSource dataSource : removedDisplayed)
            removedChildren.addAll(dataSource.getRepository().getDataSources());
        if (!removedChildren.isEmpty()) processIndependentRemovedDataSources(removedChildren);
        
        final Set<ExplorerNode> removedNodes = new HashSet();
        
        for (DataSource dataSource : removedDisplayed) {
            DataSourceDescriptor descriptor = DataSourceDescriptorFactory.getDescriptor(dataSource);
            PropertyChangeListener descriptorListener = descriptorListeners.get(descriptor);
            descriptor.removePropertyChangeListener(descriptorListener);
            descriptorListeners.remove(descriptor);
            
            ExplorerNode node = nodes.get(dataSource);
            removedNodes.add(node);
        }
        
        try { SwingUtilities.invokeAndWait(new Runnable() {
            public void run() { removeNodes(removedNodes); }
        }); } catch (Exception e) {}
    }
    
    private void updateNode(ExplorerNode node, DataSourceDescriptor descriptor) {
        node.setName(descriptor.getName());
        node.setIcon(descriptor.getIcon() == null ? null : new ImageIcon(descriptor.getIcon()));
        node.setPreferredPosition(descriptor.getPreferredPosition());
        node.setComparator(descriptor.getChildrenComparator());
        node.setAutoExpansionPolicy(descriptor.getAutoExpansionPolicy());
    }
    
    private void updateNode(final ExplorerNode node, final PropertyChangeEvent evt) {
        String property = evt.getPropertyName();
        Object newValue = evt.getNewValue();

        // Node name needs to be updated
        if (DataSourceDescriptor.PROPERTY_NAME.equals(property)) {
            String name = (String)newValue;
            Runnable updater = node.setName(name) ?
                new Runnable() {
                    public void run() { updateContainer(node.getParent()); }
                } : new Runnable() {
                    public void run() { explorerModel.nodeChanged(node); }
                };
            try { SwingUtilities.invokeAndWait(updater); } catch (Exception e) {}
        // Node icon needs to be updated
        } else if (DataSourceDescriptor.PROPERTY_ICON.equals(property)) {
            Image icon = (Image)newValue;
            node.setIcon(icon == null ? null : new ImageIcon(icon));
            try { SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { explorerModel.nodeChanged(node); }
            }); } catch (Exception e) {}
        // Node position within its parent needs to be updated
        } else if (DataSourceDescriptor.PROPERTY_PREFERRED_POSITION.equals(property)) {
            Integer preferredPosition = (Integer)newValue;
            node.setPreferredPosition(preferredPosition);
            final ExplorerNode parent = (ExplorerNode)node.getParent();
            if (parent != null) {
                try { SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        int nodeIndex = parent.getIndex(node);
                        parent.remove(node);
                        explorerModel.nodesWereRemoved(parent, new int[] { nodeIndex }, new Object[] { node });
                        parent.addNode(node);
                        explorerModel.nodesWereInserted(parent, new int[] { parent.getIndex(node) });
                    }
                }); } catch (Exception e) {}
            }
        } else if (DataSourceDescriptor.PROPERTY_CHILDREN_COMPARATOR.equals(property)) {
            final Comparator<DataSource> comparator = (Comparator<DataSource>)newValue;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    if (node.setComparator(comparator)) updateContainer(node);
                }
            });
        } else if (DataSourceDescriptor.PROPERTY_EXPANSION_POLICY.equals(property)) {
            node.setAutoExpansionPolicy((Integer)evt.getNewValue());
        }
    }

    private void updateContainer(TreeNode node) {
        // Save selection
        Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().
                                              getSelectedDataSources();

        explorerModel.nodeStructureChanged(node);

        // Try to restore selection
        ExplorerSupport.sharedInstance().selectDataSources(selectedDataSources);
    }
    
    private void addNodes(List<ExplorerNode> added) {
        Map<ExplorerNode, List<Integer>> indexes = new HashMap();
        
        // Save selection
        Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().getSelectedDataSources();
        
        // Add nodes and create parent entries
        for (ExplorerNode node : added) {
            DataSource dataSource = node.getUserObject();
            ExplorerNode nodeParent = getNodeFor(dataSource.getOwner());
            nodes.put(dataSource, node);
            nodeParent.addNode(node);
            indexes.put(nodeParent, new ArrayList());
        }
        
        // Compute children indexes
        for (ExplorerNode node : added) {
            ExplorerNode nodeParent = (ExplorerNode)node.getParent();
            indexes.get(nodeParent).add(nodeParent.getIndex(node));
        }

        // Notify tree model
        // Synchronize relative positions
        for (Map.Entry<ExplorerNode, List<Integer>> entry : indexes.entrySet()) {
            List<Integer> indexesList = entry.getValue();
            Collections.sort(indexesList);
            int[] indexesArr = new int[indexesList.size()];
            for (int i = 0; i < indexesArr.length; i++) indexesArr[i] = indexesList.get(i);
            final ExplorerNode parent = entry.getKey();
            explorerModel.nodesWereInserted(parent, indexesArr);
        }
        
        // Try to restore selection
        ExplorerSupport.sharedInstance().selectDataSources(selectedDataSources);
    }

    private void removeNodes(Set<ExplorerNode> removed) {
        Map<ExplorerNode, List<IndexNodePair>> pairs = new HashMap();
        
        // Save selection
        Set<DataSource> selectedDataSources = ExplorerSupport.sharedInstance().getSelectedDataSources();
        
        // Cache indexes and childs
        for (ExplorerNode node : removed) {
            ExplorerNode nodeParent = (ExplorerNode)node.getParent();
            List<IndexNodePair> list = pairs.get(nodeParent);
            if (list == null) {
                list = new ArrayList();
                pairs.put(nodeParent, list);
            }
            list.add(new IndexNodePair(nodeParent.getIndex(node), node));
        }
        
        // Remove nodes
        for (ExplorerNode node : removed) {
            node.removeFromParent();
            nodes.remove(node.getUserObject());
        }
        
        // Notify tree model
        // Synchronize relative positions
        for (Map.Entry<ExplorerNode, List<IndexNodePair>> entry : pairs.entrySet()) {
            List<IndexNodePair> indexesList = entry.getValue();
            Collections.sort(indexesList);
            int[] indexesArr = new int[indexesList.size()];
            Object[] childsArr = new Object[indexesList.size()];
            for (int i = 0; i < indexesArr.length; i++) {
                IndexNodePair pair = indexesList.get(i);
                indexesArr[i] = pair.index;
                childsArr[i] = pair.node;
            }
            final ExplorerNode parent = entry.getKey();
            explorerModel.nodesWereRemoved(parent, indexesArr, childsArr);
        }
        
        // Try to restore selection
        ExplorerSupport.sharedInstance().selectDataSources(selectedDataSources);
    }
    
    private void installVisibilityListener(final DataSource dataSource) {
        PropertyChangeListener visibilityListener = new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                queue.post(new Runnable() {
                    public void run() {
                        if ((Boolean)evt.getNewValue()) {
                            if (isDisplayed(dataSource.getOwner()) && !isDisplayed(dataSource))
                                processAddedDisplayableDataSources(Collections.singleton(dataSource));
                        } else {
                            if (isDisplayed(dataSource))
                                processRemovedDisplayedDataSources(Collections.singleton(dataSource));
                        }
                    }
                });
            }
        };
        dataSource.addPropertyChangeListener(DataSource.PROPERTY_VISIBLE, visibilityListener);
        visibilityListeners.put(dataSource, visibilityListener);
    }
    
    private void uninstallVisibilityListener(DataSource dataSource) {
        PropertyChangeListener visibilityListener = visibilityListeners.get(dataSource);
        dataSource.removePropertyChangeListener(DataSource.PROPERTY_VISIBLE, visibilityListener);
        visibilityListeners.remove(dataSource);
    }
    
    private boolean isDisplayed(DataSource dataSource) {
        return nodes.get(dataSource) != null;
    }
    
    private boolean isDisplayable(DataSource dataSource) {
        if (dataSource == DataSource.ROOT) return true;
        return dataSource.isVisible() && isDisplayed(dataSource.getOwner());
    }
    
    
    private ExplorerModelBuilder() {
        explorerRoot = new ExplorerNode(DataSource.ROOT);
        explorerRoot.setAutoExpansionPolicy(DataSourceDescriptor.EXPAND_ON_EACH_FIRST_CHILD);
        explorerModel = new DefaultTreeModel(explorerRoot);
        
        nodes.put(DataSource.ROOT, explorerRoot);
        
        DataSourceRepository.sharedInstance().addDataChangeListener(this, DataSource.class);
    }


    private static class RelativePositionComparator extends DataSourcesComparator {

        protected int getRelativePosition(DataSource d, int positionType) {
            try {
                // throws NumberFormatException
                return Integer.parseInt(d.getStorage().getCustomProperty(
                                        ExplorerNode.PROPERTY_RELATIVE_POSITION));
            } catch (Exception e) {
                return positionType;
            }
        }

    }


    private static class IndexNodePair implements Comparable<IndexNodePair> {
        
        public int index;
        public ExplorerNode node;
        
        public IndexNodePair(int index, ExplorerNode node) {
            this.index = index;
            this.node = node;
        }

        public int compareTo(IndexNodePair pair) {
            if (index == pair.index) return 0;
            if (index > pair.index) return 1;
            else return -1;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IndexNodePair other = (IndexNodePair) obj;
            if (this.index != other.index) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this.index;
            return hash;
        }
        
    }

}
