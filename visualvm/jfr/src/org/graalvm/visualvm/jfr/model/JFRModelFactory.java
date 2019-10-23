/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.model;

import java.util.Set;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.core.model.ModelFactory;
import org.graalvm.visualvm.core.model.ModelProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public final class JFRModelFactory extends ModelFactory<JFRModel, DataSource> {
    
    private static JFRModelFactory FACTORY;
    
    private boolean hasProviders;
    private boolean hasGenericProvider;
    

    private JFRModelFactory() {
        addFactoryChangeListener(new DataChangeListener<ModelProvider<JFRModel, DataSource>>() {
            @Override
            public void dataChanged(DataChangeEvent<ModelProvider<JFRModel, DataSource>> dce) {
                Set<ModelProvider<JFRModel, DataSource>> providers = dce.getCurrent();
                hasProviders = !providers.isEmpty();
                hasGenericProvider = hasProviders && providers.toString().contains("generic loader"); // NOI18N
            }
        });
    }
    
    
    public static synchronized JFRModelFactory getDefault() {
        if (FACTORY == null) FACTORY = new JFRModelFactory();
        return FACTORY;
    }
    
    public static JFRModel getJFRModelFor(DataSource app) {
        JFRModel model = getDefault().getModel(app);
        return model == JFRModel.OOME ? null : model;
    }
    
    
    public final boolean hasProviders() {
        return hasProviders;
    }
    
    public final boolean hasGenericProvider() {
        return hasGenericProvider;
    }
    
    
    // WORKAROUND to clean up the model after closing the snapshot view
    // Currently the JFRModel is kept on heap using a SoftReference, eventually
    // reused on subsequent snapshot open. Takes too much space for JFRModelImpl.
    @Deprecated public static void cleanupModel__Workaround(JFRModel model) {
        // Dummy JFRModelProvider with no functionality
        ModelProvider<JFRModel, DataSource> workaround = new AbstractModelProvider<JFRModel, DataSource>() {
            @Override public JFRModel createModelFor(DataSource b) { return null; }
        };
        // Registering/unregistering ModelProvider clears the Model cache
        JFRModelFactory.getDefault().registerProvider(workaround);
        JFRModelFactory.getDefault().unregisterProvider(workaround);
    }
    
}
