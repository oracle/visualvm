package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * DataSourceDescriptor for Host.UNKNOWN_HOST.
 * 
 * @author Jiri Sedlacek
 */
public class UnknownHostDescriptor extends DataSourceDescriptor {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/host/resources/remoteHosts.png", true);  // NOI18N

    /**
     * Creates new instance of UnknownHostDescriptor.
     */
    public UnknownHostDescriptor() {
        super(Host.UNKNOWN_HOST, NbBundle.getMessage(UnknownHostDescriptor.class, "LBL_Unknown_Host"), null, NODE_ICON, POSITION_LAST, EXPAND_ON_FIRST_CHILD);  // NOI18N
    }
}
