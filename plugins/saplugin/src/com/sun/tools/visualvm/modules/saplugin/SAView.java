/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.saplugin;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasupport.DataRemovedListener;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.coredump.CoreDump;
import java.beans.PropertyChangeEvent;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import org.openide.util.Utilities;
import com.sun.tools.visualvm.tools.sa.SaModelFactory;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.WeakListeners;

/**
 *
 * @author poonam
 */
public class SAView extends DataSourceView implements PropertyChangeListener, DataRemovedListener<DataSource>, ActionListener {

    private DataViewComponent dvc;
    DataSource dataSource;
    SAModelImpl saModel = null;
    private boolean isAttached = false;
    MasterViewSupport master;
    ThreadsView threadsView;
    OopInspectorView oopInspector;
    StackTraceViewer stackTraceViewer;
    CodeViewer codeviewer;
    FindPanel findPointer;
    FindPanel findInHeap;
    FindPanel findInCode;
    //Reusing an image from the sources:
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/modules/saplugin/resources/SA.png"; // NOI18N
    private String dsString = "Process";
    private boolean doNotShowMessage = false;
    public static boolean isClosing = false;


    public SAView(DataSource ds) {
        super(ds, "SA Plugin", new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 60, false);
        dataSource = ds;
        isAttached = false;

        ds.notifyWhenRemoved(this);
        ds.addPropertyChangeListener(Stateful.PROPERTY_STATE, WeakListeners.propertyChange(this, ds));

        if (ds instanceof Application) {
            dsString = "Process";
        } else if (ds instanceof CoreDump){
            dsString = "CoreDump";
        }
    }

    public DataViewComponent getDataViewComponent() {
        return dvc;
    }

    @Override
    protected void removed() {
        detachFromProcess(dataSource);
        dataSource = null;
    }
    public void propertyChange(PropertyChangeEvent evt) {
        dataRemoved(dataSource);
    }

    public void dataRemoved(DataSource ds) {        
        this.dataSource = null;
    }

    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    protected DataViewComponent createComponent() {
        // attachToProcess(dataSource);
        master = new MasterViewSupport(dataSource);
        //Master view:
        DataViewComponent.MasterView masterView = new DataViewComponent.MasterView("SA Plugin", null, master);
        DataViewComponent.MasterViewConfiguration masterConfiguration =
                new DataViewComponent.MasterViewConfiguration(false);
        dvc = new DataViewComponent(masterView, masterConfiguration);
        //addDetailsViews();
        if (saModel != null)
            saModel.setView(this);
        
        return dvc;
    }

    private void addDetailsViews() {
        Cursor cur = dvc.getCursor();
        dvc.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        //Add detail views to the component:
        threadsView = new ThreadsView("Java Threads", 10);
        stackTraceViewer = new StackTraceViewer("Java Stack Trace", 20);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Java Threads / Java Stack Trace", true), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(threadsView.getDetailsView(), DataViewComponent.TOP_LEFT);
        dvc.addDetailsView(stackTraceViewer.getDetailsView(), DataViewComponent.TOP_LEFT);

        oopInspector = new OopInspectorView(saModel, "Oop Inspector", 10);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Oop Inspector", true), DataViewComponent.TOP_RIGHT);
        dvc.addDetailsView(oopInspector.getDetailsView(), DataViewComponent.TOP_RIGHT);

        codeviewer = new CodeViewer("Code Viewer", 10);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Code Viewer", true), DataViewComponent.BOTTOM_LEFT);
        dvc.addDetailsView(codeviewer.getDetailsView(), DataViewComponent.BOTTOM_LEFT);

        findPointer = new FindPanel("Find Pointer", 10, 1);
        findInHeap = new FindPanel("Find Value in Heap", 20, 2);
        findInCode = new FindPanel("Find Value in CodeCache", 30, 3);
        dvc.configureDetailsArea(new DataViewComponent.DetailsAreaConfiguration(
                "Find Panel", true), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(findPointer.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(findInHeap.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);
        dvc.addDetailsView(findInCode.getDetailsView(), DataViewComponent.BOTTOM_RIGHT);

        dvc.hideDetailsArea(DataViewComponent.BOTTOM_LEFT);
        dvc.hideDetailsArea(DataViewComponent.BOTTOM_RIGHT);

        dvc.setCursor(cur);

    }


    public OopInspectorView getOopInspectorView() {
        return oopInspector;
    }

    public void updateOopInspectorView(Object oop) {
        Cursor cur = dvc.getCursor();
        dvc.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        oopInspector.refresh(oop);
        dvc.showDetailsArea(DataViewComponent.TOP_RIGHT);
        dvc.selectDetailsView(oopInspector.getDetailsView());

        dvc.setCursor(cur);
    }

    public void updateStackTraceView(Object thread) {
        Cursor cur = dvc.getCursor();
        dvc.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        stackTraceViewer.refresh(thread);
        dvc.selectDetailsView(stackTraceViewer.getDetailsView());
        dvc.showDetailsArea(DataViewComponent.TOP_LEFT);

        dvc.setCursor(cur);
    }

    public boolean attachToProcess(DataSource ds) {
        if (isAttached) {
            return true;
        }
        Cursor cur = dvc.getCursor();
        dvc.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        try {
            saModel = (SAModelImpl) SaModelFactory.getSAAgentFor(ds);
            isAttached = saModel.attach();

            dvc.setCursor(cur);
            return isAttached;

        } catch (Exception e) {
            e.printStackTrace();
            NotifyDescriptor nd = new NotifyDescriptor.Message(e.getCause().toString(), NotifyDescriptor.INFORMATION_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);

//            System.out.println(e.getCause().toString());
//            if (e.getCause().toString().contains("Windbg Error: AttachProcess failed!")) {
            detachFromProcess(ds);
//            }            
        }
        dvc.setCursor(cur);
        return false;
    }

    public void detachFromProcess(DataSource ds) {

        try {
            saModel = (SAModelImpl) SaModelFactory.getSAAgentFor(ds);            
            saModel.detach();
            isAttached = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshView(DataSource ds) {
        Cursor cur = dvc.getCursor();
        dvc.setCursor(new Cursor(Cursor.WAIT_CURSOR));

        threadsView.refresh();
        oopInspector.refresh(null);
        stackTraceViewer.refresh(null);
        codeviewer.refresh();
        findPointer.refresh();
        findInHeap.refresh();
        findInCode.refresh();

        dvc.showDetailsArea(DataViewComponent.TOP_LEFT);
        dvc.selectDetailsView(threadsView.getDetailsView());
        dvc.hideDetailsArea(DataViewComponent.BOTTOM_LEFT);
        dvc.hideDetailsArea(DataViewComponent.BOTTOM_RIGHT);
        dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);
        dvc.showDetailsArea(DataViewComponent.TOP_LEFT);
        dvc.setCursor(cur);

    }

    public void removeDetailsViews(DataSource ds) {
        threadsView.remove();
        oopInspector.remove();
        stackTraceViewer.remove();
        codeviewer.remove();
        findPointer.remove();
        findInHeap.remove();
        findInCode.remove();

        dvc.hideDetailsArea(DataViewComponent.TOP_LEFT);
        dvc.hideDetailsArea(DataViewComponent.TOP_RIGHT);
        dvc.hideDetailsArea(DataViewComponent.BOTTOM_LEFT);
        dvc.hideDetailsArea(DataViewComponent.BOTTOM_RIGHT);
    }

    // ----------Master View----------------------------
    private class MasterViewSupport extends JPanel {

        private JButton attachButton;
        JLabel vmInformation;
        DataSource dataSource;
        boolean firstTimeShow = true;

        public MasterViewSupport(DataSource ds) {
            this.dataSource = ds;
            saModel = (SAModelImpl) SaModelFactory.getSAAgentFor(dataSource);
            initComponents(dataSource);

            if (dataSource instanceof Application) {
                addHierarchyListener(new HierarchyListener() {

                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                            if (isShowing()) {
                                /*                                if (!isAttached && (dataSource != null)) {
                                if (attachToProcess(dataSource)) {
                                if (firstTimeShow) {
                                try {
                                saModel.readData();
                                } catch (Exception ex) {
                                ex.printStackTrace();
                                }
                                addVMInfo(dataSource);
                                addDetailsViews();
                                firstTimeShow = false;
                                }
                                }
                                setAttachButtonText("Detach from process");
                                refreshView(dataSource);
                                }   */
                            } else {/*
                                if (!dvc.isVisible())
                                    return;
                                
                                Component parent = dvc.getParent();
                                Component top = null;
                                while (parent != null) {
                                    top = parent;
                                    parent = top.getParent();
                                }
                                if (!top.isShowing())
                                    return;

                                if (!top.isVisible())
                                    return;
                                
                               if (isAttached && (dataSource != null) && (doNotShowMessage == false) ) {
                                   if (isClosing == false) {
                                    MessagePanel messagePanel = new MessagePanel();
                                    DialogDescriptor dd = new DialogDescriptor(messagePanel, "Warning!");
                                    Object result = DialogDisplayer.getDefault().notify(dd);
                                    if (result == NotifyDescriptor.OK_OPTION) {
                                        detachFromProcess(dataSource);
                                        removeDetailsViews(dataSource);
                                        setAttachButtonText("Attach to " + dsString);
                                    }
                                   }
                                }*/
                            }
                        }
                    }
                });
            }
        }

        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView("SAPlugin", null, this);  // NOI18N
        }


        private void addVMInfo(final DataSource ds) {
            JLabel label = new JLabel();
            String vminfo = null;
            vminfo = saModel.getVmVersion() + " " + saModel.getVmName() + " " + saModel.getVmInfo();
            label.setText(vminfo);
            add(label);
        }

        private void initComponents(final DataSource ds) {
            setLayout(new BorderLayout());
            setOpaque(false);
            String buttonLabel = null;

            if (isAttached) {
                buttonLabel = "Detach from " + dsString;
            } else {
                buttonLabel = "Attach to " + dsString;
            }


            attachButton = new JButton(new AbstractAction(buttonLabel) {

                public void actionPerformed(ActionEvent e) {
                    if (ds == null) {
                        return;
                    }
                    Cursor cur = dvc.getCursor();
                    dvc.setCursor(new Cursor(Cursor.WAIT_CURSOR));

                    if (isAttached) {
                        detachFromProcess(ds);
                    //    removeWarning();
                        removeDetailsViews(ds);                        
                        setAttachButtonText("Attach to " + dsString);
                    } else {
                        if (attachToProcess(ds)) {
                            if (firstTimeShow) {
                                try {
                                    saModel.readData();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                                addVMInfo(dataSource);
                                addDetailsViews();
                                firstTimeShow = false;
                            }
                            refreshView(ds);

                            if (isAttached && (ds instanceof Application) && (doNotShowMessage == false)) {
                                if (isClosing == false) {
                                    MessagePanel messagePanel = new MessagePanel();
                                    //DialogDescriptor dd = new DialogDescriptor(messagePanel, "SAPlugin Warning !");
                                    NotifyDescriptor nd = new NotifyDescriptor.Message((Object) messagePanel, NotifyDescriptor.INFORMATION_MESSAGE) ;

                                    Object result = DialogDisplayer.getDefault().notify(nd);
                                    /*if (result == NotifyDescriptor.OK_OPTION) {
                                        detachFromProcess(dataSource);
                                        removeDetailsViews(dataSource);
                                        setAttachButtonText("Attach to " + dsString);
                                    }
                                     * */
                                }
                            }
                            
                            setAttachButtonText("Detach from " + dsString);
                        }
                    }

                    dvc.setCursor(cur);
                }
            });
            attachButton.setEnabled(true);

            JPanel buttonsArea = new JPanel(new BorderLayout());
            buttonsArea.setOpaque(false);
            JPanel buttonsContainer = new JPanel(new BorderLayout(3, 0));
            buttonsContainer.setOpaque(false);
            buttonsContainer.setBorder(BorderFactory.createEmptyBorder(9, 5, 20, 10));
            buttonsContainer.add(attachButton, BorderLayout.WEST);
            buttonsArea.add(buttonsContainer, BorderLayout.NORTH);
            add(buttonsArea, BorderLayout.EAST);

        }

        void setAttachButtonText(String s) {
            if (attachButton != null) {
                attachButton.setText(s);
            }
        }
    }

    //Details views
    private class ThreadsView extends JComponent {

        private JPanel threads;
        private String caption;
        private int position;
        private DataViewComponent.DetailsView detailsView;
        private Object saListener = null;
        private JavaThreadsPanel threadsPanel = null;

        public ThreadsView(String caption, int position) {
            this.caption = caption;
            this.position = position;
            detailsView = null;
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            if (detailsView == null) {
                detailsView = new DataViewComponent.DetailsView(caption, null, position, this, null);
            }
            return detailsView;
        }

        public void refresh() {
            this.removeAll();
            threadsPanel = saModel.createJavaThreadsPanel();
            if (threadsPanel != null) {
                saListener = saModel.getSAListener();
                threadsPanel.setListener(saListener);
                threads = threadsPanel.getPanel();
                add(threads, BorderLayout.CENTER);
            }
        }

        private void initComponents() {
            setLayout(new BorderLayout());
        }

        public void remove() {
           // threadsPanel.removeListener(saListener);
            this.removeAll();
        }
    }

    private class CodeViewer extends JComponent {

        private JPanel codeviewer;
        private String caption;
        private int position;
        private DataViewComponent.DetailsView detailsView;

        public CodeViewer(String caption, int position) {
            this.caption = caption;
            this.position = position;
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            if (detailsView == null) {
              detailsView = new DataViewComponent.DetailsView(caption, null, position, this, null);
            }
            return detailsView;
        }

        public void refresh() {
            this.removeAll();
            try {
                codeviewer = saModel.createCodeViewerPanel().getPanel();
            } catch (Throwable t) {
                NotifyDescriptor nd = new NotifyDescriptor.Message("Code Viewer is not yet implemented for this platform", NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                dvc.removeDetailsView(detailsView);
                return;
            }
            add(codeviewer, BorderLayout.CENTER);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
        }

        public void remove() {
            this.removeAll();
        }
    }

    private class FindPanel extends JComponent {

        private JPanel findPanel;
        private String caption;
        private int position;
        private int mode;

        public FindPanel(String caption, int position, int mode) {
            this.caption = caption;
            this.position = position;
            this.mode = mode;
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            return new DataViewComponent.DetailsView(caption, null, position, this, null);
        }

        public void refresh() {
            this.removeAll();
            switch (mode) {
                case 1:
                    findPanel = saModel.createFindPointerPanel().getPanel();
                    break;
                case 2:
                    findPanel = saModel.createFindInHeapPanel().getPanel();
                    break;
                case 3:
                    findPanel = saModel.createFindInCodeCachePanel().getPanel();
                    break;
            }
            add(findPanel, BorderLayout.CENTER);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
        }

        public void remove() {
            this.removeAll();            
        }
    }

    private class StackTraceViewer extends JComponent {

        private JPanel traceViewer;
        private String caption;
        private int position;
        private DataViewComponent.DetailsView detailsView;

        public StackTraceViewer(String caption, int position) {
            this.caption = caption;
            this.position = position;
            detailsView = null;
            initComponents();
        }

        public DataViewComponent.DetailsView getDetailsView() {
            if (detailsView == null) {
                detailsView = new DataViewComponent.DetailsView(caption, null, position, this, null);
            }
            return detailsView;
        }

        public void refresh(Object thread) {
            this.removeAll();
            try {
                JavaStackTracePanel tracePanel = saModel.createJavaStackTracePanel();

                if (thread != null) {
                    tracePanel.setJavaThread(thread);
                }
                traceViewer = tracePanel.getPanel();
            } catch (Throwable t) {
                NotifyDescriptor nd = new NotifyDescriptor.Message("Java Stack Trace Viewer is not yet implemented for this platform", NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(nd);
                dvc.removeDetailsView(detailsView);
                return;
            }
            add(traceViewer, BorderLayout.CENTER);
        }

        private void initComponents() {
            setLayout(new BorderLayout());
        }

        public void remove() {
            this.removeAll();
        }
    }

    public class MessagePanel extends JPanel implements ItemListener {

        private String warning = new String("" +
                "<html><br>&nbsp Other tabs for this process will not be usable as long as<br>" +
                "&nbsp SAPlugin is attached to it. Please detach SAPlugin from<br>" +
                "&nbsp the process before using other tabs.<br><br></html>");
        
        public MessagePanel() {
         initComponents();
        }

        void initComponents() {
            setLayout(new BorderLayout());
            //setSize(150,100);

            JLabel msg = new JLabel(warning);
            this.add(msg,BorderLayout.NORTH);

            JCheckBox check = new JCheckBox("Do not show this message again");
            add(check, BorderLayout.SOUTH);
            check.addItemListener(this);
        }

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                doNotShowMessage = true;
            }
        }
    }
}

    
