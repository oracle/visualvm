/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.details;

import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class SourceDetailsProvider extends DetailsProvider.Basic {

    private static final String FSOURCE_NAME_MASK = "com.oracle.truffle.api.source.FileSourceImpl";    // NOI18N
    private static final String CONTENT_NAME_MASK = "com.oracle.truffle.api.source.Content+";    // NOI18N
    private static final String SOURCE_NAME_MASK = "com.oracle.truffle.api.source.Source+";     // NOI18N
    private static final String SOURCEIMPL_KEY_MASK = "com.oracle.truffle.api.source.SourceImpl$Key+";   // NOI18N
    private static final String SOURCE_SECTION_MASK = "com.oracle.truffle.api.source.SourceSection+";    // NOI18N
    private static final String ASSUMPTION_MASK = "com.oracle.truffle.api.impl.AbstractAssumption+";    // NOI18N
    private static final String HIDDEN_KEY_MASK = "com.oracle.truffle.api.object.HiddenKey"; // NOI18N
    private static final String PROPERTY_MASK = "com.oracle.truffle.object.PropertyImpl";    // NOI18N
    private static final String FRAMESLOT_MASK = "com.oracle.truffle.api.frame.FrameSlot";  // NOI18N
    private static final String BP_ENABLED_MASK = "com.oracle.truffle.api.profiles.BranchProfile$Enabled";    // NOI18N
    private static final String CP_BINARY_MASK = "com.oracle.truffle.api.profiles.ConditionProfile$Binary";    // NOI18N

    public SourceDetailsProvider() {
        super(FSOURCE_NAME_MASK,CONTENT_NAME_MASK,SOURCE_NAME_MASK,SOURCEIMPL_KEY_MASK, SOURCE_SECTION_MASK,
                ASSUMPTION_MASK,HIDDEN_KEY_MASK,PROPERTY_MASK,FRAMESLOT_MASK,
                BP_ENABLED_MASK,CP_BINARY_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        if (FSOURCE_NAME_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "path");     // NOI18N
        }
        if (ASSUMPTION_MASK.equals(className)) {
            Object val = instance.getValueOfField("isValid");   // NOI18N
            if (val instanceof Boolean) {
                boolean isValid = ((Boolean)val).booleanValue();
                return DetailsUtils.getInstanceFieldString(instance, "name") + " (" + (isValid ? "valid" : "invalid") + ")";  // NOI18N
            }
        }
        if (CONTENT_NAME_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "code");     // NOI18N
        }
        if (SOURCEIMPL_KEY_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "name");  // NOI18N
            String mimeType = DetailsUtils.getInstanceFieldString(instance, "mimeType");  // NOI18N
            return name + " ("+mimeType+")";    // NOI18N
        }
        if (SOURCE_NAME_MASK.equals(className)) {
            Object key = instance.getValueOfField("key");   // NOI18N
            if (key instanceof Instance) {
                return DetailsUtils.getInstanceString((Instance) key);
            }
            String name = DetailsUtils.getInstanceFieldString(instance, "name");  // NOI18N
            String mimeType = DetailsUtils.getInstanceFieldString(instance, "mimeType");  // NOI18N
            return name + " ("+mimeType+")";    // NOI18N
        }
        if (HIDDEN_KEY_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "name")+" (hidden)";     // NOI18N
        }
        if (PROPERTY_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "key");     // NOI18N
        }
        if (SOURCE_SECTION_MASK.equals(className)) {
            Integer charIndex = (Integer) instance.getValueOfField("charIndex");    // NOI18N
            Integer charLength = (Integer) instance.getValueOfField("charLength");  // NOI18N
            if (charIndex != null && charLength != null) {
                return DetailsUtils.getInstanceFieldString(instance, "source")+ " ["+charIndex+","+(charIndex+charLength)+"]"; // NOI18N
            }
        }
        if (FRAMESLOT_MASK.equals(className)) {
            Integer index = (Integer) instance.getValueOfField("index");    // NOI18N
            String identifier = DetailsUtils.getInstanceFieldString(instance, "identifier");    // NOI18N
            String kind = DetailsUtils.getInstanceFieldString(instance, "kind");    // NOI18N
            return "[" + index + "," + identifier + "," + kind + "]"; // NOI18N
        }
        if (BP_ENABLED_MASK.equals(className)) {
            Object val = instance.getValueOfField("visited");   // NOI18N
            if (val instanceof Boolean) {
                boolean visited = ((Boolean)val).booleanValue();
                return visited ? "VISITED" : "UNINITIALIZED";   // NOI18N
            }
            return null;
        }
        if (CP_BINARY_MASK.equals(className)) {
            Object val = instance.getValueOfField("wasTrue");   // NOI18N
            Object val1 = instance.getValueOfField("wasFalse");   // NOI18N
            if (val instanceof Boolean && val1 instanceof Boolean) {
                boolean wasTrue = ((Boolean)val).booleanValue();
                boolean wasFalse = ((Boolean)val1).booleanValue();
                return "wasTrue="+wasTrue+", wasFalse="+wasFalse; // NOI18N
            }

        }
        return null;
    }

    public View getDetailsView(String className, Instance instance) {
        if (CONTENT_NAME_MASK.equals(className)) {
            Object val = instance.getValueOfField("code");  // NOI18N
            if (val instanceof Instance) {
                Instance text = (Instance) val;
                return DetailsSupport.getDetailsView(text);
            }
            return null;
        }
        if (SOURCEIMPL_KEY_MASK.equals(className)) {
            Object val = instance.getValueOfField("characters");  // NOI18N
            if (val instanceof Instance) {
                Instance text = (Instance) val;
                return DetailsSupport.getDetailsView(text);
            }
            val = instance.getValueOfField("content");  // NOI18N
            if (val instanceof Instance) {
                Instance text = (Instance) val;
                return DetailsSupport.getDetailsView(text);
            }
            return null;
        }
        if (SOURCE_NAME_MASK.equals(className)) {
            Object val = instance.getValueOfField("content");  // NOI18N
            if (val instanceof Instance) {
                Instance content = (Instance) val;
                return DetailsSupport.getDetailsView(content);
            }
            val = instance.getValueOfField("key");  // NOI18N
            if (val instanceof Instance) {
                Instance content = (Instance) val;
                return DetailsSupport.getDetailsView(content);
            }
            return null;
        }
        if (SOURCE_SECTION_MASK.equals(className)) {
            Integer charIndex = (Integer) instance.getValueOfField("charIndex");    // NOI18N
            Integer charLength = (Integer) instance.getValueOfField("charLength");  // NOI18N
            Instance source = (Instance) instance.getValueOfField("source");     // NOI18N
            Instance content = (Instance) source.getValueOfField("content");     // NOI18N
            Instance code;

            if (charIndex != null && charLength != null) {
                if (content != null) {
                    code = (Instance) content.getValueOfField("code");     // NOI18N

                    // Likely a native method
                    // TODO: handle differently?
                    if (charLength == -1) code = (Instance) source.getValueOfField("name");     // NOI18N
                } else {
                    Instance key = (Instance) source.getValueOfField("key");     // NOI18N

                    code = (Instance) key.getValueOfField("characters");  // NOI18N
                    if (code == null) {
                        code = (Instance) key.getValueOfField("content"); // NOI18N
                    }
                    // Likely a native method
                    // TODO: handle differently?
                    if (charLength == -1) code = (Instance) key.getValueOfField("name");     // NOI18N
                }
                return new SourceSectionView(className, code, charIndex.intValue(), charLength.intValue());
            }
        }
        return null;
    }
}
