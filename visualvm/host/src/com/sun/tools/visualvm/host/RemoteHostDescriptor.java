package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

public class RemoteHostDescriptor extends DataSourceDescriptor {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/host/resources/remoteHost.png", true);   // NOI18N

    public RemoteHostDescriptor(Host host) {
        super(host, resolveName(host), null, NODE_ICON, POSITION_AT_THE_END, EXPAND_ON_FIRST_CHILD);
    }

    private static String resolveName(Host host) {
        String persistedName = host.getStorage().getCustomProperty(PROPERTY_NAME);
        if (persistedName != null) {
            return persistedName;
        } else {
            return host.getHostName();
        }
    }

    public boolean supportsRename() {
        return true;
    }
}
