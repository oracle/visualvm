/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.sampler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationSamplerView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/sampler/resources/sampler.png"; // NOI18N

    private SamplerImpl sampler;

    private ApplicationListener applicationListener;


    ApplicationSamplerView(Application application) {
        super(application, NbBundle.getMessage(ApplicationSamplerView.class, "LBL_Sampler"), // NOI18N
              new ImageIcon(ImageUtilities.loadImage(IMAGE_PATH, true)).getImage(), 35, false);

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
