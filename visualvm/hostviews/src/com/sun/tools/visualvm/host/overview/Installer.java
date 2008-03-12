/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.tools.visualvm.host.overview;

import com.sun.tools.visualvm.host.views.HostViewsSupport;
import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    public void restored() {
        HostViewsSupport.sharedInstance();
    }
}
