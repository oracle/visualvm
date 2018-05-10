package org.visualvm.demodescriptorprovider;

import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;

public class DemoDataSourceDescriptor extends DataSourceDescriptor<DemoDataSource> {

    private static final Image NODE_ICON =
            Utilities.loadImage(
            "org/visualvm/demodescriptorprovider/icon.png",
            true);

   
    public DemoDataSourceDescriptor(DemoDataSource application) {
        super(application,
                "System Monitors",
                "Descriptor for System Monitors container",
                NODE_ICON,
                POSITION_AT_THE_END,
                EXPAND_NEVER);
    }

}
