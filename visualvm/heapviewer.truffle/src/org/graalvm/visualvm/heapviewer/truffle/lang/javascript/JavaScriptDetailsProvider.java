/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapviewer.truffle.lang.javascript;

import org.graalvm.visualvm.heapviewer.truffle.details.SourceDetailsProvider;
import org.graalvm.visualvm.heapviewer.truffle.details.SourceSectionView;
import java.util.Iterator;
import java.util.Locale;
import org.graalvm.visualvm.lib.jfluid.heap.Field;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.api.DetailsSupport;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class JavaScriptDetailsProvider extends DetailsProvider.Basic {
    private static final String SYMBOL_MASK = "com.oracle.truffle.js.runtime.Symbol";   // NOI18N
    private static final String JS_NODE_MASK = "com.oracle.truffle.js.nodes.JavaScriptNode+";    // NOI18N
    private static final String JS_STRING_MASK = "com.oracle.truffle.js.runtime.objects.JSLazyString";  // NOI18N
    private static final String JS_INT_MASK = "com.oracle.truffle.js.runtime.objects.JSLazyString$JSLazyIntWrapper";    // NOI18N
    private static final String JS_FUNCTION_MASK = "com.oracle.truffle.js.runtime.builtins.JSFunctionObject+";  // NOI18N
    private static final String JS_FDATA_MASK = "com.oracle.truffle.js.runtime.builtins.JSFunctionData";  // NOI18N
    private static final String JS_FUNCTION_ROOT_NODE_MASK = "com.oracle.truffle.js.nodes.function.FunctionRootNode"; // NOI18N
    private static final String JS_CONSTRUCTOR_ROOT_NODE_MASK = "com.oracle.truffle.js.nodes.function.ConstructorRootNode"; // NOI18N
    private static final String JS_NEW_TARGET_ROOT_NODE_MASK = "com.oracle.truffle.js.nodes.function.NewTargetRootNode+"; // NOI18N
    private static final String JS_NATIVE_FUNCTION_ROOT_NODE_MASK = "com.oracle.truffle.trufflenode.node.ExecuteNativeFunctionNode$NativeFunctionRootNode"; // NOI18N
    private static final String JS_FUNCTION_TEMPLATE_MASK = "com.oracle.truffle.trufflenode.info.FunctionTemplate"; // NOI18N
    private static final String JS_REGEX_NODE_MASK = "com.oracle.truffle.regex.RegexNode+"; // NOI18N
    private static final String JS_TREGEX_NODE_MASK = "com.oracle.truffle.regex.tregex.TRegexRootNode+"; // NOi18N
    private static final String JS_JAVA_PACKAGE_MASK = "com.oracle.truffle.js.runtime.java.JavaPackageObject";  // NOI18N

    private static final String SOURCE_SECTION = "com.oracle.truffle.api.source.SourceSection";

    public JavaScriptDetailsProvider() {
        super(SYMBOL_MASK, JS_NODE_MASK, JS_STRING_MASK, JS_INT_MASK, JS_FUNCTION_MASK,
                JS_FDATA_MASK, JS_FUNCTION_ROOT_NODE_MASK, JS_CONSTRUCTOR_ROOT_NODE_MASK,
                JS_NEW_TARGET_ROOT_NODE_MASK, JS_NATIVE_FUNCTION_ROOT_NODE_MASK,
                JS_FUNCTION_TEMPLATE_MASK, JS_REGEX_NODE_MASK, JS_TREGEX_NODE_MASK,
                JS_JAVA_PACKAGE_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (SYMBOL_MASK.equals(className)) {
            String description = DetailsUtils.getInstanceFieldString(instance, "description", heap);     // NOI18N

            if (description != null) {
                return description;
            }
            return DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        }
        if (JS_STRING_MASK.equals(className)) {
            Object val = instance.getValueOfField("length");   // NOI18N
            Object vall = instance.getValueOfField("left");   // NOI18N
            Object valr = instance.getValueOfField("right");   // NOI18N

            if (val instanceof Integer) {
                String left = DetailsUtils.getInstanceString((Instance)vall, heap);

                if (valr == null || left.length() > DetailsUtils.MAX_ARRAY_LENGTH) {
                    return left;
                }
                return left + DetailsUtils.getInstanceString((Instance)valr, heap);
            }

        }
        if (JS_INT_MASK.equals(className)) {
            int value = DetailsUtils.getIntFieldValue(instance, "value", 0);        // NOI18N
            return Integer.toString(value);
        }
        if (JS_FUNCTION_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "functionData", heap); // NOI18N
        }
        if (JS_FDATA_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
            if (name == null || name.isEmpty()) {
                name = DetailsUtils.getInstanceFieldString(instance, "lazyInit", heap); // NOI18N
            }
            if (name == null || name.isEmpty()) {
                // fallback to callTarget and
                // remove everything after first space
                String callTargetName = DetailsUtils.getInstanceFieldString(instance, "callTarget", heap); // NOI18N
                int spaceIndex = callTargetName == null ? -1 : callTargetName.indexOf(' ');

                if (spaceIndex > 0) {
                    name = callTargetName.substring(0,spaceIndex);
                } else {
                    name = callTargetName;
                }
            }
            return name;
        }
        if (JS_FUNCTION_ROOT_NODE_MASK.equals(className)) {
            String name = DetailsUtils.getInstanceFieldString(instance, "internalFunctionName", heap);    // NOI18N

            if (name != null) {
                return name;
            }
            return DetailsUtils.getInstanceFieldString(instance, "functionData", heap); // NOI18N
        }
        if (JS_CONSTRUCTOR_ROOT_NODE_MASK.equals(className)) {
            Object val = getValueOfField(instance, "callTarget", instance.getJavaClass());    // NOI18N
            if (val instanceof Instance) {
                String name = DetailsUtils.getInstanceString((Instance) val, heap);
                if (name != null) {
                    return "[Construct] " + name; // NOI18N
                }
            }
        }
        if (JS_NEW_TARGET_ROOT_NODE_MASK.equals(className)) {
            Object val = getValueOfField(instance, "callTarget", instance.getJavaClass().getSuperClass());    // NOI18N
            if (val instanceof Instance) {
                String name = DetailsUtils.getInstanceString((Instance) val, heap);
                if (name != null) {
                    return "[NewTarget] " + name; // NOI18N
                }
            }
        }
        if (JS_NATIVE_FUNCTION_ROOT_NODE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "template", heap); // NOI18N
        }
        if (JS_FUNCTION_TEMPLATE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "className", heap); // NOI18N
        }
        if (JS_REGEX_NODE_MASK.equals(className)) {
            String regexClass = instance.getJavaClass().getName();
            String regexSimpleName = regexClass.substring(regexClass.lastIndexOf('.') + 1, regexClass.length()); // NOI18N
            String engineLabel = regexSimpleName.substring(0, regexSimpleName.indexOf("RegexNode"));    // NOI18N
            if (engineLabel != null) {
                String pattern = DetailsUtils.getInstanceFieldString(instance, "pattern", heap);    // NOI18N
                if (pattern == null) {
                    pattern = DetailsUtils.getInstanceFieldString(instance, "node", heap);    // NOI18N
                }
                if (engineLabel != null && pattern != null) {
                    return engineLabel.toLowerCase(Locale.US) + ": " + pattern;    // NOI18N
                }
            }
        }
        if (JS_TREGEX_NODE_MASK.equals(className)) {
            String patternSource = DetailsUtils.getInstanceFieldString(instance, "patternSource", heap);    // NOI18N

            if (patternSource != null) {
                return "TRegex fwd " + patternSource;    // NOI18N
            }
        }
        if (JS_JAVA_PACKAGE_MASK.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "packageName", heap); // NOI18N
        }
        return null;
    }

    public View getDetailsView(String className, Instance instance, Heap heap) {
        if (JS_NODE_MASK.equals(className)) {
            Instance source = (Instance) instance.getValueOfField("source");     // NOI18N
            if (source == null) return null;
            if (isSourceSection(source)) {
                return DetailsSupport.getDetailsView(source, heap);
            }
            Integer charIndexInt = (Integer) instance.getValueOfField("charIndex");    // NOI18N
            Integer charLengthInt = (Integer) instance.getValueOfField("charLength");  // NOI18N
            Instance code = getCodeString(source);
            if (code == null) return null;
            int charIndex = charIndexInt.intValue() & 0x3FFFFFFF;
            int charLength = charLengthInt.intValue() & 0x3FFFFFFF;

            return new SourceSectionView(className, code, charIndex, charLength, heap);
        }
        return null;
    }

    private Instance getCodeString(Instance source) {
        Instance content = (Instance) source.getValueOfField("content");     // NOI18N
        if (content instanceof Instance) {
            return (Instance) content.getValueOfField("code");     // NOI18N
        }
        Instance key = (Instance) source.getValueOfField("key");     // NOI18N
        if (key instanceof Instance) {
            return (Instance) key.getValueOfField("content");     // NOI18N
        }
        return null;
    }

    private Object getValueOfField(Instance instance, String name, JavaClass jcls) {
        Iterator fIt = instance.getFieldValues().iterator();

        while (fIt.hasNext()) {
            FieldValue fieldValue = (FieldValue) fIt.next();
            Field f = fieldValue.getField();

            if (f.getName().equals(name) && f.getDeclaringClass().equals(jcls)) {
                if (fieldValue instanceof ObjectFieldValue) {
                    return ((ObjectFieldValue) fieldValue).getInstance();
                } else {
                    return fieldValue.getValue();
                }
            }
        }
        return null;
    }

    private boolean isSourceSection(Instance source) {
        for (JavaClass jcls = source.getJavaClass(); jcls != null; jcls = jcls.getSuperClass()) {
            if (SOURCE_SECTION.equals(jcls.getName())) {
                return true;
            }
        }
        return false;
    }
}
