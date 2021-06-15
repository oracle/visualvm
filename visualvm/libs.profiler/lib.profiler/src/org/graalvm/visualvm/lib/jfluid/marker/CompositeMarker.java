/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.marker;

import org.graalvm.visualvm.lib.jfluid.results.cpu.marking.MarkMapping;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 *
 * @author Jaroslav Bachorik
 */
public class CompositeMarker implements Marker {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Set delegates;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of CompositeMarker */
    public CompositeMarker() {
        delegates = new LinkedHashSet();
    }

    public CompositeMarker(Set markerList) {
        this();
        delegates.addAll(markerList);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public MarkMapping[] getMappings() {
        Set markerMethods = new HashSet();

        for (Iterator iter = delegates.iterator(); iter.hasNext();) {
            Marker delegate = (Marker) iter.next();
            MarkMapping[] mMethods = delegate.getMappings();
            markerMethods.addAll(Arrays.asList(mMethods));
        }

        return (MarkMapping[]) markerMethods.toArray(new MarkMapping[0]);
    }

    public Mark[] getMarks() {
        Set allMarks = new HashSet();

        for (Iterator iter = delegates.iterator(); iter.hasNext();) {
            Marker delegate = (Marker) iter.next();
            Mark[] marks = delegate.getMarks();
            allMarks.addAll(Arrays.asList(marks));
        }
        return (Mark[]) allMarks.toArray(new Mark[0]);
    }

    public void addMarker(Marker marker) {
        if (marker == null) {
            return;
        }

        delegates.add(marker);
    }

    public void addMarkers(Collection markers) {
        if (markers == null) {
            return;
        }

        delegates.addAll(markers);
    }

    public void removeMarker(Marker marker) {
        if (marker == null) {
            return;
        }

        delegates.remove(marker);
    }

    public void removeMarkers(Collection markers) {
        if (markers == null) {
            return;
        }

        delegates.removeAll(markers);
    }
}
