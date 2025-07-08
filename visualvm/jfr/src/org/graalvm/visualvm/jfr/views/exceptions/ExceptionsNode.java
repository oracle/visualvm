/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.exceptions;

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
import org.graalvm.visualvm.lib.jfluid.utils.StringUtils;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.LanguageIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class ExceptionsNode extends CCTNode {
    
    private static final ExceptionsNode[] NO_NODES = new ExceptionsNode[0];
    
    private final ExceptionsNode parent;
    private final List<ExceptionsNode> children;
    
    final String name;
    final Icon icon;
    
    long count = 0;
    Duration duration, durationMax;
    
    
    ExceptionsNode(String name, Icon icon, ExceptionsNode parent, List<ExceptionsNode> children) {
        this.parent = parent;
        this.children = children;
        
        this.name = name;
        this.icon = icon;
    }
    
    
    final void processData(Duration duration) {
        if (parent != null) {
            count++;
            if (duration != null) { // .jfr v0 doesn't track Duration
                if (this.duration == null) this.duration = duration; else this.duration = this.duration.plus(duration);
                if (durationMax == null || durationMax.compareTo(duration) < 0) durationMax = duration;
            }
            
            parent.processData(duration);
        } else {
            if (duration != null && this instanceof Root) ((Root)this).tracksDuration = true;
        }
    }
    
    
    ExceptionsNode getChild(String name) {
        if (children != null)
            for (ExceptionsNode child : children)
                if (Objects.equals(name, child.name))
                    return child;
        return null;
    }
    
    
    @Override
    public ExceptionsNode getChild(int index) {
        return children == null ? null : children.get(index);
    }

    @Override
    public ExceptionsNode[] getChildren() {
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
    public ExceptionsNode getParent() {
        return parent;
    }

    
    protected void addChild(ExceptionsNode child) {
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
        if (!(o instanceof ExceptionsNode)) return false;
        return Objects.equals(name, ((ExceptionsNode)o).name);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    
    static final class Class extends ExceptionsNode {
        
        Class(String name, ExceptionsNode parent, boolean terminal) {
            super(name, Icons.getIcon(LanguageIcons.CLASS), parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    static final class Error extends ExceptionsNode {
        
        private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/error.png";  // NOI18N
        private static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true));
        
        Error(String name, ExceptionsNode parent, boolean terminal) {
            super(name, ICON, parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    static final class Exception extends ExceptionsNode {
        
        private static final String IMAGE_PATH = "org/graalvm/visualvm/jfr/resources/exception.png";  // NOI18N
        private static final Icon ICON = new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true));
        
        Exception(String name, ExceptionsNode parent, boolean terminal) {
            super(name, ICON, parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    
    static final class Thread extends ExceptionsNode {
        
        Thread(String name, ExceptionsNode parent, boolean terminal) {
            super(name, Icons.getIcon(ProfilerIcons.THREAD), parent, terminal ? null : new ArrayList<>());
        }
        
    }
    
    
    static final class Label extends ExceptionsNode {
        
        Label(String label, ExceptionsNode parent) {
            super(label, null, parent, null);
        }
        
        static Label createNoData(ExceptionsNode parent) {
            return new Label("<no data>", parent);
        }
        
    }
    
    
    static final class Root extends ExceptionsNode implements JFREventVisitor {
        
        private final int mode;
        private final ExceptionsViewSupport.Aggregation primary;
        private final ExceptionsViewSupport.Aggregation secondary;
        
        boolean tracksDuration = false;
            
        
        Root() {
            this(0, null, null);
        }
        
        Root(int mode, ExceptionsViewSupport.Aggregation primary, ExceptionsViewSupport.Aggregation secondary) {
            super(null, null, null, primary == null && secondary == null ? null : new ArrayList<>());
            
            this.mode = mode;
            this.primary = primary;
            this.secondary = ExceptionsViewSupport.Aggregation.NONE.equals(secondary) ? null : secondary;
        }
        

        @Override
        public boolean visit(String typeName, JFREvent event) {
            Boolean rw;
            if (mode != 2 && JFRSnapshotExceptionsViewProvider.EVENT_JAVA_ERROR.equals(typeName)) rw = Boolean.FALSE; // NOI18N
            else if (mode != 1 && JFRSnapshotExceptionsViewProvider.EVENT_JAVA_EXCEPTION.equals(typeName)) rw = Boolean.TRUE; // NOI18N
            else rw = null;
            
            if (rw != null) {
                String primaryName = getName(primary, event);
                if (primaryName == null) primaryName = "<unknown>";

                ExceptionsNode primaryNode = getChild(primaryName);
                if (primaryNode == null) {
                    primaryNode = createNode(primaryName, primary, this, secondary == null);
                    addChild(primaryNode);
                }

                if (secondary != null) {
                    String secondaryName = getName(secondary, event);
                    if (secondaryName == null) secondaryName = "<unknown>";


                    ExceptionsNode secondaryNode = primaryNode.getChild(secondaryName);
                    if (secondaryNode == null) {
                        secondaryNode = createNode(secondaryName, secondary, primaryNode, true);
                        primaryNode.addChild(secondaryNode);
                    }

                    Duration eventDuration;
                    try { eventDuration = getDuration(event); }
                    catch (JFRPropertyNotAvailableException e) { eventDuration = null; } // .jfr v0
                    secondaryNode.processData(eventDuration);
                } else {
                    Duration eventDuration;
                    try { eventDuration = getDuration(event); }
                    catch (JFRPropertyNotAvailableException e) { eventDuration = null; } // .jfr v0
                    primaryNode.processData(eventDuration);
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
        
        
        private Boolean durationMode;
        
        private Duration getDuration(JFREvent event) throws JFRPropertyNotAvailableException {
            if (Boolean.TRUE.equals(durationMode)) {                // v1+
                return event.getDuration("eventDuration");                      // NOI18N
            } else if (Boolean.FALSE.equals(durationMode)) {        // v0
                return null;                                                    // NOI18N
            } else {                                                // not initialized yet
                try {
                    Duration eventDuration = event.getDuration("eventDuration");// NOI18N
                    durationMode = Boolean.TRUE;
                    return eventDuration;
                } catch (JFRPropertyNotAvailableException e) {
                    durationMode = Boolean.FALSE;
                    return null;                                                // NOI18N
                }
            }
        }
        
        
        private static String getName(ExceptionsViewSupport.Aggregation aggregation, JFREvent event) {
            try {
                if (ExceptionsViewSupport.Aggregation.CLASS.equals(aggregation)) return decodeClassName(event.getClass("thrownClass").getName()); // NOI18N
                if (ExceptionsViewSupport.Aggregation.MESSAGE.equals(aggregation)) return decodeMessage(event.getString("message")); // NOI18N
                if (ExceptionsViewSupport.Aggregation.CLASS_MESSAGE.equals(aggregation)) return decodeClassName(event.getClass("thrownClass").getName()) + " : " + decodeMessage(event.getString("message")); // NOI18N
                if (ExceptionsViewSupport.Aggregation.THREAD.equals(aggregation)) return event.getThread("eventThread").getName(); // NOI18N
            } catch (JFRPropertyNotAvailableException e) {}
            return null;
        }
        
        private ExceptionsNode createNode(String name, ExceptionsViewSupport.Aggregation aggregation, ExceptionsNode parent, boolean terminal) {
            if (ExceptionsViewSupport.Aggregation.CLASS.equals(aggregation)) return new ExceptionsNode.Class(name, parent, terminal);
            if (ExceptionsViewSupport.Aggregation.MESSAGE.equals(aggregation)) return new ExceptionsNode.Exception(name, parent, terminal);
            if (ExceptionsViewSupport.Aggregation.CLASS_MESSAGE.equals(aggregation)) return new ExceptionsNode.Class(name, parent, terminal);
            if (ExceptionsViewSupport.Aggregation.THREAD.equals(aggregation)) return new ExceptionsNode.Thread(name, parent, terminal);
            return null;
        }
        
        
        private static String decodeClassName(String className) {
            className = StringUtils.userFormClassName(className);
            
            if (className.startsWith("L") && className.contains(";")) // NOI18N
                className = className.substring(1).replace(";", ""); // NOI18N
            
            return className;
        }
        
        private static String decodeMessage(String message) {
            return message == null || message.isEmpty() ? "<no message>" : message;
        }
        
    }
    
}
