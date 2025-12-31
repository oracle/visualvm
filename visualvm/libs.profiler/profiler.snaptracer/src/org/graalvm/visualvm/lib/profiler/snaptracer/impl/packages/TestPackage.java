/*
 * Copyright (c) 2007, 2025, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.packages;

import org.graalvm.visualvm.lib.profiler.snaptracer.TracerPackage;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProbe;
import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProbeDescriptor;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.IdeSnapshot;

/**
 *
 * @author Jiri Sedlacek
 */
class TestPackage extends TracerPackage {

    private TracerProbeDescriptor descriptor1;
    private TracerProbeDescriptor descriptor2;
    private TracerProbe probe1;
    private TracerProbe probe2;

    private IdeSnapshot snapshot;


    TestPackage(IdeSnapshot snapshot) {
        super("Test Package", "Package for testing purposes", null, 1);
        this.snapshot = snapshot;
    }


    public TracerProbeDescriptor[] getProbeDescriptors() {
        if (snapshot.hasUiGestures()) {
            descriptor1 = new TracerProbeDescriptor("UI Actions", "Shows UI actions performed by the user in the IDE", null, 1, true);
            descriptor2 = new TracerProbeDescriptor("Stack depth", "Reports the cumulative depth of all running threads", null, 2, true);
            return new TracerProbeDescriptor[] { descriptor1, descriptor2, };
        } else {
            descriptor2 = new TracerProbeDescriptor("Stack depth", "Reports the cumulative depth of all running threads", null, 2, true);
            return new TracerProbeDescriptor[] { descriptor2, };
        }
    }

    public TracerProbe getProbe(TracerProbeDescriptor descriptor) {
        if (descriptor == descriptor1) {
            if (probe1 == null) probe1 = new UiGesturesProbe(snapshot);
            return probe1;
        } else if (descriptor == descriptor2) {
            if (probe2 == null) probe2 = new TestProbe(snapshot);
            return probe2;
        } else {
            return null;
        }
    }

}
