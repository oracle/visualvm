package org.visualvm.demodescriptorprovider;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.model.AbstractModelProvider;

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
