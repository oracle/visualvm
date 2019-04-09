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
package org.graalvm.visualvm.jfr.model.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.jfr.model.JFREvent;
import org.graalvm.visualvm.jfr.model.JFREventTypeVisitor;
import org.graalvm.visualvm.jfr.model.JFREventVisitor;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRModelImpl extends JFRModel {
    
    private static final Logger LOGGER = Logger.getLogger(JFRModel.class.getName());
    
    
    private final IItemCollection itemsCollection;
    
    
    JFRModelImpl(File snapshotFile) throws IOException, CouldNotLoadRecordingException {
        itemsCollection = JfrLoaderToolkit.loadEvents(snapshotFile);
        
        initialize();
    }
    
    
    @Override
    public void visitEvents(JFREventVisitor... visitors) {
        // Notify all visitors 'init'
        for (JFREventVisitor visitor : visitors) visitor.init();
        
        // Notify visitors that are not done 'visit'
        try {
            List<JFREventVisitor> _visitors = new ArrayList(Arrays.asList(visitors));
            Iterator<IItemIterable> iterables = itemsCollection.iterator();
            while (!_visitors.isEmpty() && iterables.hasNext()) {
                Iterator<IItem> items = iterables.next().iterator();
                while (!_visitors.isEmpty() && items.hasNext()) {
                    IItem item = items.next();
                    String typeId = item.getType().getIdentifier();
                    JFREvent event = new JFREventImpl(item);
                    Iterator<JFREventVisitor> _visitorsI = _visitors.iterator();
                    while (_visitorsI.hasNext())
                        if (_visitorsI.next().visit(typeId, event))
                            _visitorsI.remove();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error visiting JFR events", e);   // NOI18N
        } finally {
            // Notify all visitors 'done'
            for (JFREventVisitor visitor : visitors) visitor.done();
        }
    }
    
    @Override
    public void visitEventTypes(JFREventTypeVisitor... visitors) {
        // Notify all visitors 'init'
        for (JFREventTypeVisitor visitor : visitors) visitor.initTypes();
        
//        // Notify visitors that are not done 'visit'
//        try (RecordingFile events = new RecordingFile(file.toPath())) {
//            Iterator<EventType> types = events.readEventTypes().iterator();
//            List<JFREventTypeVisitor> _visitors = new ArrayList(Arrays.asList(visitors));
//            while (!_visitors.isEmpty() && types.hasNext()) {
//                JFREventType type = new JFREventTypeImpl(types.next());
//                Iterator<JFREventTypeVisitor> _visitorsI = _visitors.iterator();
//                while (_visitorsI.hasNext())
//                    if (_visitorsI.next().visitType(type))
//                        _visitorsI.remove();
//            }
//        } catch (Exception e) {
//            LOGGER.log(Level.INFO, "Error visiting JFRv11 event types", e);   // NOI18N
//        } finally {
            // Notify all visitors 'done'
            for (JFREventTypeVisitor visitor : visitors) visitor.doneTypes();
//        }
    }
    
}
