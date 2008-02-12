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
package net.java.visualvm.modules.glassfish.explorer;

import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.explorer.ExplorerModelSupport;
import com.sun.tools.visualvm.core.explorer.ExplorerNode;
import com.sun.tools.visualvm.core.explorer.ExplorerNodeBuilder;
import java.util.HashMap;
import java.util.Map;
import net.java.visualvm.modules.glassfish.datasource.GlassFishServlet;
import net.java.visualvm.modules.glassfish.datasource.GlassFishWebModule;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishServletNodeBuilder implements ExplorerNodeBuilder<GlassFishServlet> {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object modelLock = new Object();
    private Map<ExplorerNode<GlassFishWebModule>, GlassFishServletsNode> servletsNodeMap = new  HashMap<ExplorerNode<GlassFishWebModule>, GlassFishServletsNode>();
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public ExplorerNode<GlassFishServlet> getNodeFor(GlassFishServlet dataSource) {
        ExplorerModelSupport modelSupport = ExplorerModelSupport.sharedInstance();
        ExplorerNode<GlassFishWebModule> parent = modelSupport.getNodeFor(dataSource.getOwner());
        GlassFishServletsNode servletsNode = null;

        synchronized (modelLock) {
            if (!servletsNodeMap.containsKey(parent)) {
                servletsNode = new GlassFishServletsNode();
                servletsNodeMap.put(parent, servletsNode);
                modelSupport.addNode(servletsNode, parent);
            } else {
                servletsNode = servletsNodeMap.get(parent);
            }
        }

        final GlassFishServletNode appNode = new GlassFishServletNode(dataSource);
        modelSupport.addNode(appNode, servletsNode);

        dataSource.notifyWhenFinished(new DataFinishedListener() {
                public void dataFinished(Object dataSource) {
                    ExplorerModelSupport.sharedInstance().removeNode(appNode);
                }
            });

        return appNode;
    }

    public void initialize() {
        ExplorerModelSupport.sharedInstance().addBuilder(this, GlassFishServlet.class);
    }
}
