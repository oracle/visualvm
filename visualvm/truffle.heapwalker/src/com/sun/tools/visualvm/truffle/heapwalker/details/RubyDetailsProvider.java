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
package com.sun.tools.visualvm.truffle.heapwalker.details;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service = DetailsProvider.class)
public class RubyDetailsProvider extends DetailsProvider.Basic {

    private static final String RUBY_OBJECT_TYPE_MASK = "org.truffleruby.language.RubyObjectType+";   // NOI18N

    public RubyDetailsProvider() {
        super(RUBY_OBJECT_TYPE_MASK);
    }

    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (RUBY_OBJECT_TYPE_MASK.equals(className)) {
            String name = instance.getJavaClass().getName();
            int index = name.lastIndexOf('$');
            
            if (index == -1) {
                index = name.lastIndexOf('.');
            }
            return name.substring(index+1);
        }
        return null;
    }
}
