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
package org.graalvm.visualvm.jfr.generic.model.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventType;
import org.graalvm.visualvm.jfr.model.JFREventTypeVisitor;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.internal.EventArray;
import org.openjdk.jmc.flightrecorder.internal.FlightRecordingLoader;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRGenericModel extends JFRModel {
    
    private static final Logger LOGGER = Logger.getLogger(JFRModel.class.getName());
    
    
    private final EventArray[] types;
    
    private final JFRGenericEventFactory factory;
    
    
    JFRGenericModel(String id, File snapshotFile) throws IOException, CouldNotLoadRecordingException {
        super(id);
        
        types = loadFile(snapshotFile);
        
        factory = JFRGenericEventFactory.resolve(types);
       
        initialize();
    }
    
    
    @Override
    public void visitEvents(JFREventVisitor... visitors) {
        // Notify all visitors 'init'
        for (JFREventVisitor visitor : visitors) visitor.init();
        
        // Notify visitors that are not done 'visit'
        try {
            List<JFREventVisitor> _visitors = new ArrayList(Arrays.asList(visitors));
            Iterator<EventArray> iterables = Arrays.asList(types).iterator();
            while (!_visitors.isEmpty() && iterables.hasNext()) {
                EventArray type = iterables.next();
                String typeId = type.getType().getIdentifier();
                Iterator<IItem> items = Arrays.asList(type.getEvents()).iterator();
                while (!_visitors.isEmpty() && items.hasNext()) {
                    JFREvent event = factory.createEvent(items.next());
                    Iterator<JFREventVisitor> _visitorsI = _visitors.iterator();
                    while (_visitorsI.hasNext())
                        if (_visitorsI.next().visit(typeId, event))
                            _visitorsI.remove();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error visiting JFR events (generic loader)", e);   // NOI18N
        } finally {
            // Notify all visitors 'done'
            for (JFREventVisitor visitor : visitors) visitor.done();
        }
    }
    
    @Override
    public void visitEventTypes(JFREventTypeVisitor... visitors) {
        // Notify all visitors 'init'
        for (JFREventTypeVisitor visitor : visitors) visitor.initTypes();
        
        // Notify visitors that are not done 'visitType'
        try {
            List<JFREventTypeVisitor> _visitors = new ArrayList(Arrays.asList(visitors));
            int typeIdx = 0;
            while (!_visitors.isEmpty() && typeIdx < types.length) {
                EventArray type = types[typeIdx];
                IType<IItem> itype = type.getType();
                String typeId = itype.getIdentifier();
                String[] typeCategory = type.getTypeCategory();
                if (typeCategory == null) typeCategory = new String[] { "Uncategorized" };
                JFREventType event = new JFRGenericEventType(typeIdx++, itype, typeCategory);
                Iterator<JFREventTypeVisitor> _visitorsI = _visitors.iterator();
                while (_visitorsI.hasNext())
                    if (_visitorsI.next().visitType(typeId, event))
                        _visitorsI.remove();
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error visiting JFR event types (generic loader)", e);   // NOI18N
        } finally {
            // Notify all visitors 'done'
            for (JFREventTypeVisitor visitor : visitors) visitor.doneTypes();
        }
    }
    
    
    private static EventArray[] loadFile(File file) throws IOException, CouldNotLoadRecordingException {
        InputStream stream = IOToolkit.openUncompressedStream(file);
        try {
            return FlightRecordingLoader.loadStream(stream, false, true);
        } finally {
            IOToolkit.closeSilently(stream);
        }
    }
    
}
