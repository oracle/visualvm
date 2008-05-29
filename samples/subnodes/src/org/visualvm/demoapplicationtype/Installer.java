package org.visualvm.demoapplicationtype;

import org.visualvm.demoapplicationtype.applicationtype.AnagramApplicationTypeFactory;
import org.visualvm.demoapplicationtype.application.AnagramApplicationProvider;
import org.openide.modules.ModuleInstall;
import org.visualvm.demoapplicationtype.datasource.AnagramDataSourceDescriptorProvider;
import org.visualvm.demoapplicationtype.model.AnagramModelProvider;
import org.visualvm.demoapplicationtype.overview.AnagramOverviewProvider;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        AnagramApplicationTypeFactory.initialize();
        AnagramOverviewProvider.initialize();
        AnagramDataSourceDescriptorProvider.initialize();
        AnagramModelProvider.initialize();
        AnagramApplicationProvider.initialize();
    }
    
    @Override
    public void uninstalled() {
        AnagramApplicationTypeFactory.shutdown();
        AnagramOverviewProvider.shutdown();
        AnagramDataSourceDescriptorProvider.shutdown();
        AnagramModelProvider.shutdown();
        AnagramApplicationProvider.shutdown();
    }
}
