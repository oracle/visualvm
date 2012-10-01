/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package com.sun.tools.visualvm.profiling.snapshot;

import com.sun.tools.visualvm.core.datasource.DataSource;
import java.awt.Image;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.JComponent;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 * This is prototype to handle npss files. It uses reflection till the proper API is
 * available in snaptracer module.
 *
 * @author Tomas Hurka
 */
@NbBundle.Messages("MSG_SnapshotLoadFailedMsg=Error while loading snapshot: {0}")
class ProfilerSnapshotNPSS extends ProfilerSnapshot {

    private Object loadedSnapshot;

    ProfilerSnapshotNPSS(File file, DataSource master) {
        super(file, master);
        try {
            FileObject primary = FileUtil.toFileObject(file);
            FileObject uigestureFO = primary.getParent().getFileObject(primary.getName(), "log"); // NOI18N
            Class ideSnapshotClass = Class.forName("org.netbeans.modules.profiler.snaptracer.impl.IdeSnapshot");  // NOI18N
            Constructor c = ideSnapshotClass.getDeclaredConstructor(FileObject.class, FileObject.class);
            c.setAccessible(true);
            loadedSnapshot =  c.newInstance(primary, uigestureFO);

        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InstantiationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public LoadedSnapshot getLoadedSnapshot() {
        throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
    }

    @Override
    Image resolveIcon() {
        return ImageUtilities.mergeImages(CPU_ICON, NODE_BADGE, 0, 0);
    }

    @Override
    protected void remove() {
        super.remove();
        loadedSnapshot = null;
    }

    @Override
    JComponent getUIComponent() {
        try {
            // TracerModel model = new TracerModel(loadedSnapshot);
            Class tracerModelClass = Class.forName("org.netbeans.modules.profiler.snaptracer.impl.TracerModel");  // NOI18N
            Class ideSnapshotClass = Class.forName("org.netbeans.modules.profiler.snaptracer.impl.IdeSnapshot");  // NOI18N
            Constructor c = tracerModelClass.getDeclaredConstructor(ideSnapshotClass);
            c.setAccessible(true);
            Object tracerModel = c.newInstance(loadedSnapshot);
            //TracerController controller = new TracerController(model);
            Class tracerControllerClass = Class.forName("org.netbeans.modules.profiler.snaptracer.impl.TracerController");  // NOI18N
            Constructor cc = tracerControllerClass.getDeclaredConstructor(tracerModelClass);
            cc.setAccessible(true);
            Object tracerController = cc.newInstance(tracerModel);
            //TracerView tracer = new TracerView(model, controller);
            Class tracerViewClass = Class.forName("org.netbeans.modules.profiler.snaptracer.impl.TracerView");  // NOI18N
            Constructor tvc = tracerViewClass.getDeclaredConstructor(tracerModelClass, tracerControllerClass);
            tvc.setAccessible(true);
            Object tracerView = tvc.newInstance(tracerModel, tracerController);
            // tracer.createComponent();
            Method tvm = tracerViewClass.getDeclaredMethod("createComponent");  // NOI18N
            tvm.setAccessible(true);
            JComponent tracerViewComponent = (JComponent) tvm.invoke(tracerView);
            return tracerViewComponent;
        } catch (InstantiationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    @Override
    void closeComponent() {
        // no op
    }
}
