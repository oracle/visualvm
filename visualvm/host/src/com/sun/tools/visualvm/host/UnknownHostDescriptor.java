package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

public class UnknownHostDescriptor extends DataSourceDescriptor {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/host/resources/remoteHosts.png", true);  // NOI18N

    public UnknownHostDescriptor() {
        super(Host.UNKNOWN_HOST, NbBundle.getMessage(UnknownHostDescriptor.class, "LBL_Unknown_Host"), null, NODE_ICON, POSITION_LAST, EXPAND_ON_FIRST_CHILD);  // NOI18N
    }
}
