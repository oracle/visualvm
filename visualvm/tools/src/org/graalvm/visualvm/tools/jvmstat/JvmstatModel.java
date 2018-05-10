/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.tools.jvmstat;

import org.graalvm.visualvm.core.model.Model;
import java.util.List;

/**
 * This class uses Jvmstat technology 
 * (http://java.sun.com/performance/jvmstat/)
 * to obtatin various information from JVM. Note that
 * Jvmstat is available in JDK 1.4.2 and up. It is also available for
 * remote Java applications if  
 * <a href=http://download.oracle.com/javase/1.5.0/docs/tooldocs/share/jstatd.html>jstatd daemon</a>
 * is running on remote host.
 * 
 * @author Tomas Hurka
 */
public abstract class JvmstatModel extends Model {
        
    /**
     * 
     * Find a named counter exported via Jvmstat.
     * 
     * This method will look for the named counter exported
     * via Jvmstat. If a counter with a given name exists, a
     * string value of such counter is returned. Otherwise,
     * the method returns <CODE>null</CODE>.
     * @param name the name of the counter to find.
     * @return the string value of the counter, or <CODE>null</CODE> 
     * if the named counter doesn't exist.
     */
    public abstract String findByName(String name);

    /**
     * 
     * Find a named counter exported via Jvmstat.
     * 
     * This method will look for the named counter exported
     * via Jvmstat. If a counter with a given name exists, a
     * {@link MonitoredValue} interface to that counter will 
     * be return. Otherwise, the method returns <CODE>null</CODE>.
     * @param name the name of the counter to find.
     * @return the {@link MonitoredValue} that can be used to
     * monitor the the named counter, or <CODE>null</CODE> 
     * if the named counter doesn't exist.
     */
    public abstract MonitoredValue findMonitoredValueByName(String name);
    
    /**
     * 
     * Find a list of the named counters exported via Jvmstat.
     * 
     * This method will look for the named counters with 
     * names matching the given pattern. This method returns a 
     * {@link List} of string values of the counters such that
     * the name of each counter matches the given pattern.
     * @param pattern a string containing a pattern as described in
     * {@link java.util.regex.Pattern}.
     * @return a List of string values of the matching counters.
     */
    public abstract List<String> findByPattern(String pattern);
    
    /**
     * Gets this connection's ID which identifies a target Java Virtual Machine.  For a
     * given Java Virtual Machine, connection ID is unique id
     * which does not change during the lifetime of the
     * Java Virtual Machine.
     *
     * @return the unique ID of a target Java Virtual Machine.
     * @since VisualVM 1.2
     */
    public abstract String getConnectionId();
    
    /**
     * Find a list of the named counters exported via Jvmstat.
     * 
     * This method will look for the named counters with 
     * names matching the given pattern. This method returns a 
     * {@link List} of {@link MonitoredValue} of the counters such that
     * the name of each counter matches the given pattern.
     * @param pattern a string containing a pattern as described in
     * {@link java.util.regex.Pattern}.
     * @return a List of {@link MonitoredValue} of the matching counters.
     */
    public abstract List<MonitoredValue> findMonitoredValueByPattern(String pattern);
    
    /**
     * 
     * adds {@link JvmstatListener}
     * @param l a implementation of {@link JvmstatListener}
     */
    public abstract void addJvmstatListener(JvmstatListener l);
    
    /**
     * 
     * removes {@link JvmstatListener}
     * @param l a implementation of {@link JvmstatListener}
     */
    public abstract void removeJvmstatListener(JvmstatListener l);
    
}
