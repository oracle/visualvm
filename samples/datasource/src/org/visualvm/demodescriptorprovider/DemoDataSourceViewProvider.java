/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.visualvm.demodescriptorprovider;

import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceViewProvider;
import com.sun.tools.visualvm.core.ui.DataSourceViewsManager;

/**
 *
 * @author geertjan
 */
public class DemoDataSourceViewProvider extends DataSourceViewProvider<DemoDataSource> {

    private static DemoDataSourceViewProvider INSTANCE = new DemoDataSourceViewProvider();

    @Override
    protected boolean supportsViewFor(DemoDataSource ds) {
        if (DemoDataSource.sharedInstance().equals(ds)) {
            return true;
        }
        return false;
    }

    @Override
    protected DataSourceView createView(DemoDataSource ds) {
        return new DemoDataSourceView(ds);
    }

    static void initialize() {
        DataSourceViewsManager.sharedInstance().addViewProvider(INSTANCE, DemoDataSource.class);
    }

    static void unregister() {
        DataSourceViewsManager.sharedInstance().removeViewProvider(INSTANCE);
    }
}
