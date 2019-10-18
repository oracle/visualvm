/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventType;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class BrowserNode extends CCTNode {
    
    private static final BrowserNode[] NO_NODES = new BrowserNode[0];
    
    private final BrowserNode parent;
    private final List<BrowserNode> children;
    
    final String name;
    final Icon icon;
    
    long eventsCount;
    
//    long time = -1;
//    String setting;
//    String value;
//    String thread;
    
    
    BrowserNode(String name, Icon icon, BrowserNode parent, List<BrowserNode> children) {
        this.parent = parent;
        this.children = children;
        
        this.name = name;
        this.icon = icon;
    }
    
    
    BrowserNode getChild(String name) {
        if (children != null)
            for (BrowserNode child : children)
                if (Objects.equals(name, child.name))
                    return child;
        return null;
    }
    
    void addEvent() {
        eventsCount++;
        if (parent != null) parent.addEvent();
    }
    
    
    @Override
    public BrowserNode getChild(int index) {
        return children == null ? null : children.get(index);
    }

    @Override
    public BrowserNode[] getChildren() {
        return children == null ? NO_NODES : children.toArray(NO_NODES);
    }

    @Override
    public int getIndexOfChild(Object child) {
        return children == null ? -1 : children.indexOf(child);
    }

    @Override
    public int getNChildren() {
        return children == null ? 0 : children.size();
    }
    
    @Override
    public boolean isLeaf() {
        return children == null ? true : children.isEmpty();
    }

    @Override
    public BrowserNode getParent() {
        return parent;
    }

    
    protected void addChild(BrowserNode child) {
        if (children != null) children.add(child);
    }
    
    protected void removeAllChildren() {
        if (children != null) children.clear();
    }
    
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BrowserNode)) return false;
        return Objects.equals(name, ((BrowserNode)o).name);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
//    public String eventsToString() {
//        return " (" + eventsCount + (eventsCount == 1 ? " event)" : " events)");
//    }
    
    
    static final class EventType extends BrowserNode {
        
        final String typeName;
        final JFREventType type;
        
        
        EventType(String typeName, JFREventType type, BrowserNode parent) {
            super(typeName == null ? "loading..." : type.getName(), null, parent, new ArrayList());
            
            this.typeName = typeName;
            this.type = type;
            
            if (type != null) {
                eventsCount++;
                parent.addEvent();
            }
        }
        

        @Override
        public String toString() {
            return type == null ? name : type.getDisplayName();
        }
        
    }
    
    
    static final class Category extends BrowserNode {
        
        Category(String name, BrowserNode parent) {
            super(name, null, parent, new ArrayList());
        }
        
    }
    
    
    static class Root extends BrowserNode implements JFREventVisitor {
        
//        private Map<Long, String> types;
        
//        private long settingID = 0;
        
        
        Root(String name) {
            this();
            EventType e = new EventType(null, null, this);
            addChild(e);
        }
        
        Root() {
            super(null, null, null, new ArrayList());
        }
        
        
//        void visitEventTypes() {};
//        
//        
//        @Override
//        public void initTypes() {
//            types = new HashMap();
//        }
//        
//        @Override
//        public boolean visitType(JFREventType type) {
//            types.put(type.getId(), type.getDisplayName());
//            return false;
//        }
        
        
        void reloadEvents(JFREventVisitor visitor) {};
        
        JFREventType type(String typeName) { return null; }
        

        @Override
        public boolean visit(String typeName, JFREvent event) {
//            if ("jdk.ActiveSetting".equals(typeName)) { // NOI18N
//                try {
                    JFREventType type = type(typeName);
//                    String eventName = typeName;
                    String eventName = type.getName();
                    
                    BrowserNode categoryNode = getOrCreateCategory(this, type.getCategory());
                    BrowserNode eventNode = categoryNode.getChild(eventName);
                    if (eventNode == null) {
                        eventNode = new EventType(typeName, type, categoryNode);
                        categoryNode.addChild(eventNode);
                    } else {
                        ((EventType)eventNode).addEvent();
                    }
//                    String eventName;
//                    try {
//                        eventName = getTypeName(event.getLong("id")); // NOI18N
//                    } catch (JFRPropertyNotAvailableException e) {
//                        eventName = event.getValue("settingFor").toString(); // NOI18N
//                    }
//
//                    BrowserNode eventNode = getChild(eventName);
//                    if (eventNode == null) {
//                        eventNode = new Event(eventName, this);
//                        addChild(eventNode);
//                    }
//
//                    String settingName = event.getString("name"); // NOI18N
//                    String settingValue = event.getString("value"); // NOI18N
//                    
//                    String threadName;
//                    try {
//                        threadName = event.getThread("eventThread").getName(); // NOI18N
//                    } catch (JFRPropertyNotAvailableException e) {
//                        threadName = "-"; // NOI18N
//                    }
//                    
//                    BrowserNode settingNode = new Setting(settingID++, settingName, settingValue, threadName, event.getInstant("eventTime").toEpochMilli(), eventNode);
//                    eventNode.addChild(settingNode);
//                } catch (JFRPropertyNotAvailableException e) {
//                    System.err.println(">>> XX " + e + " -- " + event);
//                }
//            }
            
            return false;
        }
        
        @Override
        public void done() {
//            if (types != null) {
//                types.clear();
//                types = null;
//            }
        }
        
        
        private static BrowserNode getOrCreateCategory(BrowserNode parent, List<String> category) {
            List<String> names = new ArrayList(category);
            
            while (!names.isEmpty()) {
                String name = names.remove(0);
                BrowserNode node = parent.getChild(name);
                if (!(node instanceof BrowserNode)) {
                    node = new Category(name, parent);
                    parent.addChild(node);
                }
                parent = node;
            }
            
            return parent;
        }
        
        
//        private String getTypeName(long typeID) {
//            if (types == null) visitEventTypes();
//            return types.get(typeID);
//        }
        
        
        @Override
        public int hashCode() {
            return 37;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Root;
        }
        
    }
    
}
