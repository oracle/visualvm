/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.attach.providers;

import java.io.IOException;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.profiler.attach.spi.AbstractRemotePackExporter;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "RemotePackExporter_GeneratingRemotePack=Generating Remote Pack to {0}" // NOI18N
})
final public class RemotePackExporter {

    private static final class Singleton {

        private static final RemotePackExporter INSTANCE = new RemotePackExporter();
    }

    public static RemotePackExporter getInstance() {
        return Singleton.INSTANCE;
    }

    private AbstractRemotePackExporter impl = null;

    private RemotePackExporter() {
        impl = Lookup.getDefault().lookup(AbstractRemotePackExporter.class);
    }

    public String export(final String exportPath, final String hostOS, final String jvm) throws IOException {
        if (impl == null) {
            throw new IOException();
        }

        ProgressHandle ph = ProgressHandle.createHandle(
                Bundle.RemotePackExporter_GeneratingRemotePack(impl.getRemotePackPath(exportPath, hostOS)));
        ph.setInitialDelay(500);
        ph.start();
        try {
            return impl.export(exportPath, hostOS, jvm);
        } finally {
            ph.finish();
        }
    }

    public void export(String hostOS, final String jvm) throws IOException {
        export(null, hostOS, jvm);
    }

    public boolean isAvailable() {
        return impl != null;
    }
}
