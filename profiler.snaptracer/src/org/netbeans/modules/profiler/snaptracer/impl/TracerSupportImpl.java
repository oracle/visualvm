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

package org.netbeans.modules.profiler.snaptracer.impl;

import org.netbeans.modules.profiler.snaptracer.TracerPackage;
import org.netbeans.modules.profiler.snaptracer.TracerPackageProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.profiler.snaptracer.Positionable;
import org.netbeans.modules.profiler.snaptracer.impl.packages.TestPackageProvider;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public final class TracerSupportImpl {

    private static TracerSupportImpl INSTANCE;
    private static RequestProcessor PROCESSOR;

    private final Set<TracerPackageProvider> providers;


    public static synchronized TracerSupportImpl getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TracerSupportImpl();
            PROCESSOR = new RequestProcessor("Tracer Processor", 5); // NOI18N
        }
        return INSTANCE;
    }


    public synchronized void registerPackageProvider(TracerPackageProvider provider) {
        providers.add(provider);
    }

    public synchronized void unregisterPackageProvider(TracerPackageProvider provider) {
        providers.remove(provider);
    }


    public synchronized boolean hasPackages(Object target) {
        for (TracerPackageProvider provider : providers)
            if (provider.getScope().isInstance(target))
                return true;
        return false;
    }

    public synchronized List<TracerPackage> getPackages(IdeSnapshot snapshot) {
        List<TracerPackage> packages = new ArrayList();
        for (TracerPackageProvider provider : providers)
            packages.addAll(Arrays.asList(provider.getPackages(snapshot)));
        Collections.sort(packages, Positionable.COMPARATOR);
        return packages;
    }
    
    
    public void perform(Runnable task) {
        PROCESSOR.post(task);
    }


    private TracerSupportImpl() {
        providers = new HashSet();
        registerPackageProvider(new TestPackageProvider());
    }

}
