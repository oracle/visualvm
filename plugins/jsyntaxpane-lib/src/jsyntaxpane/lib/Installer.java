/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jsyntaxpane.lib;

import jsyntaxpane.DefaultSyntaxKit;
import jsyntaxpane.util.JarServiceProvider;
import org.openide.modules.ModuleInstall;
import org.openide.util.Lookup;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        JarServiceProvider.setGlobalLoader(Lookup.getDefault().lookup(ClassLoader.class));
        DefaultSyntaxKit.initKit();
    }
}
