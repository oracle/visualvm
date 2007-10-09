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

package org.netbeans.lib.profiler.results.cpu.marking;

import java.util.Arrays;
import java.util.Collection;


/**
 *
 * @author Jaroslav Bachorik
 */
public class Mark implements Cloneable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String DEFAULT_ID = "SELF"; // NOI18N
    public static final Mark DEFAULT = new Mark(DEFAULT_ID, "Uncategorized", true); // NOI18N
    public static final char ID_NONE = (char) 0;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public final String description;
    public final String label;
    public final boolean isDefault;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Mark */
    public Mark(final String label, final String description) {
        this(label, description, false);
    }

    protected Mark(final String label, final String description, final boolean isDefault) {
        this.label = label.intern();
        this.description = description;
        this.isDefault = isDefault;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public Collection getLabels() {
        return Arrays.asList(new String[] { label });
    }

    public Object clone() {
        return new Mark(label, description);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof Mark)) {
            return false;
        }

        return label == ((Mark) other).label;
    }

    public int hashCode() {
        return label.hashCode();
    }
}
