/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.jmx.impl;

import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.jmx.JmxConnectionCustomizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JmxCustomizersSupportImpl {

    private static final Set<JmxConnectionCustomizer> customizers =
            Collections.synchronizedSet(new HashSet());

    private static JmxConnectionCustomizer defaultCustomizer;
    private static boolean defaultHidden = false;


    public static void registerCustomizer(JmxConnectionCustomizer customizer) {
        defaultHidden = defaultHidden || customizer.hidesDefault();
        customizers.add(customizer);
    }

    public static void unregisterCustomizer(JmxConnectionCustomizer customizer) {
        customizers.remove(customizer);
        if (customizer.hidesDefault()) updateDefaultHidden();
    }


    static List<JmxConnectionCustomizer> getCustomizers() {
        List<JmxConnectionCustomizer> list = new ArrayList(customizers);
        if (defaultHidden && defaultCustomizer != null)
            list.remove(defaultCustomizer);
        Collections.sort(list, Positionable.COMPARATOR);
        return list;
    }


    public static void setDefaultCustomizer(JmxConnectionCustomizer customizer) {
        if (defaultCustomizer != null)
            throw new UnsupportedOperationException("Default customizer already set"); // NOI18N
        defaultCustomizer = customizer;
    }


    private static void updateDefaultHidden() {
        boolean newDefaultHidden = false;
        synchronized(customizers) {
            for (JmxConnectionCustomizer customizer : customizers)
                if (customizer.hidesDefault()) {
                    newDefaultHidden = true;
                    break;
                }
        }
        defaultHidden = newDefaultHidden;
    }

}
