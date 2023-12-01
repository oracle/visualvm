/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.jfr.EventType;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventType;
import org.graalvm.visualvm.jfr.model.JFREventTypeVisitor;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;

/**
 *
 * @author Jiri Sedlacek
 */
public class JFRJDK11Model extends JFRModel {
    private static final Logger LOGGER = Logger.getLogger(JFRModel.class.getName());
    
    protected static final String SNAPSHOT_V1_PREFIX = "com.oracle."; // NOI18N
    protected static final int SNAPSHOT_V1_PREFIX_LENGTH = SNAPSHOT_V1_PREFIX.length();

    private int snapshotVersion;
    private final File snapshotFile;
    
    
    protected JFRJDK11Model(String id, File file) throws IOException {
        super(id);

        this.snapshotFile = file;

        // Will throw IOException for an unsupported JFR format (0.9 or 2+)
        try (RecordingFile events = new RecordingFile(snapshotFile.toPath())) {}

        initialize();
        LOGGER.log(Level.FINE, "JFRJDK11Model created for "+file);
    }


    @Override
    public void visitEvents(JFREventVisitor... visitors) {
        // Notify all visitors 'init'
        for (JFREventVisitor visitor : visitors) visitor.init();

        // Notify visitors that are not done 'visit'
        try (RecordingFile events = new RecordingFile(snapshotFile.toPath())) {
            List<JFREventVisitor> _visitors = new ArrayList(Arrays.asList(visitors));
            long id = 0;
            while (!_visitors.isEmpty() && events.hasMoreEvents()) {
                RecordedEvent revent = events.readEvent();
                String typeId = getTypeId(revent.getEventType());
                JFREvent event = createEvent(revent, id++);
                Iterator<JFREventVisitor> _visitorsI = _visitors.iterator();
                while (_visitorsI.hasNext())
                    if (_visitorsI.next().visit(typeId, event))
                        _visitorsI.remove();
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error visiting JFR events (" + getID()+ ")", e);   // NOI18N
        } finally {
            // Notify all visitors 'done'
            for (JFREventVisitor visitor : visitors) visitor.done();
        }
    }
    
    
    @Override
    public void visitEventTypes(JFREventTypeVisitor... visitors) {
        // Notify all visitors 'init'
        for (JFREventTypeVisitor visitor : visitors) visitor.initTypes();

        // Notify visitors that are not done 'visit'
        try (RecordingFile events = new RecordingFile(snapshotFile.toPath())) {
            Iterator<EventType> types = events.readEventTypes().iterator();
            List<JFREventTypeVisitor> _visitors = new ArrayList(Arrays.asList(visitors));
            while (!_visitors.isEmpty() && types.hasNext()) {
                EventType etype = types.next();
                String typeId = getTypeId(etype);
                JFREventType type = new JFRJDK11EventType(etype);
                Iterator<JFREventTypeVisitor> _visitorsI = _visitors.iterator();
                while (_visitorsI.hasNext())
                    if (_visitorsI.next().visitType(typeId, type))
                        _visitorsI.remove();
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error visiting JFR event types (" + getID()+ ")", e);   // NOI18N
        } finally {
            // Notify all visitors 'done'
            for (JFREventTypeVisitor visitor : visitors) visitor.doneTypes();
        }
    }


    protected String getTypeId(EventType eventType) {
        String typeId = eventType.getName();
        if (snapshotVersion == 0) {
            if (isV1Id(typeId)) snapshotVersion = 1;
            else snapshotVersion = 2;
        }
        return snapshotVersion == 1 ? normalizeV1Id(typeId) : typeId;
    }
    
    protected static boolean isV1Id(String typeId) {
        return typeId.startsWith(SNAPSHOT_V1_PREFIX);
    }

    protected static String normalizeV1Id(String typeId) {
        return typeId.substring(SNAPSHOT_V1_PREFIX_LENGTH);
    }

    protected JFREvent createEvent(RecordedEvent revent, long id) {
        return new JFRJDK11Event(revent, id);
    }
    
}
