/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.netbeans;

import java.io.UnsupportedEncodingException;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class JavacDetailsProvider extends DetailsProvider.Basic {

    private static final String SHAREDNAMETABLE_NAMEIMPL_MASK =
            "com.sun.tools.javac.util.SharedNameTable$NameImpl";                // NOI18N
    private static final String NAME_MASK =
            "com.sun.tools.javac.util.Name";                                    // NOI18N

    public JavacDetailsProvider() {
        super(SHAREDNAMETABLE_NAMEIMPL_MASK, NAME_MASK);
    }

    @Override
    public String getDetailsString(String className, Instance instance) {
        if (SHAREDNAMETABLE_NAMEIMPL_MASK.equals(className)) {
            return getName(instance, "length", "index", "table", "bytes");      // NOI18N
        } else if (NAME_MASK.equals(className)) {
            return getName(instance, "len", "index", "table", "names");         // NOI18N
        }
        return null;
    }

    private String getName(Instance instance, String lenField, String indexField, String tableField, String bytesField) {
        Integer length = (Integer) instance.getValueOfField(lenField);
        Integer index = (Integer) instance.getValueOfField(indexField);
        Instance table = (Instance) instance.getValueOfField(tableField);
        if (length != null && index != null && table != null) {
            PrimitiveArrayInstance bytes = (PrimitiveArrayInstance) table.getValueOfField(bytesField);
            List<String> elements = bytes.getValues();
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                String el = elements.get(index+i);
                data[i] = Byte.parseByte(el);
            }
            try {
                return new String(data, "UTF-8"); // NOI18N
            } catch (UnsupportedEncodingException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }
    
}
