/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.lib.profiler.marker;

import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.lib.profiler.results.cpu.marking.MarkMapping;
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

        return (MarkMapping[]) markerMethods.toArray(new MarkMapping[markerMethods.size()]);
    }

    public Mark[] getMarks() {
        Set allMarks = new HashSet();

        for (Iterator iter = delegates.iterator(); iter.hasNext();) {
            Marker delegate = (Marker) iter.next();
            Mark[] marks = delegate.getMarks();
            allMarks.addAll(Arrays.asList(marks));
        }
        return (Mark[]) allMarks.toArray(new Mark[allMarks.size()]);
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
