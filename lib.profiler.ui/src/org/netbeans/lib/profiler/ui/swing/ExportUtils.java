/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ExportUtils {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.swing.Bundle"); // NOI18N
    private static final String BUTTON_EXPORT = messages.getString("ExportUtils_ButtonExport"); // NOI18N
    private static final String NPS_FILE = messages.getString("ExportUtils_NpsFile"); // NOI18N
    private static final String CSV_FILE = messages.getString("ExportUtils_CsvFile"); // NOI18N
    private static final String HTML_FILE = messages.getString("ExportUtils_HtmlFile"); // NOI18N
    private static final String XML_FILE = messages.getString("ExportUtils_XmlFile"); // NOI18N
    private static final String PNG_FILE = messages.getString("ExportUtils_PngFile"); // NOI18N
    private static final String FILE_FILTER_DESCR = messages.getString("ExportUtils_FileFilterDescr"); // NOI18N
    private static final String MSG_CANNOT_OVERWRITE_SOURCE = messages.getString("ExportUtils_MsgCannotOverwriteSource"); // NOI18N
    private static final String MSG_EXPORT_SNAPSHOT_FAILED = messages.getString("ExportUtils_MsgExportSnapshotFailed"); // NOI18N
    private static final String MSG_EXPORT_IMAGE_FAILED = messages.getString("ExportUtils_MsgExportImageFailed"); // NOI18N
//    private static final String MSG_NODATA = messages.getString("ExportUtils_MsgNoData"); // NOI18N
    private static final String TITLE_OVERWRITE_FILE = messages.getString("ExportUtils_TitleOverwriteFile"); // NOI18N
    private static final String MSG_OVERWRITE_FILE = messages.getString("ExportUtils_MsgOverwriteFile"); // NOI18N
    // -----
    
    private static final Logger LOGGER = Logger.getLogger(ExportUtils.class.getName());
    
    
    public static class FormatFilter extends FileFilter {
        
        private final String name;
        private final String extension;
        
        public FormatFilter(String name, String extension) {
            this.name = name;
            this.extension = extension.startsWith(".") ? extension : "." + extension; // NOI18N
        }
        
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(extension);
        }
        
        public String getDescription() {
            return MessageFormat.format(FILE_FILTER_DESCR, name, extension);
        }
        
        public String getExtension() {
            return extension;
        }
        
    }
    
    public static final FormatFilter NPS_FILTER = new FormatFilter(NPS_FILE, "nps"); // NOI18N
    public static final FormatFilter CSV_FILTER = new FormatFilter(CSV_FILE, "csv"); // NOI18N
    public static final FormatFilter HTML_FILTER = new FormatFilter(HTML_FILE, "html"); // NOI18N
    public static final FormatFilter XML_FILTER = new FormatFilter(XML_FILE, "xml"); // NOI18N
    public static final FormatFilter PNG_FILTER = new FormatFilter(PNG_FILE, "png"); // NOI18N
    
    
    public static abstract class Exportable {
        
        public abstract String getName();
        
        public abstract boolean isEnabled();
        
        public abstract ExportProvider[] getProviders();
        
    }
    
    public static abstract class ExportProvider {
        
        public abstract FormatFilter getFormatFilter();
        
        public abstract void export(File targetFile);
        
    }
    
    public static abstract class BaseExportProvider extends ExportProvider {
        
        private final FormatFilter formatFilter;
        
        protected BaseExportProvider(FormatFilter formatFilter) { this.formatFilter = formatFilter; }
        
        public FormatFilter getFormatFilter() { return formatFilter; }
        
    }
    
    
    public static abstract class ProfilerTableExportProvider extends BaseExportProvider {
        
        private final ProfilerTable table;
        
        public ProfilerTableExportProvider(ProfilerTable table, FormatFilter formatFilter) {
            super(formatFilter);
            this.table = table;
        }
        
        public void export(File targetFile) { export(table, targetFile); }
        
        protected abstract void export(ProfilerTable table, File targetFile);
        
    }
    
    public static abstract class AbstractNPSExportProvider extends BaseExportProvider {
        
        private final File sourceFile;
        
        public AbstractNPSExportProvider(File sourceFile) {
            super(NPS_FILTER);
            this.sourceFile = sourceFile;
        }
        
        public void export(File targetFile) {
            if (targetFile.isFile() && targetFile.equals(sourceFile)) {
                ProfilerDialogs.displayError(MSG_CANNOT_OVERWRITE_SOURCE);
            } else {
                doExport(targetFile);
            }
        }
        
        protected abstract void doExport(File targetFile);
        
    }
    
    public static class CSVExportProvider extends ProfilerTableExportProvider {
        
        public CSVExportProvider(ProfilerTable table) {
            super(table, CSV_FILTER);
        }
        
        protected void export(ProfilerTable table, File targetFile) {
            exportCSV(table, ',', targetFile); // NOI18N
        }
        
    }
    
    public static class HTMLExportProvider extends ProfilerTableExportProvider {
        
        private final String name;
        
        public HTMLExportProvider(ProfilerTable table, String name) {
            super(table, HTML_FILTER);
            this.name = name;
        }
        
        protected void export(ProfilerTable table, File targetFile) {
            exportHTML(table, name, targetFile);
        }
        
    }
    
    public static class XMLExportProvider extends ProfilerTableExportProvider {
        
        private final String name;
        
        public XMLExportProvider(ProfilerTable table, String name) {
            super(table, XML_FILTER);
            this.name = name;
        }
        
        protected void export(ProfilerTable table, File targetFile) {
            exportXML(table, name, targetFile);
        }
        
    }
    
    public static class PNGExportProvider extends BaseExportProvider {
        
        private final Component component;
        
        public PNGExportProvider(Component component) {
            super(PNG_FILTER);
            this.component = component;
        }
        
        public void export(final File targetFile) {
            final BufferedImage image = UIUtils.createScreenshot(component);
            createExecutor(targetFile.getName()).submit(new Runnable() {
                public void run() {
                    try {
                        targetFile.toPath();
                        ImageIO.write(image, "PNG", targetFile); // NOI18N
                    } catch (Throwable t) {
                        LOGGER.log(Level.INFO, t.getMessage(), t);
                        String msg = t.getLocalizedMessage().replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
                        ProfilerDialogs.displayError("<html><b>" + MSG_EXPORT_IMAGE_FAILED + "</b><br><br>" + msg + "</html>"); // NOI18N
                    }
                }
            });
        }
        
    }
    
    
    public static AbstractButton exportButton(final Component parent, String tooltip, final Exportable... exportables) {
        PopupButton exportPopup = new PopupButton(Icons.getIcon(GeneralIcons.SAVE_AS)) {
            protected void populatePopup(JPopupMenu popup) {
                for (final Exportable exportable : exportables) {
                    if (exportable != null && exportable.isEnabled()) {
                        popup.add(new JMenuItem(exportable.getName()) {
                            protected void fireActionPerformed(ActionEvent e) {
                                JFileChooser fileChooser = new JFileChooser();
                                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                                fileChooser.setMultiSelectionEnabled(false);
                                fileChooser.setDialogTitle(exportable.getName());

                                fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());

                                ExportProvider[] providers = exportable.getProviders();
                                for (ExportProvider provider : providers)
                                    fileChooser.addChoosableFileFilter(provider.getFormatFilter());

                                // returning true means exporting to .nps, don't export other views
                                showExportDialog(fileChooser, parent, providers);
                            }
                        });
                    }
                }
            }
        };
        exportPopup.setToolTipText(tooltip);
        return exportPopup;
    }
    
    private static void showExportDialog(final JFileChooser fileChooser, final Component parent, final ExportProvider[] providers) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileChooser.showDialog(parent, BUTTON_EXPORT) != JFileChooser.APPROVE_OPTION) return;

                File targetFile = fileChooser.getSelectedFile();
                FileFilter filter = fileChooser.getFileFilter();

                for (ExportProvider provider : providers) {
                    FormatFilter format = provider.getFormatFilter();
                    if (filter.equals(format)) {
                        targetFile = checkFileExtension(targetFile, format.getExtension());
                        if (checkFileExists(targetFile)) provider.export(targetFile);
                        else showExportDialog(fileChooser, parent, providers);
                        break;
                    }
                }
            }
        });
    }
    
    private static boolean checkFileExists(File file) {
        return !file.isFile() ? true : ProfilerDialogs.displayConfirmation(
                                       MSG_OVERWRITE_FILE, TITLE_OVERWRITE_FILE);
    }
    
    public static File checkFileExtension(File file, String extension) {
        if (file.getName().endsWith(extension)) return file;
        return new File(file.getPath() + extension);
    }
    
    
    public static boolean exportCSV(ProfilerTable table, char separator, File file) {
        Exporter ex = new Exporter(file);
        
        char doubleQuote = '"'; // NOI18N
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        
        try {            
            for (int col = 0; col < columnCount; col++) {
                ex.write(doubleQuote);
                ex.write(table.getColumnName(col));
                ex.write(doubleQuote);
                if (col < columnCount - 1) ex.write(separator);
                else ex.writeln();
            }
            
            if (ex.failed()) return false;
            
            if (table instanceof ProfilerTreeTable) {
                ProfilerTreeTable treeTable = (ProfilerTreeTable)table;
                TreePath path = treeTable.getNextPath(treeTable.getRootPath());
                TreeNode node = (TreeNode)path.getLastPathComponent();
                int indent = path.getPathCount() - 2;
                TreePath firstPath = path;
                do {
                    if (ex.failed()) return false;
                    for (int col = 0; col < columnCount; col++) {
                        ex.write(doubleQuote);
                        if (table.getColumnClass(col) == JTree.class)
                            for (int i = 0; i < indent; i++) ex.write(' '); // NOI18N
                        ex.write  (treeTable.getStringValue(node, col));
                        ex.write(doubleQuote);
                        if (col < columnCount - 1) ex.write(separator);
                        else ex.writeln();
                    }
                    path = treeTable.getNextPath(path);
                    node = (TreeNode)path.getLastPathComponent();
                    indent = path.getPathCount() - 2;
                } while (!firstPath.equals(path));
            } else {
                for (int row = 0; row < rowCount; row++) {
                    if (ex.failed()) return false;
                    for (int col = 0; col < columnCount; col++) {
                        ex.write(doubleQuote);
                        ex.write(table.getStringValue(row, col));
                        ex.write(doubleQuote);
                        if (col < columnCount - 1) ex.write(separator);
                        else ex.writeln();
                    }
                }
            }
        } finally {
            ex.close();
        }
        
        return true;
    }
    
    public static boolean exportHTML(ProfilerTable table, String name, File file) {
        Exporter ex = new Exporter(file);
        
        int columnCount = table.getColumnCount();
        
        try {
            ex.writeln("<html>"); // NOI18N
            
            if (ex.failed()) return false;
            
            ex.writeln(" <head>"); // NOI18N
            ex.writeln("  <meta http-equiv=\"Content-type\" content=\"text/html; charset=utf-8\" />"); // NOI18N
            ex.write  ("  <title>"); // NOI18N
            ex.write  (name);
            ex.writeln("</title>"); // NOI18N
            ex.writeln(" </head>"); // NOI18N
            
            ex.writeln(" <body>"); // NOI18N
            ex.writeln("  <table>"); // NOI18N
            
            ex.writeln("   <tr>"); // NOI18N
            for (int col = 0; col < columnCount; col++) {
                ex.write  ("    <th>"); // NOI18N
                ex.write  (escapeHTML(table.getColumnName(col)));
                ex.writeln("</th>"); // NOI18N
            }
            ex.writeln("   </tr>"); // NOI18N
            
            if (ex.failed()) return false;

            if (table instanceof ProfilerTreeTable) {
                ProfilerTreeTable treeTable = (ProfilerTreeTable)table;
                TreePath path = treeTable.getNextPath(treeTable.getRootPath());
                TreeNode node = (TreeNode)path.getLastPathComponent();
                int indent = path.getPathCount() - 2;
                TreePath firstPath = path;
                do {
                    if (ex.failed()) return false;
                    ex.writeln("   <tr>"); // NOI18N
                    for (int col = 0; col < columnCount; col++) {
                        ex.write  ("    <td><pre>"); // NOI18N
                        if (table.getColumnClass(col) == JTree.class)
                            for (int i = 0; i < indent; i++) ex.write('.'); // NOI18N
                        ex.write  (escapeHTML(treeTable.getStringValue(node, col)));
                        ex.writeln("</pre></td>"); // NOI18N
                    }
                    ex.writeln("   </tr>"); // NOI18N
                    path = treeTable.getNextPath(path);
                    node = (TreeNode)path.getLastPathComponent();
                    indent = path.getPathCount() - 2;
                } while (!firstPath.equals(path));
            } else {
                int rowCount = table.getRowCount();
                for (int row = 0; row < rowCount; row++) {
                    if (ex.failed()) return false;
                    ex.writeln("   <tr>"); // NOI18N
                    for (int col = 0; col < columnCount; col++) {
                        ex.write  ("    <td><pre>"); // NOI18N
                        ex.write  (escapeHTML(table.getStringValue(row, col)));
                        ex.writeln("</pre></td>"); // NOI18N
                    }
                    ex.writeln("   </tr>"); // NOI18N
                }
            }
            
            ex.writeln("  </table>"); // NOI18N
            ex.writeln(" </body>"); // NOI18N
            ex.writeln("</html>"); // NOI18N
        } finally {
            ex.close();
        }
        
        return true;
    }
    
    public static boolean exportXML(ProfilerTable table, String name, File file) {
        Exporter ex = new Exporter(file);
        
        int columnCount = table.getColumnCount();
        
        try {
            ex.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); // NOI18N
            
            if (ex.failed()) return false;
            
            if (table instanceof ProfilerTreeTable) {
                ProfilerTreeTable treeTable = (ProfilerTreeTable)table;
                TreePath path = treeTable.getNextPath(treeTable.getRootPath());
                TreeNode node = (TreeNode)path.getLastPathComponent();
                int indent = path.getPathCount();
                TreePath firstPath = path;
                
                ex.write  ("<dataview name=\""); // NOI18N
                ex.write  (name);
                ex.writeln("\" type=\"tree\">"); // NOI18N
                ex.writeln(" <tree>"); // NOI18N
                
                do {
                    if (ex.failed()) return false;
                    ex.write  (indent(indent));
                    ex.writeln("<node>"); // NOI18N
                    for (int col = 0; col < columnCount; col++) {
                        ex.write  (indent(indent));
                        ex.write  (" <property name=\""); // NOI18N
                        ex.write  (escapeXML(treeTable.getColumnName(col)));
                        ex.write  ("\" value=\""); // NOI18N
                        ex.write  (escapeXML(treeTable.getStringValue(node, col)));
                        ex.writeln("\" />"); // NOI18N
                    }
                    
                    path = treeTable.getNextPath(path);
                    node = (TreeNode)path.getLastPathComponent();
                    int oldIndent = indent;
                    indent = path.getPathCount();
                    for (int i = 0; i <= oldIndent - indent; i++) {
                        ex.write  (indent(oldIndent - i));
                        ex.writeln("</node>"); // NOI18N
                    }
                } while (!firstPath.equals(path));
                
                ex.writeln(" </tree>"); // NOI18N
                ex.writeln("</dataview>"); // NOI18N
            } else {
                int rowCount = table.getRowCount();
                
                ex.write  ("<dataview name=\""); // NOI18N
                ex.write  (name);
                ex.writeln("\" type=\"table\">"); // NOI18N

                ex.write  (" <table rows=\""); // NOI18N
                ex.write  (Integer.toString(rowCount));
                ex.write  ("\" columns=\""); // NOI18N
                ex.write  (Integer.toString(columnCount));
                ex.writeln("\">"); // NOI18N

                ex.writeln("  <thead>"); // NOI18N
                for (int col = 0; col < columnCount; col++) {
                    ex.write  ("   <th><![CDATA["); // NOI18N
                    ex.write  (escapeXML(table.getColumnName(col)));
                    ex.writeln("]]></th>"); // NOI18N
                }
                ex.writeln("  </thead>"); // NOI18N
                
                if (ex.failed()) return false;

                ex.writeln("  <tbody>"); // NOI18N
                for (int row = 0; row < rowCount; row++) {
                    if (ex.failed()) return false;
                    ex.writeln("   <tr>"); // NOI18N
                    for (int col = 0; col < columnCount; col++) {
                        ex.write  ("    <td><![CDATA["); // NOI18N
                        ex.write  (escapeXML(table.getStringValue(row, col)));
                        ex.writeln("]]></td>"); // NOI18N
                    }
                    ex.writeln("   </tr>"); // NOI18N
                }
                ex.writeln("  </tbody>"); // NOI18N

                ex.writeln(" </table>"); // NOI18N
                ex.writeln("</dataview>"); // NOI18N
            }
        } finally {
            ex.close();
        }
        
        return true;
    }
    
    private static int LAST_indent = Integer.MIN_VALUE;
    private static String LAST_INDENT;
    private static String indent(int indent) {
        if (LAST_indent == indent) return LAST_INDENT;
        
        if (indent == 0) return ""; // NOI18N
        if (indent == 1) return " "; // NOI18N
        if (indent == 2) return "  "; // NOI18N
        if (indent == 3) return "   "; // NOI18N
        if (indent == 4) return "    "; // NOI18N
        if (indent == 5) return "     "; // NOI18N
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) sb.append(" "); // NOI18N
        LAST_indent = indent;
        LAST_INDENT = sb.toString();
        
        return LAST_INDENT;
    }
    
    
    private static String escapeHTML(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          char c = s.charAt(i);
          switch (c) {
              case '<': sb.append("&lt;"); break; // NOI18N
              case '>': sb.append("&gt;"); break; // NOI18N
              case '&': sb.append("&amp;"); break; // NOI18N
              case '"': sb.append("&quot;"); break; // NOI18N
              default: sb.append(c); break;
          }
        }
        return sb.toString();
    }
    
    private static String escapeXML(String s) {
        return s;
    }
    
    private static ExecutorService createExecutor(final String file) {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, "Export thread for " + file); // NOI18N
            }
        });
    }
    
    
    private static final class Exporter {
        
        private static final int WRT_BUF = 16384;
        private static final int STR_BUF = WRT_BUF * 2 - 512;
        
        private volatile boolean failed = false;
        
        private final File file;
        private StringBuilder buffer;
        
        private Writer writer; // created and accessed in Executor only
        private ExecutorService executor; // created and accessed in EDT only
        
        
        Exporter(File file) {
            this.file = file;
            buffer = new StringBuilder();
        }
        
        
        boolean failed() {
            return failed;
        }
        
        
        void write(char ch) {
            if (failed) return;
            
            buffer.append(ch);
            checkAutoFlush();
        }
        
        void writeln(char ch) {
            if (failed) return;
            
            buffer.append(ch).append(System.lineSeparator());
            checkAutoFlush();
        }
        
        void write(String string) {
            if (failed) return;
            
            buffer.append(string);
            checkAutoFlush();
        }
        
        void writeln(String string) {
            if (failed) return;
            
            buffer.append(string).append(System.lineSeparator());
            checkAutoFlush();
        }
        
        void writeln() {
            if (failed) return;
            
            buffer.append(System.lineSeparator());
            checkAutoFlush();
        }
        
        void flush() {
            if (failed) return;
            
            if (buffer.length() == 0) return;
            
            final StringBuilder _buffer = buffer;
            buffer = new StringBuilder();
            
            if (executor == null) executor = createExecutor(file.getName());
            executor.submit(new Runnable() {
                public void run() {
                    try {
                        if (writer == null) writer = createWriter(file);
                        writer.append(_buffer);
                    } catch (Throwable t) {
                        failed(t);
                    }
                }
            });
        }
        
        void close() {
            if (failed) return;
            
            flush();
            
            if (executor != null) executor.submit(new Runnable() {
                public void run() {
                    if (writer != null) try {
                        writer.close();
                    } catch (Throwable t) {
                        failed(t);
                    }
                }
            });
        }
        
        
        private void failed(Throwable t) {
            failed = true;
            LOGGER.log(Level.INFO, t.getMessage(), t);
            String msg = t.getLocalizedMessage().replace("<", "&lt;").replace(">", "&gt;"); // NOI18N
            ProfilerDialogs.displayError("<html><b>" + MSG_EXPORT_SNAPSHOT_FAILED + "</b><br><br>" + msg + "</html>"); // NOI18N
        }
        
        
        private void checkAutoFlush() {
            if (buffer.length() > STR_BUF) flush();
        }
        
        private static Writer createWriter(File file) throws IOException {
            file.toPath(); // will fail for invalid file
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder(); // NOI18N
            FileOutputStream out = new FileOutputStream(file);
            return new BufferedWriter(new OutputStreamWriter(out, encoder), WRT_BUF);
        }
        
    }
    
    
    // Do not create instances of this class
    private ExportUtils() {}
    
}
