/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.truffle;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.DataRemovedListener;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class ApplicationSamplerView extends DataSourceView {
    
    private static final String IMAGE_PATH = "org/graalvm/visualvm/sampler/resources/sampler.png"; // NOI18N

    private SamplerImpl sampler;

    private ApplicationListener applicationListener;


    ApplicationSamplerView(Application application) {
        super(application, NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Sampler"), // NOI18N
              new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 36, false);

    }


    protected void willBeAdded() {
        Application application = (Application)getDataSource();

        sampler = new SamplerImpl(application);

        applicationListener = new ApplicationListener() {
            public void dataRemoved(Application application) { applicationFinished(); }
        };
        application.notifyWhenRemoved(applicationListener);
        application.addPropertyChangeListener(Stateful.PROPERTY_STATE, applicationListener);
    }

    protected void removed() {
        sampler.removed();
        cleanup();
    }

    private void applicationFinished() {
        sampler.applicationFinished();
        cleanup();
    }

    private synchronized void cleanup() {
        Application application = (Application)getDataSource();

        if (applicationListener != null)
            application.removePropertyChangeListener(Stateful.PROPERTY_STATE,
                                                     applicationListener);

        applicationListener = null;
    }

    
    protected DataViewComponent createComponent() {
        DataViewComponent dvc = new DataViewComponent(
                sampler.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(false));

        sampler.setDataViewComponent(dvc);

        return dvc;
    }


    private static abstract class ApplicationListener
            implements DataRemovedListener<Application>, PropertyChangeListener {
        public abstract void dataRemoved(Application application);
        public void propertyChange(PropertyChangeEvent evt) { dataRemoved(null); }
    }

}
