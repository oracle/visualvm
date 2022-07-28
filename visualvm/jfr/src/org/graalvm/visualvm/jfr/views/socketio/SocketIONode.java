/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.socketio;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class SocketIONode extends CCTNode {
    
    private static final String UNKNOWN = "<unknown>";
    private static final SocketIONode[] NO_NODES = new SocketIONode[0];
    
    private final SocketIONode parent;
    private final List<SocketIONode> children;
    
    final String key;
    String name;
    final Icon icon;
    
    long countR, countW = 0;
    long bytesR, bytesW = 0;
    Duration durationR, durationRMax, durationW, durationWMax;
    
    
    SocketIONode(String key, String name, Icon icon, SocketIONode parent, List<SocketIONode> children) {
        this.key = key;
        this.parent = parent;
        this.children = children;
        
        this.name = name;
        this.icon = icon;
    }
    
    
    final void processRead(Duration duration, long bytes) {
        if (parent != null) {
            countR++;
            bytesR += bytes;
            if (durationR == null) durationR = duration; else durationR = durationR.plus(duration);
            if (durationRMax == null || durationRMax.compareTo(duration) < 0) durationRMax = duration;
            
            parent.processRead(duration, bytes);
        }
    }
    
    final void processWrite(Duration duration, long bytes) {
        if (parent != null) {
            countW++;
            bytesW += bytes;
            if (durationW == null) durationW = duration; else durationW = durationW.plus(duration);
            if (durationWMax == null || durationWMax.compareTo(duration) < 0) durationWMax = duration;
            
            parent.processWrite(duration, bytes);
        }
    }
    
    
    SocketIONode getChild(String key) {
        if (children != null)
            for (SocketIONode child : children)
                if (Objects.equals(key, child.key))
                    return child;
        return null;
    }
    
    
    @Override
    public SocketIONode getChild(int index) {
        return children == null ? null : children.get(index);
    }

    @Override
    public SocketIONode[] getChildren() {
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
    public SocketIONode getParent() {
        return parent;
    }

    
    protected void addChild(SocketIONode child) {
        if (children != null) children.add(child);
    }
    
    protected void removeAllChildren() {
        if (children != null) children.clear();
    }
    
    
    @Override
    public int hashCode() {
        return key.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SocketIONode)) return false;
        return Objects.equals(key, ((SocketIONode)o).key);
    }
    
    @Override
    public String toString() {
        return name;
    }

    private void setName(String newName) {
        if (name == null || newName.length() > name.length()) {
            name = newName;
        }
    }
    
    
    static final class Address extends SocketIONode {
        
        private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/host.png";  // NOI18N
        private static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true));
        
        Address(String key, String name, SocketIONode parent, boolean terminal) {
            super(key, name, ICON, parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    static final class Port extends SocketIONode {
        
        private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/port.png";  // NOI18N
        private static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true));
        
        Port(String name, SocketIONode parent, boolean terminal) {
            super(name, name, ICON, parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    
    static final class Thread extends SocketIONode {
        
        Thread(String name, SocketIONode parent, boolean terminal) {
            super(name, name, Icons.getIcon(ProfilerIcons.THREAD), parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    
    static final class Root extends SocketIONode implements JFREventVisitor {
        
        private final SocketIOViewSupport.Aggregation primary;
        private final SocketIOViewSupport.Aggregation secondary;
    
        
        Root() {
            this(null, null);
        }
        
        Root(SocketIOViewSupport.Aggregation primary, SocketIOViewSupport.Aggregation secondary) {
            super(null, null, null, null, primary == null && secondary == null ? null : new ArrayList<>());
            
            this.primary = primary;
            this.secondary = SocketIOViewSupport.Aggregation.NONE.equals(secondary) ? null : secondary;
        }
        

        @Override
        public boolean visit(String typeName, JFREvent event) {
            Boolean rw;
            if (JFRSnapshotSocketIOViewProvider.EVENT_SOCKET_READ.equals(typeName)) rw = Boolean.FALSE; // NOI18N
            else if (JFRSnapshotSocketIOViewProvider.EVENT_SOCKET_WRITE.equals(typeName)) rw = Boolean.TRUE; // NOI18N
            else rw = null;
            
            if (rw != null) {
                String primaryKey = getKey(primary, event);
                String primaryName = getName(primary, event);
                if (primaryKey == null) primaryKey = UNKNOWN;
                if (primaryName == null) primaryName = UNKNOWN;
                
                SocketIONode primaryNode = getChild(primaryKey);
                if (primaryNode == null) {
                    primaryNode = createNode(primaryKey, primaryName, primary, this, secondary == null);
                    addChild(primaryNode);
                }
                
                if (secondary != null) {
                    String secondaryKey = getKey(secondary, event);
                    String secondaryName = getName(secondary, event);
                    if (secondaryKey == null) secondaryKey = UNKNOWN;
                    if (secondaryName == null) secondaryName = UNKNOWN;
                    
                    SocketIONode secondaryNode = primaryNode.getChild(secondaryKey);
                    if (secondaryNode == null) {
                        secondaryNode = createNode(secondaryKey, secondaryName, secondary, primaryNode, true);
                        primaryNode.addChild(secondaryNode);
                    }
                    
                    processEvent(secondaryNode, secondaryName, event, rw);
                } else {
                    processEvent(primaryNode, primaryName, event, rw);
                }
            }
            
            return false;
        }
        
        private static void processEvent(SocketIONode node, String name, JFREvent event, Boolean rw) {
            try {
                node.setName(name);
                if (Boolean.FALSE.equals(rw)) node.processRead(getDuration(event), event.getLong("bytesRead")); // NOI18N
                else node.processWrite(getDuration(event), event.getLong("bytesWritten")); // NOI18N
            } catch (JFRPropertyNotAvailableException e) {
                System.err.println(">>> " + e);
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
        
        
        private static String getKey(SocketIOViewSupport.Aggregation aggregation, JFREvent event) {
            try {
                if (SocketIOViewSupport.Aggregation.ADDRESS.equals(aggregation)) {
                    return getAddress(event); // NOI18N
                }
                if (SocketIOViewSupport.Aggregation.PORT.equals(aggregation)) return getPort(event);
                if (SocketIOViewSupport.Aggregation.ADDRESS_PORT.equals(aggregation)) {
                    String address = getAddress(event); // NOI18N
                    return address + " : " + getPort(event); // NOI18N
                }
                if (SocketIOViewSupport.Aggregation.THREAD.equals(aggregation)) return event.getThread("eventThread").getName();
            } catch (JFRPropertyNotAvailableException e) {}
            return null;
        }

        private static Duration getDuration(JFREvent event) throws JFRPropertyNotAvailableException {
            return event.getDuration("eventDuration");
        }
        
        private static String getAddress(JFREvent event) throws JFRPropertyNotAvailableException {
            return event.getString("address");
        }

        private static String getName(SocketIOViewSupport.Aggregation aggregation, JFREvent event) {
            try {
                if (SocketIOViewSupport.Aggregation.ADDRESS.equals(aggregation)) return getFullAddress(event);
                if (SocketIOViewSupport.Aggregation.PORT.equals(aggregation)) return getPort(event);
                if (SocketIOViewSupport.Aggregation.ADDRESS_PORT.equals(aggregation)) {
                    return getFullAddress(event) + " : " + getPort(event); // NOI18N
                }
                if (SocketIOViewSupport.Aggregation.THREAD.equals(aggregation)) return event.getThread("eventThread").getName();
            } catch (JFRPropertyNotAvailableException e) {}
            return null;
        }

        private static String getPort(JFREvent event) throws JFRPropertyNotAvailableException {
            return String.valueOf(event.getInt("port")); // NOI18N
        }

        private static String getFullAddress(JFREvent event) throws JFRPropertyNotAvailableException {
            String address = getAddress(event); // NOI18N
            String host = event.getString("host"); // NOI18N
            if (host != null && !host.trim().isEmpty() && !host.equals(address)) address = address + " (" + host + ")"; // NOI18N
            return address;
        }
        
        private SocketIONode createNode(String key, String name, SocketIOViewSupport.Aggregation aggregation, SocketIONode parent, boolean terminal) {
            if (SocketIOViewSupport.Aggregation.ADDRESS.equals(aggregation)) return new SocketIONode.Address(key, name, parent, terminal);
            if (SocketIOViewSupport.Aggregation.PORT.equals(aggregation)) return new SocketIONode.Port(name, parent, terminal);
            if (SocketIOViewSupport.Aggregation.ADDRESS_PORT.equals(aggregation)) return new SocketIONode.Address(key, name, parent, terminal);
            if (SocketIOViewSupport.Aggregation.THREAD.equals(aggregation)) return new SocketIONode.Thread(name, parent, terminal);
            return null;
        }
        
    }
    
}
