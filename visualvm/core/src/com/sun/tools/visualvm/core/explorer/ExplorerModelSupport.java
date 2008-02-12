/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DomainTree;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import org.openide.util.RequestProcessor;

/**
 * Class for accessing the explorer tree model.
 *
 * @author Jiri Sedlacek
 */
public class ExplorerModelSupport implements DataChangeListener<DataSource> {

    private static final RequestProcessor query = new RequestProcessor("ExplorerModelQuery Processor");

    private static ExplorerModelSupport sharedInstance;
    
    private final ExplorerModel explorerModel = new ExplorerModel(ExplorerRoot.NODE);
    private final Set<DataSource> unhandledDataSources = new HashSet();
    private final DomainTree<ExplorerNodeBuilder, Class<? extends DataSource>> builders = new DomainTree(DataSource.class, new ClassesComparator());
    
    
    /**
     * Returns singleton instance of ExplorerModelSupport.
     * 
     * @return singleton instance of ExplorerModelSupport.
     */
    public static synchronized ExplorerModelSupport sharedInstance() {
        if (sharedInstance == null) sharedInstance = new ExplorerModelSupport();
        return sharedInstance;
    }
    
    
    /**
     * Returns DefaultTreeModel used for the explorer tree.
     * 
     * @return DefaultTreeModel used for the explorer tree.
     */
    public DefaultTreeModel getExplorerModel() {
        synchronized(explorerModel) {
            return explorerModel;
        }
    }
    
    
    /**
     * Adds a root node into explorer tree.
     * 
     * @param node node to be added.
     */
    public void addRootNode(ExplorerNode node) {
        addRootNodes(Collections.singleton(node));
    }
    
    /**
     * Adds root nodes into explorer tree.
     * 
     * @param nodes set of nodes to be added.
     */
    public void addRootNodes(Set<ExplorerNode> nodes) {
        addNodes(nodes, ExplorerRoot.NODE);
    }
    
    /**
     * Adds a node into explorer tree.
     * 
     * @param node node to be added,
     * @param parent parent node to which to add the node.
     */
    public void addNode(ExplorerNode node, ExplorerNode parent) {
        addNodes(Collections.singleton(node), parent);
    }
    
    /**
     * Adds nodes into explorer tree.
     * 
     * @param nodes set of nodes to be added,
     * @param parent parent node to whuch to add the nodes.
     */
    public void addNodes(final Set<ExplorerNode> nodes, final ExplorerNode parent) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized(explorerModel) {
                    parent.addNodes(nodes);
                    final int[] insertedIndexes = new int[nodes.size()];
                    Iterator<ExplorerNode> nodesIterator = nodes.iterator();
                    for (int i = 0; i < insertedIndexes.length; i++) insertedIndexes[i] = parent.getIndex(nodesIterator.next());
                    explorerModel.nodesWereInserted(parent, insertedIndexes);
                }
            }
        });
    }
    
    /**
     * Notifies the explorer tree that structure of given node has changed.
     * 
     * @param node node with changed structure.
     */
    public void updateNodeStructure(ExplorerNode node) {
        updateNodesStructure(Collections.singleton(node));
    }
    
    /**
     * Notifies the explorer tree that structure of given nodes has changed.
     * 
     * @param nodes set of nodes with changed structure.
     */
    public void updateNodesStructure(final Set<ExplorerNode> nodes) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized(explorerModel) {
                    for (ExplorerNode node : nodes) explorerModel.nodeStructureChanged(node);
                }
            }
        });
    }
    
    /**
     * Notifies the explorer that appearance of the given DataSource represented
     * in explorer tree by some node has changed.
     * 
     * @param dataSource DataSource with changed appearance.
     */
    public void updateDataSourceApperance(DataSource dataSource) {
        ExplorerNode node = getNodeFor(dataSource);
        if (node != null) updateNodeAppearance(node);
    }
    
    /**
     * Notifies the explorer that appearance of an ExplorerNode has changed.
     * 
     * @param node ExplorerNode with changed appearance.
     */
    public void updateNodeAppearance(ExplorerNode node) {
        updateNodesAppearance(Collections.singleton(node));
    }
    
    /**
     * Notifies the explorer that appearance of ExplorerNodes has changed.
     * 
     * @param nodes ExplorerNodes with changed appearance.
     */
    public void updateNodesAppearance(final Set<ExplorerNode> nodes) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized(explorerModel) {
                    for (ExplorerNode node : nodes) explorerModel.nodeChanged(node);
                }
            }
        });
    }
    
    /**
     * Removes a node from the explorer tree.
     * 
     * @param node ExplorerNode to be removed.
     */
    public void removeNode(ExplorerNode node) {
        removeNodes(Collections.singleton(node));
    }
    
    /**
     * Removes nodes from the explorer tree.
     * 
     * @param nodes set of ExplorerNode instances to be removed.
     */
    public void removeNodes(final Set<ExplorerNode> nodes) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized(explorerModel) {
                    for (ExplorerNode node : nodes) explorerModel.removeNodeFromParent(node);
                }
            }
        });
    }
    
    /**
     * Returns a node representing given DataSource in explorer tree or null if no node for the DataSource has been found.
     * 
     * @param dataSource
     * @return node representing given DataSource in explorer tree or null if no node has been found.
     */
    public ExplorerNode getNodeFor(DataSource dataSource) {
        ExplorerNodeBuilder builder = getBuilderFor(dataSource);
        return builder == null ? null : builder.getNodeFor(dataSource);
    }
    
    
    /**
     * Registers an ExplorerNodeBuilder for given scope of DataSources.
     * 
     * @param builder ExplorerNodeBuilder to register,
     * @param scope scope of DataSources for which the builder can create nodes.
     */
    public <X extends DataSource> void addBuilder(final ExplorerNodeBuilder<X> builder, final Class<? extends X> scope) {
        query.post(new Runnable() {
            public void run() {
                builders.add(builder, scope);
                Set<DataSource> unhandled = new HashSet(unhandledDataSources);
                for (DataSource dataSource : unhandled)
                    if (scope.isInstance(dataSource) && builder.getNodeFor((X)dataSource) != null) unhandledDataSources.remove(dataSource);
            }
        });
    }
    
    /**
     * Unregisters an ExplorerNodeBuilder.
     * 
     * @param builder ExplorerNodeBuilder to unregister.
     */
    public void removeBuilder(final ExplorerNodeBuilder builder) {
        query.post(new Runnable() {
            public void run() {
                builders.remove(builder);
            }
        });
    }
    
    
    public void dataChanged(final DataChangeEvent<DataSource> event) {
        query.post(new Runnable() {
            public void run() {
                if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
                    // Initial event to deliver DataSources already created by the provider before registering to it as a listener
                    processNewDataSources(event.getCurrent());
                } else {
                    // Real delta event
                    processNewDataSources(event.getAdded());
                    processRemovedDataSources(event.getRemoved());
                }
            }
        });
    }
    
    private void processNewDataSources(Set<DataSource> dataSources) {
        for (DataSource dataSource : dataSources) {
            if (getBuilderFor(dataSource) == null) unhandledDataSources.add(dataSource);
        }
    }
    
    private void processRemovedDataSources(Set<DataSource> dataSources) {
        unhandledDataSources.removeAll(dataSources);
    }
    
    private ExplorerNodeBuilder getBuilderFor(DataSource dataSource) {
        List<ExplorerNodeBuilder> compatibleBuilders = getCompatibleBuildersFor(dataSource);
        for (ExplorerNodeBuilder compatibleBuilder : compatibleBuilders)
            if (compatibleBuilder.getNodeFor(dataSource) != null) return compatibleBuilder;
        return null;
    }
    
    private List<ExplorerNodeBuilder> getCompatibleBuildersFor(DataSource dataSource) {
        List<ExplorerNodeBuilder> compatibleBuilders = builders.getItemsToClosestDomain(dataSource.getClass());
        Collections.reverse(compatibleBuilders);
        return compatibleBuilders;
    }
            
    
    private ExplorerModelSupport() {
        DataSourceRepository.sharedInstance().addDataChangeListener(ExplorerModelSupport.this, DataSource.class);
    }
    
    
    public static class ExplorerModel extends DefaultTreeModel {
        
        public ExplorerModel(ExplorerNode root) {
            super(root);
        }
        
    }
    
    
    private static class ClassesComparator implements DomainTree.DomainsComparator<ExplorerNodeBuilder, Class<? extends DataSource>> {
        
        public Result compare(Class<? extends DataSource> d1, Class<? extends DataSource> d2) {            
            if (d1.equals(d2)) return DomainTree.DomainsComparator.Result.EQUALS;
            if (d1.isAssignableFrom(d2)) return DomainTree.DomainsComparator.Result.LESS;
            if (d2.isAssignableFrom(d1)) return DomainTree.DomainsComparator.Result.MORE;
            return DomainTree.DomainsComparator.Result.NOT_COMPARABLE;
        }

        public Class<? extends DataSource> getSuperDomain(Class<? extends DataSource> d1, Class<? extends DataSource> d2) {
            // NOTE: this isn't neccessary as long as the root domain is DataSource.class
            return null;
        }

        public Comparator<ExplorerNodeBuilder> getItemsComparator(Class<? extends DataSource> d) {
            return null;
        }
    }
    
}
