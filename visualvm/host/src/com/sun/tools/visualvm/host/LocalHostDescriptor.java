package com.sun.tools.visualvm.host;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * DataSourceDescriptor for Host.LOCALHOST.
 * 
 * @author Jiri Sedlacek
 */
public class LocalHostDescriptor extends DataSourceDescriptor {

    private static final Image NODE_ICON = Utilities.loadImage("com/sun/tools/visualvm/host/resources/localHost.png", true);    // NOI18N

    /**
     * Creates new instance of LocalHostDescriptor.
     */
    public LocalHostDescriptor() {
        super(Host.LOCALHOST, NbBundle.getMessage(LocalHostDescriptor.class, "LBL_Local"), null, NODE_ICON, 0, EXPAND_ON_FIRST_CHILD);  // NOI18N
    }
}
