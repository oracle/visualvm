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
package org.graalvm.visualvm.jfr.views.recording;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Icon;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventType;
import org.graalvm.visualvm.jfr.model.JFREventTypeVisitor;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class RecordingNode extends CCTNode {
    
    private static final RecordingNode[] NO_NODES = new RecordingNode[0];
    
    private final RecordingNode parent;
    private final List<RecordingNode> children;
    
    final String name;
    final Icon icon;
    
    long time = Long.MIN_VALUE;
    String setting;
    String value;
    String thread;
    
    
    RecordingNode(String name, Icon icon, RecordingNode parent, List<RecordingNode> children) {
        this.parent = parent;
        this.children = children;
        
        this.name = name;
        this.icon = icon;
    }
    
    
    RecordingNode getChild(String name) {
        if (children != null)
            for (RecordingNode child : children)
                if (Objects.equals(name, child.name))
                    return child;
        return null;
    }
    
    
    @Override
    public RecordingNode getChild(int index) {
        return children == null ? null : children.get(index);
    }

    @Override
    public RecordingNode[] getChildren() {
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
    public RecordingNode getParent() {
        return parent;
    }

    
    protected void addChild(RecordingNode child) {
        if (children != null) children.add(child);
    }
    
    protected void removeAllChildren() {
        if (children != null) children.clear();
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    
    static final class Event extends RecordingNode {
        
        Event(String name, RecordingNode parent) {
            super(name, null, parent, new ArrayList());
        }
        
        
        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Event)) return false;
            return Objects.equals(name, ((Event)o).name);
        }

        @Override
        public String toString() {
            return name;
        }
        
    }
    
    
    static final class Setting extends RecordingNode {
        
        private final long id;
        
        
        Setting(long id, String name, String value, String thread, long time, RecordingNode parent) {
            super(name, null, parent, null);
            
            this.id = id;
            
            this.value = value;
            this.thread = thread;
            this.time = time;
        }
        
        
        @Override
        public int hashCode() {
            return Long.hashCode(id);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Setting)) return false;
            return id == ((Setting)o).id;
        }
        
    }
    
    
    static class Root extends RecordingNode implements JFREventVisitor, JFREventTypeVisitor {
        
        private Map<Long, String> types;
        
        private long settingID = 0;
        
        
        Root(String name) {
            this();
            Event e = new Event(name, this);
            addChild(e);
        }
        
        Root() {
            super(null, null, null, new ArrayList());
        }
        
        
        void visitEventTypes() {};
        
        long toRelativeNanos(Instant time) { return Long.MIN_VALUE; }
        
        
        @Override
        public void initTypes() {
            types = new HashMap();
        }
        
        @Override
        public boolean visitType(String typeName, JFREventType type) {
            types.put(type.getId(), type.getDisplayName());
            return false;
        }
        
        
        @Override
        public boolean visit(String typeName, JFREvent event) {
            if ("jdk.ActiveSetting".equals(typeName)) { // NOI18N
                try {
                    String eventName = getTypeName(event);

                    RecordingNode eventNode = getChild(eventName);
                    if (eventNode == null) {
                        eventNode = new Event(eventName, this);
                        addChild(eventNode);
                    }

                    String settingName = event.getString("name"); // NOI18N
                    String settingValue = event.getString("value"); // NOI18N
                    
                    String threadName = getThreadName(event);
                    
                    RecordingNode settingNode = new Setting(settingID++, settingName, settingValue, threadName, toRelativeNanos(event.getInstant("eventTime")), eventNode); // NOI18N
                    eventNode.addChild(settingNode);
                } catch (JFRPropertyNotAvailableException e) {
                    System.err.println(">>> XX " + e + " -- " + event);
                }
            }
            
            return false;
        }
        
        @Override
        public void done() {
            if (types != null) {
                types.clear();
                types = null;
            }
        }
        
        
        private Boolean typeMode;
        
        private String getTypeName(JFREvent event) throws JFRPropertyNotAvailableException {
            if (Boolean.TRUE.equals(typeMode)) {                // JFR loaders
                return getTypeName(event.getLong("id"));                        // NOI18N
            } else if (Boolean.FALSE.equals(typeMode)) {        // Generic loader
                return event.getValue("settingFor").toString();                 // NOI18N
            } else {                                            // not initialized yet
                try {
                    String eventName = getTypeName(event.getLong("id"));        // NOI18N
                    typeMode = Boolean.TRUE;
                    return eventName;
                } catch (JFRPropertyNotAvailableException e) {
                    String eventName = event.getValue("settingFor").toString(); // NOI18N
                    typeMode = Boolean.FALSE;
                    return eventName;
                }
            }
        }
        
        private String getTypeName(long typeID) {
            if (types == null) visitEventTypes();
            return types.get(typeID);
        }
        
        
        private Boolean threadMode;
        
        private String getThreadName(JFREvent event) throws JFRPropertyNotAvailableException {
            if (Boolean.TRUE.equals(threadMode)) {                  // v1+
                return event.getThread("eventThread").getName();                // NOI18N
            } else if (Boolean.FALSE.equals(threadMode)) {          // v0
                return "-";                                                     // NOI18N
            } else {                                                // not initialized yet
                try {
                    String eventName = event.getThread("eventThread").getName();// NOI18N
                    threadMode = Boolean.TRUE;
                    return eventName;
                } catch (JFRPropertyNotAvailableException e) {
                    threadMode = Boolean.FALSE;
                    return "-";                                                 // NOI18N
                }
            }
        }
        
        
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
