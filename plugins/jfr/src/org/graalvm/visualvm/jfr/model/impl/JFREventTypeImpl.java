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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.graalvm.visualvm.jfr.model.JFRDataDescriptor;
import org.graalvm.visualvm.jfr.model.JFREventType;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IType;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFREventTypeImpl extends JFREventType {
    
    private final long typeId;
    private final IType type;
    private final String[] category;
    
    
    JFREventTypeImpl(long typeId, IType<IItem> type, String[] category) {
        this.typeId = typeId;
        this.type = type;
        this.category = category;
    }

    
    @Override
    public long getId() {
        return typeId;
    }

    @Override
    public String getName() {
        return type.getIdentifier();
    }

    @Override
    public String getDisplayName() {
        return type.getName();
    }

    @Override
    public String getDescription() {
        return type.getDescription();
    }
    
    @Override
    public List<String> getCategory() {
        return Arrays.asList(category);
    }
    
    
    @Override
    public List<JFRDataDescriptor> getDisplayableDataDescriptors(boolean includeExperimental) {
        List<JFRDataDescriptor> descriptors = new ArrayList();
        Iterator<IAccessorKey> keys = DisplayableSupport.displayableAccessorKeys(type, includeExperimental);
        while (keys.hasNext()) descriptors.add(DisplayableSupport.getDataDescriptor(keys.next()));
        return descriptors;
    }
    
    
    @Override
    public int hashCode() {
        return type.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFREventTypeImpl ? type.equals(((JFREventTypeImpl)o).type) : false;
    }
    
}
