package org.visualvm.demodescriptorprovider;

import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {
    
    DemoAbstractDataSource ds = new DemoAbstractDataSource();

    @Override
    public void restored() {
        DemoDefaultDataSourceProvider.register();
        DemoSupport.getInstance();
    }
}
