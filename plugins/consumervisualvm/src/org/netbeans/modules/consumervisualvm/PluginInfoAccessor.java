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

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import org.netbeans.modules.consumervisualvm.api.PluginInfo;
import org.openide.filesystems.XMLFileSystem;
import org.openide.modules.ModuleInfo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/** Accessor for non-public methods of FeatureInfo
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public abstract class PluginInfoAccessor {

    public static PluginInfoAccessor DEFAULT;

    protected PluginInfoAccessor () {
        assert DEFAULT == null;
        DEFAULT = this;
    }

    public abstract String getCodeName (PluginInfo info);

    public abstract URL getPluginLayer (PluginInfo info);

    public abstract Internal getInternal (PluginInfo info);

    /** Instance associated with each FeatureInfo, which can hold the
     * internal data needed for it
     */
    public static final class Internal {

        private final PluginInfo info;
        private XMLFileSystem fs;

        public Internal (PluginInfo info) {
            this.info = info;
        }

        synchronized XMLFileSystem getXMLFileSystem () {
            if (fs == null) {
                URL url = DEFAULT.getPluginLayer (info);
                fs = new XMLFileSystem ();
                if (url != null) {
                    try {
                        fs.setXmlUrl (url);
                    } catch (IOException ex) {
                        DecoratedFileSystem.LOG.log (Level.SEVERE, "Cannot parse: " + url, ex);
                        Exceptions.printStackTrace (ex);
                    } catch (PropertyVetoException ex) {
                        DecoratedFileSystem.LOG.log (Level.SEVERE, "Cannot parse: " + url, ex);
                        Exceptions.printStackTrace (ex);
                    }
                }
            }
            return fs;

        }

        boolean isEnabled () {
            String cnb = DEFAULT.getCodeName (info);
            for (ModuleInfo mi : Lookup.getDefault ().lookupAll (ModuleInfo.class)) {
                if (cnb.equals (mi.getCodeNameBase ())) {
                    return mi.isEnabled ();
                }
            }
            return false;
        }
    }
}
