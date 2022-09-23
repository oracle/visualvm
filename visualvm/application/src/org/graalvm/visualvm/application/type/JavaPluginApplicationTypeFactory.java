/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.application.type;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.model.AbstractModelProvider;

/**
 * Factory which recognizes Java-Plugin application 
 * @author Luis-Miguel Alventosa
 */
public class JavaPluginApplicationTypeFactory
        extends AbstractModelProvider<ApplicationType, Application> {

    private static final String JAVA_PLUGIN = "-Djavaplugin.version=";  // NOI18N
    private static final String JAVA_PLUGIN2_MAIN = "sun.plugin2.main.client.PluginMain"; // NOI18N

    /**
     * Detects Java-Plugin application.
     * @return {@link JavaPluginApplicationType} instance or <code>null</code>
     * if application is not Java-Plugin application
     * @param application Application
     */
    @Override
    public ApplicationType createModelFor(Application application) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (jvm.isBasicInfoSupported()) {
            String args = jvm.getJvmArgs();
            int plugin_index = args != null ? args.indexOf(JAVA_PLUGIN) : -1;
            if (plugin_index != -1) {
                String version;
                int version_index = plugin_index + JAVA_PLUGIN.length();
                int space_index = args.indexOf(' ', version_index);
                if (space_index != -1) {
                    version = args.substring(version_index, space_index);
                } else {
                    version = args.substring(version_index);
                }
                return new JavaPluginApplicationType(version);
            }
            if (JAVA_PLUGIN2_MAIN.equals(jvm.getMainClass())) { // detect Java Plugin2
                return new JavaPluginApplicationType("2");  // NOI18N              
            }
        }
        return null;
    }
}
