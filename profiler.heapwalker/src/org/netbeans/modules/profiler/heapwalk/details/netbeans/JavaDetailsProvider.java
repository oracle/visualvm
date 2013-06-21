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
package org.netbeans.modules.profiler.heapwalk.details.netbeans;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class JavaDetailsProvider extends DetailsProvider.Basic {

    private static final String FO_INDEXABLE = "org.netbeans.modules.parsing.impl.indexing.FileObjectIndexable"; // NOI18N
    private static final String INDEXABLE = "org.netbeans.modules.parsing.spi.indexing.Indexable"; // NOI18N
    
    long lastHeapId;
    String lastSeparator;
    
    public JavaDetailsProvider() {
        super(FO_INDEXABLE,INDEXABLE);
    }

    @Override
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (FO_INDEXABLE.equals(className))  {
            String root = DetailsUtils.getInstanceFieldString(instance, "root", heap); // NOI18N
            String relpath = DetailsUtils.getInstanceFieldString(instance, "relativePath", heap); // NOI18N
            if (root != null && relpath != null) {
                return root.concat(getFileSeparator(heap)).concat(relpath);    
            }
        } else if (INDEXABLE.equals(className)) {
            return DetailsUtils.getInstanceFieldString(instance, "delegate", heap); // NOI18N
        }
        return null;
    }
    
    private String getFileSeparator(Heap heap) {
        if (lastHeapId != System.identityHashCode(heap)) {
            lastSeparator = heap.getSystemProperties().getProperty("file.separator","/"); // NOI18N
        }
        return lastSeparator;
    }
}
