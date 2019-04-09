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
package org.graalvm.visualvm.jfr.jdk11.model.impl;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.jfr.JFRSnapshot;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;

/**
 *
 * @author Jiri Sedlacek
 */
public final class JFRJDK11ModelProvider extends AbstractModelProvider<JFRModel, DataSource> {
    
    private static final Logger LOGGER = Logger.getLogger(JFRJDK11ModelProvider.class.getName());
    
    
    private JFRJDK11ModelProvider() {}
    
    
    public static void register() {
        JFRModelFactory.getDefault().registerProvider(new JFRJDK11ModelProvider());
    }
    

    @Override
    public JFRModel createModelFor(DataSource dataSource) {
        if (dataSource instanceof JFRSnapshot) {
            JFRSnapshot snapshot = (JFRSnapshot)dataSource;
            File file = snapshot.getFile();
            try {
                return new JFRJDK11Model(file);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not load JFR snapshot (JDK11 loader): " + file);   // NOI18N
//                LOGGER.log(Level.INFO, "Could not load JFR snapshot (JDK11 loader)", e);   // NOI18N
            }
        }
        
        return null;
    }
    
    @Override
    public int priority() {
        return 1000;
    }
    
}
