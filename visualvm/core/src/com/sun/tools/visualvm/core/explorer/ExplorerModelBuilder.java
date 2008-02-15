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
import com.sun.tools.visualvm.core.datasource.DataSourceContainer;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
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
class ExplorerModelBuilder {
    
    private static ExplorerModelBuilder instance;
    
    private final ExplorerNode explorerRoot;
    private final DefaultTreeModel explorerModel;
    
    private final Map<DataSource, ExplorerNode> nodes;
    private final Map<DataSource, PropertyChangeListener> visibilityListeners = new HashMap();
    private final Map<DataSourceContainer, DataChangeListener> repositoryListeners = new HashMap();
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
    
    boolean isNodeInTree(DataSource dataSource) {
        return getNodeFor(dataSource) != null;
    }
    
    
    private void processAddedDataSources(final Set<DataSource> added) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                Set<ExplorerNode> addedNodes = new HashSet();
        
                for (DataSource dataSource : added) {

                    if (dataSource.isVisible()) {
                        // Process DataSource
                        if (dataSource != DataSource.ROOT) {
                            final ExplorerNode node = new ExplorerNode(dataSource);
                            addedNodes.add(node);
                            
                            DataSourceDescriptor descriptor = DataSourceDescriptorFactory.getDataSourceDescriptorFor(dataSource);
                            PropertyChangeListener descriptorListener = new PropertyChangeListener() {
                                public void propertyChange(PropertyChangeEvent evt) {
                                    updateNode(node, evt);
                                }
                            };
                            descriptor.addPropertyChangeListener(descriptorListener);
                            descriptorListeners.put(descriptor, descriptorListener);
                            updateNode(node, descriptor);
                        }

                        // Process repository of the DataSource
                        DataSourceContainer repository = dataSource.getRepository();
                        DataChangeListener repositoryListener = new DataChangeListener() {
                            public void dataChanged(DataChangeEvent event) {
                                processRemovedDataSources(event.getRemoved());
                                processAddedDataSources(event.getAdded());
                            }
                        };
                        repository.addDataChangeListener(repositoryListener, DataSource.class);
                        repositoryListeners.put(repository, repositoryListener);
                    }

                    // Track visibility of the DataSource
                    PropertyChangeListener visibilityListener = new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {
                            processRemovedDataSources(Collections.singleton((DataSource)evt.getSource()));
                            processAddedDataSources(Collections.singleton((DataSource)evt.getSource()));
                        }
                    };
                    dataSource.addPropertyChangeListener(DataSource.PROPERTY_VISIBLE, visibilityListener);
                    visibilityListeners.put(dataSource, visibilityListener);

                }

                addNodes(addedNodes);
                
            }
        });
    }
    
    private void processRemovedDataSources(final Set<DataSource> removed) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                
                Set<ExplorerNode> removedNodes = new HashSet();

                for (DataSource dataSource : removed) {

                    // Track visibility of the DataSource
                    PropertyChangeListener visibilityListener = visibilityListeners.get(dataSource);
                    dataSource.removePropertyChangeListener(DataSource.PROPERTY_VISIBLE, visibilityListener);
                    visibilityListeners.remove(dataSource);

                    // Process repository of the DataSource
                    DataSourceContainer repository = dataSource.getRepository();
                    DataChangeListener repositoryListener = repositoryListeners.get(repository);
                    if (repositoryListener != null) {
                        repository.removeDataChangeListener(repositoryListener);
                        repositoryListeners.remove(repository);
                        processRemovedDataSources(repository.getDataSources());

                        // Process DataSource
                        ExplorerNode node = getNodeFor(dataSource);
                        removedNodes.add(node);
                        
                        DataSourceDescriptor descriptor = DataSourceDescriptorFactory.getDataSourceDescriptorFor(dataSource);
                        descriptor.removePropertyChangeListener(descriptorListeners.get(descriptor));
                        descriptorListeners.remove(descriptor);
                    }

                }

                removeNodes(removedNodes);
                
            }
        });
    }
    
    private void updateNode(ExplorerNode node, DataSourceDescriptor descriptor) {
        node.setName(descriptor.getName());
        node.setIcon(descriptor.getIcon() == null ? null : new ImageIcon(descriptor.getIcon()));
        node.setPreferredPosition(descriptor.getPreferredPosition());
    }
    
    private void updateNode(final ExplorerNode node, final PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String property = evt.getPropertyName();
                Object newValue = evt.getNewValue();

                // Node name needs to be updated
                if (DataSourceDescriptor.PROPERTY_NAME.equals(property)) {
                    String name = (String)newValue;
                    node.setName(name);
                    explorerModel.nodeChanged(node);
                // Node icon needs to be updated
                } else if (DataSourceDescriptor.PROPERTY_ICON.equals(property)) {
                    Image icon = (Image)newValue;
                    node.setIcon(icon == null ? null : new ImageIcon(icon));
                    explorerModel.nodeChanged(node);
                // Node position within its parent needs to be updated
                } else if (DataSourceDescriptor.PROPERTY_PREFERRED_POSITION.equals(property)) {
                    Integer preferredPosition = (Integer)newValue;
                    node.setPreferredPosition(preferredPosition);
                    ExplorerNode parent = (ExplorerNode)node.getParent();
                    if (parent != null) {
                        parent.addNode(node);
                        explorerModel.nodesWereInserted(parent, new int[] { parent.getIndex(node) });
                    }
                }
            }
        });
    }
    
    private void addNodes(Set<ExplorerNode> added) {
        Map<ExplorerNode, List<Integer>> indexes = new HashMap();
        
        // Save selection
        DataSource selectedDataSource = ExplorerSupport.sharedInstance().getSelectedDataSource();
        
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
        for (ExplorerNode parent : indexes.keySet()) {
            List<Integer> indexesList = indexes.get(parent);
            Collections.sort(indexesList);
            int[] indexesArr = new int[indexesList.size()];
            for (int i = 0; i < indexesArr.length; i++) indexesArr[i] = indexesList.get(i);
            explorerModel.nodesWereInserted(parent, indexesArr);
        }
        
        // Try to restore selection
        ExplorerSupport.sharedInstance().selectDataSource(selectedDataSource);
    }
    
    private void removeNodes(Set<ExplorerNode> removed) {
        Map<ExplorerNode, List<IndexNodePair>> pairs = new HashMap();
        
        // Save selection
        DataSource selectedDataSource = ExplorerSupport.sharedInstance().getSelectedDataSource();
        
        // Setup map of indexes
        for (ExplorerNode node : removed) {
            ExplorerNode nodeParent = (ExplorerNode)node.getParent();
            pairs.put(nodeParent, new ArrayList());
        }

        // Remove nodes and cache indexes and childs
        for (ExplorerNode node : removed) {
            ExplorerNode nodeParent = (ExplorerNode)node.getParent();
            int index = nodeParent.getIndex(node);
            pairs.get(nodeParent).add(new IndexNodePair(index, node));
            node.removeFromParent();
            nodes.remove(node.getUserObject());
        }
        
        // Notify tree model
        for (ExplorerNode parent : pairs.keySet()) {
            List<IndexNodePair> indexesList = pairs.get(parent);
            Collections.sort(indexesList);
            int[] indexesArr = new int[indexesList.size()];
            Object[] childsArr = new Object[indexesList.size()];
            for (int i = 0; i < indexesArr.length; i++) {
                IndexNodePair pair = indexesList.get(i);
                indexesArr[i] = pair.index;
                childsArr[i] = pair.node;
            }
            explorerModel.nodesWereRemoved(parent, indexesArr, childsArr);
        }
        
        // Try to restore selection
        ExplorerSupport.sharedInstance().selectDataSource(selectedDataSource);
    }
    
    
    private ExplorerModelBuilder() {
        explorerRoot = new ExplorerNode(DataSource.ROOT);
        explorerModel = new DefaultTreeModel(explorerRoot);
        
        nodes = Collections.synchronizedMap(new HashMap());
        nodes.put(DataSource.ROOT, explorerRoot);
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() { processAddedDataSources(Collections.singleton((DataSource)DataSource.ROOT)); }
        });
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
        
    }

}
