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

package com.sun.tools.visualvm.core.application;

import com.sun.tools.visualvm.core.explorer.ApplicationNode;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.explorer.ExplorerModelSupport;
import com.sun.tools.visualvm.core.explorer.ExplorerNodeBuilder;
import com.sun.tools.visualvm.core.explorer.ExplorerNode;
import com.sun.tools.visualvm.core.model.apptype.ApplicationType;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;

/**
 *
 * @author Jiri Sedlacek
 */
class ApplicationNodeBuilder implements ExplorerNodeBuilder<Application> {

    private final Map<Application, ApplicationNode> nodesCache = Collections.synchronizedMap(new HashMap());


    public synchronized ApplicationNode getNodeFor(Application application) {
        ApplicationNode applicationNode = nodesCache.get(application);
        if (applicationNode == null) applicationNode = createApplicationNode(application);
        return applicationNode;
    }
    
    
    private ApplicationNode createApplicationNode(final Application application) {
        ApplicationType appType = ApplicationTypeFactory.getApplicationTypeFor(application);
        final ApplicationNode applicationNode = appType == null ?
            new ApplicationNode(application) : new ApplicationNode(application, appType.getName(), new ImageIcon(appType.getIcon()));
        final ExplorerModelSupport support = ExplorerModelSupport.sharedInstance();
        final ExplorerNode hostNode = support.getNodeFor(application.getHost());
        support.addNode(applicationNode, hostNode);
        nodesCache.put(application, applicationNode);
        
        application.notifyWhenFinished(new DataFinishedListener() {
            public void dataFinished(Object dataSource) { removeApplicationNode(application); }
        });
        
        return applicationNode;
    }
    
    private void removeApplicationNode(Application application) {
        ApplicationNode node = nodesCache.get(application);
        nodesCache.remove(application);
        ExplorerModelSupport.sharedInstance().removeNode(node);         
    }
    
    
    void initialize() {
        ExplorerModelSupport.sharedInstance().addBuilder(this, Application.class);
    }

}
