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

package org.netbeans.modules.profiler.heapwalk.ui;

import java.awt.BorderLayout;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapSummary;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaFrameGCRoot;
import org.netbeans.lib.profiler.ui.UIConstants;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.JTitledPanel;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.heapwalk.AnalysisController;
import org.netbeans.modules.profiler.heapwalk.HeapFragmentWalker;
import org.netbeans.modules.profiler.heapwalk.OverviewController;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.io.File;
import java.io.StringWriter;
import java.lang.Thread.State;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.GCRoot;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;
import org.netbeans.lib.profiler.heap.ThreadObjectGCRoot;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.utils.GoToSourceHelper;
import org.netbeans.modules.profiler.utils.JavaSourceLocation;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public class OverviewControllerUI extends JTitledPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    
    private static final String SHOW_SYSPROPS_URL = "file:/sysprops"; // NOI18N
    private static final String SHOW_THREADS_URL = "file:/threads"; // NOI18N
    private static final String OPEN_THREADS_URL = "file:/stackframe/";     // NOI18N
    private static final String CLASS_URL_PREFIX = "file://class/"; // NOI18N
    private static final String INSTANCE_URL_PREFIX = "file://instance/";   // NOI18N
    private static ImageIcon ICON_INFO = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/infoTab.png", false); // NOI18N
    
    
    // -----
    // I18N String constants
    private static final String VIEW_TITLE = NbBundle.getMessage(OverviewControllerUI.class, "OverviewControllerUI_ViewTitle"); // NOI18N
    private static final String VIEW_DESCR = NbBundle.getMessage(OverviewControllerUI.class, "OverviewControllerUI_ViewDescr"); // NOI18N
    private static final String IN_PROGRESS_MSG = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_InProgressMsg"); // NOI18N
    private static final String NOT_AVAILABLE_MSG = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_NotAvailableMsg"); // NOI18N
    private static final String SYSTEM_PROPERTIES_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_SystemPropertiesString"); // NOI18N
    private static final String SUMMARY_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_SummaryString"); // NOI18N
    private static final String ENVIRONMENT_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_EnvironmentString"); // NOI18N
    private static final String FILE_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_FileItemString"); // NOI18N
    private static final String FILE_SIZE_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_FileSizeItemString"); // NOI18N
    private static final String DATE_TAKEN_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_DateTakenItemString"); // NOI18N
    private static final String TOTAL_BYTES_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_TotalBytesItemString"); // NOI18N
    private static final String TOTAL_CLASSES_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_TotalClassesItemString"); // NOI18N
    private static final String TOTAL_INSTANCES_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_TotalInstancesItemString"); // NOI18N
    private static final String CLASSLOADERS_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_ClassloadersItemString"); // NOI18N
    private static final String GCROOTS_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_GcRootsItemString"); // NOI18N
    private static final String FINALIZERS_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_FinalizersItemString"); // NOI18N
    private static final String OS_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class, "OverviewControllerUI_OsItemString"); // NOI18N
    private static final String ARCHITECTURE_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_ArchitectureItemString"); // NOI18N
    private static final String JAVA_HOME_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_JavaHomeItemString"); // NOI18N
    private static final String JVM_ITEM_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_JvmItemString"); // NOI18N
    private static final String SHOW_SYSPROPS_LINK_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_ShowSysPropsLinkString"); // NOI18N
    private static final String THREADS_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_ThreadsString"); // NOI18N
    private static final String SHOW_THREADS_LINK_STRING = NbBundle.getMessage(OverviewControllerUI.class,
            "OverviewControllerUI_ShowThreadsLinkString"); // NOI18N
    private static final String CANNOT_RESOLVE_CLASS_MSG = NbBundle.getMessage(AnalysisController.class,
            "AnalysisController_CannotResolveClassMsg"); // NOI18N
    private static final String CANNOT_RESOLVE_INSTANCE_MSG = NbBundle.getMessage(AnalysisController.class,
            "AnalysisController_CannotResolveInstanceMsg"); // NOI18N
    // -----
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private AbstractButton presenter;
    private HTMLTextArea dataArea;
    
    // --- UI definition ---------------------------------------------------------
    private Properties systemProperties;
    private HeapFragmentWalker heapFragmentWalker;
    
    // --- Private implementation ------------------------------------------------
    private JavaClass java_lang_Class;
    private Instance instanceToSelect;
    private boolean systemPropertiesComputed = false;
    private boolean showSysprops = false;
    private boolean showThreads = false;
    private String stackTrace;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    // --- Constructors ----------------------------------------------------------
    public OverviewControllerUI(OverviewController overviewController) {
        super(VIEW_TITLE,ICON_INFO,true);
        heapFragmentWalker = overviewController.getSummaryController().getHeapFragmentWalker();
        java_lang_Class = heapFragmentWalker.getHeapFragment().getJavaClassByName(Class.class.getName());
        initComponents();
        refreshSummary();
    }
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    // --- Public interface ------------------------------------------------------

    public void showInThreads(Instance instance) {
        if (!showThreads) {
            showThreads = true;
            instanceToSelect = instance;
            refreshSummary();
            return;
        }
        String referenceId = String.valueOf(instance.getInstanceId());
        
        dataArea.scrollToReference(referenceId);
        Document d = dataArea.getDocument();
        HTMLDocument doc = (HTMLDocument) d;
        HTMLDocument.Iterator iter = doc.getIterator(HTML.Tag.A);
        for (; iter.isValid(); iter.next()) {
            AttributeSet a = iter.getAttributes();
            String nm = (String) a.getAttribute(HTML.Attribute.NAME);
            if ((nm != null) && nm.equals(referenceId)) {
                dataArea.select(iter.getStartOffset(),iter.getEndOffset());
                dataArea.requestFocusInWindow();
            }
        }
    }
    
    private Properties getSystemProperties() {
        if (!systemPropertiesComputed) {
            systemProperties = heapFragmentWalker.getHeapFragment().getSystemProperties();
            systemPropertiesComputed = true;
        }
        
        return systemProperties;
    }
    
    private String computeEnvironment() {
        Properties sysprops = getSystemProperties();
        
        if (sysprops == null) {
            return NOT_AVAILABLE_MSG;
        }
        
        String patchLevel = sysprops.getProperty("sun.os.patch.level", ""); // NOI18N
        String os = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(OS_ITEM_STRING,
                new Object[] {
            sysprops.getProperty("os.name", NOT_AVAILABLE_MSG), // NOI18N
            sysprops.getProperty("os.version", ""), // NOI18N
            ("unknown".equals(patchLevel) ? "" : patchLevel) // NOI18N
        });
        
        String arch = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(ARCHITECTURE_ITEM_STRING,
                new Object[] {
            sysprops.getProperty("os.arch", NOT_AVAILABLE_MSG), // NOI18N
            sysprops.getProperty("sun.arch.data.model", "?") + "bit" // NOI18N
        });
        
        String jdk = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(JAVA_HOME_ITEM_STRING,
                new Object[] { sysprops.getProperty("java.home", NOT_AVAILABLE_MSG) }); // NOI18N
        
        String jvm = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(JVM_ITEM_STRING,
                new Object[] {
            sysprops.getProperty("java.vm.name", NOT_AVAILABLE_MSG), // NOI18N
            sysprops.getProperty("java.vm.version", ""), // NOI18N
            sysprops.getProperty("java.vm.info", "") // NOI18N
        });
        
        return "<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/sysinfo.png'>&nbsp;&nbsp;" // NOI18N
                + ENVIRONMENT_STRING + "</b><br><hr>" + os + "<br>" + arch + "<br>" + jdk + "<br>" + jvm; // NOI18N
    }
    
    private String computeSummary() {
        File file = heapFragmentWalker.getHeapDumpFile();
        Heap heap = heapFragmentWalker.getHeapFragment();
        HeapSummary hsummary = heap.getSummary();
        long finalizers = computeFinalizers(heap);
        int nclassloaders = 0;
        JavaClass cl = heap.getJavaClassByName("java.lang.ClassLoader"); // NOI18N
        
        if (cl != null) {
            nclassloaders = cl.getInstancesCount();
            
            Collection<JavaClass> jcs = cl.getSubClasses();
            
            for (JavaClass jc : jcs) {
                nclassloaders += jc.getInstancesCount();
            }
        }
        NumberFormat numberFormat = (NumberFormat)NumberFormat.getInstance().clone();
        numberFormat.setMaximumFractionDigits(1);
        
        String filename = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(FILE_ITEM_STRING,
                new Object[] {
            file != null && file.exists() ? file.getAbsolutePath() : NOT_AVAILABLE_MSG
        });
        
        String filesize = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(FILE_SIZE_ITEM_STRING,
                new Object[] {
            file != null && file.exists() ?
                numberFormat.format(file.length()/(1024 * 1024.0)) + " MB" : // NOI18N
                NOT_AVAILABLE_MSG
        });
        
        String dateTaken = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(DATE_TAKEN_ITEM_STRING, new Object[] { new Date(hsummary.getTime()).toString() });
        
        String liveBytes = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(TOTAL_BYTES_ITEM_STRING,
                new Object[] { numberFormat.format(hsummary.getTotalLiveBytes()) });
        
        String liveClasses = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(TOTAL_CLASSES_ITEM_STRING,
                new Object[] { numberFormat.format(heap.getAllClasses().size()) });
        
        String liveInstances = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(TOTAL_INSTANCES_ITEM_STRING,
                new Object[] {
            numberFormat.format(hsummary.getTotalLiveInstances())
        });
        
        String classloaders = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(CLASSLOADERS_ITEM_STRING,
                new Object[] { numberFormat.format(nclassloaders) });
        
        String gcroots = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(GCROOTS_ITEM_STRING,
                new Object[] { numberFormat.format(heap.getGCRoots().size()) });
        
        String finalizersInfo = "&nbsp;&nbsp;&nbsp;&nbsp;" // NOI18N
                + MessageFormat.format(FINALIZERS_ITEM_STRING,
                new Object[] { numberFormat.format(finalizers) });
        
        return "<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/resources/memory.png'>&nbsp;&nbsp;" // NOI18N
                + SUMMARY_STRING + "</b><br><hr>" + dateTaken + "<br>" + filename + "<br>" + filesize + "<br><br>" + liveBytes // NOI18N
                + "<br>" + liveClasses + "<br>" + liveInstances + "<br>" + classloaders + "<br>" + gcroots + "<br>" + finalizersInfo; // NOI18N
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
    
    private String computeSystemProperties(boolean showSystemProperties) {
        Properties sysprops = getSystemProperties();
        
        if (sysprops == null) {
            return NOT_AVAILABLE_MSG;
        }
        
        return "<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/properties.png'>&nbsp;&nbsp;" // NOI18N
                + SYSTEM_PROPERTIES_STRING + "</b><br><hr>" // NOI18N
                + (showSystemProperties ? formatSystemProperties(sysprops)
                : ("&nbsp;&nbsp;&nbsp;&nbsp;<a href='" + SHOW_SYSPROPS_URL + "'>" + SHOW_SYSPROPS_LINK_STRING + "</a>")); // NOI18N
    }
    
    private String computeThreads(boolean showThreads) {
        return "<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/resources/threadsWindow.png'>&nbsp;&nbsp;" // NOI18N
                + THREADS_STRING + "</b><br><hr>" // NOI18N
                + (showThreads ? getStackTrace()
                : ("&nbsp;&nbsp;&nbsp;&nbsp;<a href='" + SHOW_THREADS_URL + "'>" + SHOW_THREADS_LINK_STRING + "</a><br>&nbsp;")); // NOI18N
        // NOTE: the above HTML string should be terminated by newline to workaround HTML rendering bug in JDK 5, see Issue 120157
    }
    
    private synchronized String getStackTrace() {
        if(stackTrace == null) {
            StringWriter sw = new StringWriter();
            Heap h = heapFragmentWalker.getHeapFragment();
            Collection<GCRoot> roots = h.getGCRoots();
            Map<ThreadObjectGCRoot,Map<Integer,List<JavaFrameGCRoot>>> javaFrameMap = computeJavaFrameMap(roots);
            // Use this to enable VisualVM color scheme for threads dumps:
            // sw.append("<pre style='color: #cc3300;'>"); // NOI18N
            sw.append("<pre>"); // NOI18N
            for (GCRoot root : roots) {
                if(root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                    ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot)root;
                    Instance threadInstance = threadRoot.getInstance();
                    if (threadInstance != null) {
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
                        StackTraceElement stack[] = threadRoot.getStackTrace();
                        Map<Integer,List<JavaFrameGCRoot>> localsMap = javaFrameMap.get(threadRoot);
                        // --- Use this to enable VisualVM color scheme for threads dumps: ---
                        // sw.append("&nbsp;&nbsp;<span style=\"color: #0033CC\">"); // NOI18N
                        sw.append("&nbsp;&nbsp;<b>");   // NOI18N
                        // -------------------------------------------------------------------
                        sw.append("\""+threadName+"\""+(daemon.booleanValue() ? " daemon" : "")+" prio="+priority);   // NOI18N
                        if (threadId != null) {
                            sw.append(" tid="+threadId);    // NOI18N
                        }
                        if (threadStatus != null) {
                            State tState = sun.misc.VM.toThreadState(threadStatus.intValue());
                            sw.append(" "+tState);          // NOI18N
                        }
                        // --- Use this to enable VisualVM color scheme for threads dumps: ---
                        // sw.append("</span><br>"); // NOI18N
                        sw.append("</b><br>");   // NOI18N
                        // -------------------------------------------------------------------
                        if(stack != null) {
                            for(int i = 0; i < stack.length; i++) {
                                String stackElHref;
                                StackTraceElement stackElement = stack[i];
                                
                                if (heapFragmentWalker.getHeapDumpProject() != null) {
                                    String className = stackElement.getClassName();
                                    String method = stackElement.getMethodName();
                                    int lineNo = stackElement.getLineNumber();
                                    String stackUrl = OPEN_THREADS_URL+className+"|"+method+"|"+lineNo; // NOI18N
                                    
                                    // --- Use this to enable VisualVM color scheme for threads dumps: ---
                                    // stackElHref = "&nbsp;&nbsp;<a style=\"color: #CC3300;\" href=\""+stackUrl+"\">"+stackElement+"</a>"; // NOI18N
                                    stackElHref = "<a href=\""+stackUrl+"\">"+stackElement+"</a>";    // NOI18N
                                    // -------------------------------------------------------------------
                                } else {
                                    stackElHref = stackElement.toString();
                                }
                                sw.append("\tat "+stackElHref+"<br>");  // NOI18N
                                if (localsMap != null) {
                                    List<JavaFrameGCRoot> locals = localsMap.get(Integer.valueOf(i));
                                    
                                    if (locals != null) {
                                        for (JavaFrameGCRoot localVar : locals) {
                                            Instance localInstance = localVar.getInstance();
                                            sw.append("\t   Local Variable: "+printInstance(localInstance)+"<br>"); // NOI18N
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        sw.append("&nbsp;&nbsp;Unknown thread"); // NOI18N
                    }
                    sw.append("<br>");  // NOI18N
                }
            }
            sw.append("</pre>"); // NOI18N
            stackTrace = sw.toString();
        }
        return stackTrace;
    }
    
    private void refreshSummary() {
        if (!showSysprops && !showThreads) {
            dataArea.setText(IN_PROGRESS_MSG);
        }
        
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                String summary = "<nobr>" + computeSummary() + "</nobr>"; // NOI18N
                String environment = "<nobr>" + computeEnvironment() + "</nobr>"; // NOI18N
                String properties = "<nobr>" + computeSystemProperties(showSysprops) + "</nobr>"; // NOI18N
                String threads = "<nobr>" + computeThreads(showThreads) + "</nobr>"; // NOI18N
                final String dataAreaText = summary + "<br><br>" // NOI18N
                        + environment + "<br><br>" // NOI18N
                        + properties + "<br><br>" // NOI18N
                        + threads;
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        dataArea.setText(dataAreaText);
                        if (instanceToSelect != null) {
                            showInThreads(instanceToSelect);
                            instanceToSelect = null;
                        } else {
                            dataArea.setCaretPosition(0);
                        }
                    }
                });
            }
        });
    }
    
    private String formatSystemProperties(Properties properties) {
        StringBuffer text = new StringBuffer(200);
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
    
    private void initComponents() {
        // dataArea
        dataArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                if (url == null) return;
                String urls = url.toString();
                if (urls.equals(SHOW_SYSPROPS_URL)) {
                    showSysprops = true;
                } else if (urls.equals(SHOW_THREADS_URL)) {
                    showThreads = true;
                } else if (urls.startsWith(OPEN_THREADS_URL)) {
                    urls = urls.substring(OPEN_THREADS_URL.length());
                    String parts[] = urls.split("\\|"); // NOI18N
                    String className = parts[0];
                    String method = parts[1];
                    int linenumber = Integer.parseInt(parts[2]);
                    GoToSourceHelper.openSource(heapFragmentWalker.getHeapDumpProject(),
                            new JavaSourceLocation(className, method, linenumber));
                } else if (urls.startsWith(INSTANCE_URL_PREFIX)) {
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
                refreshSummary();
            }
        };
        dataArea.setSelectionColor(UIConstants.TABLE_SELECTION_BACKGROUND_COLOR);
        JScrollPane dataAreaScrollPane = new JScrollPane(dataArea,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataAreaScrollPane.setBorder(BorderFactory.createMatteBorder(5, 5, 5, 5,
                                        UIUtils.getProfilerResultsBackground()));
        dataAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        dataAreaScrollPane.getHorizontalScrollBar().setUnitIncrement(10);

        JPanel contentsPanel = new JPanel();
        contentsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, getTitleBorderColor()));
        contentsPanel.setLayout(new BorderLayout());
        contentsPanel.setOpaque(true);
        contentsPanel.setBackground(dataArea.getBackground());
        contentsPanel.add(dataAreaScrollPane, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(contentsPanel, BorderLayout.CENTER);
        
        // UI tweaks
        setBackground(dataArea.getBackground());
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
        
        if (in.getJavaClass().equals(java_lang_Class)) {
            JavaClass javaClass = heapFragmentWalker.getHeapFragment().getJavaClassByID(in.getInstanceId());
            className = javaClass.getName();
            return "<a href='"+ CLASS_URL_PREFIX + className + "' name='" + javaClass.getJavaClassId() + "'>class " + className + "</a>"; // NOI18N
        }
        className = in.getJavaClass().getName();
        return "<a href='"+ INSTANCE_URL_PREFIX + className + "/" + in.getInstanceNumber() + "' name='" + in.getInstanceId() + "'>" + className + '#' + in.getInstanceNumber() + "</a>"; // NOI18N
    }

}
