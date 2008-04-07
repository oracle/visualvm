package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

public class UnknownHostDescriptor extends DataSourceDescriptor {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/host/resources/remoteHosts.png", true);

    public UnknownHostDescriptor() {
        super(Host.UNKNOWN_HOST, "<Unknown Host>", null, NODE_ICON, POSITION_LAST, EXPAND_ON_FIRST_CHILD);
    }
}
