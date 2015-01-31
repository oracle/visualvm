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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.api.ProfilerDialogs;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ExportUtils {
    
    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.swing.Bundle"); // NOI18N
    public static final String ACTION_EXPORT = messages.getString("ExportUtils_ActionExport"); // NOI18N
    private static final String NPS_FILE = messages.getString("ExportUtils_NpsFile"); // NOI18N
    private static final String CSV_FILE = messages.getString("ExportUtils_CsvFile"); // NOI18N
    private static final String HTML_FILE = messages.getString("ExportUtils_HtmlFile"); // NOI18N
    private static final String XML_FILE = messages.getString("ExportUtils_XmlFile"); // NOI18N
    private static final String PNG_FILE = messages.getString("ExportUtils_PngFile"); // NOI18N
    private static final String FILE_FILTER_DESCR = messages.getString("ExportUtils_FileFilterDescr"); // NOI18N
    private static final String MSG_CANNOT_OVERWRITE_SOURCE = messages.getString("ExportUtils_MsgCannotOverwriteSource"); // NOI18N
    private static final String MSG_EXPORT_SNAPSHOT_FAILED = messages.getString("ExportUtils_MsgExportSnapshotFailed"); // NOI18N
    private static final String MSG_EXPORT_IMAGE_FAILED = messages.getString("ExportUtils_MsgExportImageFailed"); // NOI18N
    private static final String MSG_NODATA = messages.getString("ExportUtils_MsgNoData"); // NOI18N
    private static final String TITLE_OVERWRITE_FILE = messages.getString("ExportUtils_TitleOverwriteFile"); // NOI18N
    private static final String MSG_OVERWRITE_FILE = messages.getString("ExportUtils_MsgOverwriteFile"); // NOI18N
    // -----
    
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
    
    public static class NPSExportProvider extends BaseExportProvider {
        
        private final File sourceFile;
        
        public NPSExportProvider(File sourceFile) {
            super(NPS_FILTER);
            this.sourceFile = sourceFile;
        }
        
        public void export(File targetFile) {
            if (targetFile.isFile() && targetFile.equals(sourceFile)) {
                ProfilerDialogs.displayError(MSG_CANNOT_OVERWRITE_SOURCE);
            } else {
                try {
                    Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                                                                         StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException ex) {
                    System.err.println(ex);
                    ProfilerDialogs.displayError(MSG_EXPORT_SNAPSHOT_FAILED);
                }
            }
        }
        
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
                        ImageIO.write(image, "PNG", targetFile); // NOI18N
                    } catch (IOException ex) {
                        System.err.println(ex);
                        ProfilerDialogs.displayError(MSG_EXPORT_IMAGE_FAILED);
                    }
                }
            });
        }
        
    }
    
    
    public static Action exportAction(final Exportable exportable, final String name, final Component parent) {
        Action action = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ExportProvider[] providers = exportable == null ? null : exportable.getProviders();
                
                if (providers == null || providers.length == 0) {
                    ProfilerDialogs.displayWarning(MSG_NODATA, name, null);
                } else {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    fileChooser.setMultiSelectionEnabled(false);
                    fileChooser.setDialogTitle(name);

                    fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());

                    for (ExportProvider provider : providers)
                        fileChooser.addChoosableFileFilter(provider.getFormatFilter());

                    showExportDialog(fileChooser, parent, providers);
                }
            }
        };
        
        return action;
    }
    
    private static void showExportDialog(final JFileChooser fileChooser, final Component parent, final ExportProvider[] providers) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileChooser.showDialog(parent, ACTION_EXPORT) != JFileChooser.APPROVE_OPTION) return;

                File targetFile = fileChooser.getSelectedFile();
                FileFilter filter = fileChooser.getFileFilter();

                for (ExportProvider provider : providers) {
                    FormatFilter format = provider.getFormatFilter();
                    if (filter.equals(format)) {
                        targetFile = checkFileExtesion(targetFile, format.getExtension());
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
    
    public static File checkFileExtesion(File file, String extension) {
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
            
            for (int row = 0; row < rowCount; row++) {
                for (int col = 0; col < columnCount; col++) {
                    ex.write(doubleQuote);
                    ex.write(table.getStringValue(row, col));
                    ex.write(doubleQuote);
                    if (col < columnCount - 1) ex.write(separator);
                    else ex.writeln();
                }
            }
        } finally {
            ex.close();
        }
        
        return true;
    }
    
    public static boolean exportHTML(ProfilerTable table, String name, File file) {
        Exporter ex = new Exporter(file);
        
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        
        try {
            ex.writeln("<html>"); // NOI18N
            
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

            for (int row = 0; row < rowCount; row++) {
                ex.writeln("   <tr>"); // NOI18N
                for (int col = 0; col < columnCount; col++) {
                    ex.write  ("    <td><pre>"); // NOI18N
                    ex.write  (escapeHTML(table.getStringValue(row, col)));
                    ex.writeln("</pre></td>"); // NOI18N
                }
                ex.writeln("   </tr>"); // NOI18N
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
        
        int rowCount = table.getRowCount();
        int columnCount = table.getColumnCount();
        
        try {
            ex.writeln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); // NOI18N
            ex.write  ("<ExportedView Name=\""); // NOI18N
            ex.write  (name);
            ex.writeln("\" type=\"table\">"); // NOI18N
            
            ex.write  (" <TableData NumRows=\""); // NOI18N
            ex.write  (Integer.toString(rowCount));
            ex.write  ("\" NumColumns=\""); // NOI18N
            ex.write  (Integer.toString(columnCount));
            ex.writeln("\">"); // NOI18N
            
            ex.writeln("  <TableHeader>"); // NOI18N
            for (int col = 0; col < columnCount; col++) {
                ex.write  ("   <TableColumn><![CDATA["); // NOI18N
                ex.write  (escapeXML(table.getColumnName(col)));
                ex.writeln("]]></TableColumn>"); // NOI18N
            }
            ex.writeln("  </TableHeader>"); // NOI18N

            ex.writeln("  <TableBody>"); // NOI18N
            for (int row = 0; row < rowCount; row++) {
                ex.writeln("   <TableRow>"); // NOI18N
                for (int col = 0; col < columnCount; col++) {
                    ex.write  ("    <TableColumn><![CDATA["); // NOI18N
                    ex.write  (escapeXML(table.getStringValue(row, col)));
                    ex.writeln("]]></TableColumn>"); // NOI18N
                }
                ex.writeln("   </TableRow>"); // NOI18N
            }
            ex.writeln("  </TableBody>"); // NOI18N
            
            ex.writeln(" </TableData>"); // NOI18N
            ex.writeln("</ExportedView>"); // NOI18N
        } finally {
            ex.close();
        }
        
        return true;
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
        
        private final File file;
        private StringBuilder buffer;
        
        private Writer writer; // created and accessed in Executor only
        private ExecutorService executor; // created and accessed in EDT only
        
        
        Exporter(File file) {
            this.file = file;
            buffer = new StringBuilder();
        }
        
        
        void write(char ch) {
            buffer.append(ch);
            checkAutoFlush();
        }
        
        void writeln(char ch) {
            buffer.append(ch).append(System.lineSeparator());
            checkAutoFlush();
        }
        
        void write(String string) {
            buffer.append(string);
            checkAutoFlush();
        }
        
        void writeln(String string) {
            buffer.append(string).append(System.lineSeparator());
            checkAutoFlush();
        }
        
        void writeln() {
            buffer.append(System.lineSeparator());
            checkAutoFlush();
        }
        
        void flush() {
            if (buffer.length() == 0) return;
            
            final StringBuilder _buffer = buffer;
            buffer = new StringBuilder();
            
            if (executor == null) executor = createExecutor(file.getName());
            executor.submit(new Runnable() {
                public void run() {
                    try {
                        if (writer == null) writer = createWriter(file);
                        writer.append(_buffer);
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            });
        }
        
        void close() {
            flush();
            
            if (executor != null) executor.submit(new Runnable() {
                public void run() {
                    if (writer != null) try {
                        writer.close();
                    } catch (IOException e) {
                        System.err.println(e);
                    }
                }
            });
        }
        
        
        private void checkAutoFlush() {
            if (buffer.length() > STR_BUF) flush();
        }
        
        private static Writer createWriter(File file) throws IOException {
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder(); // NOI18N
            FileOutputStream out = new FileOutputStream(file);
            return new BufferedWriter(new OutputStreamWriter(out, encoder), WRT_BUF);
        }
        
    }
    
}
