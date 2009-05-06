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

package org.netbeans.modules.profiler.ppoints;

import org.netbeans.api.java.source.UiUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.editor.Registry;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.ui.components.table.EnhancedTableCellRenderer;
import org.netbeans.lib.profiler.ui.components.table.LabelTableCellRenderer;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.text.NbDocument;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 *
 * @author Jiri Sedlacek
 */
public class Utils {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class JavaEditorContext {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Document document;
        private FileObject fileObject;
        private JTextComponent textComponent;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public JavaEditorContext(JTextComponent textComponent, Document document, FileObject fileObject) {
            this.textComponent = textComponent;
            this.document = document;
            this.fileObject = fileObject;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Document getDocument() {
            return document;
        }

        public FileObject getFileObject() {
            return fileObject;
        }

        public JTextComponent getTextComponent() {
            return textComponent;
        }
    }

    private static class ProfilingPointPresenterListRenderer extends DefaultListCellRenderer {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel renderer = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            renderer.setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 5));

            if (value instanceof ProfilingPoint) {
                renderer.setText(((ProfilingPoint) value).getName());
                renderer.setIcon(((ProfilingPoint) value).getFactory().getIcon());
                renderer.setEnabled(((ProfilingPoint) value).isEnabled());
            } else if (value instanceof ProfilingPointFactory) {
                renderer.setText(((ProfilingPointFactory) value).getType());
                renderer.setIcon(((ProfilingPointFactory) value).getIcon());
                renderer.setEnabled(true);
            } else {
                renderer.setIcon(null);
                renderer.setEnabled(true);
            }

            return renderer;
        }
    }

    private static class ProfilingPointPresenterRenderer extends LabelTableCellRenderer {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProfilingPointPresenterRenderer() {
            super(SwingConstants.LEADING);
            //      setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 5)); // TODO: enable once Scope is implemented
            setBorder(BorderFactory.createEmptyBorder(0, 7, 0, 5));
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return new ProfilingPointPresenterRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                                                                                       column);
        }

        protected void setValue(JTable table, Object value, int row, int column) {
            if (table != null) {
                setFont(table.getFont());
            }

            if (value instanceof ProfilingPoint) {
                label.setText(((ProfilingPoint) value).getName());
                label.setIcon(((ProfilingPoint) value).getFactory().getIcon());
                label.setEnabled(((ProfilingPoint) value).isEnabled());
            } else if (value instanceof ProfilingPointFactory) {
                label.setText(((ProfilingPointFactory) value).getType());
                label.setIcon(((ProfilingPointFactory) value).getIcon());
                label.setEnabled(true);
            } else {
                label.setText(""); //NOI18N
                label.setIcon(null);
                label.setEnabled(true);
            }
        }
    }

    private static class ProfilingPointScopeRenderer extends LabelTableCellRenderer {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProfilingPointScopeRenderer() {
            super(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return new ProfilingPointScopeRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        protected void setValue(JTable table, Object value, int row, int column) {
            label.setText(""); //NOI18N

            if (value instanceof ProfilingPoint) {
                label.setIcon(((ProfilingPoint) value).getFactory().getScopeIcon());
                label.setEnabled(((ProfilingPoint) value).isEnabled());
            } else if (value instanceof ProfilingPointFactory) {
                label.setIcon(((ProfilingPointFactory) value).getScopeIcon());
                label.setEnabled(true);
            } else {
                label.setIcon(EMPTY_ICON);
                label.setEnabled(true);
            }
        }
    }

    private static class ProjectPresenterListRenderer extends DefaultListCellRenderer {
        //~ Inner Classes --------------------------------------------------------------------------------------------------------

        private class Renderer extends DefaultListCellRenderer {
            //~ Methods ----------------------------------------------------------------------------------------------------------

            public void setFont(Font font) {
            }

            public void setFontEx(Font font) {
                super.setFont(font);
            }
        }

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Renderer renderer = new Renderer();
        private boolean firstFontSet = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel rendererOrig = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            renderer.setComponentOrientation(rendererOrig.getComponentOrientation());
            renderer.setFontEx(rendererOrig.getFont());
            renderer.setOpaque(rendererOrig.isOpaque());
            renderer.setForeground(rendererOrig.getForeground());
            renderer.setBackground(rendererOrig.getBackground());
            renderer.setEnabled(rendererOrig.isEnabled());
            renderer.setBorder(rendererOrig.getBorder());

            if ((value != null) && value instanceof Project) {
                ProjectInformation pi = ProjectUtils.getInformation((Project) value);
                renderer.setText(pi.getDisplayName());
                renderer.setIcon(pi.getIcon());

                if (ProjectUtilities.getMainProject() == value) {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.BOLD)); // bold for main project
                } else {
                    renderer.setFontEx(renderer.getFont().deriveFont(Font.PLAIN));
                }
            } else {
                renderer.setText(rendererOrig.getText());
                renderer.setIcon(EMPTY_ICON);
            }

            return renderer;
        }
    }

    private static class ProjectPresenterRenderer extends LabelTableCellRenderer {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Font font;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ProjectPresenterRenderer() {
            super(SwingConstants.LEADING);
            setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
            font = label.getFont();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Component getTableCellRendererComponentPersistent(JTable table, Object value, boolean isSelected,
                                                                 boolean hasFocus, int row, int column) {
            return new ProjectPresenterRenderer().getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        protected void setValue(JTable table, Object value, int row, int column) {
            if ((value != null) && (value instanceof Project || value instanceof ProfilingPoint)) {
                if (table != null) {
                    setFont(table.getFont());
                }

                if (value instanceof ProfilingPoint) {
                    label.setEnabled(((ProfilingPoint) value).isEnabled());
                    value = ((ProfilingPoint) value).getProject();
                } else {
                    label.setEnabled(true);
                }

                ProjectInformation pi = ProjectUtils.getInformation((Project) value);
                label.setText(pi.getDisplayName());
                label.setIcon(table.isEnabled() ? pi.getIcon()
                                                : new ImageIcon(GrayFilter.createDisabledImage(((ImageIcon) pi.getIcon()).getImage())));
                label.setFont((ProjectUtilities.getMainProject() == value) ? font.deriveFont(Font.BOLD) : font); // bold for main project
            } else {
                label.setText(""); //NOI18N
                label.setIcon(null);
                label.setEnabled(true);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String FULL_DATE_FORMAT = NbBundle.getMessage(Utils.class, "Utils_FullDateFormat"); // NOI18N
    private static final String FULL_DATE_FORMAT_HIRES = NbBundle.getMessage(Utils.class, "Utils_FullDateFormatHiRes"); // NOI18N
    private static final String TODAY_DATE_FORMAT = NbBundle.getMessage(Utils.class, "Utils_TodayDateFormat"); // NOI18N
    private static final String TODAY_DATE_FORMAT_HIRES = NbBundle.getMessage(Utils.class, "Utils_TodayDateFormatHiRes"); // NOI18N
    private static final String DAY_DATE_FORMAT = NbBundle.getMessage(Utils.class, "Utils_DayDateFormat"); // NOI18N
    private static final String CANNOT_OPEN_SOURCE_MSG = NbBundle.getMessage(Utils.class, "Utils_CannotOpenSourceMsg"); // NOI18N
    private static final String INVALID_PP_LOCATION_MSG = NbBundle.getMessage(Utils.class, "Utils_InvalidPPLocationMsg"); // NOI18N
                                                                                                           // -----
    private static final String PROJECT_DIRECTORY_MARK = "{$projectDirectory}"; // NOI18N

    // TODO: Move to more "universal" location
    public static final ImageIcon EMPTY_ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/empty16.gif", false); // NOI18N
    private static final ProjectPresenterRenderer projectRenderer = new ProjectPresenterRenderer();
    private static final ProjectPresenterListRenderer projectListRenderer = new ProjectPresenterListRenderer();
    private static final EnhancedTableCellRenderer scopeRenderer = new ProfilingPointScopeRenderer();
    private static final ProfilingPointPresenterRenderer presenterRenderer = new ProfilingPointPresenterRenderer();
    private static final ProfilingPointPresenterListRenderer presenterListRenderer = new ProfilingPointPresenterListRenderer();
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat(FULL_DATE_FORMAT);
    private static final SimpleDateFormat fullDateFormatHiRes = new SimpleDateFormat(FULL_DATE_FORMAT_HIRES);
    private static final SimpleDateFormat todayDateFormat = new SimpleDateFormat(TODAY_DATE_FORMAT);
    private static final SimpleDateFormat todayDateFormatHiRes = new SimpleDateFormat(TODAY_DATE_FORMAT_HIRES);
    private static final SimpleDateFormat dayDateFormat = new SimpleDateFormat(DAY_DATE_FORMAT);

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getAbsolutePath(Project project, String sourceFileRelativePath) {
        if (project == null) { // no project context for file

            File file = new File(sourceFileRelativePath);

            return file.exists() ? sourceFileRelativePath : null; // return sourceFileRelativePath if absolute path, null otherwise
        }

        return new File(sourceFileRelativePath.replace(PROJECT_DIRECTORY_MARK,
                                                       FileUtil.toFile(project.getProjectDirectory()).getAbsolutePath()))
                                                                                                                                                                                                                                                                                                                                                                       .getAbsolutePath(); // expand relative path to absolute
    }

    public static String getClassName(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) {
            return null;
        }

        int documentOffset = getDocumentOffset(location);

        if (documentOffset == -1) {
            return null;
        }

        return SourceUtils.getEnclosingClassName(fileObject, documentOffset);
    }
    
    public static String getMethodName(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) {
            return null;
        }

        int documentOffset = getDocumentOffset(location);

        if (documentOffset == -1) {
            return null;
        }

        return SourceUtils.getEnclosingMethodName(fileObject, documentOffset);
    }

    public static CodeProfilingPoint.Location getCurrentLocation(int lineOffset) {
        JavaEditorContext mostActiveContext = getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        File currentFile = FileUtil.toFile(mostActiveJavaSource);

        if (currentFile == null) {
            return CodeProfilingPoint.Location.EMPTY; // Happens for AbstractFileObject, for example JDK classes
        }

        String fileName = currentFile.getAbsolutePath();

        int lineNumber = NbDocument.findLineNumber((StyledDocument) mostActiveContext.getDocument(),
                                                   mostActiveContext.getTextComponent().getCaret().getDot()) + 1; // Line is 0-based, needs to be 1-based for CodeProfilingPoint.Location

        if (lineNumber == -1) {
            lineNumber = 1;
        }

        return new CodeProfilingPoint.Location(fileName, lineNumber,
                                               lineOffset /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */);
    }

    public static Project getCurrentProject() {
        Project currentProject = getMostActiveJavaProject();

        if (currentProject == null) {
            currentProject = ProjectUtilities.getMainProject();
        }

        return currentProject;
    }

    public static CodeProfilingPoint.Location getCurrentSelectionEndLocation(int lineOffset) {
        JavaEditorContext mostActiveContext = getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        JTextComponent mostActiveTextComponent = mostActiveContext.getTextComponent();

        if (mostActiveTextComponent.getSelectedText() == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        String fileName = FileUtil.toFile(mostActiveJavaSource).getAbsolutePath();
        int lineNumber = NbDocument.findLineNumber((StyledDocument) mostActiveContext.getDocument(),
                                                   mostActiveTextComponent.getSelectionEnd()) + 1; // Line is 0-based, needs to be 1-based for CodeProfilingPoint.Location

        if (lineNumber == -1) {
            lineNumber = 1;
        }

        return new CodeProfilingPoint.Location(fileName, lineNumber,
                                               lineOffset /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */);
    }

    public static CodeProfilingPoint.Location[] getCurrentSelectionLocations() {
        JavaEditorContext mostActiveContext = getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return new CodeProfilingPoint.Location[0];
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return new CodeProfilingPoint.Location[0];
        }

        JTextComponent mostActiveTextComponent = mostActiveContext.getTextComponent();

        if (mostActiveTextComponent.getSelectedText() == null) {
            return new CodeProfilingPoint.Location[0];
        }

        File file = FileUtil.toFile(mostActiveJavaSource);

        if (file == null) {
            return new CodeProfilingPoint.Location[0]; // Most likely Java source
        }

        String fileName = file.getAbsolutePath();

        int startLineNumber = NbDocument.findLineNumber((StyledDocument) mostActiveContext.getDocument(),
                                                        mostActiveTextComponent.getSelectionStart()) + 1; // Line is 0-based, needs to be 1-based for CodeProfilingPoint.Location

        if (startLineNumber == -1) {
            startLineNumber = 1;
        }

        int endLineNumber = NbDocument.findLineNumber((StyledDocument) mostActiveContext.getDocument(),
                                                      mostActiveTextComponent.getSelectionEnd()) + 1; // Line is 0-based, needs to be 1-based for CodeProfilingPoint.Location

        if (endLineNumber == -1) {
            endLineNumber = 1;
        }

        return new CodeProfilingPoint.Location[] {
                   new CodeProfilingPoint.Location(fileName, startLineNumber,
                                                   CodeProfilingPoint.Location.OFFSET_START /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */),
                   new CodeProfilingPoint.Location(fileName, endLineNumber,
                                                   CodeProfilingPoint.Location.OFFSET_END /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */)
               };
    }

    public static CodeProfilingPoint.Location getCurrentSelectionStartLocation(int lineOffset) {
        JavaEditorContext mostActiveContext = getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        FileObject mostActiveJavaSource = mostActiveContext.getFileObject();

        if (mostActiveJavaSource == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        JTextComponent mostActiveTextComponent = mostActiveContext.getTextComponent();

        if (mostActiveTextComponent.getSelectedText() == null) {
            return CodeProfilingPoint.Location.EMPTY;
        }

        String fileName = FileUtil.toFile(mostActiveJavaSource).getAbsolutePath();
        int lineNumber = NbDocument.findLineNumber((StyledDocument) mostActiveContext.getDocument(),
                                                   mostActiveTextComponent.getSelectionStart()) + 1; // Line is 0-based, needs to be 1-based for CodeProfilingPoint.Location

        if (lineNumber == -1) {
            lineNumber = 1;
        }

        return new CodeProfilingPoint.Location(fileName, lineNumber,
                                               lineOffset /* TODO: get real line offset if lineOffset isn't OFFSET_START nor OFFSET_END */);
    }

    public static int getDocumentOffset(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) {
            return -1;
        }

        DataObject dataObject = null;

        try {
            dataObject = DataObject.find(fileObject);
        } catch (DataObjectNotFoundException ex) {
        }

        if (dataObject == null) {
            return -1;
        }

        EditorCookie editorCookie = (EditorCookie) dataObject.getCookie(EditorCookie.class);

        if (editorCookie == null) {
            return -1;
        }

        StyledDocument document = null;

        try {
            document = editorCookie.openDocument(); // blocks until the document is loaded
        } catch (IOException ex) {
        }

        if (document == null) {
            return -1;
        }
        
        int linePosition;
        int lineOffset;

        try {
            linePosition = NbDocument.findLineOffset(document, location.getLine() - 1); // Line is 1-based, needs to be 0-based for NbDocument
            
            if (location.isLineStart()) {
                lineOffset = 0;
            } else if (location.isLineEnd()) {
                lineOffset = NbDocument.findLineOffset(document, location.getLine()) - linePosition - 1; // TODO: workaround to get line length, could fail at the end of last line!!!
            } else {
                lineOffset = location.getOffset();
            }
        } catch (Exception e) {
            return -1;
        }

        return linePosition + lineOffset;
    }

    public static double getDurationInMicroSec(long startTimestamp, long endTimestamp) {
        ProfilingSessionStatus session = Profiler.getDefault().getTargetAppRunner().getProfilingSessionStatus();
        double countsInMicroSec = session.timerCountsInSecond[0] / 1000000L;

        return (endTimestamp - startTimestamp) / countsInMicroSec;
    }

    //  public static DataObject getDataObject(CodeProfilingPoint.Location location) {
    //    // URL
    //    String url = location.getFile();
    //
    //    // FileObject
    //    FileObject file = null;
    //    try {
    //      file = URLMapper.findFileObject(new File(url).toURI().toURL());
    //    } catch (MalformedURLException e) {}
    //    if (file == null) return null;
    //
    //    // DataObject
    //    DataObject dao = null;
    //    try {
    //      dao = DataObject.find(file);
    //    } catch (DataObjectNotFoundException ex) {}
    //
    //    return dao;
    //  }
    public static Line getEditorLine(CodeProfilingPoint.Location location) {
        // URL
        String url = location.getFile();

        // FileObject
        FileObject file = null;

        try {
            file = URLMapper.findFileObject(new File(url).toURI().toURL());
        } catch (MalformedURLException e) {
        }

        if (file == null) {
            return null;
        }

        // DataObject
        DataObject dao = null;

        try {
            dao = DataObject.find(file);
        } catch (DataObjectNotFoundException ex) {
            return null;
        }

        // LineCookie of pp
        LineCookie lineCookie = (LineCookie) dao.getCookie(LineCookie.class);

        if (lineCookie == null) {
            return null;
        }

        // Line.Set of pp - real line where pp is defined
        Line.Set lineSet = lineCookie.getLineSet();

        if (lineSet == null) {
            return null;
        }

        try {
            return lineSet.getCurrent(location.getLine() - 1); // Line is 1-based, needs to be 0-based for Line.Set
        } catch (Exception e) {
        }

        return null;
    }
    
    public static boolean isValidLocation(CodeProfilingPoint.Location location) {
        // Fail if location not in method
        String methodName = Utils.getMethodName(location);
        if (methodName == null) return false;
        
        // Succeed if location in method body
        if (location.isLineStart()) return true;
        else if (location.isLineEnd()) {
            CodeProfilingPoint.Location startLocation = new CodeProfilingPoint.Location(
                    location.getFile(), location.getLine(), CodeProfilingPoint.Location.OFFSET_START);
            if (methodName.equals(Utils.getMethodName(startLocation))) return true;
        }

        Line line = getEditorLine(location); 
        if (line == null) return false;
        
        // Fail if location immediately after method declaration - JUST A BEST GUESS!
        String lineText = line.getText().trim();
        if (lineText.endsWith("{") && lineText.indexOf("{") == lineText.lastIndexOf("{")) return false; // NOI18N
        
        return true;
    }
    
    public static void checkLocation(CodeProfilingPoint.Single ppoint) {
        if (!isValidLocation(ppoint.getLocation())) NetBeansProfiler.getDefaultNB().displayWarningAndWait(
                MessageFormat.format(INVALID_PP_LOCATION_MSG, new Object[] { ppoint.getName() }));
    }
    
    public static void checkLocation(CodeProfilingPoint.Paired ppoint) {
        if (!isValidLocation(ppoint.getStartLocation())) NetBeansProfiler.getDefaultNB().displayWarningAndWait(
                MessageFormat.format(INVALID_PP_LOCATION_MSG, new Object[] { ppoint.getName() }));
        else if (ppoint.usesEndLocation() && !isValidLocation(ppoint.getEndLocation())) NetBeansProfiler.getDefaultNB().displayWarningAndWait(
                MessageFormat.format(INVALID_PP_LOCATION_MSG, new Object[] { ppoint.getName() }));
    }

    public static Project getMostActiveJavaProject() {
        JavaEditorContext mostActiveContext = getMostActiveJavaEditorContext();

        if (mostActiveContext == null) {
            return null;
        }

        FileObject mostActiveFileObject = mostActiveContext.getFileObject();

        if (mostActiveFileObject == null) {
            return null;
        }

        return FileOwnerQuery.getOwner(mostActiveFileObject);
    }

    public static ListCellRenderer getPresenterListRenderer() {
        return presenterListRenderer;
    }

    public static EnhancedTableCellRenderer getPresenterRenderer() {
        return presenterRenderer;
    }

    public static CodeProfilingPoint[] getProfilingPointsOnLine(CodeProfilingPoint.Location location) {
        if ((location == null) || (location == CodeProfilingPoint.Location.EMPTY)) {
            return new CodeProfilingPoint[0];
        }

        File file = new File(location.getFile());
        int lineNumber = location.getLine();

        List<CodeProfilingPoint> lineProfilingPoints = new ArrayList();
        List<CodeProfilingPoint> profilingPoints = ProfilingPointsManager.getDefault()
                                                                         .getProfilingPoints(CodeProfilingPoint.class, null);

        for (CodeProfilingPoint profilingPoint : profilingPoints) {
            for (CodeProfilingPoint.Annotation annotation : profilingPoint.getAnnotations()) {
                CodeProfilingPoint.Location loc = profilingPoint.getLocation(annotation);

                if ((loc.getLine() == lineNumber) && new File(loc.getFile()).equals(file)) {
                    lineProfilingPoints.add(profilingPoint);

                    break;
                }
            }
        }

        return lineProfilingPoints.toArray(new CodeProfilingPoint[lineProfilingPoints.size()]);
    }

    // TODO: should be moved to ProjectUtilities
    public static ListCellRenderer getProjectListRenderer() {
        return projectListRenderer;
    }

    // TODO: should be moved to ProjectUtilities
    public static EnhancedTableCellRenderer getProjectRenderer() {
        return projectRenderer;
    }

    public static String getRelativePath(Project project, String sourceFileAbsolutePath) {
        if (project == null) {
            return sourceFileAbsolutePath; // no project context for file
        }

        String projectDirectoryAbsolutePath = FileUtil.toFile(project.getProjectDirectory()).getAbsolutePath();

        if (!sourceFileAbsolutePath.startsWith(projectDirectoryAbsolutePath)) {
            return sourceFileAbsolutePath; // file not placed in project directory
        }

        File file = FileUtil.normalizeFile(new File(sourceFileAbsolutePath));

        return PROJECT_DIRECTORY_MARK + "/" // NOI18N
               + FileUtil.getRelativePath(project.getProjectDirectory(), FileUtil.toFileObject(file)); // file placed in project directory => relative path used
    }

    public static EnhancedTableCellRenderer getScopeRenderer() {
        return scopeRenderer;
    }

    public static String getThreadClassName(int threadID) {
        // TODO: get the thread class name for RuntimeProfilingPoint.HitEvent.threadId
        return null;
    }

    public static String getThreadName(int threadID) {
        // TODO: get the thread name for RuntimeProfilingPoint.HitEvent.threadId
        return "&lt;unknown thread, id=" + threadID + "&gt;"; // NOI18N (not used)
    }

    public static long getTimeInMillis(final long hiResTimeStamp) {
        ProfilingSessionStatus session = Profiler.getDefault().getTargetAppRunner().getProfilingSessionStatus();
        long statupInCounts = session.startupTimeInCounts;
        long startupMillis = session.startupTimeMillis;
        long countsInMillis = session.timerCountsInSecond[0] / 1000L;

        return startupMillis + ((hiResTimeStamp - statupInCounts) / countsInMillis);
    }

    public static String getUniqueName(String name, String nameSuffix, Project project) {
        List<ProfilingPoint> projectProfilingPoints = ProfilingPointsManager.getDefault().getProfilingPoints(project, true);
        List<String> projectProfilingPointsNames = new LinkedList();

        for (ProfilingPoint projectProfilingPoint : projectProfilingPoints) {
            projectProfilingPointsNames.add(projectProfilingPoint.getName());
        }

        int index = 0;
        String indexStr = ""; // NOI18N

        while (projectProfilingPointsNames.contains(name + indexStr + nameSuffix)) {
            indexStr = " " + Integer.toString(++index); // NOI18N
        }

        return name + indexStr + nameSuffix; // NOI18N
    }

    public static String formatLocalProfilingPointTime(long timestamp) {
        Date now = new Date();
        Date date = new Date(timestamp);

        if (dayDateFormat.format(now).equals(dayDateFormat.format(date))) {
            return todayDateFormat.format(date);
        } else {
            return fullDateFormat.format(date);
        }
    }

    public static String formatProfilingPointTime(long timestamp) {
        long timestampInMillis = getTimeInMillis(timestamp);
        Date now = new Date();
        Date date = new Date(timestampInMillis);

        if (dayDateFormat.format(now).equals(dayDateFormat.format(date))) {
            return todayDateFormat.format(date);
        } else {
            return fullDateFormat.format(date);
        }
    }

    public static String formatProfilingPointTimeHiRes(long timestamp) {
        long timestampInMillis = getTimeInMillis(timestamp);
        Date now = new Date();
        Date date = new Date(timestampInMillis);

        if (dayDateFormat.format(now).equals(dayDateFormat.format(date))) {
            return todayDateFormatHiRes.format(date);
        } else {
            return fullDateFormatHiRes.format(date);
        }
    }

    public static void openLocation(CodeProfilingPoint.Location location) {
        File file = FileUtil.normalizeFile(new File(location.getFile()));
        final FileObject fileObject = FileUtil.toFileObject(file);

        if ((fileObject == null) || !fileObject.isValid()) {
            return;
        }

        final int documentOffset = getDocumentOffset(location);

        if (documentOffset == -1) {
            NetBeansProfiler.getDefaultNB().displayError(CANNOT_OPEN_SOURCE_MSG);
            return;
        }

        IDEUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    UiUtils.open(fileObject, documentOffset);
                } // this MUST use UiUtils since there is no replacement method yet
            });
    }

    private static JavaEditorContext getMostActiveJavaEditorContext() {
        Iterator componentIterator = Registry.getComponentIterator();

        while (componentIterator.hasNext()) {
            JTextComponent component = (JTextComponent) componentIterator.next();
            Document document = component.getDocument();
            FileObject fileObject = NbEditorUtilities.getFileObject(document);

            if ((fileObject != null) && fileObject.getExt().equalsIgnoreCase("java")) {
                return new JavaEditorContext(component, document, fileObject); // NOI18N
            }
        }

        return null;
    }
}
