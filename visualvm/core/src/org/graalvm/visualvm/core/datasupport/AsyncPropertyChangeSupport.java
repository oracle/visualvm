/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.datasupport;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Jaroslav Bachorik
 */
public class AsyncPropertyChangeSupport extends PropertyChangeSupport {
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Constructs a AsyncPropertyChangeSupport object.
     *
     * @param sourceBean  The bean to be given as the source for any
     *        events.
     * @throws NullPointerException if {@code sourceBean} is
     *         {@code null}
     */
    public AsyncPropertyChangeSupport(Object sourceBean) {
        super(sourceBean);
    }


    /**
     * {@inheritDoc}
     *
     * <p>
     * The listeners are notified asynchronously but the order of events
     * is kept.
     *
     * @throws NullPointerException if {@code evt} is
     *         {@code null}
     * @since 1.6
     */
    @Override
    public void firePropertyChange(final PropertyChangeEvent evt) {
        if (evt == null) {
            throw new NullPointerException();
        }
        if (getPropertyChangeListeners().length>0) {
            executor.submit(new Runnable() {

                public void run() {
                    AsyncPropertyChangeSupport.super.firePropertyChange(evt);
                }
            });
        }
    }

}
