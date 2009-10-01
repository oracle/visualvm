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

package org.netbeans.modules.profiler.ppoints.ui;

import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ppoints.CodeProfilingPoint;
import org.netbeans.modules.profiler.ppoints.ProfilingPointsManager;
import org.netbeans.modules.profiler.ppoints.Utils;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;
import java.io.File;


/**
 *
 * @author Jiri Sedlacek
 */
public class ShowOppositeProfilingPointAction extends NodeAction {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class InvocationLocationDescriptor {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private CodeProfilingPoint.Location location;
        private CodeProfilingPoint.Location oppositeLocation;
        private CodeProfilingPoint.Paired profilingPoint;
        private boolean isStartLocation;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public InvocationLocationDescriptor(CodeProfilingPoint.Paired profilingPoint, CodeProfilingPoint.Location location) {
            this.profilingPoint = profilingPoint;
            this.location = location;
            computeOppositeLocation();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public CodeProfilingPoint.Location getLocation() {
            return location;
        }

        public CodeProfilingPoint.Location getOppositeLocation() {
            return oppositeLocation;
        }

        public CodeProfilingPoint.Paired getProfilingPoint() {
            return profilingPoint;
        }

        public boolean isStartLocation() {
            return isStartLocation;
        }

        private void computeOppositeLocation() {
            CodeProfilingPoint.Paired profilingPoint = getProfilingPoint();

            if (profilingPoint == null) {
                return;
            }

            CodeProfilingPoint.Location startLocation = profilingPoint.getStartLocation();

            if (new File(startLocation.getFile()).equals(new File(location.getFile()))
                    && (startLocation.getLine() == location.getLine())) {
                oppositeLocation = profilingPoint.getEndLocation();
                isStartLocation = true;
            } else {
                oppositeLocation = profilingPoint.getStartLocation();
                isStartLocation = false;
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String NO_END_DEFINED_MSG = NbBundle.getMessage(ShowOppositeProfilingPointAction.class,
                                                                         "ShowOppositeProfilingPointAction_NoEndDefinedMsg"); // NOI18N
    private static final String NO_DATA_STRING = NbBundle.getMessage(ShowOppositeProfilingPointAction.class,
                                                                     "ShowOppositeProfilingPointAction_NoDataString"); // NOI18N
    private static final String END_ACTION_NAME = NbBundle.getMessage(ShowOppositeProfilingPointAction.class,
                                                                      "ShowOppositeProfilingPointAction_EndActionName"); // NOI18N
    private static final String START_ACTION_NAME = NbBundle.getMessage(ShowOppositeProfilingPointAction.class,
                                                                        "ShowOppositeProfilingPointAction_StartActionName"); // NOI18N
                                                                                                                             // -----

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ShowOppositeProfilingPointAction() {
        setIcon(null);
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return new HelpCtx(ShowOppositeProfilingPointAction.class);
    }

    public String getName() {
        InvocationLocationDescriptor locationDescriptor = getCurrentLocationDescriptor();

        if (locationDescriptor.getProfilingPoint() == null) {
            return NO_DATA_STRING; // should never happen!
        }

        return locationDescriptor.isStartLocation() ? END_ACTION_NAME : START_ACTION_NAME;
    }

    protected boolean asynchronous() {
        return false;
    }

    protected boolean enable(Node[] node) {
        if (ProfilingPointsManager.getDefault().isProfilingSessionInProgress()) {
            return false;
        }

        return getCurrentLocationDescriptor().getProfilingPoint() != null;
    }

    protected void performAction(Node[] node) {
        InvocationLocationDescriptor locationDescriptor = getCurrentLocationDescriptor();

        if (locationDescriptor.getProfilingPoint() == null) {
            return; // should never happen!
        }

        CodeProfilingPoint.Location oppositeLocation = locationDescriptor.getOppositeLocation();

        if (oppositeLocation != null) {
            Utils.openLocation(oppositeLocation);
        } else {
            NetBeansProfiler.getDefaultNB().displayWarning(NO_END_DEFINED_MSG);
        }
    }

    private InvocationLocationDescriptor getCurrentLocationDescriptor() {
        CodeProfilingPoint.Location currentLocation = Utils.getCurrentLocation(0);
        CodeProfilingPoint[] profilingPointsOnLine = Utils.getProfilingPointsOnLine(currentLocation);

        if (profilingPointsOnLine.length > 0) {
            for (CodeProfilingPoint profilingPoint : profilingPointsOnLine) {
                if (profilingPoint instanceof CodeProfilingPoint.Paired) {
                    return new InvocationLocationDescriptor((CodeProfilingPoint.Paired) profilingPoint, currentLocation);
                }
            }
        }

        return new InvocationLocationDescriptor(null, currentLocation);
    }
}
