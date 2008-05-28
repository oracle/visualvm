
package org.visualvm.demodescriptorprovider;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;

final class DemoSupport {

    private static DemoSupport instance;

    public static synchronized DemoSupport getInstance() {
        if (instance == null) {
            instance = new DemoSupport();
        }
        return instance;
    }

    private DemoSupport() {
          DataSourceDescriptorFactory.getDefault().registerProvider(new DemoDataSourceDescriptorProvider());
    }


}
