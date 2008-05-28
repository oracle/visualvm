/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.visualvm.demodescriptorprovider;

import com.sun.tools.visualvm.core.datasource.DataSource;

/**
 *
 * @author geertjan
 */
public class DemoDefaultDataSourceProvider {

    static void register() {
        DemoDefaultDataSourceProvider support = new DemoDefaultDataSourceProvider();
        support.initContainer();
    }

    private void initContainer() {
        DemoAbstractDataSource container = DemoAbstractDataSource.sharedInstance();
        DataSource.ROOT.getRepository().addDataSource(container);
    }
    
}