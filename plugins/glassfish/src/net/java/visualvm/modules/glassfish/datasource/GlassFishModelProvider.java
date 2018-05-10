/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package net.java.visualvm.modules.glassfish.datasource;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.datasource.DataSourceRepository;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import java.util.Set;
import net.java.visualvm.modules.glassfish.GlassFishApplicationType;
import org.openide.util.RequestProcessor;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishModelProvider implements DataChangeListener<Application>, DataRemovedListener<Application> {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final GlassFishModelProvider INSTANCE = new GlassFishModelProvider();
    private final DataRemovedListener<Application> removalListener = new DataRemovedListener<Application>() {
        public void dataRemoved(Application app) {
            processFinishedApplication(app);
        }
    };
    
    private GlassFishModelProvider() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public void dataChanged(DataChangeEvent<Application> event) {
        if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
            // Initial event to deliver DataSources already created by the provider before registering to it as a listener
            // NOTE: already existing hosts are treated as new for this provider
            Set<Application> newApplications = event.getCurrent();

            for (Application app : newApplications) {
                processNewApplication(app);
            }
        } else {
            // Real delta event
            Set<Application> newApplications = event.getAdded();

            for (Application app : newApplications) {
                processNewApplication(app);
            }
        }
    }

    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataChangeListener(INSTANCE, Application.class);
    }

    public static void shutdown() {
        DataSourceRepository.sharedInstance().removeDataChangeListener(INSTANCE);
    }
    
    public void dataRemoved(Application application) {
        processFinishedApplication(application);
    }

    private void processFinishedApplication(Application app) {
        // TODO: remove listener!!!
        Set<GlassFishModel> roots = app.getRepository().getDataSources(GlassFishModel.class);
        app.getRepository().removeDataSources(roots);
    }

    private void processNewApplication(final Application app) {
        if (ApplicationTypeFactory.getApplicationTypeFor(app) instanceof GlassFishApplicationType) {
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    GlassFishModel gfm = new GlassFishModel(app);
                    app.getRepository().addDataSource(gfm);
                    app.notifyWhenRemoved(removalListener);
                }
            }, 1500);
        }
    }
}
