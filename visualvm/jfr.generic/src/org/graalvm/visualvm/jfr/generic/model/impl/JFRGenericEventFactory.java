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
package org.graalvm.visualvm.jfr.generic.model.impl;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.internal.EventArray;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class JFRGenericEventFactory {
    
    abstract JFRGenericEvent createEvent(IItem item, long id);
    
    
    static JFRGenericEventFactory resolve(EventArray[] types) {
        for (EventArray typeA : types) {
            IType type = typeA.getType();
            if (type.getAccessor(JfrAttributes.END_TIME.getKey()) != null) return new V0();
            if (type.getAccessor(JfrAttributes.DURATION.getKey()) != null) return new V1();
        }
        
        throw new RuntimeException("No JFRGenericEventFactory resolved!");
    }
    
    
    static class V0 extends JFRGenericEventFactory {

        @Override JFRGenericEvent createEvent(IItem item, long id) { return new JFRGenericEvent.V0(item, id); }
        
    }
    
    static class V1 extends JFRGenericEventFactory {

        @Override JFRGenericEvent createEvent(IItem item, long id) { return new JFRGenericEvent.V1(item, id); }
        
    }
    
}
