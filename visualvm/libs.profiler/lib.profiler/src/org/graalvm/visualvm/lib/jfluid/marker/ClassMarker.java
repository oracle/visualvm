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

import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.results.cpu.marking.MarkMapping;
import org.graalvm.visualvm.lib.jfluid.utils.Wildcards;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ClassMarker implements Marker {
    private static Logger LOGGER = Logger.getLogger(ClassMarker.class.getName());
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Map markMap;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of ClassMarker */
    public ClassMarker() {
        markMap = new HashMap();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public MarkMapping[] getMappings() {
        List mappings = new ArrayList();

        for (Iterator iter = markMap.keySet().iterator(); iter.hasNext();) {
            String className = (String) iter.next();
            ClientUtils.SourceCodeSelection markerMethod = new ClientUtils.SourceCodeSelection(className, Wildcards.ALLWILDCARD,
                                                                                               ""); // NOI18N
            markerMethod.setMarkerMethod(true);
            mappings.add(new MarkMapping(markerMethod, (Mark) markMap.get(className)));
        }

        return (MarkMapping[]) mappings.toArray(new MarkMapping[0]);
    }

    public Mark[] getMarks() {
        return (Mark[])new HashSet(markMap.values()).toArray(new Mark[0]);
    }

    public void addClassMark(String className, Mark mark) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Marking class " + className + " with " + mark.getId());
        }

        markMap.put(className, mark);
    }

    public void removeClassMark(String className) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Unmarking class " + className);
        }
        markMap.remove(className);
    }

    public void resetClassMarks() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Unmarking all classes");
        }
        markMap.clear();
    }
}
