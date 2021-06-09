/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jmx.impl;

import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.jmx.JmxApplicationsSupport;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.graalvm.visualvm.core.VisualVM;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;

/**
 * Handling of --openjmx commandline option
 *
 * @author Jiri Sedlacek
 */
public final class OpenJmxApplication extends OptionProcessor {
    
    private Option openjmx = Option.requiredArgument(Option.NO_SHORT_NAME, "openjmx"); // NOI18N

    public OpenJmxApplication() {
        openjmx = Option.shortDescription(openjmx, "org.graalvm.visualvm.jmx.impl.Bundle","MSG_OPENJMX");
    }
    
    protected Set<Option> getOptions() {
        return Collections.singleton(openjmx);
    }

    protected void process(Env env, Map<Option, String[]> optionValues) throws CommandException {
        String[] connectionStrings = optionValues.get(openjmx);
        if (connectionStrings != null && connectionStrings.length > 0)
            openJmxApplication(connectionStrings[0]);
    }

    private void openJmxApplication(final String connectionString) {
        VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
                JmxApplication application = (JmxApplication)JmxApplicationsSupport.getInstance().
                        createJmxApplicationInteractive(connectionString, null, null, null);
                if (application != null) {
                    JmxPropertiesProvider.setCustomizer(application,
                            JmxConnectionSupportImpl.getDefaultCustomizer());
                    DataSourceWindowManager.sharedInstance().openDataSource(application);
                }
            }
        });
    }
}
