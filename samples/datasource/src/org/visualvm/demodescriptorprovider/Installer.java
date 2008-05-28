package org.visualvm.demodescriptorprovider;

import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    DemoDataSource ds = new DemoDataSource();

    @Override
    public void restored() {
        DemoDefaultDataSourceProvider.register();
        DemoSupport.getInstance();
        DemoDataSourceViewProvider.initialize();
    }

    @Override
    public void uninstalled() {
        DemoDataSourceViewProvider.unregister();
    }
}
