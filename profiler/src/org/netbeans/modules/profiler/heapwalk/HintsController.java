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

package org.netbeans.modules.profiler.heapwalk;

import java.awt.Color;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.netbeans.modules.profiler.heapwalk.ui.HintsControllerUI;
import org.openide.util.NbBundle;


/**
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class HintsController extends AbstractController {
    
    private static final String CLASS_URL_PREFIX = "file://class/"; // NOI18N
    private static final String INSTANCE_URL_PREFIX = "file://instance/";   // NOI18N
    // I18N String constants
    private static final String CANNOT_RESOLVE_CLASS_MSG = NbBundle.getMessage(AnalysisController.class,
            "AnalysisController_CannotResolveClassMsg"); // NOI18N
    private static final String CANNOT_RESOLVE_INSTANCE_MSG = NbBundle.getMessage(AnalysisController.class,
            "AnalysisController_CannotResolveInstanceMsg"); // NOI18N
    private static final String NO_DATA_MSG = NbBundle.getMessage(HintsController.class,
            "HintsController_NoData"); // NOI18N
    private static final String CLASS_NAME_MSG = NbBundle.getMessage(HintsController.class,
            "HintsController_ClassName"); // NOI18N
    private static final String RETAINED_SIZE_MSG = NbBundle.getMessage(HintsController.class,
            "HintsController_RetainedSize"); // NOI18N
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private SummaryController summaryController;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    public HintsController(SummaryController summaryController) {
        this.summaryController = summaryController;
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public SummaryController getSummaryController() {
        return summaryController;
    }
    // --- Internal interface ----------------------------------------------------
    
    public void createNavigationHistoryPoint() {
        summaryController.getHeapFragmentWalker().createNavigationHistoryPoint();
    }
    
    public void showURL(URL url) {
        String urls = url.toString();
        HeapFragmentWalker heapFragmentWalker = summaryController.getHeapFragmentWalker();
        
        if (urls.startsWith(INSTANCE_URL_PREFIX)) {
            urls = urls.substring(INSTANCE_URL_PREFIX.length());
            
            String[] id = urls.split("/"); // NOI18N
            JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByName(id[0]);
            
            if (c != null) {
                List<Instance> instances = c.getInstances();
                Instance i = null;
                int instanceNumber = Integer.parseInt(id[1]);
                if (instanceNumber <= instances.size()) i = instances.get(instanceNumber - 1);
                
                if (i != null) {
                    heapFragmentWalker.getClassesController().showInstance(i);
                } else {
                    NetBeansProfiler.getDefaultNB()
                            .displayError(MessageFormat.format(CANNOT_RESOLVE_INSTANCE_MSG,
                            new Object[] { id[1], c.getName() }));
                }
            } else {
                NetBeansProfiler.getDefaultNB()
                        .displayError(MessageFormat.format(CANNOT_RESOLVE_CLASS_MSG, new Object[] { id[0] }));
            }
        } else if (urls.startsWith(CLASS_URL_PREFIX)) {
            urls = urls.substring(CLASS_URL_PREFIX.length());
            
            JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByName(urls);
            
            if (c != null) {
                heapFragmentWalker.getClassesController().showClass(c);
            } else {
                NetBeansProfiler.getDefaultNB().displayError(MessageFormat.format(CANNOT_RESOLVE_CLASS_MSG, new Object[] { urls }));
            }
        }
    }

    public void computeBiggestObjects(final int number) {
        BrowserUtils.performTask(new Runnable() {
            public void run() {
                int retainedSizesState = getSummaryController().getHeapFragmentWalker().
                                         computeRetainedSizes(false);

                final String result = retainedSizesState == HeapFragmentWalker.
                                      RETAINED_SIZES_COMPUTED ?
                                      findBiggestObjects(number) : NO_DATA_MSG;

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ((HintsControllerUI)getPanel()).setResult(result);
                    }
                });
            }
        });
    }
    
    private String findBiggestObjects(int number) {
        Heap heap = getSummaryController().getHeapFragmentWalker().getHeapFragment();
        List<Instance> bigObjects = heap.getBiggestObjectsByRetainedSize(number);
        StringBuffer output = new StringBuffer();
        JavaClass java_lang_Class = heap.getJavaClassByName(Class.class.getName());
        NumberFormat formatter =  NumberFormat.getInstance();

        final boolean[] oddRow = new boolean[1];
        Color oddRowBackground = UIUtils.getDarker(
                        UIUtils.getProfilerResultsBackground());
        final String oddRowBackgroundString =
                "rgb(" + oddRowBackground.getRed() + "," + //NOI18N
                         oddRowBackground.getGreen() + "," + //NOI18N
                         oddRowBackground.getBlue() + ")"; //NOI18N
        
        output.append("<table border='0' width='100%'>");  // NOI18N
        output.append("<tr style='background-color:");  // NOI18N
        output.append(oddRowBackgroundString + ";'>");  // NOI18N
        addHeading(output, CLASS_NAME_MSG);
        addHeading(output, RETAINED_SIZE_MSG);
        output.append("</tr>"); // NOI18N
        for(Instance in : bigObjects) {
            output.append(oddRow[0] ? "<tr style='background-color: " + // NOI18N
                                      oddRowBackgroundString + ";'>" :  // NOI18N
                                      "<tr>");  // NOI18N
            if (in.getJavaClass().equals(java_lang_Class)) {
                JavaClass javaClass = heap.getJavaClassByID(in.getInstanceId());
                addCell(output,printClass(javaClass), false);
            } else {
                addCell(output,printInstance(in), false);
            }
            addCell(output,formatter.format(in.getRetainedSize()), true);
            output.append("</tr>");   // NOI18N
            oddRow[0] = !oddRow[0];
        }
        output.append("</table>");   // NOI18N
        return output.toString();
    }
    
    // --- Private implementation ------------------------------------------------
    
    protected AbstractButton createControllerPresenter() {
        return ((HintsControllerUI) getPanel()).getPresenter();
    }
    
    private void addHeading(StringBuffer output,String text) {
        addTag(output,text,"th", false);   // NOI18N
    }

    private void addCell(StringBuffer output,String text, boolean ralign) {
        addTag(output,text,"td", ralign);   // NOI18N
    }

    private void addTag(StringBuffer output,String text,String tag, boolean ralign) {
        output.append("<" + tag + (ralign ? " style='text-align: right;'>" : ">"));   // NOI18N
        output.append(text);
        output.append("</" + tag + ">");   // NOI18N
    }
    
    private String printInstance(Instance in) {
        String className = in.getJavaClass().getName();
        return "<a href='" + INSTANCE_URL_PREFIX + className + "/" + in.getInstanceNumber() + "'>" + className + '#' + in.getInstanceNumber() + "</a>"; // NOI18N
    }
    
    private String printClass(JavaClass jcls) {
        String className = jcls.getName();
        return "<a href='" + CLASS_URL_PREFIX + className + "'>class " + className + "</a>"; // NOI18N
    }
    
    
    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new HintsControllerUI(this);
    }
    
}
