/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.ui;

/**
 *
 * @author Jiri Sedlacek
 */
public final class UIThresholds {
    
    // Non-responding threshold for populating the Tools menu
    public static final int TOOLS_LOAD = Integer.getInteger("heapviewer.toolsLoadThreshold", 100); // NOI18N
    
    // Non-responding threshold for populating views (Heap Summary, System Information etc.)
    public static final int VIEW_LOAD = Integer.getInteger("heapviewer.viewDataLoadThreshold", 50); // NOI18N
    
    // Non-responding threshold for populating children nodes in treetable views (Objects, Threads etc.)
    public static final int MODEL_CHILDREN = Integer.getInteger("heapviewer.modelChildrenLoadThreshold", 50); // NOI18N
    
    
    // Initial delay before ProgressNode starts displaying progress details
    public static final int PROGRESS_INITIAL_DELAY = Integer.getInteger("heapviewer.progressInitialDelay", 1000); // NOI18N
    
    // ProgressNode progress details update rate
    public static final int PROGRESS_REFRESH_RATE = Integer.getInteger("heapviewer.progressRefreshRate", 500); // NOI18N
    
    
    // Maximum number of classes displayed as roots in the Objects view
    public static final int MAX_TOPLEVEL_CLASSES = Integer.getInteger("heapviewer.toplevelClassesThreshold", 100000); // NOI18N
    
    // Maximum number of classes displayed as children of a class container in the Objects view
    public static final int MAX_CONTAINER_CLASSES = Integer.getInteger("heapviewer.containerClassesThreshold", 100); // NOI18N
    
    // Maximum number of instances displayed as roots in the Objects view
    public static final int MAX_TOPLEVEL_INSTANCES = Integer.getInteger("heapviewer.toplevelInstancesThreshold", 300); // NOI18N
    
    // Maximum number of instances displayed as children of a class in the Objects view
    public static final int MAX_CLASS_INSTANCES = Integer.getInteger("heapviewer.classInstancesThreshold", 100); // NOI18N
    
    // Maximum number of instances displayed as children of a class container in the Objects view
    public static final int MAX_CONTAINER_INSTANCES = Integer.getInteger("heapviewer.containerInstancesThreshold", 100); // NOI18N
    
    // Maximum number of items displayed as children of an array in the Objects view
    public static final int MAX_ARRAY_ITEMS = Integer.getInteger("heapviewer.arrayItemsThreshold", 100); // NOI18N
    
    // Maximum number of fields displayed as children of an instance in the Objects view
    public static final int MAX_INSTANCE_FIELDS = Integer.getInteger("heapviewer.instanceFieldsThreshold", 200); // NOI18N
    
    // Maximum number of references displayed as children of an instance in the Objects view
    public static final int MAX_INSTANCE_REFERENCES = Integer.getInteger("heapviewer.instanceReferencesThreshold", 100); // NOI18N
    
    
    // Number of sample objects
    public static final int SAMPLE_OBJECTS_COUNT = 100;
    
    // Minimum number of objects for which the sample objects node is created
    public static final int SAMPLE_OBJECTS_THRESHOLD = 1000;
    
    
    private UIThresholds() {}
    
}
