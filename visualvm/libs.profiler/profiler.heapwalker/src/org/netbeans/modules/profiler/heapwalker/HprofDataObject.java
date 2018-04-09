/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalker;

import java.io.IOException;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Node;
import org.openide.nodes.Children;
import org.openide.util.Lookup;

/**
 * HPROF heapdump DataObject
 *
 * @author Tomas Hurka
 */
@DataObject.Registration(
    iconBase = "org/netbeans/modules/profiler/heapwalk/ui/icons/impl/snapshotDataObject.png", 
    mimeType = "application/x-netbeans-profiler-hprof",
    position=10
)
public class HprofDataObject extends MultiDataObject implements OpenCookie {
    
    public HprofDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        
    }
    
    @Override
    protected Node createNodeDelegate() {
        return new DataNode(this, Children.LEAF, getLookup());
    }
    
    @Override
    public Lookup getLookup() {
        return getCookieSet().getLookup();
    }
    
    public void open() {
        final FileObject heapDumpFo = getPrimaryFile();
        BrowserUtils.performTask(new Runnable() {
            public void run() {
                if (heapDumpFo != null) {
                    HeapWalkerManager.getDefault().openHeapWalker(FileUtil.toFile(heapDumpFo));
                }
            }
        });
    }

    @Override
    protected void handleDelete() throws IOException {
        HeapWalkerManager.getDefault().deleteHeapDump(FileUtil.toFile(getPrimaryFile()));
    }
}
