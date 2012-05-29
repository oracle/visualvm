/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler;

import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import java.awt.*;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import org.netbeans.lib.profiler.common.CommonUtils;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;


/** A panel with mini graphs intended to be displayed in the output area.
 *
 * TODO: support changing layout when docked vertically
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "TelemetryOverviewPanel_TelemetryOverviewAccessDescr=Profiler telemetry overview",
    "LAB_TelemetryOverviewPanelName=VM Telemetry Overview"
})
public final class TelemetryOverviewPanel extends ProfilerTopComponent {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "TelemetryOverviewPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static TelemetryOverviewPanel defaultInstance;
    private static final Image windowIcon = Icons.getImage(ProfilerIcons.WINDOW_TELEMETRY_OVERVIEW);
    private static final String ID = "profiler_to"; // NOI18N // for winsys persistence
    private static final Dimension PREFFERED_SIZE = new Dimension(580, 430);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final MonitoringGraphsPanel graphsPanel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Initializes the Form
     */
    public TelemetryOverviewPanel() {
        setName(Bundle.LAB_TelemetryOverviewPanelName());
        setIcon(windowIcon);
        setToolTipText(Bundle.TelemetryOverviewPanel_TelemetryOverviewAccessDescr());

        graphsPanel = new MonitoringGraphsPanel();
        JScrollPane graphsPanelScroll = new JScrollPane(graphsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        graphsPanelScroll.setBorder(BorderFactory.createEmptyBorder());
        graphsPanelScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        graphsPanelScroll.getHorizontalScrollBar().setUnitIncrement(20);
        graphsPanelScroll.getHorizontalScrollBar().setBlockIncrement(20);
        setLayout(new BorderLayout());
        add(graphsPanelScroll, BorderLayout.CENTER);

        setFocusable(true);
        setRequestFocusEnabled(true);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized TelemetryOverviewPanel getDefault() {
        if (defaultInstance == null) {
            CommonUtils.runInEventDispatchThreadAndWait(new Runnable() {
                public void run() {
                    defaultInstance = (TelemetryOverviewPanel) WindowManager.getDefault().findTopComponent(ID);
                    if (defaultInstance == null) defaultInstance = new TelemetryOverviewPanel();
                }
            });
        }

        return defaultInstance;
    }

    /** Possibly closes the window avoiding unnecessary initialization if not created and displayed yet. */
    public static synchronized void closeIfOpened() {
        CommonUtils.runInEventDispatchThread(new Runnable() {
            public void run() {
                if (defaultInstance != null && defaultInstance.isOpened()) defaultInstance.close();
            }
        });
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    public Dimension getPrefferedSize() {
        return PREFFERED_SIZE;
    }
    
    protected Component defaultFocusOwner() {
        return graphsPanel;
    }

    public boolean needsDocking() {
        return WindowManager.getDefault().findMode(this) == null;
    }

    public void open() {
        if (needsDocking()) { // needs docking

            Mode mode = WindowManager.getDefault().findMode("output"); // NOI18N

            if (mode != null) {
                mode.dockInto(this);
            }
        }

        super.open();
    }

    /**
     * Subclasses are encouraged to override this method to provide preferred value
     * for unique TopComponent Id returned by getID. Returned value is used as starting
     * value for creating unique TopComponent ID.
     * Value should be preferably unique, but need not be.
     */
    protected String preferredID() {
        return ID;
    }
}
