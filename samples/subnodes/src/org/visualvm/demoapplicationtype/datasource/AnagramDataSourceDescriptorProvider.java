package org.visualvm.demoapplicationtype.datasource;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.model.AbstractModelProvider;


public class AnagramDataSourceDescriptorProvider extends AbstractModelProvider<DataSourceDescriptor, DataSource> {

    final private static AnagramDataSourceDescriptorProvider INSTANCE = new AnagramDataSourceDescriptorProvider();

    private AnagramDataSourceDescriptorProvider() {
    }

    @Override
    public DataSourceDescriptor createModelFor(DataSource anagramDS) {
        if (anagramDS instanceof AnagramDataSource) {
            return ((AnagramDataSource) anagramDS).getDescriptor();
        }
        return null;
    }

    public static void initialize() {
        DataSourceDescriptorFactory.getDefault().registerProvider(INSTANCE);
    }

    public static void shutdown() {
        DataSourceDescriptorFactory.getDefault().unregisterProvider(INSTANCE);
    }
}
