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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author Jiri Sedlacek
 */
public class DefaultExplorerNode<T> extends DefaultMutableTreeNode implements ExplorerNode<T> {

    private String name;
    private Icon icon;
    private int preferredPosition;


    public DefaultExplorerNode(String name, Icon icon) {
        this(name, icon, POSITION_AT_THE_END, null);
    }

    public DefaultExplorerNode(String name, Icon icon, T userObject) {
        this(name, icon, POSITION_AT_THE_END, userObject);
    }

    public DefaultExplorerNode(String name, Icon icon, int preferredPosition) {
        this(name, icon, preferredPosition, null);
    }

    public DefaultExplorerNode(String name, Icon icon, int preferredPosition, T userObject) {
        super(userObject);
        
        this.name = name;
        this.icon = icon;
        this.preferredPosition = preferredPosition;
    }
    
    
    public String getName() {
        return name;
    }
    
    public Icon getIcon() {
        return icon;
    }
    
    public int getPreferredPosition() {
        return preferredPosition;
    }
    
    public T getUserObject() {
        return (T)super.getUserObject();
    };
    

    public void addNode(ExplorerNode newChild) {
        addNodes(Collections.singleton(newChild));
    }
    
    public void addNodes(Set<ExplorerNode> newChildren) {
        List<ExplorerNode> sortedNewChildren = new ArrayList(newChildren);
        Collections.sort(sortedNewChildren);
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
    }
    
    public int compareTo(Object o) {
        ExplorerNode node = (ExplorerNode)o;
        int preferredNodePosition = node.getPreferredPosition();
        if (preferredPosition == preferredNodePosition) return 0;
        if (preferredPosition > preferredNodePosition) return 1;
        return -1;
    }
    
    public String toString() {
        return getName();
    }

}
