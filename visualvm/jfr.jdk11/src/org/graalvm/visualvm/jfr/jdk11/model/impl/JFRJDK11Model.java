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
import java.io.IOException;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import org.graalvm.visualvm.jfr.jdk9.model.impl.JFRJDK9Model;
import org.graalvm.visualvm.jfr.model.JFREvent;

/**
 *
 * @author Jiri Sedlacek
 */
public class JFRJDK11Model extends JFRJDK9Model {
    
    private int snapshotVersion;
    
    
    protected JFRJDK11Model(String id, File file) throws IOException {
        // Will throw IOException for an unsupported JFR format (0.9)
        super(id, file);
    }
    
    
    @Override
    protected String getTypeId(EventType eventType) {
        String typeId = eventType.getName();
        if (snapshotVersion == 0) {
            if (isV1Id(typeId)) snapshotVersion = 1;
            else snapshotVersion = 2;
        }
        return snapshotVersion == 1 ? normalizeV1Id(typeId) : typeId;
    }
    
    @Override
    protected JFREvent createEvent(RecordedEvent revent) {
        return new JFRJDK11Event(revent);
    }
    
}
