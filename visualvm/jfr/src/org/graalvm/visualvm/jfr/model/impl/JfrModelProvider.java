/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.model.impl;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.tools.attach.AttachModel;
import org.graalvm.visualvm.tools.attach.AttachModelFactory;
import org.graalvm.visualvm.tools.jfr.JfrModel;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;

/**
 *
 * @author Tomas Hurka
 */
public class JfrModelProvider extends AbstractModelProvider<JfrModel, Application> {

    public JfrModel createModelFor(Application app) {
        JfrModelImpl jfr = getJFRModel(app);
        if (jfr != null && jfr.isJfrAvailable()) {
            return jfr;
        }
        return null;
    }

    private static JfrModelImpl getJFRModel(Application app) {
        AttachModel attach = AttachModelFactory.getAttachFor(app);
        if (attach != null) {
            return new JfrModelImpl(attach);
        }
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
        if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
            return new JfrModelImpl(jmxModel);
        }
        return null;
    }

}
