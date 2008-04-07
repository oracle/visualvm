package com.sun.tools.visualvm.coredump;

import com.sun.tools.visualvm.core.snapshot.SnapshotDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

public class CoreDumpDescriptor extends SnapshotDescriptor<CoreDump> {

    private static final Image ICON = Utilities.loadImage("com/sun/tools/visualvm/coredump/resources/coredump.png", true);

    public CoreDumpDescriptor(CoreDump coreDump) {
        super(coreDump, ICON);
    }
}
