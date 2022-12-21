/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.core.model.Model;
import org.graalvm.visualvm.jfr.utils.ValuesConverter;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class JFRModel extends Model {
    
    private static final Logger LOGGER = Logger.getLogger(JFRModel.class.getName());
    
    private final String id;
    
    
    protected JFRModel(String id) {
        this.id = id;
    }
    
    
    protected String getID() {
        return id;
    }
    
    
    public abstract void visitEvents(JFREventVisitor... visitors);
    
    public abstract void visitEventTypes(JFREventTypeVisitor... visitors);
    
    
    private Instant jvmStartTime;
    private Instant jvmShutdownTime;
    private String jvmShutdownReason;
    
    private Instant firstEventTime;
    private Instant lastEventTime;
    private long eventsCount = 0;
    private long experimentalCount = 0;
    
    private long firstEventTimeMs;
    
    private String jvmFlags;
    private String jvmArgs;
    private String javaArgs;
    private Properties sysProps;
    
    
    private Map<String, Boolean> checkedEvents;
    
    public boolean containsEvent(Class<? extends JFREventChecker> eventCheckerClass) {
        Boolean contains = checkedEvents == null ? null : checkedEvents.get(eventCheckerClass.getName());
        return Boolean.TRUE.equals(contains);
    }
    
    
    public JFREvent getEvent(final long eventID) {
        final JFREvent[] foundEvent = new JFREvent[] { null };
        
        visitEvents(new JFREventVisitor() {
            @Override
            public boolean visit(String typeName, JFREvent event) {
                boolean found = eventID == event.getID();
                if (found) foundEvent[0] = event;
                return found;
            }
        });
        
        return foundEvent[0];
    }
    
    
    public Instant getJvmStartTime() {
        return jvmStartTime;
    }

    public Instant getJvmShutdownTime() {
        return jvmShutdownTime;
    }

    public String getJvmShutdownReason() {
        return jvmShutdownReason;
    }
    
    
    public Instant getFirstEventTime() {
        return firstEventTime;
    }

    public Instant getLastEventTime() {
        return lastEventTime;
    }
    
    public long getEventsCount() {
        return eventsCount;
    }
    
    public long getExperimentalEventsCount() {
        return experimentalCount;
    }
    

    public Properties getSystemProperties() {
        return sysProps;
    }

    public String getJvmFlags() {
        return jvmFlags;
    }

    public String getJvmArgs() {
        return jvmArgs;
    }

    public String getJavaCommand() {
        return javaArgs;
    }
    
    
    public String getVmVersion() {
        return findByName("java.vm.version"); // NOI18N
    }
    
    public String getJavaHome() {
        return findByName("java.home"); // NOI18N
    }
    
    public String getVmInfo() {
        return findByName("java.vm.info"); // NOI18N
    }
    
    public String getVmName() {
        return findByName("java.vm.name"); // NOI18N
    }
    
    private String findByName(String key) {
        Properties p = getSystemProperties();
        return p == null ? null : p.getProperty(key);
    }
    
    
    // To be used for times related to snapshot, can be safely stored to long
    public Duration toRelativeTime(Instant absoluteTime) {
        return Duration.between(firstEventTime, absoluteTime);
    }
    
    // To be used for relative times created using toRelativeTime(Instant)
    public Instant toAbsoluteTime(Duration relativeTime) {
        return firstEventTime.plus(relativeTime);
    }
    
    // To be used for relative times created using toRelativeTime(Instant)
    public Instant nsToAbsoluteTime(long nanos) {
        return firstEventTime.plusNanos(nanos);
    }
    
    // To be used for relative times created using toRelativeTime(Instant)
    public long nsToAbsoluteMillis(long nanos) {
        return firstEventTimeMs + ValuesConverter.nanosToMillis(nanos);
    }
    
    
    protected final void initialize() {
        sysProps = new Properties();
        
        final Set<String> experimentalTypes = new HashSet<>();
        visitEventTypes(new JFREventTypeVisitor() {
            @Override
            public boolean visitType(String typeName, JFREventType eventType) {
                if (eventType.isExperimental()) experimentalTypes.add(typeName);
                return false;
            }
        });

        visitEvents(new JFREventVisitor() {
            private List<? extends JFREventChecker> checkers;
            @Override
            public void init() {
                checkedEvents = new HashMap<>();
                checkers = new ArrayList<>(Lookup.getDefault().lookupAll(JFREventChecker.class));
            }
            @Override
            public boolean visit(String typeName, JFREvent event) {
                eventsCount++;
                if (experimentalTypes.contains(typeName)) experimentalCount++;
                
                if (!checkers.isEmpty()) {
                    Iterator<? extends JFREventChecker> checkersI = checkers.iterator();
                    while (checkersI.hasNext()) {
                        JFREventChecker checker = checkersI.next();
                        if (checker.checksEventType(typeName)) {
                            checkersI.remove();
                            checkedEvents.put(checker.getClass().getName(), Boolean.TRUE);
                        }
                    }
                }

                try {
                    Instant eventTime = event.getInstant("eventTime"); // NOI18N
                    if (firstEventTime == null) {
                        firstEventTime = eventTime;
                        lastEventTime = eventTime;
                    } else {
                        if (firstEventTime.isAfter(eventTime)) firstEventTime = eventTime;
                        if (lastEventTime.isBefore(eventTime)) lastEventTime = eventTime;
                    }
                } catch (JFRPropertyNotAvailableException e) {}

                try {
                    switch (typeName) {
                        case TYPE_JVM_INFORMATION:
                            jvmStartTime = event.getInstant("jvmStartTime"); // NOI18N
                            jvmFlags = event.getString("jvmFlags"); // NOI18N
                            jvmArgs = event.getString("jvmArguments"); // NOI18N
                            javaArgs = event.getString("javaArguments"); // NOI18N
                            break;
                            
                        case TYPE_SYSTEM_PROPERTY:
                            sysProps.put(event.getString("key"), event.getString("value")); // NOI18N
                            break;
                            
                        case TYPE_SHUTDOWN:
                            jvmShutdownTime = event.getInstant("eventTime"); // NOI18N
                            jvmShutdownReason = event.getString("reason"); // NOI18N
                            break;
                    }
                } catch (JFRPropertyNotAvailableException e) {
                    LOGGER.log(Level.INFO, "JFR model initialization info (" + getID()+ ")", e); // NOI18N
                }

                return false;
            }
        });
        
        firstEventTimeMs = ValuesConverter.instantToMillis(firstEventTime);
    }
    
    private static final String TYPE_JVM_INFORMATION = "jdk.JVMInformation"; // NOI18N
    private static final String TYPE_SYSTEM_PROPERTY = "jdk.InitialSystemProperty"; // NOI18N
    private static final String TYPE_SHUTDOWN = "jdk.Shutdown"; // NOI18N
    
    
    // --- Workaround until GH-141 is available ---
    static final JFRModel OOME = new JFRModel("OOME") { // NOI18N
        @Override public void visitEvents(JFREventVisitor... visitors) {}
        @Override public void visitEventTypes(JFREventTypeVisitor... visitors) {}
    };
    
}
