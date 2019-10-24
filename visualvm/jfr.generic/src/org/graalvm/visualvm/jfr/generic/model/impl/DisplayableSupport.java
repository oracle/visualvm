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

import java.util.Iterator;
import org.graalvm.visualvm.jfr.model.JFRDataDescriptor;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.LinearKindOfQuantity;
import org.openjdk.jmc.common.util.TypeHandling;
import org.openjdk.jmc.flightrecorder.JfrAttributes;

/**
 *
 * @author Jiri Sedlacek
 */
final class DisplayableSupport {
    
    static Iterator<IAccessorKey> displayableAccessorKeys(final IType type, final boolean includeExperimental) {
        return new Iterator<IAccessorKey>() {
            private final String ID_STACKTRACE;
            
            private final Iterator<IAccessorKey> master;
            private IAccessorKey next;
            
            {
                ID_STACKTRACE = JfrAttributes.EVENT_STACKTRACE.getIdentifier();
                
                master = type.getAccessorKeys().keySet().iterator();
                next = computeNext();
            }
            
            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public IAccessorKey next() {
                IAccessorKey ret = next;
                next = computeNext();
                return ret;
            }
            
            private IAccessorKey computeNext() {
                while (master.hasNext()) {
                    IAccessorKey _next = master.next();
                    if (isDisplayable(_next)) return _next;
                }
                return null;
            }
            
            private boolean isDisplayable(IAccessorKey key) {
                return !ID_STACKTRACE.equals(key.getIdentifier());
            }
        };
    }
    
    
    static JFRDataDescriptor getDataDescriptor(IAccessorKey key) {
        String dataName = TypeHandling.getValueString(key);
        String dataDescription = TypeHandling.getVerboseString(key);
        boolean isNumericData = key.getContentType() instanceof LinearKindOfQuantity;
        return new JFRDataDescriptor(dataName, dataDescription, null, null, isNumericData);
    }
    
    
    private DisplayableSupport() {}
    
}
