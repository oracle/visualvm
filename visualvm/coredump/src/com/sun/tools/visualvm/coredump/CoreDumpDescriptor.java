package com.sun.tools.visualvm.coredump;

import com.sun.tools.visualvm.core.snapshot.SnapshotDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

/**
 * DataSourceDescriptor for CoreDump.
 *
 * @author Jiri Sedlacek
 */
public class CoreDumpDescriptor extends SnapshotDescriptor<CoreDump> {

    private static final Image ICON = Utilities.loadImage("com/sun/tools/visualvm/coredump/resources/coredump.png", true);  // NOI18N

    /**
     * Creates new instance of CoreDumpDescriptor.
     * 
     * @param coreDump CoreDump for the descriptor.
     */
    public CoreDumpDescriptor(CoreDump coreDump) {
        super(coreDump, ICON);
    }
}
