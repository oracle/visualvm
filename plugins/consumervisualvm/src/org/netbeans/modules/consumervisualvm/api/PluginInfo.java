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
package org.netbeans.modules.consumervisualvm.api;

import java.net.URL;
import org.netbeans.modules.consumervisualvm.PluginInfoAccessor;
import org.netbeans.modules.consumervisualvm.PluginInfoAccessor.Internal;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public final class PluginInfo {

    private final String codeName;
    private final URL pluginLayer;
    private Internal internal = new Internal (this);

    private PluginInfo (String codeName, URL pluginLayer) {
        this.codeName = codeName;
        this.pluginLayer = pluginLayer;
    }

    private static PluginInfo create (String codeName, URL pluginLayer) {
        return new PluginInfo (codeName, pluginLayer);
    }

    static PluginInfo create (FileObject fo) {
        Object cnb = fo.getAttribute ("codeName"); // NOI18N
        Object layer = fo.getAttribute ("delegateLayer"); // NOI18N

        return create ((String) cnb, (URL) layer);
    }
    

    static {
        PluginInfoAccessor.DEFAULT = new PluginInfoAccessor () {

            @Override
            public String getCodeName (PluginInfo info) {
                return info.codeName;
            }

            @Override
            public URL getPluginLayer (PluginInfo info) {
                return info.pluginLayer;
            }

            @Override
            public Internal getInternal (PluginInfo info) {
                return info.internal;
            }
        };
    }
}
