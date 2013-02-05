/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.profiler.heapwalk.details;

//import java.awt.Component;
import java.util.List;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

/**
 * Class to provide custom/detailed information for an Instance.
 *
 * @author Jiri Sedlacek
 */
public abstract class InstanceDetailsProvider {
    
    private static final int MAX_ARRAY_LENGTH = 128;
    
    
    public String getDetailsString(Instance instance) {
        return null;
    }
    
//    public Component getDetailsView(Instance instance) {
//        return null;
//    }
    
    
    public static boolean isInstanceOf(Instance instance, String clsName) {
        return clsName.equals(instance.getJavaClass().getName());
    }
    
    public static boolean isSubclassOf(Instance instance, String clsName) {
        JavaClass cls = instance.getJavaClass();
        while (cls != null) {
            if (cls.getName().equals(clsName)) return true;
            cls = cls.getSuperClass();
        }
        return false;
    }
    
    public static String getArrayValue(Instance instance) {
        return getArrayValue(instance, 0, -1);
    }
    
    public static String getArrayValue(Instance instance, int offset, int count) {
        if (instance instanceof PrimitiveArrayInstance) {
            PrimitiveArrayInstance array = (PrimitiveArrayInstance)instance;
            List<String> chars = array.getValues();
            int charsSize = count < 0 ? chars.size() : Math.min(count, chars.size());
            boolean truncated = false;

            if (charsSize > MAX_ARRAY_LENGTH) {
                truncated = true;
                charsSize = MAX_ARRAY_LENGTH;
            }

            StringBuilder value = new StringBuilder();
            value.append("\"");
            for (int i = offset; i < offset+charsSize; i++)
                value.append(chars.get(i));
            if (truncated) 
                value.append("...");
            value.append("\"");

            return value.toString();
        }
        return null;
    }
    
    public static String getArrayFieldValue(Instance instance, String field) {
        return getArrayFieldValue(instance, field, -1);
    }
    
    public static String getArrayFieldValue(Instance instance, String field, int count) {
        Object value = instance.getValueOfField(field); // NOI18N
        if (value instanceof Instance) return getArrayValue((Instance)value, 0, count);
        return null;
    }
    
    public static String getStringValue(Instance instance) {
        if (isInstanceOf(instance, String.class.getName())) {
            PrimitiveArrayInstance chars = (PrimitiveArrayInstance)instance.getValueOfField("value"); // NOI18N
            if (chars != null) {
                Integer offset = (Integer) instance.getValueOfField("offset"); // NOI18N
                Integer len = (Integer) instance.getValueOfField("count"); // NOI18N
                
                if (offset == null) {
                    offset = Integer.valueOf(0);
                }
                if (len == null) {
                    len = new Integer(chars.getLength());
                }
                return getArrayValue(chars, offset.intValue(), len.intValue());
            }
        }
        return null;
    }
    
    public static String getStringFieldValue(Instance instance, String field) {
        Object value = instance.getValueOfField(field);
        if (value instanceof Instance) return getStringValue((Instance)value);
        return null;
    }
    
    public static int getIntFieldValue(Instance instance, String field) {
        Object value = instance.getValueOfField(field);
        return value instanceof Integer ? ((Integer)value).intValue() : Integer.MIN_VALUE;
    }
    
}
