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

//import com.sun.tools.visualvm.core.datasource.Application;
//import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
//import com.sun.tools.visualvm.core.explorer.ExplorerModelSupport;
//import com.sun.tools.visualvm.core.explorer.ExplorerNode;
//import com.sun.tools.visualvm.core.explorer.ExplorerNodeBuilder;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import net.java.visualvm.modules.glassfish.datasource.GlassFishRoot;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishModelNodeBuilder/* implements ExplorerNodeBuilder<GlassFishRoot> */{
//    private final Map<GlassFishRoot, GlassFishModelNode> nodesCache = new ConcurrentHashMap<GlassFishRoot, GlassFishModelNode>();
//    
//    public ExplorerNode<GlassFishRoot> getNodeFor(GlassFishRoot dataSource) {
//        if (nodesCache.containsKey(dataSource)) {
//            return nodesCache.get(dataSource);
//        }
//        final GlassFishModelNode newNode = new GlassFishModelNode(dataSource);
//        ExplorerModelSupport modelSupport = ExplorerModelSupport.sharedInstance();
//        ExplorerNode<Application> parent = modelSupport.getNodeFor(dataSource.getApplication());
//        modelSupport.addNode(newNode, parent);
//        nodesCache.put(dataSource, newNode);
//        
//        dataSource.notifyWhenFinished(new DataFinishedListener() {
//            public void dataFinished(Object dataSource) { 
//                nodesCache.remove(dataSource);
//                ExplorerModelSupport.sharedInstance().removeNode(newNode);   
//            }
//        });
//        
//        return newNode;
//    }
//    
//    public void initialize() {
//        ExplorerModelSupport.sharedInstance().addBuilder(this, GlassFishRoot.class);
//    }

}
