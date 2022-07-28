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
package org.graalvm.visualvm.jfr.views.gc;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.Icon;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRPropertyNotAvailableException;
import org.graalvm.visualvm.lib.jfluid.results.CCTNode;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class GcNode extends CCTNode {
    
    private static final GcNode[] NO_NODES = new GcNode[0];
    
    private final GcNode parent;
    private final List<GcNode> children;
    
    final String name;
    final Icon icon;
    
    String cause;
    long gcid = -1;
    long count = 0;
    
    Duration sumOfPauses;
    Duration longestPause;
    
    
    GcNode(String name, Icon icon, GcNode parent, List<GcNode> children) {
        this.parent = parent;
        this.children = children;
        
        this.name = name;
        this.icon = icon;
    }
    
    
    final void processData(Duration _sumOfPauses, Duration _longestPause, JFREvent event) throws JFRPropertyNotAvailableException {
        if (parent != null) {
            processDataImpl(event);
            
            if (sumOfPauses == null) sumOfPauses = _sumOfPauses; else sumOfPauses = sumOfPauses.plus(_sumOfPauses);
            if (longestPause == null || longestPause.compareTo(_longestPause) < 0) longestPause = _longestPause;
            
            parent.processData(_sumOfPauses, _longestPause, event);
        }
    }
    
    protected void processDataImpl(JFREvent event) throws JFRPropertyNotAvailableException {}
    
    
    GcNode getChild(String name) {
        if (children != null)
            for (GcNode child : children)
                if (Objects.equals(name, child.name))
                    return child;
        return null;
    }
    
    
    @Override
    public GcNode getChild(int index) {
        return children == null ? null : children.get(index);
    }

    @Override
    public GcNode[] getChildren() {
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
    public GcNode getParent() {
        return parent;
    }

    
    protected void addChild(GcNode child) {
        if (children != null) children.add(child);
    }
    
    protected void removeAllChildren() {
        if (children != null) children.clear();
    }
    
    
    @Override
    public int hashCode() {
        return Objects.hash(name, gcid);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof GcNode)) return false;
        if (gcid == -1 && ((GcNode)o).gcid == -1) return Objects.equals(name, ((GcNode)o).name);
        else return gcid == ((GcNode)o).gcid && Objects.equals(name, ((GcNode)o).name);
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    
    static final class Name extends GcNode {
        
        Name(String name, GcNode parent, boolean terminal) {
            super(name, null, parent, terminal ? null : new ArrayList<>());
        }
        
        protected void processDataImpl(JFREvent event) throws JFRPropertyNotAvailableException {
            count++;
        }
        
    }
    
    static final class Cause extends GcNode {
        
        Cause(String cause, GcNode parent, boolean terminal) {
            super(cause, null, parent, terminal ? null : new ArrayList<>());
        }
        
        protected void processDataImpl(JFREvent event) throws JFRPropertyNotAvailableException {
            count++;
        }
        
    }
    
    
    static final class Event extends GcNode {
        
        Event(String name, GcNode parent, boolean terminal) {
            super(name, Icons.getIcon(ProfilerIcons.RUN_GC), parent, terminal ? null : new ArrayList<>());
        }
        
        protected void processDataImpl(JFREvent event) throws JFRPropertyNotAvailableException {
            cause = event.getString("cause"); // NOI18N
            gcid = event.getLong("gcId"); // NOI18N
        }
        
    }
    
    static final class Phase extends GcNode {
        
        private final int idx;
        
        Phase(String name, Duration duration, int idx, GcNode parent) {
            super(name, null, parent, null);
            sumOfPauses = duration;
            this.idx = idx;
        }
        
        @Override
        public int hashCode() {
            return idx;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof GcNode.Phase)) return false;
            return idx == ((GcNode.Phase)o).idx;
        }
        
    }
    
    
    static final class Label extends GcNode {
        
        Label(String label, GcNode parent) {
            super(label, null, parent, null);
        }
        
        static Label createNoData(GcNode parent) {
            return new Label("<no data>", parent);
        }
        
    }
    
    
    static final class Root extends GcNode implements JFREventVisitor {
        
        private final GcViewSupport.Aggregation primary;
        private final GcViewSupport.Aggregation secondary;
    
        
        Root() {
            this(null, null);
        }
        
        Root(GcViewSupport.Aggregation primary, GcViewSupport.Aggregation secondary) {
            super(null, null, null, primary == null && secondary == null ? null : new ArrayList<>());
            
            this.primary = primary;
            this.secondary = secondary;
        }
        
        
        private static class PhaseRecord {
            final Instant time;
            final String name;
            final Duration duration;
            
            PhaseRecord(Instant time, String name, Duration duration) {
                this.time = time;
                this.name = name;
                this.duration = duration;
            }
            
            static String phaseName(String typeName) {
                if (JFRSnapshotGcViewProvider.EVENT_GCPHASE_CONCURRENT.equals(typeName)) return "[concurrent]";
                if (JFRSnapshotGcViewProvider.EVENT_GCPHASE_PARALLEL.equals(typeName)) return "[parallel]";
                if (JFRSnapshotGcViewProvider.EVENT_GCPHASE_PAUSE.equals(typeName)) return "[pause]";
                if (JFRSnapshotGcViewProvider.EVENT_GCPHASE_PAUSE_LVL1.equals(typeName)) return "[pause level 1]";
                if (JFRSnapshotGcViewProvider.EVENT_GCPHASE_PAUSE_LVL2.equals(typeName)) return "[pause level 2]";
                if (JFRSnapshotGcViewProvider.EVENT_GCPHASE_PAUSE_LVL3.equals(typeName)) return "[pause level 3]";
                if (JFRSnapshotGcViewProvider.EVENT_GCPHASE_PAUSE_LVL4.equals(typeName)) return "[pause level 4]";
                return "[unknown phase]";
            }
        }
        
        
        private List<GcNode.Event> events;
        private Map<Long, List<PhaseRecord>> records;
        
        @Override
        public void init() {
            if (GcViewSupport.Aggregation.PHASE.equals(secondary)) {
                events = new ArrayList<>();
                records = new HashMap<>();
            }
        }

        @Override
        public boolean visit(String typeName, JFREvent event) {
            if (JFRSnapshotGcViewProvider.EVENT_GARBAGE_COLLECTION.equals(typeName)) {
                String primaryName = getName(primary, event);
                if (primaryName == null) primaryName = "<unknown>";
                
                if (GcViewSupport.Aggregation.NONE.equals(primary)) {
                    GcNode.Event node = new GcNode.Event(primaryName, this, events == null);
                    addChild(node);
                    if (events != null) events.add(node);
                    try {
                        node.processData(event.getDuration("sumOfPauses"), event.getDuration("longestPause"), event); // NOI18N
                    } catch (JFRPropertyNotAvailableException e) {}
                } else {
                    GcNode primaryNode = getChild(primaryName);
                    if (primaryNode == null) {
                        primaryNode = createNode(primaryName, primary, this, false);
                        addChild(primaryNode);
                    }

//                    if (secondary != null) {
                        String secondaryName = getName(GcViewSupport.Aggregation.NONE, event);
                        if (secondaryName == null) secondaryName = "<unknown>";


//                        GcNode secondaryNode = primaryNode.getChild(secondaryName);
//                        if (secondaryNode == null) {
//                            secondaryNode = createNode(secondaryName, GcViewSupport.Aggregation.NONE, primaryNode, true);
                            GcNode.Event secondaryNode = new GcNode.Event(secondaryName, primaryNode, events == null);
                            primaryNode.addChild(secondaryNode);
                            if (events != null) events.add(secondaryNode);
//                        }

                        try {
                            secondaryNode.processData(event.getDuration("sumOfPauses"), event.getDuration("longestPause"), event); // NOI18N
                        } catch (JFRPropertyNotAvailableException e) {}
//                    } else {
//                        try {
//                            primaryNode.processData(event.getDuration("sumOfPauses"), event.getDuration("longestPause"), event); // NOI18N
//                        } catch (JFRPropertyNotAvailableException e) {}
//                    }
                }
            } else if (records != null && typeName != null && typeName.startsWith(JFRSnapshotGcViewProvider.PREFIX_GCPHASE)) {
                try {
                    long gcId = event.getLong("gcId"); // NOI18N
                    List<PhaseRecord> prlist = records.get(gcId);
                    if (prlist == null) {
                        prlist = new ArrayList<>();
                        records.put(gcId, prlist);
                    }
                    
                    Instant ptime = event.getInstant("eventTime"); // NOI18N
                    String pname = PhaseRecord.phaseName(typeName) + " - " + event.getString("name"); // NOI18N
                    Duration pduration = event.getDuration("eventDuration"); // NOI18N
                    prlist.add(new PhaseRecord(ptime, pname, pduration));
                } catch (JFRPropertyNotAvailableException e) {}
            }
            
            return false;
        }
        
        @Override
        public void done() {
            if (records != null) {
                for (GcNode.Event event : events) {
                    List<PhaseRecord> precords = records.get(event.gcid);
                    if (precords != null) {
                        precords.sort(new Comparator<PhaseRecord>() {
                            @Override public int compare(PhaseRecord pr1, PhaseRecord pr2) { return pr1.time.compareTo(pr2.time); }
                        });
                        
                        int idx = 0;
                        for (PhaseRecord precord : precords)
                            event.addChild(new GcNode.Phase(precord.name, precord.duration, idx++, event));
                    }
                }
                
                events.clear();
                events = null;
                
                records.clear();
                records = null;
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
        
        
        private static String getName(GcViewSupport.Aggregation aggregation, JFREvent event) {
            try {
                if (GcViewSupport.Aggregation.NONE.equals(aggregation)) return event.getString("name") + " - " + event.getString("cause"); // NOI18N
                if (GcViewSupport.Aggregation.NAME.equals(aggregation)) return event.getString("name"); // NOI18N
                if (GcViewSupport.Aggregation.CAUSE.equals(aggregation)) return event.getString("cause"); // NOI18N
            } catch (JFRPropertyNotAvailableException e) {}
            return null;
        }
        
        private GcNode createNode(String name, GcViewSupport.Aggregation aggregation, GcNode parent, boolean terminal) {
            if (GcViewSupport.Aggregation.NONE.equals(aggregation)) return new GcNode.Event(name, parent, terminal);
            if (GcViewSupport.Aggregation.NAME.equals(aggregation)) return new GcNode.Name(name, parent, terminal);
            if (GcViewSupport.Aggregation.CAUSE.equals(aggregation)) return new GcNode.Cause(name, parent, terminal);
            return null;
        }
        
    }
    
}
