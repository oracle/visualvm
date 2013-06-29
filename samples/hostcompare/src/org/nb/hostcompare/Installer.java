
package org.nb.hostcompare;

import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        HostViewProvider.initialize();
    }

    @Override
    public void uninstalled() {
        HostViewProvider.unregister();
    }
}
