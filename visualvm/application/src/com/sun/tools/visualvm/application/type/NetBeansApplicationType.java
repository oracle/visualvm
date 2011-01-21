/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.application.type;

import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.Application;
import java.awt.Image;
import java.util.Iterator;
import java.util.Set;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import static com.sun.tools.visualvm.application.type.NetBeansApplicationTypeFactory.NB_CLUSTER;

/**
 * This {@link ApplicationType} represents NetBeans application from version 4.0.
 * @author Tomas Hurka
 */
public class NetBeansApplicationType extends ApplicationType {
    Application application;
    String name;
    Set<String> clusters;
    
    NetBeansApplicationType(Application app,Jvm jvm,Set<String> cls) {
        application = app;
        clusters = cls;
    }
    
    /**
     * Returns set of BetBeans' clusters.
     *
     */
    public Set<String> getClusters() {
        return clusters;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "NetBeans " + getVersion();    // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public String getVersion() {
        Iterator<String> clIt = getClusters().iterator();
        while(clIt.hasNext()) {
            String cluster = clIt.next();
            if (cluster.startsWith(NB_CLUSTER)) {
                String ver = cluster.substring(NB_CLUSTER.length());
                
                if (ver.length()>0 && Character.isDigit(ver.charAt(0))) {
                    return ver;
                }
            }
            if (cluster.equals(NB_CLUSTER)) {
                return "6.9+";  // NOI18N
            }
        }
        return NbBundle.getMessage(NetBeansApplicationType.class, "LBL_Unknown");   // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return NbBundle.getMessage(NetBeansApplicationType.class, "DESCR_NetBeansApplicationType");   // NOI18N
    }
    
    /**
     * {@inheritDoc}
     */
    public Image getIcon() {
        String iconPath = "com/sun/tools/visualvm/application/type/resources/NetBeans.png"; // NOI18N
        return ImageUtilities.loadImage(iconPath, true);
    }
}
