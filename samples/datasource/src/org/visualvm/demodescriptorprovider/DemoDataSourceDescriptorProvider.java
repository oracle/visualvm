package org.visualvm.demodescriptorprovider;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;

public class DemoDataSourceDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor, DataSource> {

    DemoDataSourceDescriptorProvider() {
    }

    public DataSourceDescriptor createModelFor(DataSource ds) {
        if (ds instanceof DemoDataSource) {
            return new DemoDataSourceDescriptor((DemoDataSource) ds);
        }
        return null;
    }
}
