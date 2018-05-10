/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.visualvm.demoapplicationtype;

import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    private static AnagramApplicationTypeFactory INSTANCE = new AnagramApplicationTypeFactory();

    @Override
    public void restored() {
        ApplicationTypeFactory.getDefault().registerProvider(INSTANCE);
        AnagramViewPluginProvider.initialize();
    }

    @Override
    public void uninstalled() {
        ApplicationTypeFactory.getDefault().unregisterProvider(INSTANCE);
        AnagramViewPluginProvider.uninitialize();
    }
}

