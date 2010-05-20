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
import com.sun.tools.visualvm.core.datasource.Storage;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 *
 * @author Jiri Sedlacek
 */
final class ExplorerNode extends DefaultMutableTreeNode implements Positionable {

    static final String PROPERTY_RELATIVE_POSITION = "prop_relative_position"; // NOI18N
    
    private String name;
    private Icon icon;
    private int preferredPosition;
    private int autoExpansionPolicy;

    private boolean defaultComparator = true;
    private ExplorerNodesComparator comparator = new ExplorerNodesComparator(new PositionableComparator());
    private final List<DataSource> endPositions = Collections.synchronizedList(new ArrayList());
    private final List<DataSource> lastPositions = Collections.synchronizedList(new ArrayList());

    private boolean firstExpansionFlag = true;
    
    
    public ExplorerNode(DataSource dataSource) {
        super(dataSource);
    }
    
    
    public String getName() {
        return name;
    }
    
    public Icon getIcon() {
        return icon;
    }
    
    public DataSource getUserObject() {
        return (DataSource)super.getUserObject();
    }
    
    
    public void addNode(ExplorerNode newChild) {
        addNodes(Collections.singleton(newChild));
    }
    
    public void addNodes(Set<ExplorerNode> newChildren) {
        int originalChildCount = getChildCount();
        
        for (ExplorerNode child : newChildren) {
            add(child);
            checkAddRelativePosition(child);
        }
        sortChildren();

        boolean shouldExpand = false;
        boolean firstChildAdded = originalChildCount == 0 && getChildCount() > 0;
        switch (autoExpansionPolicy) {
//            case DataSourceDescriptor.EXPAND_NEVER:
//                break;
            case DataSourceDescriptor.EXPAND_ON_FIRST_CHILD:
                if (firstExpansionFlag && firstChildAdded) shouldExpand = true;
                firstExpansionFlag = false;
                break;
            case DataSourceDescriptor.EXPAND_ON_EACH_FIRST_CHILD:
                if (firstChildAdded) shouldExpand = true;
                break;
            case DataSourceDescriptor.EXPAND_ON_EACH_NEW_CHILD:
            case DataSourceDescriptor.EXPAND_ON_EACH_CHILD_CHANGE:
                if (newChildren.size() > 0) shouldExpand = true;
                break;
        }
        if (shouldExpand) ExplorerSupport.sharedInstance().expandNode(this);
    }
    
    public void remove(MutableTreeNode aChild) {
        super.remove(aChild);

        checkRemoveRelativePosition((ExplorerNode)aChild);

        if (autoExpansionPolicy == DataSourceDescriptor.EXPAND_ON_EACH_CHILD_CHANGE)
            ExplorerSupport.sharedInstance().expandNode(this);
    }
    
    
    public String toString() {
        return getName();
    }
    
    
    public int getPreferredPosition() {
        return preferredPosition;
    }
    
    
    boolean setName(String name) {
        // No parent, no structure changes
        if (parent == null) {
            this.name = name;
            return false;
        // Name changed
        } else if (this.name == null || !this.name.equals(name)) {
            this.name = name;
            ((ExplorerNode)parent).sortChildren();
            return true;
        // Name unchanged
        } else {
            return false;
        }
    }
    
    void setIcon(Icon icon) {
        this.icon = icon;
    }
    
    void setPreferredPosition(int preferredPosition) {
        this.preferredPosition = preferredPosition;
    }

    boolean setComparator(Comparator<DataSource> comparator) {
        boolean change = false;

        if (comparator == null) {
            change = !defaultComparator;
            if (change) this.comparator = new ExplorerNodesComparator(new PositionableComparator());
            defaultComparator = true;
        } else {
            change = defaultComparator || !this.comparator.uses(comparator);
            if (change) this.comparator = new ExplorerNodesComparator(comparator);
            defaultComparator = false;
        }

        if (change) sortChildren();
        return change;
    }
    
    void setAutoExpansionPolicy(int autoExpansionPolicy) {
        this.autoExpansionPolicy = autoExpansionPolicy;
        firstExpansionFlag = true;
    }


    private void sortChildren() {
        if (getChildCount() == 0) return;
        Collections.sort(children, comparator);
    }


    void syncRelativePositions() {
        List<DataSource> nodes = new ArrayList();

        synchronized (endPositions) { nodes.addAll(endPositions); }
        if (!nodes.isEmpty()) updateRelativePositions(nodes);

        nodes.clear();

        synchronized (lastPositions) { nodes.addAll(lastPositions); }
        if (!nodes.isEmpty()) updateRelativePositions(nodes);
    }

    private void updateRelativePositions(List<DataSource> positions) {
        for (int i = 0; i < positions.size(); i++) {
            Storage s = positions.get(i).getStorage();
            if (s.getCustomProperty(DataSourceDescriptor.PROPERTY_PREFERRED_POSITION) != null) {
                String posS = s.getCustomProperty(PROPERTY_RELATIVE_POSITION);
                Integer posI = null;
                if (posS != null)
                    try { posI = Integer.valueOf(posS); } catch (NumberFormatException e) {}
                if (posI == null || posI.intValue() != i)
                    s.setCustomProperty(PROPERTY_RELATIVE_POSITION, Integer.toString(i));
            }
        }
    }

    private void checkAddRelativePosition(ExplorerNode node) {
        int pos = node.getPreferredPosition();
        DataSource d = node.getUserObject();
        if (pos == Positionable.POSITION_AT_THE_END) addEndPosition(d);
        else if (pos == Positionable.POSITION_LAST) addLastPosition(d);
    }

    private void checkRemoveRelativePosition(ExplorerNode node) {
        int pos = node.getPreferredPosition();
        DataSource d = node.getUserObject();
        if (pos == Positionable.POSITION_AT_THE_END) removeEndPosition(d);
        else if (pos == Positionable.POSITION_LAST) removeLastPosition(d);
    }

    private void addEndPosition(DataSource node) {
        endPositions.add(node);
    }

    private void removeEndPosition(DataSource node) {
        endPositions.remove(node);
    }

    private void addLastPosition(DataSource node) {
        lastPositions.add(node);
    }

    private void removeLastPosition(DataSource node) {
        lastPositions.remove(node);
    }


    private class PositionableComparator extends DataSourcesComparator {

        protected int getRelativePosition(DataSource d, int positionType) {
            if (positionType == Positionable.POSITION_AT_THE_END)
                return endPositions.indexOf(d);

            else if (positionType == Positionable.POSITION_LAST)
                return lastPositions.indexOf(d);
            
            else
                return positionType;
        }

    }

}
