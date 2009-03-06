/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk;

import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractButton;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.heapwalk.memorylint.Utils;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.netbeans.modules.profiler.heapwalk.oql.OQLEngine;
import org.netbeans.modules.profiler.heapwalk.oql.OQLException;
import org.netbeans.modules.profiler.heapwalk.oql.ObjectVisitor;
import org.netbeans.modules.profiler.heapwalk.oql.model.ReferenceChain;
import org.netbeans.modules.profiler.heapwalk.oql.model.Snapshot;
import org.netbeans.modules.profiler.heapwalk.ui.OQLControllerUI;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik
 * @author Jiri Sedlacek
 */
public class OQLController extends AbstractTopLevelController
                implements NavigationHistoryManager.NavigationHistoryCapable {
    // -----
    // I18N String constants

    private static final String CANNOT_RESOLVE_CLASS_MSG = NbBundle.getMessage(AnalysisController.class,
            "AnalysisController_CannotResolveClassMsg"); // NOI18N
    private static final String CANNOT_RESOLVE_INSTANCE_MSG = NbBundle.getMessage(AnalysisController.class,
            "AnalysisController_CannotResolveInstanceMsg"); // NOI18N

    private HeapFragmentWalker heapFragmentWalker;

    private ResultsController resultsController;
    private QueryController queryController;
    private SavedController savedController;

    final private AtomicBoolean analysisRunning = new AtomicBoolean(false);
    private OQLEngine engine = null;


    // --- Constructor ---------------------------------------------------------

    public OQLController(HeapFragmentWalker heapFragmentWalker) {
        this.heapFragmentWalker = heapFragmentWalker;

        if (OQLEngine.isOQLSupported()) {
            engine = new OQLEngine(new Snapshot(heapFragmentWalker.getHeapFragment()));

            resultsController = new ResultsController(this);
            queryController = new QueryController(this);
            savedController = new SavedController();
        }
    }


    // --- Public interface ----------------------------------------------------

    public void executeQuery(String query) {
        executeQueryImpl(query, null);
    }

    public void cancelQuery() {
        analysisRunning.compareAndSet(true, false);
    }

    public boolean isQueryRunning() {
        return analysisRunning.get();
    }


    // --- Internal interface --------------------------------------------------
    
    public HeapFragmentWalker getHeapFragmentWalker() {
        return heapFragmentWalker;
    }

    public ResultsController getResultsController() {
        return resultsController;
    }

    public QueryController getQueryController() {
        return queryController;
    }

    public SavedController getSavedController() {
        return savedController;
    }


    // --- AbstractTopLevelController implementation ---------------------------

    protected AbstractButton[] createClientPresenters() {
        return new AbstractButton[] {
            resultsController.getPresenter(),
            queryController.getPresenter(),
            savedController.getPresenter()
        };
    }

    protected AbstractButton createControllerPresenter() {
        return ((OQLControllerUI) getPanel()).getPresenter();
    }

    protected JPanel createControllerUI() {
        return new OQLControllerUI(this);
    }


    // --- NavigationHistoryManager.NavigationHistoryCapable implementation ----

    public NavigationHistoryManager.Configuration getCurrentConfiguration() {
        return new NavigationHistoryManager.Configuration();
    }

    public void configure(NavigationHistoryManager.Configuration configuration) {
        heapFragmentWalker.switchToHistoryOQLView();
    }


    // --- Private implementation ----------------------------------------------

    private void executeQueryImpl(final String oqlQuery, String queryName) {
        final BoundedRangeModel progressModel = new DefaultBoundedRangeModel(0, 10, 0, 100);

        queryController.queryStarted(progressModel, queryName);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BrowserUtils.performTask(new Runnable() {
                    public void run() {
                        final AtomicInteger counter = new AtomicInteger(100);
                        progressModel.setMaximum(100);

                        final StringBuilder sb = new StringBuilder();

                        try {
                            analysisRunning.compareAndSet(false, true);
                            engine.executeQuery(oqlQuery, new ObjectVisitor() {

                                public boolean visit(Object o) {
                                    sb.append("<div>"); // NOI18N
                                    dump(o, sb);
                                    sb.append("</div>"); // NOI18N
                                    int value = progressModel.getValue() + 1;
                                    if (value > progressModel.getMaximum()) {
                                        value = progressModel.getMinimum() + 1;
                                    }
                                    progressModel.setValue(value);
                                    return counter.decrementAndGet() == 0 || !analysisRunning.get(); // process all hits while the analysis is running
                                }
                            });
                            analysisRunning.compareAndSet(true, false);
                            queryController.queryFinished();
                            resultsController.setResult(sb.toString());
                        } catch (OQLException oQLException) {
                            StringBuilder errorMessage = new StringBuilder();
                            errorMessage.append("<h2>").append(NbBundle.getMessage(OQLController.class, "OQL_QUERY_ERROR")).append("</h2>"); // NOI18N
                            errorMessage.append(NbBundle.getMessage(OQLController.class, "OQL_QUERY_PLZ_CHECK")); // NOI18N
                            resultsController.setResult(errorMessage.toString());
                            queryController.queryFinished();
                            cancelQuery();
                        }
                    }
                });
            }
        });
    }

    private void dump(Object o, StringBuilder sb) {
        if (o == null) {
            return;
        }
        if (o instanceof Instance) {
            Instance i = (Instance) o;
            sb.append(printInstance(i));
        } else if (o instanceof JavaClass) {
            JavaClass c = (JavaClass)o;
            sb.append(printClass(c));
        } else if (o instanceof ReferenceChain) {
            ReferenceChain rc = (ReferenceChain) o;
            sb.append("<h4>Reference Chain</h4>");
            while (rc != null) {
                sb.append(printInstance(rc.getObj())).append("&gt;"); // NOI18N
                rc = rc.getNext();
            }
            sb.delete(sb.length() - 5, sb.length());
        } else if (o instanceof Map) {
            Set<Map.Entry> entries = ((Map)o).entrySet();
            sb.append("<span>{"); // NOI18N
            for(Map.Entry entry : entries) {
                dump(entry.getKey(), sb);
                sb.append(" = "); // NOI18N
                dump(entry.getValue(), sb);
                sb.append(", "); // NOI18N
            }
            sb.delete(sb.length() - 2, sb.length());
            sb.append("}</span>"); // NOI18N
        } else {
            sb.append(o.toString());
        }
    }

    private OQLEngine getEngine() {
        return engine;
    }

    private void showURL(URL url) {
        String urls = url.toString();

        if (urls.startsWith("file://instance/")) { // NOI18N
            urls = urls.substring("file://instance/".length()); // NOI18N

            int indexPos = urls.indexOf("#"); // NOI18N
            int pointerPos = urls.indexOf("@"); // NOI18N
            String clzName = null;

            if (indexPos > -1 || pointerPos > -1) {
                clzName = urls.substring(0, Math.max(indexPos, pointerPos));
            }

            Instance i = null;
            String identifier = null;
            if (indexPos > -1) {
                identifier = urls.substring(indexPos + 1);
                JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByName(clzName);

                if (c != null) {
                    List<Instance> instances = c.getInstances();
                    int instanceNumber = Integer.parseInt(identifier);
                    if (instanceNumber <= instances.size()) {
                        i = instances.get(instanceNumber - 1);
                    }
                } else {
                    NetBeansProfiler.getDefaultNB().displayError(MessageFormat.format(CANNOT_RESOLVE_CLASS_MSG, new Object[]{clzName}));
                }
            } else if (pointerPos > -1) {
                identifier = urls.substring(pointerPos + 1);

                i = heapFragmentWalker.getHeapFragment().getInstanceByID(Long.parseLong(identifier));
            }

            if (i != null) {
                heapFragmentWalker.getClassesController().showInstance(i);
            } else {
                NetBeansProfiler.getDefaultNB().displayError(MessageFormat.format(CANNOT_RESOLVE_INSTANCE_MSG,
                        new Object[]{identifier, clzName}));
            }
        } else if (urls.startsWith("file://class/")) { // NOI18N
            urls = urls.substring("file://class/".length()); // NOI18N

            JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByID(Long.parseLong(urls));

            if (c != null) {
                heapFragmentWalker.getClassesController().showClass(c);
            } else {
                NetBeansProfiler.getDefaultNB().displayError(MessageFormat.format(CANNOT_RESOLVE_CLASS_MSG, new Object[]{urls}));
            }
        }
    }

    private static String printClass(JavaClass cls) {
        if (cls == null) {
            return NbBundle.getMessage(Utils.class, "LBL_UnknownClass"); // NOI18N
        }

        String clsName = cls.getName();
        String fullName = clsName; // NOI18N
        String field = ""; // NOI18N

        // now you can wrap it with a/href to given class
        int dotIdx = clsName.lastIndexOf('.'); // NOI18N
        int colonIdx = clsName.lastIndexOf(':'); // NOI18N

        if (colonIdx == -1) {
            colonIdx = clsName.lastIndexOf(';'); // NOI18N
        }

        if (colonIdx > 0) {
            fullName = clsName.substring(0, colonIdx);
            field = "." + clsName.substring(colonIdx + 1); // NOI18N
        }

        String dispName = clsName.substring(dotIdx + 1);

        return "<a href='file://class/" + cls.getJavaClassId() + "'>" + fullName + "</a>" + field; // NOI18N
    }

    private static String printInstance(Instance in) {
        String className = in.getJavaClass().getName();

        return "<a href='file://instance/" + className + "@" + in.getInstanceId() + "'>" + className + '#' + in.getInstanceNumber() + "</a>"; // NOI18N
//        return "<a href='file://instance/" + className + "/" + in.getInstanceNumber() + "'>" + className + '#' + in.getInstanceNumber() + "</a>"; // NOI18N
//        return in.getJavaClass().getName() + '@' + Long.toHexString(in.getInstanceId()) + '#' + in.getInstanceNumber();
    }


    // --- Controllers ---------------------------------------------------------

    public static class ResultsController extends AbstractController {

        private OQLController oqlController;


        public ResultsController(OQLController oqlController) {
            this.oqlController = oqlController;
        }

        public void setResult(String result) {
            ((OQLControllerUI.ResultsUI)getPanel()).setResult(result);
        }

        public void showURL(URL url) {
            oqlController.showURL(url);
        }

        public OQLController getOQLController() {
            return oqlController;
        }

        protected AbstractButton createControllerPresenter() {
            return ((OQLControllerUI.ResultsUI)getPanel()).getPresenter();
        }

        protected JPanel createControllerUI() {
            return new OQLControllerUI.ResultsUI(this);
        }

    }

    
    public static class QueryController extends AbstractController {

        private OQLController oqlController;


        public QueryController(OQLController oqlController) {
            this.oqlController = oqlController;
        }


        public OQLController getOQLController() {
            return oqlController;
        }

        public void executeQuery(String oqlQuery) {
            oqlController.executeQueryImpl(oqlQuery, null);
        }

        public void cancelQuery() {
            oqlController.cancelQuery();
        }

        public void saveQuery(String oqlQuery, String name) {

        }

        
        private void queryStarted(BoundedRangeModel model, String queryName) {
            ((OQLControllerUI.QueryUI)getPanel()).queryStarted(model, queryName);
        }

        private void queryFinished() {
            ((OQLControllerUI.QueryUI)getPanel()).queryFinished();
        }

        protected AbstractButton createControllerPresenter() {
            return ((OQLControllerUI.QueryUI)getPanel()).getPresenter();
        }

        protected JPanel createControllerUI() {
            return new OQLControllerUI.QueryUI(this, oqlController.getEngine());
        }

    }


    public static class SavedController extends AbstractController {

        protected AbstractButton createControllerPresenter() {
            return ((OQLControllerUI.SavedUI)getPanel()).getPresenter();
        }

        protected JPanel createControllerUI() {
            JPanel ui = new OQLControllerUI.SavedUI();
            ui.setVisible(false);
            return ui;
        }

    }

}
