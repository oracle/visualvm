package org.visualvm.demoapplicationtype.applicationtype;

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
        return "Anagram Game";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Application type for Anagram Game";
    }

    @Override
    public Image getIcon() {
        return Utilities.loadImage("org/visualvm/demoapplicationtype/resources/AnagramGame.png", true);
    }
}
