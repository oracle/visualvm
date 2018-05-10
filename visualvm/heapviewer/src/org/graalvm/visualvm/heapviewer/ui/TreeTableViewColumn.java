/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer.ui;

import java.util.Objects;
import javax.swing.SwingConstants;
import javax.swing.table.TableColumn;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.ui.swing.renderer.HideableBarRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.ProfilerRenderer;
import org.graalvm.visualvm.heapviewer.model.DataType;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TreeTableViewColumn_ColName=Name",
    "TreeTableViewColumn_ColLogicalValue=Logical Value",
    "TreeTableViewColumn_ColCount=Count",
    "TreeTableViewColumn_ColSize=Size",
    "TreeTableViewColumn_ColRetained=Retained",
    "TreeTableViewColumn_ColObjectId=Object ID"
})
public class TreeTableViewColumn extends TableColumn {
    
    private final int position;
    private final DataType dataType;

    private final boolean initiallyVisible;
    private final boolean initiallySorting;


    public TreeTableViewColumn(String name, int position, DataType dataType, boolean sortingColumn) {
        this(name, position, dataType, true, sortingColumn);
    }

    public TreeTableViewColumn(String name, int position, DataType dataType, boolean initiallyVisible, boolean initiallySorting) {
        this.position = position;
        this.dataType = dataType;

        this.initiallyVisible = initiallyVisible;
        this.initiallySorting = initiallySorting;

        setHeaderValue(name);
    }


    public int getPosition() { return position; }

    public DataType getDataType() { return dataType; }
    
    public boolean initiallyVisible() { return initiallyVisible; }
    
    public boolean initiallySorting() { return initiallySorting; }
    
    
    public int getPreferredWidth() { return -1; }
    
    public ProfilerRenderer getRenderer() { return null; }
    
    
    public static TreeTableViewColumn[] classes(Heap heap, boolean sort) {
        return new TreeTableViewColumn[] {
            new Name(heap),
            new LogicalValue(heap),
            new Count(heap),
            new OwnSize(heap, true, sort),
            new RetainedSize(heap),
            new ObjectID(heap)
        };
    }
    
    public static TreeTableViewColumn[] classesMinimal(Heap heap, boolean sort) {
        return new TreeTableViewColumn[] {
            new Name(heap),
            new LogicalValue(heap),
            new Count(heap, false, false),
            new OwnSize(heap, false, false),
            new RetainedSize(heap, true, sort),
            new ObjectID(heap)
        };
    }
    
    public static TreeTableViewColumn[] classesPlain(Heap heap) {
        return new TreeTableViewColumn[] {
            new Name(heap),
            new LogicalValue(heap),
            new Count(heap, false, false),
            new OwnSize(heap, false, false),
            new RetainedSize(heap, false, false),
            new ObjectID(heap)
        };
    }
    
    public static TreeTableViewColumn[] instances(Heap heap, boolean sort) {
        return new TreeTableViewColumn[] {
            new Name(heap),
            new LogicalValue(heap),
            new OwnSize(heap, true, sort),
            new RetainedSize(heap),
            new ObjectID(heap)
        };
    }
    
    public static TreeTableViewColumn[] instancesMinimal(Heap heap, boolean sort) {
        return new TreeTableViewColumn[] {
            new Name(heap),
            new LogicalValue(heap),
            new OwnSize(heap, false, false),
            new RetainedSize(heap, true, sort),
            new ObjectID(heap)
        };
    }
    
    public static TreeTableViewColumn[] instancesPlain(Heap heap) {
        return new TreeTableViewColumn[] {
            new Name(heap),
            new LogicalValue(heap),
            new OwnSize(heap, false, false),
            new RetainedSize(heap, false, false),
            new ObjectID(heap)
        };
    }
    
    
    public static class Name extends TreeTableViewColumn {
        
        public Name(Heap heap) {
            this(heap, false);
        }
        
        public Name(Heap heap, boolean initiallySorting) {
            super(Bundle.TreeTableViewColumn_ColName(), 100, DataType.NAME, true, initiallySorting);
        }
        
    }
    
    public static class LogicalValue extends TreeTableViewColumn {
        
        private final LabelRenderer renderer;
        private final int preferredWidth;
        
        public LogicalValue(Heap heap) {
            this(heap, false, false);
        }
        
        public LogicalValue(Heap heap, boolean initiallyVisible, boolean initiallySorting) {
            super(Bundle.TreeTableViewColumn_ColLogicalValue(), 150, DataType.LOGICAL_VALUE, initiallyVisible, initiallySorting);
            
            renderer = new LabelRenderer() {
                public void setValue(Object value, int row) {
                    if (Objects.equals(value, DataType.OBJECT_ID.getNoValue())) setText("-"); // NOI18N
                    else if (Objects.equals(value, DataType.OBJECT_ID.getUnsupportedValue())) setText(""); // NOI18N
                    else if (Objects.equals(value, DataType.OBJECT_ID.getNotAvailableValue())) setText("n/a"); // NOI18N
                    else super.setValue(value, row);
                }
            };
            renderer.setValue("A typical-length logical value to setup the column width", -1); // NOI18N
            preferredWidth = renderer.getPreferredSize().width + 20;
        }
        
        public int getPreferredWidth() { return preferredWidth; }
        
        public ProfilerRenderer getRenderer() { return renderer; }
        
    }
    
    public static class Count extends TreeTableViewColumn {
        
        private final HideableBarRenderer renderer;
        private final int preferredWidth;
        
        public Count(Heap heap) {
            this(heap, true, false);
        }
        
        public Count(Heap heap, boolean initiallyVisible, boolean initiallySorting) {
            super(Bundle.TreeTableViewColumn_ColCount(), 200, DataType.COUNT, initiallyVisible, initiallySorting);
            
            renderer = new HideableBarRenderer(HeapViewerNumberRenderer.decimalInstance(DataType.COUNT));
            renderer.setMaxValue(Integer.MAX_VALUE / 1000);
            preferredWidth = renderer.getMaxNoBarWidth() - 20;
            renderer.setMaxValue(heap.getSummary().getTotalLiveInstances());
        }
        
        public int getPreferredWidth() { return preferredWidth; }
        
        public ProfilerRenderer getRenderer() { return renderer; }
        
    }
    
    public static class OwnSize extends TreeTableViewColumn {
        
        private final HideableBarRenderer renderer;
        private final int preferredWidth;
        
        public OwnSize(Heap heap) {
            this(heap, true, true);
        }
        
        public OwnSize(Heap heap, boolean initiallyVisible, boolean initiallySorting) {
            super(Bundle.TreeTableViewColumn_ColSize(), 300, DataType.OWN_SIZE, initiallyVisible, initiallySorting);
            
            renderer = new HideableBarRenderer(HeapViewerNumberRenderer.bytesInstance(DataType.OWN_SIZE));
            renderer.setMaxValue(Integer.MAX_VALUE / 100);
            preferredWidth = renderer.getMaxNoBarWidth() - 20;
            renderer.setMaxValue(heap.getSummary().getTotalLiveBytes());
        }
        
        public int getPreferredWidth() { return preferredWidth; }
        
        public ProfilerRenderer getRenderer() { return renderer; }
        
    }
    
    public static class RetainedSize extends TreeTableViewColumn {
        
        private final HideableBarRenderer renderer;
        private final int preferredWidth;
        
        public RetainedSize(Heap heap) {
            this(heap, true, false);
        }
        
        public RetainedSize(Heap heap, boolean initiallyVisible, boolean initiallySorting) {
            super(Bundle.TreeTableViewColumn_ColRetained(), 400, DataType.RETAINED_SIZE, initiallyVisible, initiallySorting);
            
            renderer = new HideableBarRenderer(HeapViewerNumberRenderer.bytesInstance(DataType.RETAINED_SIZE));
            renderer.setMaxValue(Integer.MAX_VALUE / 100);
            preferredWidth = renderer.getMaxNoBarWidth() - 20;
            renderer.setMaxValue(heap.getSummary().getTotalLiveBytes());
        }
        
        public int getPreferredWidth() { return preferredWidth; }
        
        public ProfilerRenderer getRenderer() { return renderer; }
        
    }
    
    public static class ObjectID extends TreeTableViewColumn {
        
        private final LabelRenderer renderer;
        private final int preferredWidth;
        
        public ObjectID(Heap heap) {
            this(heap, false, false);
        }
        
        public ObjectID(Heap heap, boolean initiallyVisible, boolean initiallySorting) {
            super(Bundle.TreeTableViewColumn_ColObjectId(), 500, DataType.OBJECT_ID, initiallyVisible, initiallySorting);
            
            renderer = new LabelRenderer() {
                public void setValue(Object value, int row) {
                    if (value == null) setText(""); // NOI18N
                    else if (Objects.equals(value, DataType.OBJECT_ID.getNoValue())) setText("-"); // NOI18N
                    else if (Objects.equals(value, DataType.OBJECT_ID.getUnsupportedValue())) setText(""); // NOI18N
                    else if (Objects.equals(value, DataType.OBJECT_ID.getNotAvailableValue())) setText("n/a"); // NOI18N
                    else setText("0x" + Long.toHexString((Long)value)); // NOI18N
                }
            };
            renderer.setHorizontalAlignment(SwingConstants.TRAILING);
            renderer.setValue(Long.MAX_VALUE - 123, -1);
            preferredWidth = renderer.getPreferredSize().width + 20;
        }
        
        public int getPreferredWidth() { return preferredWidth; }
        
        public ProfilerRenderer getRenderer() { return renderer; }
        
    }
    
    
    public static abstract class Provider {
    
        public abstract TreeTableViewColumn[] getColumns(Heap heap, String viewID);

    }
    
}
