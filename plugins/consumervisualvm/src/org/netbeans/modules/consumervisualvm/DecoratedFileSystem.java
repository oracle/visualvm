/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.consumervisualvm;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.consumervisualvm.PluginInfoAccessor.Internal;
import org.netbeans.modules.consumervisualvm.api.PluginInfo;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.MultiFileSystem;
import org.openide.filesystems.XMLFileSystem;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Jirka Rechtacek
 */
public class DecoratedFileSystem extends MultiFileSystem implements Runnable {

    final static Logger LOG = Logger.getLogger (DecoratedFileSystem.class.getPackage ().toString ());
    private static RequestProcessor RP = new RequestProcessor ("ConsumerVisualVM"); // NOI18N

    private static Lookup consumerVisualVM;

    public DecoratedFileSystem () {
        RP.post (this, 2000);
    //SwingUtilities.invokeLater (this);
    }

    public void run () {
        Lookup l = consumerVisualVM ();
        try {
            Class.forName ("org.netbeans.modules.consumervisualvm.api.PluginInfo");
        } catch (ClassNotFoundException ex) {
            // XXX: why ClassNotFoundException sometime?
            LOG.log (Level.FINE, ex.getLocalizedMessage (), ex);
            return;
        }
        Lookup.Result<PluginInfo> result = l.lookupResult (PluginInfo.class);

        List<XMLFileSystem> delegate = new ArrayList<XMLFileSystem> ();
        for (PluginInfo pi : result.allInstances ()) {
            Internal internal = PluginInfoAccessor.DEFAULT.getInternal (pi);
            if (! internal.isEnabled ()) {
                delegate.add (internal.getXMLFileSystem ());
            }
        }
        setDelegates (delegate.toArray (new FileSystem[0]));
    }

    public static DecoratedFileSystem getInstance () {
        return Lookup.getDefault ().lookup (DecoratedFileSystem.class);
    }

    public void refresh () {
        RP.post (this).waitFinished ();
    }

    public URL getParentFileSystem (FileObject template) {
        Lookup.Result<PluginInfo> result = consumerVisualVM ().lookupResult (PluginInfo.class);

        String path = template.getPath ();
        for (PluginInfo pi : result.allInstances ()) {
            Internal internal = PluginInfoAccessor.DEFAULT.getInternal (pi);
            XMLFileSystem fs = internal.getXMLFileSystem ();
            if (fs.findResource (path) != null) {
                return fs.getXmlUrl ();
            }
        }
        return null;
    }

    public String getPluginCodeName (FileObject template) {
        Lookup.Result<PluginInfo> result = consumerVisualVM ().lookupResult (PluginInfo.class);

        String path = template.getPath ();
        for (PluginInfo pi : result.allInstances ()) {
            Internal internal = PluginInfoAccessor.DEFAULT.getInternal (pi);
            XMLFileSystem fs = internal.getXMLFileSystem ();
            if (fs.findResource (path) != null) {
                return PluginInfoAccessor.DEFAULT.getCodeName (pi);
            }
        }
        return null;
    }

    private static synchronized Lookup consumerVisualVM () {
        if (consumerVisualVM != null) {
            return consumerVisualVM;
        }
        return consumerVisualVM = Lookups.forPath ("ConsumerVisualVM"); // NOI18N

    }
}
