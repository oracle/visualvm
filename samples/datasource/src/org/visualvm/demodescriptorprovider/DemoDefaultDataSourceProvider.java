/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.visualvm.demodescriptorprovider;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;

/**
 *
 * @author geertjan
 */
public class DemoDefaultDataSourceProvider {

    static void register() {
        DemoDefaultDataSourceProvider support = new DemoDefaultDataSourceProvider();
        support.initContainer();
        DataSourceRepository.sharedInstance().getDataSources(DemoDataSource.class);
    }

    private void initContainer() {
        DemoDataSource container = DemoDataSource.sharedInstance();
        DataSource.ROOT.getRepository().addDataSource(container);
    }
    
}