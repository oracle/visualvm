/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.lib.profiler.results.cpu.marking.MarkMapping;
import org.netbeans.lib.profiler.utils.Wildcards;
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

        return (MarkMapping[]) mappings.toArray(new MarkMapping[mappings.size()]);
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
