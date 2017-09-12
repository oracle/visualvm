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
package com.sun.tools.visualvm.truffle.heapwalker.ruby;

import com.sun.tools.visualvm.truffle.heapwalker.DynamicObject;
import java.util.Collection;
import java.util.List;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.netbeans.modules.profiler.heapwalk.details.api.DetailsSupport;
import com.sun.tools.visualvm.truffle.heapwalker.TruffleStackTraces;
import org.netbeans.modules.profiler.heapwalker.v2.HeapContext;

/**
 *
 * @author Jiri Sedlacek
 */
class RubyThreadsHTML {
    
    private static final String CLASS_URL_PREFIX = "file://class/"; // NOI18N
    private static final String INSTANCE_URL_PREFIX = "file://instance/";   // NOI18N
    
    static String getThreads(HeapContext context) {
//        long start = System.currentTimeMillis();
        
        StringBuilder sb = new StringBuilder();
        
        Heap heap = context.getFragment().getHeap();
        JavaClass javaClassClass = heap.getJavaClassByName(Class.class.getName());
        
        TruffleStackTraces tst = new TruffleStackTraces(heap);
        Collection<TruffleStackTraces.StackTrace> threads = tst.getStackTraces();
        
        sb.append("<pre>"); // NOI18N

        if (threads != null) {
            for (TruffleStackTraces.StackTrace st : threads) {
                sb.append("<b>&nbsp;&nbsp;Thread " + DetailsSupport.getDetailsString(st.getThread(), heap) + "</b>");        
                sb.append("<br>");  // NOI18N

                List<TruffleStackTraces.Frame> frames = st.getFrames();
                for (TruffleStackTraces.Frame f : frames) {
                    List<FieldValue> fields = f.getFieldValues();
                    sb.append("    at "+f.getName()+"()");
                    sb.append("<br>");  // NOI18N

    //                if (!fields.isEmpty()) sb.append("        Locals:");
    //                sb.append("<br>");  // NOI18N

                    for (FieldValue fv : fields) {
    //                    String val;
    //                    if (fv instanceof ObjectFieldValue) {
    //                        Instance i = ((ObjectFieldValue)fv).getInstance();
    //                        val = i.getJavaClass().getName()+"#"+i.getInstanceNumber();
    //                    } else {
    //                        val = fv.getValue();
    //                    }
                        if (!(fv instanceof ObjectFieldValue)) continue;
                        Instance instance = ((ObjectFieldValue)fv).getInstance();
                        if (instance == null) continue;
                        sb.append("       <span style=\"color: #666666\">local object:</span> " + printInstance(instance, heap, javaClassClass));
                        sb.append("<br>");  // NOI18N
                    }
                }
            }
        } else {
            sb.append("<b>&nbsp;&nbsp;Thread not available");
        }
        
        sb.append("<br>");  // NOI18N
        sb.append("</pre>"); // NOI18N
        
//        System.err.println(">>> RUBY Threads computed in " + (System.currentTimeMillis() - start));
        
        return sb.toString();
    }
    
    private static String printInstance(Instance in, Heap h, JavaClass jc) {
        String className;
        JavaClass jcls;
        
        if (in == null) {
            return "null";
        }
        jcls = in.getJavaClass();
        if (jcls == null) {
            return "unknown instance #"+in.getInstanceId(); // NOI18N
        }
        if (jcls.equals(jc)) {
            JavaClass javaClass = h.getJavaClassByID(in.getInstanceId());
            
            if (javaClass != null) {
                className = javaClass.getName();
                return "<a href='"+ CLASS_URL_PREFIX + className + "/" + javaClass.getJavaClassId() + "'>class " + className + "</a>"; // NOI18N
            }
        }
        
        className = jcls.getName();
        String instanceString;
        if (DynamicObject.isDynamicObject(in)) {
            DynamicObject dobj = new DynamicObject(in);
            String type = dobj.getType(h);
            instanceString = "<a href='"+ INSTANCE_URL_PREFIX + className + "/" + in.getInstanceNumber() + "/" + in.getInstanceId() + "' name='" + in.getInstanceId() + "'>" + type + "#" + in.getInstanceNumber() + "</a>";
            String logValue = RubyNodes.getLogicalValue(dobj, type, h);
            if (logValue != null) instanceString += " <span style=\"color: #666666\">: " + logValue + "</span>";
        } else {
            instanceString = "<a href='"+ INSTANCE_URL_PREFIX + className + "/" + in.getInstanceNumber() + "/" + in.getInstanceId() + "' name='" + in.getInstanceId() + "'>" + className + '#' + in.getInstanceNumber() + "</a>"; // NOI18N
            String logValue = DetailsSupport.getDetailsString(in, h);
            if (logValue != null) instanceString += " <span style=\"color: #666666\">(" + logValue + ")</span>";
        }
        
        return instanceString;
    }
    
}
