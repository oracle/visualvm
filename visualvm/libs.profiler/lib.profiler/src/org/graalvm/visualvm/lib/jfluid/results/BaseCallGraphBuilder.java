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

package org.graalvm.visualvm.lib.jfluid.results;

import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class BaseCallGraphBuilder implements ProfilingResultListener, CCTProvider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static final Logger LOGGER = Logger.getLogger(BaseCallGraphBuilder.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected List<Runnable> afterBatchCommands = new ArrayList<>();
    protected ProfilingSessionStatus status;
    protected final Set<CCTProvider.Listener> cctListeners = new CopyOnWriteArraySet<>();
    protected WeakReference<ProfilerClient> clientRef;
    protected boolean batchNotEmpty = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BaseCallGraphBuilder */
    public BaseCallGraphBuilder() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void addListener(CCTProvider.Listener listener) {
        cctListeners.add(listener);
    }

    public void onBatchStart() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Starting batch"); // NOI18N
        }

        afterBatchCommands.clear();
        batchNotEmpty = false;
        doBatchStart();
    }

    public void onBatchStop() {
        doBatchStop();

        if (batchNotEmpty) {
            fireCCTEstablished(false);
        } else {
            fireCCTEstablished(true);
        }

        if (!afterBatchCommands.isEmpty()) {
            for (Runnable afterBatchCommand : afterBatchCommands) {
                afterBatchCommand.run();
            }

            afterBatchCommands.clear();
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Finishing batch"); // NOI18N
        }
    }

    public void removeAllListeners() {
        cctListeners.clear();
    }

    public void removeListener(CCTProvider.Listener listener) {
        cctListeners.remove(listener);
    }

    public void reset() {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Resetting CallGraphBuilder"); // NOI18N
        }

        try {
            doReset();
            fireCCTReset();
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
        }
    }

    public void shutdown() {
        status = null;
        afterBatchCommands.clear();
        doShutdown();
    }

    public void startup(ProfilerClient profilerClient) {
        status = profilerClient.getStatus();
        clientRef = new WeakReference<>(profilerClient);
        doStartup(profilerClient);
    }

    protected abstract RuntimeCCTNode getAppRootNode();

    protected abstract void doBatchStart();

    protected abstract void doBatchStop();

    protected abstract void doReset();

    protected abstract void doShutdown();

    protected abstract void doStartup(ProfilerClient profilerClient);

    protected ProfilerClient getClient() {
        if (clientRef == null) {
            return null;
        }

        return clientRef.get();
    }

    private void fireCCTEstablished(boolean empty) {
        RuntimeCCTNode appNode = getAppRootNode();

        if (appNode == null) {
            return;
        }

        for (Listener cctListener : cctListeners) {
            cctListener.cctEstablished(appNode, empty);
        }
    }

    private void fireCCTReset() {
        for (Listener cctListener : cctListeners) {
            cctListener.cctReset();
        }
    }
}
