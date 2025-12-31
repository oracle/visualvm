/*
 * Copyright (c) 2019, 2025, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.locks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class LocksNode extends CCTNode {
    
    private static final LocksNode[] NO_NODES = new LocksNode[0];
    
    private final LocksNode parent;
    private final List<LocksNode> children;
    
    final String name;
    final Icon icon;
    
    long count = 0;
    Duration duration, durationMax;
    
    
    LocksNode(String name, Icon icon, LocksNode parent, List<LocksNode> children) {
        this.parent = parent;
        this.children = children;
        
        this.name = name;
        this.icon = icon;
    }
    
    
    final void processData(Duration duration) {
        if (parent != null) {
            count++;
            if (this.duration == null) this.duration = duration; else this.duration = this.duration.plus(duration);
            if (durationMax == null || durationMax.compareTo(duration) < 0) durationMax = duration;
            
            parent.processData(duration);
        }
    }
    
    
    LocksNode getChild(String name) {
        if (children != null)
            for (LocksNode child : children)
                if (Objects.equals(name, child.name))
                    return child;
        return null;
    }
    
    
    @Override
    public LocksNode getChild(int index) {
        return children == null ? null : children.get(index);
    }

    @Override
    public LocksNode[] getChildren() {
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
        return children == null || children.isEmpty();
    }

    @Override
    public LocksNode getParent() {
        return parent;
    }

    
    protected void addChild(LocksNode child) {
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
        if (!(o instanceof LocksNode)) return false;
        return Objects.equals(name, ((LocksNode)o).name);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    
    static final class LockClass extends LocksNode {
        
        LockClass(String name, LocksNode parent, boolean terminal) {
            super(name, Icons.getIcon(LanguageIcons.CLASS), parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    static final class LockObject extends LocksNode {
        
        private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/lock.png";  // NOI18N
        private static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true));
        
        LockObject(String name, LocksNode parent, boolean terminal) {
            super(name, ICON, parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    
    static final class Thread extends LocksNode {
        
        final boolean blocking;
        
        Thread(String name, boolean blocking, LocksNode parent, boolean terminal) {
            super(name, "<timed out>".equals(name) ? null : Icons.getIcon(ProfilerIcons.THREAD), parent, terminal ? null : new ArrayList<>());
            this.blocking = blocking;
        }
        
    }
    
    
    static final class Label extends LocksNode {
        
        Label(String label, LocksNode parent) {
            super(label, null, parent, null);
        }
        
        static Label createNoData(LocksNode parent) {
            return new Label("<no data>", parent);
        }
        
    }
    
    
    static final class Root extends LocksNode implements JFREventVisitor {
        
        private final int mode;
        private final LocksViewSupport.Aggregation primary;
        private final LocksViewSupport.Aggregation secondary;
    
        
        Root() {
            this(0, null, null);
        }
        
        Root(int mode, LocksViewSupport.Aggregation primary, LocksViewSupport.Aggregation secondary) {
            super(null, null, null, primary == null && secondary == null ? null : new ArrayList<>());
            
            this.mode = mode;
            this.primary = primary;
            this.secondary = LocksViewSupport.Aggregation.NONE.equals(secondary) ? null : secondary;
        }
        

        @Override
        public boolean visit(String typeName, JFREvent event) {
            Boolean rw;
            if (mode != 2 && JFRSnapshotLocksViewProvider.EVENT_MONITOR_ENTER.equals(typeName)) rw = Boolean.FALSE; // NOI18N
            else if (mode != 1 && JFRSnapshotLocksViewProvider.EVENT_MONITOR_WAIT.equals(typeName)) rw = Boolean.TRUE; // NOI18N
            else rw = null;
            
            if (rw != null) {
                String primaryName = getName(primary, event, rw);
                if (primaryName == null) primaryName = "<unknown>";
                
                LocksNode primaryNode = getChild(primaryName);
                if (primaryNode == null) {
                    primaryNode = createNode(primaryName, primary, this, secondary == null);
                    addChild(primaryNode);
                }
                
                if (secondary != null) {
                    String secondaryName = getName(secondary, event, rw);
                    if (secondaryName == null) secondaryName = "<unknown>";
                    
                    LocksNode secondaryNode = primaryNode.getChild(secondaryName);
                    if (secondaryNode == null) {
                        secondaryNode = createNode(secondaryName, secondary, primaryNode, true);
                        primaryNode.addChild(secondaryNode);
                    }
                    
                    try {
                        secondaryNode.processData(event.getDuration("eventDuration"));
                    } catch (JFRPropertyNotAvailableException e) {}
                } else {
                    try {
                        primaryNode.processData(event.getDuration("eventDuration"));
                    } catch (JFRPropertyNotAvailableException e) {}
                }
            }
            
            return false;
        }
        
        
        @Override
        public int hashCode() {
            return 37;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Root;
        }
        
        
        private static String getName(LocksViewSupport.Aggregation aggregation, JFREvent event, boolean eventMode) {
            try {
                if (LocksViewSupport.Aggregation.CLASS.equals(aggregation)) return decodeClassName(event.getClass("monitorClass").getName()); // NOI18N
                if (LocksViewSupport.Aggregation.OBJECT.equals(aggregation)) return decodeClassName(event.getClass("monitorClass").getName()) + "(0x" + Long.toHexString(event.getLong("address")) + ")"; // NOI18N
                if (LocksViewSupport.Aggregation.THREAD_BLOCKED.equals(aggregation)) return event.getThread("eventThread").getName();
                if (LocksViewSupport.Aggregation.THREAD_BLOCKING.equals(aggregation)) return getThreadName(event, eventMode);
            } catch (JFRPropertyNotAvailableException e) {}
            return null;
        }
        
        private static String getThreadName(JFREvent event, boolean eventMode) throws JFRPropertyNotAvailableException {
            JFRThread thread = eventMode ? event.getThread("notifier") : event.getThread("previousOwner"); // NOI18N
            return thread == null ? (eventMode ? "<timed out>" : "<unknown thread>") : thread.getName(); // NOI18N
        }
        
        private static LocksNode createNode(String name, LocksViewSupport.Aggregation aggregation, LocksNode parent, boolean terminal) {
            if (LocksViewSupport.Aggregation.CLASS.equals(aggregation)) return new LocksNode.LockClass(name, parent, terminal);
            if (LocksViewSupport.Aggregation.OBJECT.equals(aggregation)) return new LocksNode.LockObject(name, parent, terminal);
            if (LocksViewSupport.Aggregation.THREAD_BLOCKED.equals(aggregation)) return new LocksNode.Thread(name, false, parent, terminal);
            if (LocksViewSupport.Aggregation.THREAD_BLOCKING.equals(aggregation)) return new LocksNode.Thread(name, true, parent, terminal);
            return null;
        }
        
        
        private static String decodeClassName(String className) {
            className = StringUtils.userFormClassName(className);
            
            if (className.startsWith("L") && className.contains(";")) // NOI18N
                className = className.substring(1).replace(";", ""); // NOI18N
            
            return className;
        }
        
    }
    
}
