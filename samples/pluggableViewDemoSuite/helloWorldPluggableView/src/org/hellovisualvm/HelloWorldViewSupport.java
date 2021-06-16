/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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
package org.hellovisualvm;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.ui.DataSourceViewsManager;
import org.graalvm.visualvm.core.ui.PluggableDataSourceViewProvider;

/**
 *
 * @author geertjan
 */
public class HelloWorldViewSupport {

    private static HelloWorldViewSupport instance;

    private HelloWorldViewProvider helloWorldPluggableView = new HelloWorldViewProvider();


    public static synchronized HelloWorldViewSupport getInstance() {
        if (instance == null) {
            instance = new HelloWorldViewSupport();
        }
        return instance;
    }

    public PluggableDataSourceViewProvider getApplicationPluggableView() {
        return helloWorldPluggableView;
    }

    public HelloWorldViewSupport() {
        DataSourceViewsManager.sharedInstance().addViewProvider(helloWorldPluggableView, Application.class);
    }

}
