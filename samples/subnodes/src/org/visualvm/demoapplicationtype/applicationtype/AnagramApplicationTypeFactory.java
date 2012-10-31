package org.visualvm.demoapplicationtype.applicationtype;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.type.ApplicationType;
import com.sun.tools.visualvm.application.type.ApplicationTypeFactory;
import com.sun.tools.visualvm.application.type.MainClassApplicationTypeFactory;


public class AnagramApplicationTypeFactory extends MainClassApplicationTypeFactory {

    private final static AnagramApplicationTypeFactory INSTANCE = new AnagramApplicationTypeFactory();

    @Override
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        if ("com.toy.anagrams.ui.Anagrams".equals(mainClass)) {
            return new AnagramApplicationType(app.getPid());
            
        }
        return null;
    }

    public static void initialize() {
        ApplicationTypeFactory.getDefault().registerProvider(INSTANCE);
    }

    public static void shutdown() {
        ApplicationTypeFactory.getDefault().unregisterProvider(INSTANCE);
    }
}
