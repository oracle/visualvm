package com.sun.tools.visualvm.heapdump;

import com.sun.tools.visualvm.core.snapshot.SnapshotDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

public class HeapDumpDescriptor extends SnapshotDescriptor<HeapDump> {

    private static final Image ICON = Utilities.loadImage("com/sun/tools/visualvm/heapdump/resources/heapdump.png", true);

    public HeapDumpDescriptor(HeapDump heapDump) {
        super(heapDump, ICON);
    }
}
