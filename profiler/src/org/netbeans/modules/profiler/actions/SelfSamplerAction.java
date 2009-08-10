/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.actions;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.DataOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.lib.profiler.common.ProfilingSettingsPresets;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.StackTraceSnapshotBuilder;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.ResultsManager;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik, Tomas Hurka
 */
public class SelfSamplerAction extends AbstractAction implements AWTEventListener {
    final private static class Singleton {
        static final SelfSamplerAction INSTANCE = new SelfSamplerAction();
    }

    // -----
    // I18N String constants
    private static final String ACTION_NAME_START = NbBundle.getMessage(SelfSamplerAction.class, "SelfSamplerAction_ActionNameStart");
    private static final String ACTION_NAME_STOP = NbBundle.getMessage(SelfSamplerAction.class, "SelfSamplerAction_ActionNameStop");
//    private static final String ACTION_DESCR = NbBundle.getMessage(SelfSamplerAction.class, "SelfSamplerAction_ActionDescription");
    private static final String THREAD_NAME = NbBundle.getMessage(SelfSamplerAction.class, "SelfSamplerAction_ThreadName");
    private static final String DEBUG_ARG = "-Xdebug"; // NOI18N
    private static final Logger LOGGER = Logger.getLogger(SelfSamplerAction.class.getName());

    private final AtomicReference<Controller> RUNNING = new AtomicReference<Controller>();
    private Boolean runMode;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    private SelfSamplerAction() {
        putValue(Action.NAME, ACTION_NAME_START);
//        putValue(Action.SHORT_DESCRIPTION, ACTION_DESCR);
        putValue(Action.SMALL_ICON,
            ImageUtilities.loadImageIcon(
                "org/netbeans/modules/profiler/actions/resources/openSnapshot.png" //NOI18N
        , false)
        );
        if (System.getProperty(SelfSamplerAction.class.getName() + ".sniff") != null) { //NOI18N
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
        }
    }

    public static final SelfSamplerAction getInstance() {
        return Singleton.INSTANCE;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        Controller c;
        if (RUNNING.compareAndSet(null, c = new Controller(THREAD_NAME))) {
            putValue(Action.NAME, ACTION_NAME_STOP);
            putValue(Action.SMALL_ICON,
                ImageUtilities.loadImageIcon(
                    "org/netbeans/modules/profiler/actions/resources/modifyProfiling.png" //NOI18N
                , false)
            );
            c.run();
        } else if ((c = RUNNING.getAndSet(null)) != null) {
            putValue(Action.NAME, ACTION_NAME_START);
            putValue(Action.SMALL_ICON,
                    ImageUtilities.loadImageIcon(
                    "org/netbeans/modules/profiler/actions/resources/openSnapshot.png" //NOI18N
                    , false)
            );
            c.actionPerformed(new ActionEvent(this, 0, "show")); // NOI18N
        }

    }

    public void eventDispatched(AWTEvent event) {
        KeyEvent kevent = (KeyEvent)event;
        if (kevent.getID() == KeyEvent.KEY_RELEASED && kevent.getKeyCode() == KeyEvent.VK_ALT_GRAPH) { // AltGr
            actionPerformed(new ActionEvent(this, event.getID(), "shortcut")); //NOI18N
            kevent.consume();
        }
    }

    @Override
    public Object getValue(String key) {
        Object o = super.getValue(key);
        if (o == null && key.startsWith("logger-") && isRunMode()) { // NOI18N
            return new Controller(key);
        }
        return o;
    }

    private synchronized boolean isRunMode() {
        if (runMode == null) {
            runMode = Boolean.TRUE;
            String reason = null;

            // check if we are debugged
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            List<String> args = runtime.getInputArguments();
            if (args.contains(DEBUG_ARG)) {
                reason = "running in debug mode";   // NOI18N
                runMode = Boolean.FALSE;
            }
            
            if (!runMode.booleanValue()) {
                LOGGER.log(Level.INFO,"Slowness detector disabled - "+ reason); // NOI18N
            }
        }
        return runMode.booleanValue();
    }


    private static final class Controller implements Runnable, ActionListener {
        private final String name;
        private StackTraceSnapshotBuilder builder;
        private ThreadFactory threadFactory;
        private ScheduledExecutorService executor;
        private long startTime;

        public Controller(String n) {
            name = n;
        }
        /**
         * @return the builder
         */
        private synchronized StackTraceSnapshotBuilder getBuilder() {
            if (builder == null) {
                builder = new StackTraceSnapshotBuilder();
                threadFactory = new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r, name);
                    }
                };
                builder.setIgnoredThreads(Collections.singleton(name));
            }
            return builder;
        }

        public void run() {
            final StackTraceSnapshotBuilder b = getBuilder();
            final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
            startTime = System.currentTimeMillis();
            executor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        b.addStacktrace(threadBean.getThreadInfo(threadBean.getAllThreadIds(),Integer.MAX_VALUE), System.nanoTime());
                    } catch (Throwable ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }, 10, 10, TimeUnit.MILLISECONDS);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                executor.shutdown();
                executor.awaitTermination(100, TimeUnit.MILLISECONDS);
                if ("cancel".equals(e.getActionCommand())) {
                    return;
                }
                CPUResultsSnapshot snapshot = getBuilder().createSnapshot(startTime);
                LoadedSnapshot loadedSnapshot = new LoadedSnapshot(snapshot, ProfilingSettingsPresets.createCPUPreset(), null, null);
                if ("write".equals(e.getActionCommand())) {
                    DataOutputStream dos = (DataOutputStream)e.getSource();
                    loadedSnapshot.save(dos);
                    return;
                }
                loadedSnapshot.setSaved(true);
                ResultsManager.getDefault().openSnapshot(loadedSnapshot);
            } catch (CPUResultsSnapshot.NoDataAvailableException ex) {
                return;
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                getBuilder().reset();
            }
        }
    }
}
