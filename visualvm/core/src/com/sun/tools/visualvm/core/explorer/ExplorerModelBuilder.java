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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

/**
 *
 * @author Jiri Sedlacek
 */
// TODO: synchronize?
public class ExplorerModelBuilder implements DataChangeListener<DataSource> {
    
    private static ExplorerModelBuilder instance;
    
    private final Map<DataSource, ExplorerNode> nodes;
    private final ExplorerNode explorerRoot;
    private final DefaultTreeModel explorerModel;
    
    
    public static synchronized ExplorerModelBuilder getInstance() {
        if (instance == null) instance = new ExplorerModelBuilder();
        return instance;
    }
    
    
    public TreeModel getModel() {
        return explorerModel;
    }
    
    
    public void dataChanged(DataChangeEvent event) {
//        System.err.println(">>> Data changed...");
//        System.err.println(">>>    added:   " + event.getAdded());
//        System.err.println(">>>    removed: " + event.getRemoved());
//        System.err.println(">>> ---------------------------------");
        processAddedDataSource(event.getAdded());
        processRemovedDataSources(event.getRemoved());
    }
    
    
    private void processAddedDataSource(Set<DataSource> added) {
        Set<ExplorerNode> addedNodes = new HashSet();
        
        for (DataSource dataSource : added) {
            // DataSource is finished and shouldn't be displayed at all
            if (dataSource.isFinished()) continue;
            
            // DataSource doesn't have parent or is not visible, will be added once visible & parent defined
            if (dataSource.getOwner() == null || !dataSource.isVisible()) scheduleNodeCreate(dataSource);
            
            // DataSource has parent and is visible, create node
            else addedNodes.add(createNodeFor(dataSource));
        }
        
        addNodes(addedNodes);
    }
    
    private void processRemovedDataSources(Set<DataSource> removed) {
        Set<ExplorerNode> removedNodes = new HashSet();
        
        for (DataSource dataSource : removed) {
            ExplorerNode node = getNodeFor(dataSource);
            if (node != null) removedNodes.add(node);
        }
        
        removeNodes(removedNodes);
    }
    
    private void scheduleNodeCreate(final DataSource dataSource) {
        dataSource.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (dataSource.isFinished()) dataSource.removePropertyChangeListener(this);
                else if (dataSource.isVisible() && dataSource.getOwner() != null) {
                    dataSource.removePropertyChangeListener(this);
                    createNodeFor(dataSource);
                }
            }
        });
    }
    
    private ExplorerNode getNodeFor(DataSource dataSource) {
        return nodes.get(dataSource);
    }
    
    private boolean isNodeInTree(DataSource dataSource) {
        return getNodeFor(dataSource) != null;
    }
    
    private ExplorerNode createNodeFor(final DataSource dataSource) {
        final ExplorerNode node = new ExplorerNode(dataSource);
        DataSourceDescriptor descriptor = DataSourceDescriptorFactory.getDescriptor(dataSource);
        
        // Update node appearance according to descriptor changes
        descriptor.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) { updateNode(node, evt); }
        });
        
        // Add/remove the node according to DataSource visibility/owner
        dataSource.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) { updateNode(dataSource, node, evt); }
        });
        
        node.setName(descriptor.getName());
        node.setIcon(descriptor.getIcon() == null ? null : new ImageIcon(descriptor.getIcon()));
        node.setPreferredPosition(descriptor.getPreferredPosition());
        
        return node;
    }
    
    private void updateNode(ExplorerNode node, PropertyChangeEvent evt) {
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
    
    private void updateNode(DataSource dataSource, ExplorerNode node, PropertyChangeEvent evt) {
        // DataSource became visible and parent defined, node needs to be added
        if (dataSource.isVisible() && dataSource.getOwner() != null && !isNodeInTree(dataSource))
            addNodes(Collections.singleton(node));
        // DataSource became invisible and/or parent not defined, node needs to be removed
        else if ((!dataSource.isVisible() || dataSource.getOwner() == null) && isNodeInTree(dataSource))
            removeNodes(Collections.singleton(node));
        // DataSource has different parent, node needs to be moved
        else if (DataSource.PROPERTY_OWNER.equals(evt.getPropertyName())) {
            if (isNodeInTree(dataSource)) removeNodes(Collections.singleton(node));
            addNodes(Collections.singleton(node));
        }
    }
    
    private void addNodes(Set<ExplorerNode> added) {
        Set<ExplorerNode> parents = new HashSet();
                
        for (ExplorerNode node : added) {
            DataSource dataSource = node.getUserObject();
            ExplorerNode nodeParent = getNodeFor(dataSource.getOwner());
            nodes.put(dataSource, node);
            nodeParent.addNode(node);
            parents.add(nodeParent);
        }
        
        for (ExplorerNode parent : parents) explorerModel.nodeStructureChanged(parent);
    }
    
    private void removeNodes(Set<ExplorerNode> removed) {
        Set<ExplorerNode> parents = new HashSet();
        
        for (ExplorerNode node : removed) {
            ExplorerNode nodeParent = (ExplorerNode)node.getParent();
            node.removeFromParent();
            parents.add(nodeParent);
            nodes.remove(node.getUserObject());
        }
        
        for (ExplorerNode parent : parents) explorerModel.nodeStructureChanged(parent);
    }
    
    
    private ExplorerModelBuilder() {
        explorerRoot = createNodeFor(DataSource.ROOT);
        explorerModel = new DefaultTreeModel(explorerRoot);
        nodes = new HashMap();
        nodes.put(DataSource.ROOT, explorerRoot);
    }

}
