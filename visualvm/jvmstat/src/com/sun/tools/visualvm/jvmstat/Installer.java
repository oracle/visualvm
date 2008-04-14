package com.sun.tools.visualvm.jvmstat;

import com.sun.tools.visualvm.jvmstat.application.JvmstatApplicationProvider;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModelFactory;
import com.sun.tools.visualvm.tools.jvmstat.JvmJvmstatModelFactory;
import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {
    
    public void restored() {
        JvmstatModelFactory.getDefault().registerProvider(new JvmstatModelProvider());
        JvmJvmstatModelFactory.getDefault().registerProvider(new JvmJvmstatModelProvider());
        JvmJvmstatModelFactory.getDefault().registerProvider(new JRockitJvmJvmstatModelProvider());
        JvmstatApplicationProvider.register();
    }
    
}
