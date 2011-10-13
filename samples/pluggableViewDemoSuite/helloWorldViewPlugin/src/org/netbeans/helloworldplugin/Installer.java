/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.helloworldplugin;

import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
       HelloWorldViewPluginProvider.initialize();
    }

    @Override
    public void uninstalled() {
        HelloWorldViewPluginProvider.uninitialize();
    }
}

