/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Utils;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
class ExplorerModelBuilder implements DataChangeListener<DataSource> {
    
    private static final RequestProcessor queue = new RequestProcessor("Explorer Builder Processor");
    
    private static ExplorerModelBuilder instance;
    
    private final ExplorerNode explorerRoot;
    private final DefaultTreeModel explorerModel;
    
    private final Map<DataSource, ExplorerNode> nodes;
    private final Map<DataSource, PropertyChangeListener> visibilityListeners = new HashMap();
    private final Map<DataSourceDescriptor, PropertyChangeListener> descriptorListeners = new HashMap();
    
    
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
        final Set<ExplorerNode> addedNodes = new HashSet();
        
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
        node.setAutoExpansionPolicy(descriptor.getAutoExpansionPolicy());
    }
    
    private void updateNode(final ExplorerNode node, final PropertyChangeEvent evt) {
        String property = evt.getPropertyName();
        Object newValue = evt.getNewValue();

        // Node name needs to be updated
        if (DataSourceDescriptor.PROPERTY_NAME.equals(property)) {
            String name = (String)newValue;
            node.setName(name);
            try { SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { explorerModel.nodeChanged(node); }
            }); } catch (Exception e) {}
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
                parent.addNode(node);
                try { SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() { explorerModel.nodesWereInserted(parent, new int[] { parent.getIndex(node) }); }
                }); } catch (Exception e) {}
            }
        } else if (DataSourceDescriptor.PROPERTY_EXPANSION_POLICY.equals(property)) {
            node.setAutoExpansionPolicy((Integer)evt.getNewValue());
        }
    }
    
    private void addNodes(Set<ExplorerNode> added) {
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
        for (Map.Entry<ExplorerNode, List<Integer>> entry : indexes.entrySet()) {
            List<Integer> indexesList = entry.getValue();
            Collections.sort(indexesList);
            int[] indexesArr = new int[indexesList.size()];
            for (int i = 0; i < indexesArr.length; i++) indexesArr[i] = indexesList.get(i);
            explorerModel.nodesWereInserted(entry.getKey(), indexesArr);
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
            explorerModel.nodesWereRemoved(entry.getKey(), indexesArr, childsArr);
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
        
        nodes = new HashMap();
        nodes.put(DataSource.ROOT, explorerRoot);
        
        DataSourceRepository.sharedInstance().addDataChangeListener(this, DataSource.class);
    }
    
    
    private static class IndexNodePair implements Comparable {
        
        public int index;
        public ExplorerNode node;
        
        public IndexNodePair(int index, ExplorerNode node) {
            this.index = index;
            this.node = node;
        }

        public int compareTo(Object o) {
            IndexNodePair pair = (IndexNodePair)o;
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
