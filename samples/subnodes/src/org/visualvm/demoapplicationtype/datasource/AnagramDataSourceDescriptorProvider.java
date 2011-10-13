package org.visualvm.demoapplicationtype.datasource;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;


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
