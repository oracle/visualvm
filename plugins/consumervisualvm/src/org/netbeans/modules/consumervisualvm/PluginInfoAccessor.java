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
