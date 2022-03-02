/*
 * Copyright (c) 2022, 2022 Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.streaming.network;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.ui.DataSourceViewPlugin;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;

/**
 *
 * @author Tomas Hurka
 */
public class NetworkViewPlugin extends DataSourceViewPlugin {

    private final NetworkModel model;
    private NetworkViewComponent view;

    @Override
    public DataViewComponent.DetailsView createView(int location) {
        switch (location) {
            case DataViewComponent.TOP_LEFT:
                view = new NetworkViewComponent(model);
                return view.getDetailsView();
            default:
                return null;
        }
    }

    @Override
    protected void willBeAdded() {
        model.initialize();
    }

    @Override
    protected void removed() {
        model.cleanup();
    }

    NetworkModel getModel() {
        return model;
    }

    NetworkViewPlugin(DataSource dataSource, NetworkModel model) {
        super(dataSource);
        this.model = model;
    }
}
