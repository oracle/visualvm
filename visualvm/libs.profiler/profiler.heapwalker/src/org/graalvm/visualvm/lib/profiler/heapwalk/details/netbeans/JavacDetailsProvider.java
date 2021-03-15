/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.graalvm.visualvm.lib.profiler.heapwalk.details.netbeans;

import java.io.UnsupportedEncodingException;
import java.util.List;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
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
    public String getDetailsString(String className, Instance instance, Heap heap) {
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
