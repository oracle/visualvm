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
