/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl.packages;

import java.io.IOException;
import org.netbeans.modules.profiler.snaptracer.ItemValueFormatter;
import org.netbeans.modules.profiler.snaptracer.ProbeItemDescriptor;
import org.netbeans.modules.profiler.snaptracer.TracerProbe;
import org.netbeans.modules.profiler.snaptracer.impl.IdeSnapshot;
import org.openide.util.Exceptions;

/**
 *
 * @author Jiri Sedlacek
 */
class TestProbe extends TracerProbe {
    
//    private int items;
    private IdeSnapshot snapshot;
    
    
    public TestProbe(IdeSnapshot snapshot) {
        super(descriptors(1));
        this.snapshot = snapshot;
//        this.items = items;
    }

    public long[] getItemValues(int sampleIndex) {
        return values(sampleIndex);
    }
    
    
    private static ProbeItemDescriptor[] descriptors(int items) {
        ProbeItemDescriptor[] descriptors = new ProbeItemDescriptor[items];
        descriptors[0] = ProbeItemDescriptor.continuousLineItem("Cumulative stack depth",
                             "Reports the cumulative depth of all running threads", ItemValueFormatter.DEFAULT_DECIMAL);
//        for (int i = 0; i < descriptors.length; i++)
//            descriptors[i] = ProbeItemDescriptor.continuousLineItem("Item " + i,
//                             "Description " + i, ItemValueFormatter.SIMPLE);
        return descriptors;
    }
    
    private long[] values(int sampleIndex) {
        long[] values = new long[1];
        try {
            values[0] = snapshot.getValue(sampleIndex, 0);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
//        for (int i = 0; i < values.length; i++)
//            values[i] = (long)(Math.random() * 10000);
        return values;
    }

}
