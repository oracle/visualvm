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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jdk.jfr.EventType;
import jdk.jfr.Experimental;
import jdk.jfr.ValueDescriptor;
import org.graalvm.visualvm.jfr.model.JFRDataDescriptor;
import org.graalvm.visualvm.jfr.model.JFREventType;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRJDK11EventType extends JFREventType {
    
    private final EventType type;
    
    
    JFRJDK11EventType(EventType type) {
        this.type = type;
    }
    
    
    @Override
    public long getId() {
        return type.getId();
    }

    @Override
    public String getName() {
        return type.getName();
    }

    @Override
    public String getDisplayName() {
        return isExperimental() ? "[Experimental] " + type.getLabel() : type.getLabel();
    }

    @Override
    public String getDescription() {
        return type.getDescription();
    }
    
    @Override
    public List<String> getCategory() {
        return type.getCategoryNames();
    }
    
    
    @Override
    public boolean isExperimental() {
        return type.getAnnotation(Experimental.class) != null;
    }
    
    
    @Override
    public List<JFRDataDescriptor> getDisplayableDataDescriptors(boolean includeExperimental) {
        List<JFRDataDescriptor> ddescriptors = new ArrayList<>();
        Iterator<ValueDescriptor> vdescriptors = DisplayableSupport.displayableValueDescriptors(type, includeExperimental);
        while (vdescriptors.hasNext()) ddescriptors.add(DisplayableSupport.getDataDescriptor(vdescriptors.next()));
        return ddescriptors;
    }
    
    
    @Override
    public int hashCode() {
        return type.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFRJDK11EventType ? type.equals(((JFRJDK11EventType)o).type) : false;
    }
    
}
