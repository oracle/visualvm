/*
 *  Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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

    private int maxEndPosition = -1;
    private int maxLastPosition = -1;
    private final Map<DataSource, Integer> endPositions = Collections.synchronizedMap(new HashMap());
    private final Map<DataSource, Integer> lastPositions = Collections.synchronizedMap(new HashMap());

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


    void sortChildren() {
        if (getChildCount() == 0) return;
        Collections.sort(children, comparator);
    }


    private void checkAddRelativePosition(ExplorerNode node) {
        int pos = node.getPreferredPosition();
        DataSource d = node.getUserObject();
        if (pos == Positionable.POSITION_AT_THE_END) addPosition(d, endPositions);
        else if (pos == Positionable.POSITION_LAST) addPosition(d, lastPositions);
    }

    private void addPosition(DataSource node, Map<DataSource, Integer> positions) {
        Storage s = node.getStorage();
        String PREF = DataSourceDescriptor.PROPERTY_PREFERRED_POSITION;
        
        int nodePos = 0;
        boolean posDirty = true;
        try {
            // throws NullPointerException
            nodePos = getMaxPosition(positions) + 1;
            // throws NumberFormatException
            nodePos = Integer.parseInt(s.getCustomProperty(PROPERTY_RELATIVE_POSITION));
            posDirty = false;
        } catch (Exception e) {}

        if (s.getCustomProperty(PREF) != null && posDirty)
            s.setCustomProperty(PROPERTY_RELATIVE_POSITION, Integer.toString(nodePos));

        positions.put(node, nodePos);
        updateMaxPosition(positions, nodePos, false);
    }

    private void checkRemoveRelativePosition(ExplorerNode node) {
        int pos = node.getPreferredPosition();
        DataSource d = node.getUserObject();
        if (pos == Positionable.POSITION_AT_THE_END) removePosition(d, endPositions);
        else if (pos == Positionable.POSITION_LAST) removePosition(d, lastPositions);
    }

    private void removePosition(DataSource node, Map<DataSource, Integer> positions) {
        int nodePos = positions.remove(node);
        updateMaxPosition(positions, nodePos, true);
    }
    
    private int getMaxPosition(Map<DataSource, Integer> positions) {
        if (positions == endPositions) return maxEndPosition;
        else return maxLastPosition;
    }

    private void setMaxPosition(Map<DataSource, Integer> positions, int newMax) {
        if (positions == endPositions) maxEndPosition = newMax;
        else maxLastPosition = newMax;
    }

    private void updateMaxPosition(Map<DataSource, Integer> positions, int position, boolean remove) {
        int maxPos = getMaxPosition(positions);
        if (!remove && maxPos < position)
            setMaxPosition(positions, position);
        else if (remove && maxPos == position) {
            int newMax = -1;
            Collection<Integer> values = positions.values();
            for (int i : values) if (i > newMax) newMax = i;
            setMaxPosition(positions, newMax);
        }
    }


    private class PositionableComparator extends DataSourcesComparator {

        protected int getRelativePosition(DataSource d, int positionType) {
            if (positionType == Positionable.POSITION_AT_THE_END)
                return endPositions.get(d);

            else if (positionType == Positionable.POSITION_LAST)
                return lastPositions.get(d);
            
            else
                return positionType;
        }

    }

}
