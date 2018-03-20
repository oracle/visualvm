/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.heapviewer.truffle.javascript;

import com.sun.tools.visualvm.heapviewer.truffle.DynamicObject;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;

/**
 *
 * @author Jiri Sedlacek
 */
class JavaScriptDynamicObject extends DynamicObject {
    
    private String jsType;
    
    
    JavaScriptDynamicObject(Instance instance) {
        super(instance);
    }
    
    JavaScriptDynamicObject(String type, Instance instance) {
        super(instance);
        if (!type.startsWith("<")) jsType = type;
    }
    
    
    public String getType(Heap heap) {
        if (jsType == null) {
            jsType = JavaScriptHeapFragment.fromHeap(heap).getObjectType(this);
            if (jsType.startsWith("<")) { // NOI18N
                jsType = DetailsSupport.getDetailsString(getShape(), heap);
                if (jsType.startsWith("JS")) jsType = jsType.substring(2); // NOI18N
            }
        }
        return jsType;
    }
    
    
//    static String getJSTypeOrig(Instance instance, Heap heap) {
//        DynamicObject dobj = new DynamicObject(instance);
//        List<FieldValue> staticFields = dobj.getStaticFieldValues();
//        for (FieldValue staticField : staticFields) {
//            if ("__proto__ (hidden)".equals(staticField.getField().getName())) {
//                Instance prototype = ((ObjectFieldValue)staticField).getInstance();
//                DynamicObject dprototype = new DynamicObject(prototype);
//                ObjectFieldValue constructorValue = (ObjectFieldValue)dprototype.getFieldValue("constructor");
//                if (constructorValue != null) {
//                    Instance constructor = constructorValue.getInstance();
//                    DynamicObject dconstructor = new DynamicObject(constructor);
//                    String type = JavaScriptNodes.getLogicalValue(dconstructor, dconstructor.getType(heap), heap);
//                    if (type == null) return "<unknown logical value for " + dconstructor.getType(heap) +">";
//                    return type.endsWith("()") ? type.substring(0, type.length() - 2) : type;
//                } else {
//                    return "<unknown constructorValue>";
//                }
//            }
//        }
//        return "<unknown type>";
//    }
    
    
    boolean isJavaScriptObject() {
        return JavaScriptHeapFragment.JS_LANG_ID.equals(getLanguageId().getName());
    }
    
}
