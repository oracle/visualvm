/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.snaptracer.impl.icons;

import java.util.Map;
import org.graalvm.visualvm.lib.profiler.spi.IconsProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.spi.IconsProvider.class)
public final class TracerIconsProviderImpl extends IconsProvider.Basic {

    @Override
    protected final void initStaticImages(Map<String, String> cache) {
        cache.put(TracerIcons.INCREMENT, "increment.png"); // NOI18N
        cache.put(TracerIcons.DECREMENT, "decrement.png"); // NOI18N
        cache.put(TracerIcons.RESET, "reset.png"); // NOI18N
        cache.put(TracerIcons.GENERIC_ACTION, "genericAction.png"); // NOI18N
        cache.put(TracerIcons.MOUSE_WHEEL_HORIZONTAL, "hmwheel.png"); // NOI18N
        cache.put(TracerIcons.MOUSE_WHEEL_VERTICAL, "vmwheel.png"); // NOI18N
        cache.put(TracerIcons.MOUSE_WHEEL_ZOOM, "zmwheel.png"); // NOI18N
        cache.put(TracerIcons.MARK, "mark.png"); // NOI18N
        cache.put(TracerIcons.MARK_CLEAR, "markClear.png"); // NOI18N
        cache.put(TracerIcons.MARK_HIGHLIGHT, "markHighl.png"); // NOI18N
        cache.put(TracerIcons.SELECT_ALL, "selectAll.png"); // NOI18N
        cache.put(TracerIcons.PROBE, "probe.png"); // NOI18N
        cache.put(TracerIcons.TRACER, "tracer.png"); // NOI18N
        cache.put(TracerIcons.TRACER_32, "tracer32.png"); // NOI18N
    }

}
