/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.visualvm.demoapplicationtype;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.MainClassApplicationTypeFactory;

public class AnagramApplicationTypeFactory extends MainClassApplicationTypeFactory {

    @Override
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {

        //TODO: Specify the name of the application's main class here:
        if ("com.toy.anagrams.ui.Anagrams".equals(mainClass)) {
            return new AnagramApplicationType(app.getPid());
        }
        return null;

    }
}
