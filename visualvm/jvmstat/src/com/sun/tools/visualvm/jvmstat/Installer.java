package com.sun.tools.visualvm.jvmstat;

import com.sun.tools.visualvm.jvmstat.application.JvmstatApplicationProvider;
import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {
    
    public void restored() {
        JvmstatApplicationProvider.register();
    }
    
}
