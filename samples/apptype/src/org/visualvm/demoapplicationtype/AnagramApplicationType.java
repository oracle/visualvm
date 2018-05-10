/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.visualvm.demoapplicationtype;

import org.graalvm.visualvm.application.type.ApplicationType;
import java.awt.Image;
import org.openide.util.Utilities;

public class AnagramApplicationType extends ApplicationType {

    protected final int appPID;

    public AnagramApplicationType(int pid) {
        appPID = pid;
    }

    @Override
    public String getName() {
        return "Anagram";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Application type for Anagram";
    }

    @Override
    public Image getIcon() {
        return Utilities.loadImage("org/graalvm/visualvm/core/ui/resources/snapshot.png", true);
    }
}
