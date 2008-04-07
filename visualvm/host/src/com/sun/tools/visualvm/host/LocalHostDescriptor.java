package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

public class LocalHostDescriptor extends DataSourceDescriptor {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/host/resources/localHost.png", true);

    public LocalHostDescriptor() {
        super(Host.LOCALHOST, "Local", null, NODE_ICON, 0, EXPAND_ON_FIRST_CHILD);
    }
}
