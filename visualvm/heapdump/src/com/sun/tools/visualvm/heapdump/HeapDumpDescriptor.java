package com.sun.tools.visualvm.heapdump;

import com.sun.tools.visualvm.core.snapshot.SnapshotDescriptor;
import com.sun.tools.visualvm.core.snapshot.SnapshotsSupport;
import java.awt.Image;
import org.openide.util.Utilities;

public class HeapDumpDescriptor extends SnapshotDescriptor<HeapDump> {

    private static final Image ICON = SnapshotsSupport.getInstance().createSnapshotIcon(
            Utilities.loadImage("com/sun/tools/visualvm/heapdump/resources/heapdumpBase.png", true)); // NOI18N

    public HeapDumpDescriptor(HeapDump heapDump) {
        super(heapDump, ICON);
    }
}
