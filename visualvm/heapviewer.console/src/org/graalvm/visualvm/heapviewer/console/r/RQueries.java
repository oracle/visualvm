/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.heapviewer.console.r;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
//import org.graalvm.visualvm.lib.profiler.heapwalk.OQLSupport;
//import org.graalvm.visualvm.lib.profiler.heapwalk.ui.icons.HeapWalkerIcons;
//import org.graalvm.visualvm.lib.profiler.oql.repository.api.OQLQueryCategory;
//import org.graalvm.visualvm.lib.profiler.oql.repository.api.OQLQueryDefinition;
//import org.graalvm.visualvm.lib.profiler.oql.repository.api.OQLQueryRepository;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RQueries_LoadingProgress=Loading Saved R scripts...",
    "RQueries_PopupCaptionLoad=<html><b>Load R Script</b>: Select Source</html>",
    "RQueries_PopupCustomScripts=Custom Scripts:",
    "RQueries_PopupNoSaved=<no saved scripts>",
    "RQueries_PopupExternalScripts=External Scripts:",
    "RQueries_PopupLoadFromFile=Load From File...",
    "RQueries_PopupPredefinedScripts=Predefined Scripts:",
    "RQueries_PopupLoadingScripts=Loading Saved R scripts...",
    "RQueries_PopupCaptionSave=<html><b>Save R Script</b>: Select Target</html>",
    "RQueries_PopupSaveNew=Save As New...",
    "RQueries_PopupSaveFile=Save To File...",
    "RQueries_LoadExternalCaption=Load External R Script",
    "RQueries_RFileFilter=R Script Files ({0})",
    "RQueries_InvalidScript=Invalid R script file.",
    "RQueries_LoadFailed=Failed to load R script.",
    "RQueries_SaveFailed=Failed to save R script.",
    "RQueries_SaveExternalCaption=Save External R Script",
    "RQueries_CurrentScriptFlag=[current]"
        
})
final class RQueries {
    
    static final Icon ICON_LOAD = ImageUtilities.image2Icon(ImageUtilities.loadImage(RQueries.class.getPackage().getName().replace('.', '/') + "/rLoad.png", true));
    static final Icon ICON_SAVE = Icons.getIcon(GeneralIcons.SAVE);
    private static final Icon ICON_EMPTY = Icons.getIcon(GeneralIcons.EMPTY);
    
    private static final int EXTERNAL_QUERIES_CACHE = 5;
    
    
    private static RQueries INSTANCE;
    
//    private CustomOQLQueries customQueries;
//    private List<? extends OQLQueryCategory> predefinedCategories;
    
    private List<Query> externalQueries;
    
//    private JPopupMenu tempPopup;
//    private Query tempCurrentQuery;
//    private String tempQueryText;
//    private Handler tempHandler;
//    private boolean tempLoad;
    
    
    public static synchronized RQueries instance() {
        if (INSTANCE == null) INSTANCE = new RQueries();
        return INSTANCE;
    }
    
    
    public void populateLoadQuery(JPopupMenu popup, Query currentQuery, final Handler handler) {
//        if (customQueries == null || predefinedCategories == null) {
//            JMenuItem progressItem = new JMenuItem(Bundle.RQueries_LoadingProgress(), Icons.getIcon(HeapWalkerIcons.PROGRESS));
//            progressItem.setEnabled(false);
//            popup.add(progressItem);
//            
//            tempPopup = popup;
//            tempCurrentQuery = currentQuery;
//            tempHandler = handler;
//            
//            tempLoad = true;
//            
//            return;
//        }
//        
//        tempPopup = null;
//        tempCurrentQuery = null;
//        tempQueryText = null;
//        tempHandler = null;

        popup.add(new PopupCaption(Bundle.RQueries_PopupCaptionLoad()));
        
        popup.add(new PopupSpacer(3));
        
        // --- Test query for development purposes
        
        popup.add(new PopupSeparator("Sample Scripts"));
        
        String script1 = "grid.rect(width = 0.5, height = 0.45, gp=gpar(col=\"blue\",lwd=3))\n" +
                         "grid.circle(x = 0.5, y = 0.5, r = 0.45, gp=gpar(col=\"red\",lwd=10))\n" +
                         "grid.text(\"Box and Circle\")";
        Query sample1 = new Query(script1, "Sample Script 1", "Sample script drawing blue rectangle and red circle", null);
        popup.add(new QueryMenuItem(sample1, currentQuery, null, ICON_LOAD, null, handler));
        String script2 = "s<-HeapClasses[order(HeapClasses$Instances,decreasing=TRUE),];\n" +
                         "x<-s[1:15,];\n" +
                         "print(x);";
        Query sample2 = new Query(script2, "Sample Script 2", "Sorts classes by number of instances and displays first 15 rows", null);
        popup.add(new QueryMenuItem(sample2, currentQuery, null, ICON_LOAD, null, handler));
        
        // ---
        
//        popup.add(new PopupSeparator(Bundle.RQueries_PopupCustomScripts()));
//        
//        if (customQueries.isEmpty()) {
//            JMenuItem noItems = new JMenuItem(Bundle.RQueries_PopupNoSaved(), ICON_EMPTY);
//            noItems.setEnabled(false);
//            popup.add(noItems);
//        } else {
//            for (final OQLSupport.Query query : customQueries.list())
//                popup.add(new QueryMenuItem(query, currentQuery, ICON_LOAD, null, handler));
//        }
        
        popup.add(new PopupSeparator(Bundle.RQueries_PopupExternalScripts()));
        popup.add(new JMenuItem(Bundle.RQueries_PopupLoadFromFile(), ICON_EMPTY) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { loadFromFile(handler); }
                });
            }
        });
        if (externalQueries != null && !externalQueries.isEmpty()) {
            popup.add(new PopupSpacer(5));
            for (final Query query : externalQueries)
                popup.add(new QueryMenuItem(query, currentQuery, null, ICON_LOAD, null, handler));
        }
        
//        if (!predefinedCategories.isEmpty()) {
//            popup.add(new PopupSpacer(3));
//            popup.add(new PopupSeparator(Bundle.RQueries_PopupPredefinedScripts()));
//            
//            for (OQLQueryCategory category : predefinedCategories) {
//                final JMenu categoryMenu = new JMenu(category.getName()) {
//                    protected void fireStateChanged() {
//                        boolean active = isSelected() || isArmed();
//                        StatusDisplayer.getDefault().setStatusText(active ? category.getDescription() : null);
//                        super.fireStateChanged();
//                    }
//                };
////                categoryMenu.setToolTipText(category.getDescription());
//                popup.add(categoryMenu);
//                
//                List<? extends OQLQueryDefinition> queries = category.listQueries();
//                for (final OQLQueryDefinition queryDef : queries)
//                    categoryMenu.add(new QueryMenuItem(new OQLSupport.Query(queryDef), currentQuery, ICON_LOAD, categoryMenu, handler));
//            }
//        }
    }
    
    public void populateSaveQuery(JPopupMenu popup, final Query currentQuery, final String queryText, final Handler handler) {
//        if (customQueries == null) {
//            JMenuItem progressItem = new JMenuItem(Bundle.RQueries_PopupLoadingScripts(), Icons.getIcon(HeapWalkerIcons.PROGRESS));
//            progressItem.setEnabled(false);
//            popup.add(progressItem);
//            
//            tempPopup = popup;
//            tempCurrentQuery = currentQuery;
//            tempQueryText = queryText;
//            tempHandler = handler;
//            
//            tempLoad = false;
//            
//            return;
//        }
//        
//        tempPopup = null;
//        tempCurrentQuery = null;
//        tempQueryText = null;
//        tempHandler = null;
        
        popup.add(new PopupCaption(Bundle.RQueries_PopupCaptionSave()));
        
        popup.add(new PopupSpacer(3));
//        popup.add(new PopupSeparator(Bundle.RQueries_PopupCustomScripts()));
//        
//        popup.add(new JMenuItem(Bundle.RQueries_PopupSaveNew(), ICON_EMPTY) {
//            protected void fireActionPerformed(ActionEvent e) {
//                super.fireActionPerformed(e);
//                
//                OQLSupport.Query query = OQLQueryCustomizer.saveCustomizer(currentQuery, queryText);
//                if (query == null) return;
//                
//                String name = query.getName();
//                int nameExt = 0;
//                while (containsQuery(customQueries.list(), query))
//                    query.setName(name + " " + ++nameExt); // NOI18N
//                
//                customQueries.add(query);
//                
//                if (handler != null) handler.querySelected(query);
//            }
//        });
//        
//        if (!customQueries.isEmpty()) {
//            popup.add(new PopupSpacer(5));
//            for (final OQLSupport.Query query : customQueries.list())
//                popup.add(new QueryMenuItem(query, currentQuery, ICON_SAVE, null, handler));
//        }
        
        popup.add(new PopupSeparator(Bundle.RQueries_PopupExternalScripts()));
        popup.add(new JMenuItem(Bundle.RQueries_PopupSaveFile(), ICON_EMPTY) {
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { saveToFile(currentQuery, queryText, handler); }
                });
            }
        });
        if (externalQueries != null && !externalQueries.isEmpty()) {
            popup.add(new PopupSpacer(5));
            for (final Query query : externalQueries)
                popup.add(new QueryMenuItem(query, currentQuery, queryText, ICON_SAVE, null, handler) {
                    protected void handleActionPerformed(Query query, Handler handler) {
                        saveQuery(query, handler);
                    }
                });
        }  
    }
    
    
    private void loadAllQueries() {
//        new RequestProcessor("OQL Scripts Loader").post(new Runnable() { // NOI18N
//            public void run() {
//                customQueries = CustomOQLQueries.instance();
//                predefinedCategories = OQLQueryRepository.getInstance().listCategories();
//                
//                SwingUtilities.invokeLater(new Runnable() {
//                    public void run() {
//                        if (tempPopup != null && tempPopup.isShowing()) {
//                            JPopupMenu popup = tempPopup;
//                            popup.removeAll();
//                            if (tempLoad) populateLoadQuery(popup, tempCurrentQuery, tempHandler);
//                            else populateSaveQuery(popup, tempCurrentQuery, tempQueryText, tempHandler);
//                            popup.pack();
//                        }
//                    }
//                });
//            }
//        });
    }
    
    
    private File lastDirectory;
    
    private void loadFromFile(final Handler handler) {
        JFileChooser chooser = new JFileChooser();

        if (lastDirectory != null) chooser.setCurrentDirectory(lastDirectory);

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogTitle(Bundle.RQueries_LoadExternalCaption());
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String fname = f.getName().toLowerCase();
                if (fname.endsWith(".r") || fname.endsWith(".txt")) return true; // NOI18N
                return false;
            }
            public String getDescription() {
                return Bundle.RQueries_RFileFilter("*.r, *.txt"); // NOI18N
            }
        });

        if (chooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    try {
                        if (!file.isFile() || !file.canRead()) {
                            ProfilerDialogs.displayError(Bundle.RQueries_InvalidScript());
                            return;
                        }
                        
                        String script = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        String name = file.getName();
                        String description = file.getAbsolutePath();
                        final Query query = new Query(script, name, description, file);
                        
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                handler.querySelected(query);
                        
                                if (externalQueries == null) externalQueries = new ArrayList(EXTERNAL_QUERIES_CACHE);
                                if (containsQuery(externalQueries, query)) return;

                                if (externalQueries.size() == EXTERNAL_QUERIES_CACHE)
                                    externalQueries.remove(externalQueries.size() - 1);

                                externalQueries.add(0, query);
                            }
                        });
                    } catch (IOException ex) {
                        ProfilerDialogs.displayError(Bundle.RQueries_LoadFailed());
                        Exceptions.printStackTrace(ex);
                    }
                }
            });
        }
    }
    
    private void saveToFile(Query query, final String queryText, final Handler handler) {
        JFileChooser chooser = new JFileChooser();
        
        if (query == null) {
            String name = "query.r"; // NOI18N
            String descr = lastDirectory == null ? null : new File(lastDirectory, name).getPath();
            query = new Query(queryText, name, descr, null);
        }
        
        String descr = query.getDescription();
        File defaultFile = descr == null ? null : new File(descr);
        if (defaultFile != null && defaultFile.isFile()) {
            chooser.setSelectedFile(defaultFile);
        } else {
            if (lastDirectory == null) defaultFile = new File(query.getName());
            else defaultFile = new File(lastDirectory, query.getName());
            if (lastDirectory != null) chooser.setCurrentDirectory(lastDirectory);
            chooser.setSelectedFile(defaultFile);
        }

        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogTitle(Bundle.RQueries_SaveExternalCaption());
        chooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String fname = f.getName().toLowerCase();
                if (fname.endsWith(".r") || fname.endsWith(".txt")) return true; // NOI18N
                return false;
            }
            public String getDescription() {
                return Bundle.RQueries_RFileFilter("*.r, *.txt"); // NOI18N
            }
        });

        if (chooser.showSaveDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            
            String fname = file.getName().toLowerCase();
            if (!fname.endsWith(".r") && !fname.endsWith(".txt")) // NOI18N
                file = new File(file.getParentFile(), file.getName() + ".r"); // NOI18N
            
            String script = queryText;
            String name = file.getName();
            String description = file.getAbsolutePath();
            
            saveQuery(new Query(script, name, description, file), handler);
        }
    }
    
    private void saveQuery(final Query query, final Handler handler) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                try {
                    File file = query.getFile();

                    if (file.isFile() && !file.canWrite()) {
                        ProfilerDialogs.displayError(Bundle.RQueries_InvalidScript());
                        return;
                    }

                    Files.write(file.toPath(), query.getScript().getBytes());

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            handler.querySelected(query);
                            
                            if (externalQueries == null) externalQueries = new ArrayList(EXTERNAL_QUERIES_CACHE);
                            if (containsQuery(externalQueries, query)) return;

                            if (externalQueries.size() == EXTERNAL_QUERIES_CACHE)
                                externalQueries.remove(externalQueries.size() - 1);

                            externalQueries.add(0, query);
                        }
                    });
                } catch (IOException ex) {
                    ProfilerDialogs.displayError(Bundle.RQueries_SaveFailed());
                    Exceptions.printStackTrace(ex);
                }
            }
        });
    }
    
    
    private static boolean sameQuery(Query query1, Query query2) {
        if (query1 == null || query2 == null) return false;
        return query1.getName().equals(query2.getName());
    }
    
    private static boolean containsQuery(List<Query> queries, Query query) {
        for (Query q : queries)
            if (sameQuery(q, query)) return true;
        return false;
    }
    
    
    private RQueries() {
        loadAllQueries();
    }
    
    
    static class Handler {
        
        protected void querySelected(Query query) {}
        
    }
    
    
    private static class PopupCaption extends JPanel {
        
        PopupCaption(String caption) {
            super(new BorderLayout());
            
            setOpaque(true);
            setBackground(UIUtils.getUnfocusedSelectionBackground());
//            setBackground(UIUtils.getProfilerResultsBackground());
//            setBackground(UIManager.getColor("InternalFrame.borderHighlight"));
//            setBackground(UIManager.getColor("ToolTip.background"));
            
            JLabel captionL = new JLabel(caption);
            captionL.setForeground(UIUtils.getUnfocusedSelectionForeground());
//            captionL.setForeground(UIManager.getColor("InternalFrame.activeTitleForeground"));
//            captionL.setBorder(BorderFactory.createEmptyBorder(3, 3, 4, 3));
            captionL.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 40));
            add(captionL, BorderLayout.CENTER);
            
//            add(UIUtils.createHorizontalSeparator(), BorderLayout.SOUTH);
//            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtils.getDisabledLineColor().brighter()));
        }
        
    }
    
    private static class PopupSpacer extends JPanel {
        
        private final int size;
        
        PopupSpacer(int size) {
            this.size = size;
            setOpaque(false);
        }
        
        public Dimension getMinimumSize() {
            return new Dimension(0, size);
        }
        
        public Dimension getPreferredSize() {
            return getMinimumSize();
        }
        
    }
    
    private static class PopupSeparator extends JPanel {
    
        public PopupSeparator(String text) {
            setLayout(new BorderLayout());
            setOpaque(false);

            JLabel l = new JLabel(text);
            l.setBorder(BorderFactory.createEmptyBorder(8, 5, 3, 3));
            if (UIUtils.isWindowsLookAndFeel()) l.setOpaque(true);
            l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() - 1));
            if (UIUtils.isWindowsLookAndFeel()) l.setForeground(UIUtils.getDisabledLineColor());

            add(l, BorderLayout.WEST);

            if (UIUtils.isGTKLookAndFeel()) {
                add(UIUtils.createHorizontalSeparator(), BorderLayout.CENTER);
            } else {
                JComponent sep = new JPopupMenu.Separator();
                add(sep, BorderLayout.CENTER);

                if (UIUtils.isOracleLookAndFeel()) {
                    setOpaque(true);
                    setBackground(sep.getBackground());
                    l.setForeground(sep.getForeground());
                }
            }
        }

        public void doLayout() {
            super.doLayout();
            Component c = getComponent(1);

            int h = c.getPreferredSize().height;
            Rectangle b = c.getBounds();

            b.y = (b.height - h) / 2;
            b.height = h;
            c.setBounds(b);
        }

        public Dimension getPreferredSize() {
            Dimension d = getComponent(0).getPreferredSize();
            d.width += 25;
            return d;
        }

    }
    
    private static class QueryMenuItem extends JMenuItem {
        
        private final Query query;
        private final String queryText;
        private final Icon icon;
        private final Handler handler;
        
        QueryMenuItem(Query query, Query current, String queryText, Icon icon, JMenu owner, Handler handler) {
            super(getName(query, current, owner), ICON_EMPTY);
            
            this.query = query;
            this.queryText = queryText;
            this.icon = icon;
            this.handler = handler;
        }
        
        protected void fireActionPerformed(ActionEvent e) {
            super.fireActionPerformed(e);
            if (queryText != null) query.setScript(queryText);
            handleActionPerformed(query, handler);
        }
        
        protected void handleActionPerformed(Query query, Handler handler) {
            handler.querySelected(query);
        }
        
        protected void fireStateChanged() {
            boolean active = isSelected() || isArmed();
            setIcon(active ? icon : ICON_EMPTY);
            StatusDisplayer.getDefault().setStatusText(active ? query.getDescription() : null);
            super.fireStateChanged();
        }
        
        private static String getName(Query query, Query current, JMenu owner) {
            String name = query.getName();
            if (sameQuery(query, current)) {
                name = "<html><b>" + name + "</b>&nbsp;<span style='color: gray;'>" + Bundle.RQueries_CurrentScriptFlag() + "</span></html>"; // NOI18N
                if (owner != null) owner.setText("<html><b>" + owner.getText() + "</b></html>"); // NOI18N
            }
            return name;
        }
        
    }
    
    
    // copied from OQLSupport.Query
    public static final class Query {

        private String script;
        private String name;
        private String description;
        private File file;

        
//        public Query(OQLQueryDefinition qdef) {
//            this(qdef.getContent(), qdef.getName(), qdef.getDescription());
//        }

        public Query(String script, String name, String description, File file) {
            setScript(script);
            setName(name);
            setDescription(description);
            this.file = file;
        }


        public void setScript(String script) {
            if (script == null)
                throw new IllegalArgumentException("Script cannot be null"); // NOI18N
            this.script = script;
        }

        public String getScript() {
            return script;
        }

        public void setName(String name) {
            this.name = normalizeString(name);
            if (this.name == null)
                throw new IllegalArgumentException("Name cannot be null"); // NOI18N
        }

        public String getName() {
            return name;
        }

        public void setDescription(String description) {
            this.description = normalizeString(description);
        }

        public String getDescription() {
            return description;
        }
        
        public File getFile() {
            return file;
        }

        public String toString() {
            return name;
        }

        private static String normalizeString(String string) {
            String normalizedString = null;
            if (string != null) {
                normalizedString = string.trim();
                if (normalizedString.length() == 0) normalizedString = null;
            }
            return normalizedString;
        }

    }
    
}
