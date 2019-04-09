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
package org.graalvm.visualvm.jfr.views.socketio;

import java.text.NumberFormat;
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
    
    private static final SocketIONode[] NO_NODES = new SocketIONode[0];
    
    private final SocketIONode parent;
    private final List<SocketIONode> children;
    
    final String name;
    final Icon icon;
    
    long countR, countW = 0;
    long bytesR, bytesW = 0;
    Duration durationR, durationRMax, durationW, durationWMax;
    
    
    SocketIONode(String name, Icon icon, SocketIONode parent, List<SocketIONode> children) {
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
    
    
    SocketIONode getChild(String name) {
        if (children != null)
            for (SocketIONode child : children)
                if (Objects.equals(name, child.name))
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
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SocketIONode)) return false;
        return Objects.equals(name, ((SocketIONode)o).name);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    
    static final class Address extends SocketIONode {
        
        private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/host.png";  // NOI18N
        private static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true));
        
        Address(String name, SocketIONode parent, boolean terminal) {
            super(name, ICON, parent, terminal ? null : new ArrayList());
        }
        
    }
    
    static final class Port extends SocketIONode {
        
        private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/port.png";  // NOI18N
        private static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true));
        
        Port(String name, SocketIONode parent, boolean terminal) {
            super(name, ICON, parent, terminal ? null : new ArrayList());
        }
        
    }
    
    
    static final class Thread extends SocketIONode {
        
        Thread(String name, SocketIONode parent, boolean terminal) {
            super(name, Icons.getIcon(ProfilerIcons.THREAD), parent, terminal ? null : new ArrayList());
        }
        
    }
    
    
    static final class Root extends SocketIONode implements JFREventVisitor {
        
        private final SocketIOViewSupport.Aggregation primary;
        private final SocketIOViewSupport.Aggregation secondary;
    
        
        Root() {
            this(null, null);
        }
        
        Root(SocketIOViewSupport.Aggregation primary, SocketIOViewSupport.Aggregation secondary) {
            super(null, null, null, primary == null && secondary == null ? null : new ArrayList());
            
            this.primary = primary;
            this.secondary = SocketIOViewSupport.Aggregation.NONE.equals(secondary) ? null : secondary;
        }
        

        @Override
        public boolean visit(String typeName, JFREvent event) {
            Boolean rw;
            if ("jdk.SocketRead".equals(typeName)) rw = Boolean.FALSE; // NOI18N
            else if ("jdk.SocketWrite".equals(typeName)) rw = Boolean.TRUE; // NOI18N
            else rw = null;
            
            if (rw != null) {
                String primaryName = getName(primary, event);
                if (primaryName == null) primaryName = "<unknown>";
                
                SocketIONode primaryNode = getChild(primaryName);
                if (primaryNode == null) {
                    primaryNode = createNode(primaryName, primary, this, secondary == null);
                    addChild(primaryNode);
                }
                
                if (secondary != null) {
                    String secondaryName = getName(secondary, event);
                    if (secondaryName == null) secondaryName = "<unknown>";

                    
                    SocketIONode secondaryNode = primaryNode.getChild(secondaryName);
                    if (secondaryNode == null) {
                        secondaryNode = createNode(secondaryName, secondary, primaryNode, true);
                        primaryNode.addChild(secondaryNode);
                    }
                    
                    processEvent(secondaryNode, event, rw);
                } else {
                    processEvent(primaryNode, event, rw);
                }
            }
            
            return false;
        }
        
        
        private static void processEvent(SocketIONode node, JFREvent event, Boolean rw) {
            try {
                if (Boolean.FALSE.equals(rw)) node.processRead(event.getDuration("eventDuration"), event.getLong("bytesRead")); // NOI18N
                else node.processWrite(event.getDuration("eventDuration"), event.getLong("bytesWritten")); // NOI18N
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
        
        
        private static String getName(SocketIOViewSupport.Aggregation aggregation, JFREvent event) {
            try {
                if (SocketIOViewSupport.Aggregation.ADDRESS.equals(aggregation)) {
                    String address = event.getString("address"); // NOI18N
                    String host = event.getString("host"); // NOI18N
                    return host == null || host.trim().isEmpty() ? address : address + " (" + host + ")"; // NOI18N
                }
                if (SocketIOViewSupport.Aggregation.PORT.equals(aggregation)) return getPort(event);
                if (SocketIOViewSupport.Aggregation.ADDRESS_PORT.equals(aggregation)) {
                    String address = event.getString("address"); // NOI18N
                    String host = event.getString("host"); // NOI18N
                    if (host != null && !host.trim().isEmpty()) address = address + " (" + host + ")"; // NOI18N
                    return address + " : " + getPort(event); // NOI18N
                }
                if (SocketIOViewSupport.Aggregation.THREAD.equals(aggregation)) return event.getThread("eventThread").getName();
            } catch (JFRPropertyNotAvailableException e) {}
            return null;
        }
        
        private static String getPort(JFREvent event) throws JFRPropertyNotAvailableException {
            return NumberFormat.getIntegerInstance().format(event.getInt("port")); // NOI18N
        }
        
        private SocketIONode createNode(String name, SocketIOViewSupport.Aggregation aggregation, SocketIONode parent, boolean terminal) {
            if (SocketIOViewSupport.Aggregation.ADDRESS.equals(aggregation)) return new SocketIONode.Address(name, parent, terminal);
            if (SocketIOViewSupport.Aggregation.PORT.equals(aggregation)) return new SocketIONode.Port(name, parent, terminal);
            if (SocketIOViewSupport.Aggregation.ADDRESS_PORT.equals(aggregation)) return new SocketIONode.Address(name, parent, terminal);
            if (SocketIOViewSupport.Aggregation.THREAD.equals(aggregation)) return new SocketIONode.Thread(name, parent, terminal);
            return null;
        }
        
    }
    
}
