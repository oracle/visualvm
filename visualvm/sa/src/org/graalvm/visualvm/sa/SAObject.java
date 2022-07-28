/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sa;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author Tomas Hurka
 */
class SAObject {
    final Object instance;
    Map<String,List<Method>> methodCache;
    private static Map<Class<?>,Class<?>> primitiveTypes;

    static {
        primitiveTypes = new HashMap<>();
        primitiveTypes.put(Integer.class,Integer.TYPE);
    }

    SAObject(Object i) {
        instance = i;
        if (i != null) {
            methodCache = new HashMap<>();
            Method[] methods = i.getClass().getMethods();
            for (int j = 0; j < methods.length; j++) {
                Method method = methods[j];
                String name = method.getName();
                int pars = method.getParameterTypes().length;
                String id = methodId(name,pars);
                List<Method> mlist = Collections.singletonList(method);
                List<Method> oldlist = methodCache.put(id,mlist);
                if (oldlist != null) {
                    List<Method> unitedList = new ArrayList<>(mlist);
                    unitedList.addAll(oldlist);
                    methodCache.put(id,unitedList);
                }
            }
        }
    }
    
    boolean isNull() {
        return instance == null;
    }
    
    Object invoke(String methodName,Object... parameters) throws IllegalAccessException, InvocationTargetException {
        String mid = methodId(methodName,parameters.length);
        List<Method> methods = methodCache.get(mid);
        Method method = null;
        
        if (methods == null) {
            throw new IllegalAccessException("No method "+mid); // NOI18N
        }
        if (methods.size()==1) {
            method = methods.get(0);
        } else {
            Class<?>[] parClasses = new Class<?>[parameters.length];
            
            for (int i = 0; i < parameters.length; i++) {
                Class<?> cl = parameters[i].getClass();
                if (primitiveTypes.containsKey(cl)) {
                    cl = primitiveTypes.get(cl);
                }
                parClasses[i] = cl;
            }
            for (Method m : methods) {
                if (Arrays.deepEquals(m.getParameterTypes(),parClasses)) {
                    method = m;
                    break;
                }
            }
        }
        if (method == null) {
            throw new IllegalArgumentException(instance+" "+methodName+" "+Arrays.toString(parameters));
        }
        return method.invoke(instance,parameters);
    }
    
    SAObject invokeSA(String methodName,Object... parameters) throws IllegalAccessException, InvocationTargetException {
        return new SAObject(invoke(methodName,parameters));
    }
    
    private static String methodId(String name,int pars) {
        return name.concat("#").concat(Integer.toString(pars)); // NOI18N
    }
    
    public String toString() {
        if (instance != null) {
            return instance.toString();
        }
        return "<null>";    // NOI18N
    }

    public int hashCode() {
        if (instance == null)
            return 0;
        return instance.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof SAObject) {
            SAObject saobj = (SAObject) obj;
            if (instance == null) {
                return saobj.instance == null;
            }
            return instance.equals(saobj.instance);
        }
        return false;
    }
}
