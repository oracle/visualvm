package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

/**
 * DataSourceDescriptor for remote hosts.
 * Jiri Sedlacek
 */
public class RemoteHostDescriptor extends DataSourceDescriptor {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/host/resources/remoteHost.png", true);   // NOI18N

    /**
     * Creates new instance of RemoteHostDescriptor for a given host.
     * 
     * @param host Host for which to create the descriptor.
     */
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
