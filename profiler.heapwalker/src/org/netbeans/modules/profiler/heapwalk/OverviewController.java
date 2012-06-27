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

package org.netbeans.modules.profiler.heapwalk;

import java.io.File;
import java.lang.Thread.State;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaFrameGCRoot;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.ThreadObjectGCRoot;
import org.netbeans.modules.profiler.heapwalk.ui.OverviewControllerUI;
import javax.swing.AbstractButton;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapSummary;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.api.GoToSource;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.heapwalk.ui.icons.HeapWalkerIcons;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
@NbBundle.Messages({
    "OverviewController_NotAvailableMsg=<not available>",
    "OverviewController_SystemPropertiesString=System properties:",
    "OverviewController_SummaryString=Basic info:",
    "OverviewController_EnvironmentString=Environment:",
    "OverviewController_FileItemString=<b>File: </b>{0}",
    "OverviewController_FileSizeItemString=<b>File size: </b>{0}",
    "OverviewController_DateTakenItemString=<b>Date taken: </b>{0}",
    "OverviewController_TotalBytesItemString=<b>Total bytes: </b>{0}",
    "OverviewController_TotalClassesItemString=<b>Total classes: </b>{0}",
    "OverviewController_TotalInstancesItemString=<b>Total instances: </b>{0}",
    "OverviewController_ClassloadersItemString=<b>Classloaders: </b>{0}",
    "OverviewController_GcRootsItemString=<b>GC roots: </b>{0}",
    "OverviewController_FinalizersItemString=<b>Number of objects pending for finalization: </b>{0}",
    "OverviewController_OOMELabelString=<b>Heap dumped on OutOfMemoryError exception</b>",
    "OverviewController_OOMEItemString=<b>Thread causing OutOfMemoryError exception: </b>{0}",
    "OverviewController_OsItemString=<b>OS: </b>{0} ({1}) {2}",
    "OverviewController_ArchitectureItemString=<b>Architecture: </b>{0} {1}",
    "OverviewController_JavaHomeItemString=<b>Java Home: </b>{0}",
    "OverviewController_JavaVersionItemString=<b>Java Version: </b>{0}",
    "OverviewController_JavaVendorItemString=<b>Java Vendor: </b>{0}",
    "OverviewController_JvmItemString=<b>JVM: </b>{0}  ({1}, {2})",
    "OverviewController_ShowSysPropsLinkString=Show System Properties",
    "OverviewController_ThreadsString=Threads at the heap dump:",
    "OverviewController_ShowThreadsLinkString=Show Threads"
})
public class OverviewController extends AbstractController {

    public static final String SHOW_SYSPROPS_URL = "file:/sysprops"; // NOI18N
    public static final String SHOW_THREADS_URL = "file:/threads"; // NOI18N
    private static final String OPEN_THREADS_URL = "file:/stackframe/";     // NOI18N
    private static final String CLASS_URL_PREFIX = "file://class/"; // NOI18N
    private static final String INSTANCE_URL_PREFIX = "file://instance/";   // NOI18N
    private static final String THREAD_URL_PREFIX = "file://thread/";   // NOI18N
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private HeapFragmentWalker heapFragmentWalker;
    private SummaryController summaryController;
    private boolean systemPropertiesComputed = false;
    private Properties systemProperties;
    private String stackTrace;
    private JavaClass java_lang_Class;
    private ThreadObjectGCRoot oome;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public OverviewController(SummaryController summaryController) {
        this.summaryController = summaryController;
        heapFragmentWalker = summaryController.getHeapFragmentWalker();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public SummaryController getSummaryController() {
        return summaryController;
    }

    // --- Internal interface ----------------------------------------------------

    protected AbstractButton createControllerPresenter() {
        return ((OverviewControllerUI) getPanel()).getPresenter();
    }

    // --- Protected implementation ----------------------------------------------
    protected JPanel createControllerUI() {
        return new OverviewControllerUI(this);
    }

    void showInThreads(Instance instance) {
        ((OverviewControllerUI) getPanel()).showInThreads(instance);
    }
    
    public String computeSummary() {
        File file = heapFragmentWalker.getHeapDumpFile();
        Heap heap = heapFragmentWalker.getHeapFragment();
        HeapSummary hsummary = heap.getSummary();
        long finalizers = computeFinalizers(heap);
        int nclassloaders = 0;
        JavaClass cl = heap.getJavaClassByName("java.lang.ClassLoader"); // NOI18N
        NumberFormat numberFormat = (NumberFormat)NumberFormat.getInstance().clone();
        numberFormat.setMaximumFractionDigits(1);
        
        oome = getOOMEThread(heap);
        if (cl != null) {
            nclassloaders = cl.getInstancesCount();
            
            Collection<JavaClass> jcs = cl.getSubClasses();
            
            for (JavaClass jc : jcs) {
                nclassloaders += jc.getInstancesCount();
            }
        }
        
        String filename = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_FileItemString(
                    file != null && file.exists() ? file.getAbsolutePath() : 
                        Bundle.OverviewController_NotAvailableMsg());
        
        String filesize = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_FileSizeItemString(
                    file != null && file.exists() ?
                        numberFormat.format(file.length()/(1024 * 1024.0)) + " MB" : // NOI18N
                        Bundle.OverviewController_NotAvailableMsg());
        
        String dateTaken = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_DateTakenItemString(new Date(hsummary.getTime()).toString());
        
        String liveBytes = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_TotalBytesItemString(numberFormat.format(hsummary.getTotalLiveBytes()));
        
        String liveClasses = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_TotalClassesItemString(numberFormat.format(heap.getAllClasses().size()));
        
        String liveInstances = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_TotalInstancesItemString(numberFormat.format(hsummary.getTotalLiveInstances()));
        
        String classloaders = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_ClassloadersItemString(numberFormat.format(nclassloaders));
        
        String gcroots = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_GcRootsItemString(numberFormat.format(heap.getGCRoots().size()));
        
        String finalizersInfo = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_FinalizersItemString(numberFormat.format(finalizers));

        String oomeString = "";
        if (oome != null) {
            Instance thread = oome.getInstance();
            String threadName = htmlize(getThreadName(thread));
            String threadUrl = "<a href='"+ THREAD_URL_PREFIX + thread.getJavaClass().getName() + "/" + thread.getInstanceId() + "'>" + threadName + "</a>"; // NOI18N
            oomeString = "<br><br>&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_OOMELabelString() + "<br>&nbsp;&nbsp;&nbsp;&nbsp;"
                + Bundle.OverviewController_OOMEItemString(threadUrl);
        }
        String memoryRes = Icons.getResource(ProfilerIcons.HEAP_DUMP);
        return "<b><img border='0' align='bottom' src='nbresloc:/" + memoryRes + "'>&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_SummaryString() + "</b><br><hr>" + dateTaken + "<br>" + filename + "<br>" + filesize + "<br><br>" + liveBytes // NOI18N
                + "<br>" + liveClasses + "<br>" + liveInstances + "<br>" + classloaders + "<br>" + gcroots + "<br>" + finalizersInfo + oomeString; // NOI18N
    }
    
    public String computeEnvironment() {
        Properties sysprops = getSystemProperties();
        
        if (sysprops == null) {
            return Bundle.OverviewController_NotAvailableMsg();
        }
        
        String patchLevel = sysprops.getProperty("sun.os.patch.level", ""); // NOI18N
        String os = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_OsItemString(
                    sysprops.getProperty("os.name", Bundle.OverviewController_NotAvailableMsg()), // NOI18N
                    sysprops.getProperty("os.version", ""), // NOI18N
                    ("unknown".equals(patchLevel) ? "" : patchLevel) // NOI18N
        );
        
        String arch = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_ArchitectureItemString(
                    sysprops.getProperty("os.arch", Bundle.OverviewController_NotAvailableMsg()), // NOI18N
                    sysprops.getProperty("sun.arch.data.model", "?") + "bit" // NOI18N
        );
        
        String jdk = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_JavaHomeItemString(
                    sysprops.getProperty("java.home", Bundle.OverviewController_NotAvailableMsg())); // NOI18N

        String version = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_JavaVersionItemString(
                    sysprops.getProperty("java.version", Bundle.OverviewController_NotAvailableMsg())); // NOI18N
        
        String jvm = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_JvmItemString(
                    sysprops.getProperty("java.vm.name", Bundle.OverviewController_NotAvailableMsg()), // NOI18N
                    sysprops.getProperty("java.vm.version", ""), // NOI18N
                    sysprops.getProperty("java.vm.info", "") // NOI18N
        );

        String vendor = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_JavaVendorItemString(
                    sysprops.getProperty("java.vendor", Bundle.OverviewController_NotAvailableMsg())); // NOI18N
        
        String sysinfoRes = Icons.getResource(HeapWalkerIcons.SYSTEM_INFO);
        return "<b><img border='0' align='bottom' src='nbresloc:/" + sysinfoRes + "'>&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_EnvironmentString() + "</b><br><hr>" + os + "<br>" + arch + "<br>" + jdk + "<br>" + version + "<br>" + jvm + "<br>" + vendor; // NOI18N
    }
    
    public String computeSystemProperties(boolean showSystemProperties) {
        Properties sysprops = getSystemProperties();
        
        if (sysprops == null) {
            return Bundle.OverviewController_NotAvailableMsg();
        }
        
        String propertiesRes = Icons.getResource(HeapWalkerIcons.PROPERTIES);
        return "<b><img border='0' align='bottom' src='nbresloc:/" + propertiesRes + "'>&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_SystemPropertiesString() + "</b><br><hr>" // NOI18N
                + (showSystemProperties ? formatSystemProperties(sysprops)
                : ("&nbsp;&nbsp;&nbsp;&nbsp;<a href='" + SHOW_SYSPROPS_URL + "'>" + Bundle.OverviewController_ShowSysPropsLinkString() + "</a>")); // NOI18N
    }
    
    public String computeThreads(boolean showThreads) {
        String threadsWindowRes = Icons.getResource(ProfilerIcons.WINDOW_THREADS);
        return "<b><img border='0' align='bottom' src='nbresloc:/" + threadsWindowRes + "'>&nbsp;&nbsp;" // NOI18N
                + Bundle.OverviewController_ThreadsString() + "</b><br><hr>" // NOI18N
                + (showThreads ? getStackTrace()
                : ("&nbsp;&nbsp;&nbsp;&nbsp;<a href='" + SHOW_THREADS_URL + "'>" + Bundle.OverviewController_ShowThreadsLinkString() + "</a><br>&nbsp;")); // NOI18N
        // NOTE: the above HTML string should be terminated by newline to workaround HTML rendering bug in JDK 5, see Issue 120157
    }
    
    public void showURL(String urls) {
        if (urls.startsWith(OPEN_THREADS_URL)) {
            urls = urls.substring(OPEN_THREADS_URL.length());
            String parts[] = urls.split("\\|"); // NOI18N
            String className = parts[0];
            String method = parts[1];
            int linenumber = Integer.parseInt(parts[2]);
            GoToSource.openSource(heapFragmentWalker.getHeapDumpProject(), className, method, linenumber);
        } else if (urls.startsWith(INSTANCE_URL_PREFIX)) {
            urls = urls.substring(INSTANCE_URL_PREFIX.length());

            String[] id = urls.split("/"); // NOI18N
            long instanceId = Long.parseLong(id[2]);
            Instance i = heapFragmentWalker.getHeapFragment().getInstanceByID(instanceId);

            if (i != null) {
                heapFragmentWalker.getClassesController().showInstance(i);
            } else {
                ProfilerDialogs.displayError(Bundle.AnalysisController_CannotResolveInstanceMsg(id[1], id[0]));
            }
        } else if (urls.startsWith(CLASS_URL_PREFIX)) {
            urls = urls.substring(CLASS_URL_PREFIX.length());
            String[] id = urls.split("/"); // NOI18N
            long jclsid = Long.parseLong(id[1]);

            JavaClass c = heapFragmentWalker.getHeapFragment().getJavaClassByID(jclsid);

            if (c != null) {
                heapFragmentWalker.getClassesController().showClass(c);
            } else {
                ProfilerDialogs.displayError(Bundle.AnalysisController_CannotResolveClassMsg(id[0]));
            }
        }   else if (urls.startsWith(THREAD_URL_PREFIX)) {
            urls = urls.substring(THREAD_URL_PREFIX.length());
            String[] id = urls.split("/"); // NOI18N
            long threadid = Long.parseLong(id[1]);
            
            showInThreads(heapFragmentWalker.getHeapFragment().getInstanceByID(threadid));
        } 
 
    }
            
    private long computeFinalizers(Heap heap) {
        JavaClass finalizerClass = heap.getJavaClassByName("java.lang.ref.Finalizer"); // NOI18N
        if (finalizerClass != null) {
            Instance queue = (Instance) finalizerClass.getValueOfStaticField("queue"); // NOI18N
            if (queue != null) {
                Long len = (Long) queue.getValueOfField("queueLength"); // NOI18N
                if (len != null) {
                    return len.longValue();
                }
            }
        }
        return -1;
    }
    
    private ThreadObjectGCRoot getOOMEThread(Heap heap) {
        Collection<GCRoot> roots = heap.getGCRoots();

        for (GCRoot root : roots) {
            if(root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                StackTraceElement[] stackTrace = threadRoot.getStackTrace();
                
                if (stackTrace!=null && stackTrace.length>=1) {
                    StackTraceElement ste = stackTrace[0];
                    
                    if (OutOfMemoryError.class.getName().equals(ste.getClassName()) && "<init>".equals(ste.getMethodName())) {
                        return threadRoot;
                    }
                }
            }
        }
        return null;
    }
    
    private Properties getSystemProperties() {
        if (!systemPropertiesComputed) {
            systemProperties = heapFragmentWalker.getHeapFragment().getSystemProperties();
            systemPropertiesComputed = true;
        }
        
        return systemProperties;
    }
    
    private String formatSystemProperties(Properties properties) {
        StringBuilder text = new StringBuilder(200);
        List keys = new ArrayList();
        Enumeration en = properties.propertyNames();
        Iterator keyIt;
        
        while (en.hasMoreElements()) {
            keys.add(en.nextElement());
        }
        Collections.sort(keys);
        keyIt = keys.iterator();
        
        while (keyIt.hasNext()) {
            String key = (String) keyIt.next();
            String val = properties.getProperty(key);
            
            if ("line.separator".equals(key) && val != null) {  // NOI18N
                val = val.replace("\n", "\\n"); // NOI18N
                val = val.replace("\r", "\\r"); // NOI18N
            }
            
            text.append("<nobr>&nbsp;&nbsp;&nbsp;&nbsp;<b>"); // NOI18N
            text.append(key);
            text.append("</b>="); // NOI18N
            text.append(val);
            text.append("</nobr><br>"); // NOI18N
        }
        
        return text.toString();
    }
    
    private synchronized String getStackTrace() {
        if(stackTrace == null) {
            boolean gotoSourceAvailable = heapFragmentWalker.getHeapDumpProject() != null && GoToSource.isAvailable();
            StringBuilder sb = new StringBuilder();
            Heap h = heapFragmentWalker.getHeapFragment();
            Collection<GCRoot> roots = h.getGCRoots();
            Map<ThreadObjectGCRoot,Map<Integer,List<JavaFrameGCRoot>>> javaFrameMap = computeJavaFrameMap(roots);
            // Use this to enable VisualVM color scheme for threads dumps:
            // sw.append("<pre style='color: #cc3300;'>"); // NOI18N
            sb.append("<pre>"); // NOI18N
            for (GCRoot root : roots) {
                if(root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                    ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                    Instance threadInstance = threadRoot.getInstance();
                    if (threadInstance != null) {
                        String threadName = getThreadName(threadInstance);
                        Boolean daemon = (Boolean)threadInstance.getValueOfField("daemon"); // NOI18N
                        Integer priority = (Integer)threadInstance.getValueOfField("priority"); // NOI18N
                        Long threadId = (Long)threadInstance.getValueOfField("tid");    // NOI18N
                        Integer threadStatus = (Integer)threadInstance.getValueOfField("threadStatus"); // NOI18N
                        StackTraceElement stack[] = threadRoot.getStackTrace();
                        Map<Integer,List<JavaFrameGCRoot>> localsMap = javaFrameMap.get(threadRoot);
                        String style="";

                        if (threadRoot.equals(oome)) {
                            style="style=\"color: #FF0000\"";
                        }                        
                        // --- Use this to enable VisualVM color scheme for threads dumps: ---
                        // sw.append("&nbsp;&nbsp;<span style=\"color: #0033CC\">"); // NOI18N
                        sb.append("&nbsp;&nbsp;<a name=").append(threadInstance.getInstanceId()).append("></a><b ").append(style).append(">");   // NOI18N
                        // -------------------------------------------------------------------
                        sb.append("\"").append(htmlize(threadName)).append("\"").append(daemon.booleanValue() ? " daemon" : "").append(" prio=").append(priority);   // NOI18N
                        if (threadId != null) {
                            sb.append(" tid=").append(threadId);    // NOI18N
                        }
                        if (threadStatus != null) {
                            State tState = sun.misc.VM.toThreadState(threadStatus.intValue());
                            sb.append(" ").append(tState);          // NOI18N
                        }
                        // --- Use this to enable VisualVM color scheme for threads dumps: ---
                        // sw.append("</span><br>"); // NOI18N
                        sb.append("</b><br>");   // NOI18N
                        // -------------------------------------------------------------------
                        if(stack != null) {
                            for(int i = 0; i < stack.length; i++) {
                                String stackElHref;
                                StackTraceElement stackElement = stack[i];
                                String stackElementText = htmlize(stackElement.toString());
                                
                                if (gotoSourceAvailable) {
                                    String className = stackElement.getClassName();
                                    String method = stackElement.getMethodName();
                                    int lineNo = stackElement.getLineNumber();
                                    String stackUrl = OPEN_THREADS_URL+className+"|"+method+"|"+lineNo; // NOI18N
                                    
                                    // --- Use this to enable VisualVM color scheme for threads dumps: ---
                                    // stackElHref = "&nbsp;&nbsp;<a style=\"color: #CC3300;\" href=\""+stackUrl+"\">"+stackElement+"</a>"; // NOI18N
                                    stackElHref = "<a href=\""+stackUrl+"\">"+stackElementText+"</a>";    // NOI18N
                                    // -------------------------------------------------------------------
                                } else {
                                    stackElHref = stackElementText;
                                }
                                sb.append("\tat ").append(stackElHref).append("<br>");  // NOI18N
                                if (localsMap != null) {
                                    List<JavaFrameGCRoot> locals = localsMap.get(Integer.valueOf(i));
                                    
                                    if (locals != null) {
                                        for (JavaFrameGCRoot localVar : locals) {
                                            Instance localInstance = localVar.getInstance();
                                            
                                            if (localInstance != null) {
                                                sb.append("\t   Local Variable: ").append(printInstance(localInstance)).append("<br>"); // NOI18N
                                            } else {
                                                sb.append("\t   Unknown Local Variable<br>"); // NOI18N                                                
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        sb.append("&nbsp;&nbsp;Unknown thread"); // NOI18N
                    }
                    sb.append("<br>");  // NOI18N
                }
            }
            sb.append("</pre>"); // NOI18N
            stackTrace = sb.toString();
        }
        return stackTrace;
    }

    private String getThreadName(final Instance threadInstance) {
        Object threadName = threadInstance.getValueOfField("name");  // NOI18N
        PrimitiveArrayInstance chars;
        int offset = 0;
        int len;
        
        if (threadName == null) {
            return "*null*"; // NOI18N
        }
        if (threadName instanceof PrimitiveArrayInstance) {
            chars = (PrimitiveArrayInstance)threadName;
            len = chars.getLength();
        } else {
            Instance stringInstance = (Instance)threadName;
            assert stringInstance.getJavaClass().getName().equals(String.class.getName());

            chars = (PrimitiveArrayInstance) stringInstance.getValueOfField("value"); // NOI18N
            if (chars != null) {
                Integer oi = (Integer) stringInstance.getValueOfField("offset");  // NOI18N
                Integer ci = (Integer) stringInstance.getValueOfField("count");   // NOI18N
                if (oi != null) {
                    offset = oi.intValue();
                }
                if (ci != null) {
                    len = ci.intValue();
                } else {
                    len = chars.getLength();
                }
            } else {
                return "*null*"; // NOI18N
            }
        }
        List<String> charsList = chars.getValues();
        List<String> stringList = charsList.subList(offset, offset+len);
        char charArr[] = new char[stringList.size()];
        int j = 0;

        for(String ch:stringList) {
            charArr[j++] = ch.charAt(0);
        }
        return new String(charArr);
    }


    private Map<ThreadObjectGCRoot,Map<Integer,List<JavaFrameGCRoot>>> computeJavaFrameMap(Collection<GCRoot> roots) {
        Map<ThreadObjectGCRoot,Map<Integer,List<JavaFrameGCRoot>>> javaFrameMap = new HashMap();
        
        for (GCRoot root : roots) {
            if (GCRoot.JAVA_FRAME.equals(root.getKind())) {
                JavaFrameGCRoot frameGCroot = (JavaFrameGCRoot) root;
                ThreadObjectGCRoot threadObj = frameGCroot.getThreadGCRoot();
                Integer frameNo = Integer.valueOf(frameGCroot.getFrameNumber());
                Map<Integer,List<JavaFrameGCRoot>> stackMap = javaFrameMap.get(threadObj);
                List<JavaFrameGCRoot> locals;
                
                if (stackMap == null) {
                    stackMap = new HashMap();
                    javaFrameMap.put(threadObj,stackMap);
                }
                locals = stackMap.get(frameNo);
                if (locals == null) {
                    locals = new ArrayList(2);
                    stackMap.put(frameNo,locals);
                }
                locals.add(frameGCroot);
            }
        }
        return javaFrameMap;
    }

    private String printInstance(Instance in) {
        String className;
        JavaClass jcls = in.getJavaClass();
        
        if (jcls == null) {
            return "unknown instance #"+in.getInstanceId(); // NOI18N
        }
        if (jcls.equals(getJavaClass())) {
            JavaClass javaClass = heapFragmentWalker.getHeapFragment().getJavaClassByID(in.getInstanceId());
            
            if (javaClass != null) {
                className = javaClass.getName();
                return "<a href='"+ CLASS_URL_PREFIX + className + "/" + javaClass.getJavaClassId() + "'>class " + className + "</a>"; // NOI18N
            }
        }
        className = jcls.getName();
        return "<a href='"+ INSTANCE_URL_PREFIX + className + "/" + in.getInstanceNumber() + "/" + in.getInstanceId() + "' name='" + in.getInstanceId() + "'>" + className + '#' + in.getInstanceNumber() + "</a>"; // NOI18N
    }

    private JavaClass getJavaClass() {
        if (java_lang_Class == null) {
            java_lang_Class = heapFragmentWalker.getHeapFragment().getJavaClassByName(Class.class.getName());
        }
        return java_lang_Class;
    }

    private static String htmlize(String value) {
            return value.replace(">", "&gt;").replace("<", "&lt;");     // NOI18N
    }
}
