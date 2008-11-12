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

package org.netbeans.modules.profiler.heapwalk;

import java.io.StringWriter;
import java.lang.Thread.State;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import org.netbeans.api.project.Project;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.ThreadObjectGCRoot;
import org.netbeans.modules.profiler.heapwalk.ui.ThreadsControllerUI;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 *
 * @author Jiri Sedlacek
 */
public class ThreadsController extends AbstractTopLevelController implements NavigationHistoryManager.NavigationHistoryCapable {
    
    private static final String THREADS_URL_PREFIX = "file://stackframe/";     // NOI18N 
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    
    public static class Configuration extends NavigationHistoryManager.Configuration {
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private HeapFragmentWalker heapFragmentWalker;
    private String stackTrace;
    private Project project;
    
    // --- Constructors ----------------------------------------------------------
    public ThreadsController(HeapFragmentWalker heapFragmentWalker) {
        this.heapFragmentWalker = heapFragmentWalker;
        project = heapFragmentWalker.getHeapDumpProject();
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized String getStackTrace() {
        if(stackTrace == null) {
            StringWriter sw = new StringWriter();
            Heap h = heapFragmentWalker.getHeapFragment();
            Collection<GCRoot> roots = h.getGCRoots();
            sw.append("<pre>");
            for (GCRoot root : roots) {
                if(root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                    ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                    Instance threadInstance = threadRoot.getInstance();
                    PrimitiveArrayInstance chars = (PrimitiveArrayInstance)threadInstance.getValueOfField("name");  // NOI18N
                    List<String> charsList = chars.getValues();
                    char charArr[] = new char[charsList.size()];
                    int j = 0;
                    for(String ch: charsList) {
                        charArr[j++] = ch.charAt(0);
                    }                    
                    String threadName = new String(charArr);
                    Boolean daemon = (Boolean)threadInstance.getValueOfField("daemon"); // NOI18N
                    Integer priority = (Integer)threadInstance.getValueOfField("priority"); // NOI18N
                    Long threadId = (Long)threadInstance.getValueOfField("tid");    // NOI18N
                    Integer threadStatus = (Integer)threadInstance.getValueOfField("threadStatus"); // NOI18N
                    State tState = sun.misc.VM.toThreadState(threadStatus.intValue());
                    StackTraceElement stack[] = threadRoot.getStackTrace();
                    sw.append("<span style=\"color: #0033CC\">");   // NOI18N
                    sw.append("\""+threadName+"\""+(daemon.booleanValue() ? " daemon" : "")+" prio="+priority+" tid="+threadId+" "+tState);    // NOI18N
                    sw.append("</span><br>");   // NOI18N
                    if(stack != null) {
                        for(int i = 0; i < stack.length; i++) {
                            String stackElHref;
                            StackTraceElement stackElement = stack[i];

                            if (project != null) {
                                String className = stackElement.getClassName();
                                String method = stackElement.getMethodName();
                                int lineNo = stackElement.getLineNumber();
                                String stackUrl = THREADS_URL_PREFIX+className+"|"+method+"|"+lineNo;

                                stackElHref = "<a style=\"color: #CC3300;\" href=\""+stackUrl+"\">"+stackElement+"</a>";    // NOI18N
                            } else {
                                stackElHref = stackElement.toString();
                            }
                            sw.append("\tat "+stackElHref+"<br>");  // NOI18N
                        }
                    }
                    sw.append("<br>");  // NOI18N
                }
            }
            sw.append("</pre>");
            stackTrace = sw.toString();
        }
        return stackTrace;
    }
    
    
    // --- NavigationHistoryManager.NavigationHistoryCapable implementation ------
    
    public Configuration getCurrentConfiguration() {
        return new Configuration();
    }
    
    public void configure(NavigationHistoryManager.Configuration configuration) {
        if (configuration instanceof Configuration) {
            //            Configuration c = (Configuration) configuration;
            heapFragmentWalker.switchToHistoryThreadsView();
        } else {
            throw new IllegalArgumentException("Unsupported configuration: " + configuration); // NOI18N
        }
    }
    
    
    protected AbstractButton[] createClientPresenters() {
        return new AbstractButton[] {};
    }
    
    protected AbstractButton createControllerPresenter() {
        return ((ThreadsControllerUI) getPanel()).getPresenter();
    }
    
    public void showURL(URL url) {
        String urls = url.toString();

        if (urls.startsWith(THREADS_URL_PREFIX)) { 
            urls = urls.substring(THREADS_URL_PREFIX.length());
            String parts[] = urls.split("\\|");
            String className = parts[0];
            String method = parts[1];
            int linenumber = Integer.parseInt(parts[2]);
            SourceUtils.openSource(project,className,method,null);
        }        
    }
    
    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new ThreadsControllerUI(this);
    }
}
