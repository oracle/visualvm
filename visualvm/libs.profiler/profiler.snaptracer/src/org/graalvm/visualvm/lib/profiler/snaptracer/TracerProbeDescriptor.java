/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.snaptracer;

import javax.swing.Icon;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.snaptracer.impl.icons.TracerIcons;

/**
 * TracerProbeDescriptor defines how a TracerProbe appears in the Tracer UI.
 *
 * @author Jiri Sedlacek
 */
public final class TracerProbeDescriptor implements Positionable {

    private final String name;
    private final String description;
    private final Icon icon;
    private final int preferredPosition;
    private final boolean available;


    /**
     * Creates new instance of TracerProbeDescriptor.
     *
     * @param name probe name
     * @param description probe description
     * @param icon probe icon
     * @param preferredPosition preferred position of the probe in UI
     * @param available availability of the probe in actual context
     */
    public TracerProbeDescriptor(String name, String description, Icon icon,
                                 int preferredPosition, boolean available) {
        this.name = name;
        this.description = description;
        this.icon = icon != null ? icon : Icons.getIcon(TracerIcons.PROBE);
        this.preferredPosition = preferredPosition;
        this.available = available;
    }


    /**
     * Returns probe name.
     *
     * @return probe name
     */
    public String getProbeName() { return name; }

    /**
     * Returns probe description.
     *
     * @return probe description
     */
    public String getProbeDescription() { return description; }

    /**
     * Returns probe icon.
     *
     * @return probe icon
     */
    public Icon getProbeIcon() { return icon; }

    /**
     * Returns preferred position of the probe in UI.
     *
     * @return preferred position of the probe in UI
     */
    public int getPreferredPosition() { return preferredPosition; }

    /**
     * Returns true if the probe is available in current context, false otherwise.
     *
     * @return true if the probe is available in current context, false otherwise
     */
    public boolean isProbeAvailable() { return available; }

}
