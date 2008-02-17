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
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptor;
import java.util.ArrayList;
import java.util.Collections;
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
    
    private String name;
    private Icon icon;
    private int preferredPosition;
    private int autoExpansionPolicy;
    
    private boolean firstExpansionFlag = true;
    
    
    public ExplorerNode(DataSource dataSource) {
        super(dataSource);
    }
    
    public ExplorerNode(DataSource dataSource, String name, Icon icon, int preferredPosition) {
        super(dataSource);
        setName(name);
        setIcon(icon);
        setPreferredPosition(preferredPosition);
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
        
        List<ExplorerNode> sortedNewChildren = new ArrayList(newChildren);
        Collections.sort(sortedNewChildren, Positionable.COMPARATOR);
        int insertPosition = 0;
        for (ExplorerNode newChild : sortedNewChildren) {
            int newChildPreferredPosition = newChild.getPreferredPosition();
            if (insertPosition == getChildCount()) {
                add(newChild);
                insertPosition++;
            } else {
                ExplorerNode node = (ExplorerNode)getChildAt(insertPosition);
                while (node.getPreferredPosition() <= newChildPreferredPosition && insertPosition < getChildCount()) {
                    insertPosition++;
                    if (insertPosition < getChildCount()) node = (ExplorerNode)getChildAt(insertPosition);
                }
                if (insertPosition == getChildCount()) add(newChild);
                else insert(newChild, insertPosition);
                insertPosition++;
            }
        }
        
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
        if (autoExpansionPolicy == DataSourceDescriptor.EXPAND_ON_EACH_CHILD_CHANGE)
            ExplorerSupport.sharedInstance().expandNode(this);
    }
    
    
    public String toString() {
        return getName();
    }
    
    
    public int getPreferredPosition() {
        return preferredPosition;
    }
    
    
    void setName(String name) {
        this.name = name;
    }
    
    void setIcon(Icon icon) {
        this.icon = icon;
    }
    
    void setPreferredPosition(int preferredPosition) {
        this.preferredPosition = preferredPosition;
    }
    
    void setAutoExpansionPolicy(int autoExpansionPolicy) {
        this.autoExpansionPolicy = autoExpansionPolicy;
        firstExpansionFlag = true;
    }

}
