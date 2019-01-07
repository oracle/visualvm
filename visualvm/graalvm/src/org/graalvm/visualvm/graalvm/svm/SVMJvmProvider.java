/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.graalvm.svm;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModelFactory;

/**
 *
 * @author Tomas Hurka
 */
public class SVMJvmProvider extends AbstractModelProvider<Jvm, Application> {
    private static final String SVM_VM_NAME = "Substrate VM"; // NOI18N
    private static final String VM_NAME = "java.property.java.vm.name"; // NOI18N

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public Jvm createModelFor(Application app) {
        Jvm jvm = null;
        JvmstatModel jvmstat = JvmstatModelFactory.getJvmstatFor(app);

        if (jvmstat != null) {
            String vmName = jvmstat.findByName(VM_NAME);
            if (SVM_VM_NAME.equals(vmName)) {
                jvm = new SVMJVMImpl(app, jvmstat);
            }
        }
        return jvm;
    }
}
