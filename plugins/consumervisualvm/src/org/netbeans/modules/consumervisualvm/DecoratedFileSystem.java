/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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
