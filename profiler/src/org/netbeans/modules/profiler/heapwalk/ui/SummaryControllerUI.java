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

package org.netbeans.modules.profiler.heapwalk.ui;

import java.util.Enumeration;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapSummary;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.heapwalk.SummaryController;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
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
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 *
 * @author Jiri Sedlacek
 */
public class SummaryControllerUI extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // --- Presenter -------------------------------------------------------------
    private static class Presenter extends JToggleButton {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static ImageIcon ICON_INFO = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/profiler/resources/infoTab.png")); // NOI18N

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Presenter() {
            super();
            setText(VIEW_TITLE);
            setToolTipText(VIEW_DESCR);
            setIcon(ICON_INFO);
            setMargin(new java.awt.Insets(getMargin().top, getMargin().top, getMargin().bottom, getMargin().top));
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String VIEW_TITLE = NbBundle.getMessage(SummaryControllerUI.class, "SummaryControllerUI_ViewTitle"); // NOI18N
    private static final String VIEW_DESCR = NbBundle.getMessage(SummaryControllerUI.class, "SummaryControllerUI_ViewDescr"); // NOI18N
    private static final String IN_PROGRESS_MSG = NbBundle.getMessage(SummaryControllerUI.class,
                                                                      "SummaryControllerUI_InProgressMsg"); // NOI18N
    private static final String NOT_AVAILABLE_MSG = NbBundle.getMessage(SummaryControllerUI.class,
                                                                        "SummaryControllerUI_NotAvailableMsg"); // NOI18N
    private static final String SYSTEM_PROPERTIES_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                               "SummaryControllerUI_SystemPropertiesString"); // NOI18N
    private static final String SUMMARY_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                     "SummaryControllerUI_SummaryString"); // NOI18N
    private static final String ENVIRONMENT_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                         "SummaryControllerUI_EnvironmentString"); // NOI18N
    private static final String FILE_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                       "SummaryControllerUI_FileItemString"); // NOI18N
    private static final String FILE_SIZE_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                            "SummaryControllerUI_FileSizeItemString"); // NOI18N
    private static final String DATE_TAKEN_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                             "SummaryControllerUI_DateTakenItemString"); // NOI18N
    private static final String TOTAL_BYTES_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                              "SummaryControllerUI_TotalBytesItemString"); // NOI18N
    private static final String TOTAL_CLASSES_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                                "SummaryControllerUI_TotalClassesItemString"); // NOI18N
    private static final String TOTAL_INSTANCES_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                                  "SummaryControllerUI_TotalInstancesItemString"); // NOI18N
    private static final String CLASSLOADERS_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                               "SummaryControllerUI_ClassloadersItemString"); // NOI18N
    private static final String GCROOTS_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                          "SummaryControllerUI_GcRootsItemString"); // NOI18N
    private static final String OS_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class, "SummaryControllerUI_OsItemString"); // NOI18N
    private static final String ARCHITECTURE_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                               "SummaryControllerUI_ArchitectureItemString"); // NOI18N
    private static final String JAVA_HOME_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                            "SummaryControllerUI_JavaHomeItemString"); // NOI18N
    private static final String JVM_ITEM_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                      "SummaryControllerUI_JvmItemString"); // NOI18N
    private static final String SHOW_SYSPROPS_LINK_STRING = NbBundle.getMessage(SummaryControllerUI.class,
                                                                                "SummaryControllerUI_ShowSysPropsLinkString"); // NOI18N
                                                                                                                               // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AbstractButton presenter;
    private HTMLTextArea dataArea;

    // --- UI definition ---------------------------------------------------------
    private JSplitPane contentsSplit;
    private Properties systemProperties;
    private SummaryController summaryController;

    // --- Private implementation ------------------------------------------------
    private boolean systemPropertiesComputed = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public SummaryControllerUI(SummaryController summaryController) {
        this.summaryController = summaryController;

        initComponents();
        createData(false);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public AbstractButton getPresenter() {
        if (presenter == null) {
            presenter = new Presenter();
        }

        return presenter;
    }

    private Properties getSystemProperties() {
        if (!systemPropertiesComputed) {
            systemProperties = summaryController.getHeapFragmentWalker().getHeapFragment().getSystemProperties();
            systemPropertiesComputed = true;
        }

        return systemProperties;
    }

    private String computeEnvironment() {
        Properties systemProperties = getSystemProperties();

        if (systemProperties == null) {
            return NOT_AVAILABLE_MSG;
        }

        String patchLevel = systemProperties.getProperty("sun.os.patch.level", "");
        String os = "&nbsp;&nbsp;&nbsp;&nbsp;"
                    + MessageFormat.format(OS_ITEM_STRING,
                                           new Object[] {
                                               systemProperties.getProperty("os.name", NOT_AVAILABLE_MSG),
                                               systemProperties.getProperty("os.version", ""),
                                               ("unknown".equals(patchLevel) ? "" : patchLevel)
                                           }); // NOI18N

        String arch = "&nbsp;&nbsp;&nbsp;&nbsp;"
                      + MessageFormat.format(ARCHITECTURE_ITEM_STRING,
                                             new Object[] {
                                                 systemProperties.getProperty("os.arch", NOT_AVAILABLE_MSG),
                                                 systemProperties.getProperty("sun.arch.data.model", "?") + "bit"
                                             }); // NOI18N

        String jdk = "&nbsp;&nbsp;&nbsp;&nbsp;"
                     + MessageFormat.format(JAVA_HOME_ITEM_STRING,
                                            new Object[] { systemProperties.getProperty("java.home", NOT_AVAILABLE_MSG) }); // NOI18N

        String jvm = "&nbsp;&nbsp;&nbsp;&nbsp;"
                     + MessageFormat.format(JVM_ITEM_STRING,
                                            new Object[] {
                                                systemProperties.getProperty("java.vm.name", NOT_AVAILABLE_MSG),
                                                systemProperties.getProperty("java.vm.version", ""),
                                                systemProperties.getProperty("java.vm.info", "")
                                            }); // NOI18N

        return "<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/sysinfo.png'>&nbsp;&nbsp;"
               + ENVIRONMENT_STRING + "</b><br><hr>" + os + "<br>" + arch + "<br>" + jdk + "<br>" + jvm; // NOI18N
    }

    private String computeSummary() {
        File file = summaryController.getHeapFragmentWalker().getHeapDumpFile();
        Heap heap = summaryController.getHeapFragmentWalker().getHeapFragment();
        HeapSummary hsummary = heap.getSummary();

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
        
        String filename = "&nbsp;&nbsp;&nbsp;&nbsp;"
                          + MessageFormat.format(FILE_ITEM_STRING,
                                                 new Object[] {
                                                     file != null && file.exists() ? file.getAbsolutePath() : NOT_AVAILABLE_MSG
                                                 }); // NOI18N

        String filesize = "&nbsp;&nbsp;&nbsp;&nbsp;"
                          + MessageFormat.format(FILE_SIZE_ITEM_STRING,
                                                 new Object[] {
                                                     file != null && file.exists() ? 
                                                         numberFormat.format(file.length()/(1024 * 1024.0)) + " MB" :
                                                         NOT_AVAILABLE_MSG
                                                 }); // NOI18N

        String dateTaken = "&nbsp;&nbsp;&nbsp;&nbsp;"
                           + MessageFormat.format(DATE_TAKEN_ITEM_STRING, new Object[] { new Date(hsummary.getTime()).toString() }); // NOI18N

        String liveBytes = "&nbsp;&nbsp;&nbsp;&nbsp;"
                           + MessageFormat.format(TOTAL_BYTES_ITEM_STRING,
                                                  new Object[] { numberFormat.format(hsummary.getTotalLiveBytes()) }); // NOI18N

        String liveClasses = "&nbsp;&nbsp;&nbsp;&nbsp;"
                             + MessageFormat.format(TOTAL_CLASSES_ITEM_STRING,
                                                    new Object[] { numberFormat.format(heap.getAllClasses().size()) }); // NOI18N

        String liveInstances = "&nbsp;&nbsp;&nbsp;&nbsp;"
                               + MessageFormat.format(TOTAL_INSTANCES_ITEM_STRING,
                                                      new Object[] {
                                                          numberFormat.format(hsummary.getTotalLiveInstances())
                                                      }); // NOI18N

        String classloaders = "&nbsp;&nbsp;&nbsp;&nbsp;"
                              + MessageFormat.format(CLASSLOADERS_ITEM_STRING,
                                                     new Object[] { numberFormat.format(nclassloaders) }); // NOI18N

        String gcroots = "&nbsp;&nbsp;&nbsp;&nbsp;"
                         + MessageFormat.format(GCROOTS_ITEM_STRING,
                                                new Object[] { numberFormat.format(heap.getGCRoots().size()) }); // NOI18N

        return "<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/resources/memory.png'>&nbsp;&nbsp;"
               + SUMMARY_STRING + "</b><br><hr>" + dateTaken + "<br>" + filename + "<br>" + filesize + "<br><br>" + liveBytes
               + "<br>" + liveClasses + "<br>" + liveInstances + "<br>" + classloaders + "<br>" + gcroots; // NOI18N
    }

    private String computeSystemProperties(boolean showSystemProperties) {
        Properties systemProperties = getSystemProperties();

        if (systemProperties == null) {
            return NOT_AVAILABLE_MSG;
        }

        return "<b><img border='0' align='bottom' src='nbresloc:/org/netbeans/modules/profiler/heapwalk/ui/resources/properties.png'>&nbsp;&nbsp;"
               + SYSTEM_PROPERTIES_STRING + "</b><br><hr>"
               + (showSystemProperties ? formatSystemProperties(systemProperties)
                                       : ("&nbsp;&nbsp;&nbsp;&nbsp;<a href='#'>" + SHOW_SYSPROPS_LINK_STRING + "</a><br>&nbsp;")); // NOI18N
        // NOTE: the above HTML string should be terminated by newline to workaround HTML rendering bug in JDK 5, see Issue 120157
    }

    private void createData(final boolean showSystemProperties) {
        if (!showSystemProperties) {
            dataArea.setText(IN_PROGRESS_MSG);
        }

        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    String summary = "<nobr>" + computeSummary() + "</nobr>"; // NOI18N
                    String environment = "<nobr>" + computeEnvironment() + "</nobr>"; // NOI18N
                    String properties = "<nobr>" + computeSystemProperties(showSystemProperties) + "</nobr>"; // NOI18N
                    final String dataAreaText = summary + "<br><br>" // NOI18N
                                                + environment + "<br><br>" // NOI18N
                                                + properties;

                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                dataArea.setText(dataAreaText);
                                dataArea.setCaretPosition(0);
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
        setLayout(new GridBagLayout());

        GridBagConstraints constraints;

        // Top separator
        JSeparator separator = new JSeparator() {
            public Dimension getMaximumSize() {
                return new Dimension(super.getMaximumSize().width, 1);
            }

            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 1);
            }
        };

        separator.setBackground(getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        add(separator, constraints);

        // dataArea
        dataArea = new HTMLTextArea() {
                protected void showURL(URL url) {
                    createData(true);
                }
            };

        JScrollPane dataAreaScrollPane = new JScrollPane(dataArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        dataAreaScrollPane.setBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        dataAreaScrollPane.setBackground(dataArea.getBackground());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);
        add(dataAreaScrollPane, constraints);

        // UI tweaks
        setBackground(dataArea.getBackground());
    }
}
