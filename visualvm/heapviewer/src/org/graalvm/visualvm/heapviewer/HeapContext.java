/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapviewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public final class HeapContext {
    
    private final File file;
    private final Lookup.Provider project;
    private final HeapFragment fragment;
    
    private Collection<HeapContext> otherContexts;
    
    
    private HeapContext(File file, Lookup.Provider project, HeapFragment fragment) {
        this.file = file;
        this.project = project;
        this.fragment = fragment;
    }
    
    public File getFile() {
        return file;
    }
    
    
    public Lookup.Provider getProject() {
        return project;
    }
    
    public HeapFragment getFragment() {
        return fragment;
    }
    
    
    public Collection<HeapContext> getOtherContexts() {
        return otherContexts;
    }
    
    
    public static HeapContext[] allContexts(HeapViewer heapViewer) {
        File file = heapViewer.getFile();
        Lookup.Provider project = heapViewer.getProject();
        
        List<HeapFragment> fragments = heapViewer.getFragments();
        List<HeapContext> contexts = new ArrayList(fragments.size());
        
        for (HeapFragment fragment : fragments)
            contexts.add(new HeapContext(file, project, fragment));
        
        for (HeapContext context : contexts) {
            List<HeapContext> otherContexts = new ArrayList(contexts);
            otherContexts.remove(context);
            context.otherContexts = Collections.unmodifiableCollection(otherContexts);
        }
        
        return contexts.toArray(new HeapContext[0]);
    }
    
}
