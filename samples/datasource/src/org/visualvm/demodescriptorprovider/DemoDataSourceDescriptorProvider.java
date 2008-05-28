/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.visualvm.demodescriptorprovider;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import javax.swing.JOptionPane;

/**
 *
 * @author geertjan
 */
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
