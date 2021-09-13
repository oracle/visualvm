/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.heap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

enum Progress {
    COMPUTE_INSTANCES,
    COMPUTE_REFERENCES,
    FILL_HEAP_TAG_BOUNDS,
    COMPUTE_GC_ROOTS,
    COMPUTE_RETAINED_SIZE,
    COMPUTE_RETAINED_SIZE_BY_CLASS;

    Handle start() {
        return new Handle(this);
    }

    private static List<Listener> listeners = Collections.emptyList();
    synchronized static void register(Listener onChange) {
        if (listeners.isEmpty()) {
            listeners = Collections.singletonList(onChange);
        } else {
            List<Listener> copy = new ArrayList<>(listeners);
            copy.add(onChange);
            listeners = copy;
        }
    }

    private synchronized static void notifyUpdates(Handle h, Type type) {
        for (Listener onChange : listeners) {
            switch (type) {
                case STARTED: onChange.started(h); break;
                case PROGRESS: onChange.progress(h); break;
                default: onChange.finished(h);
            }
        }
    }

    private enum Type {
        STARTED, PROGRESS, FINISHED;
    }

    static interface Listener {
        void started(Handle h);
        void progress(Handle h);
        void finished(Handle h);
    }

    static final class Handle implements AutoCloseable {
        final Progress type;
        private long value;
        private long startOffset;
        private long endOffset;

        private Handle(Progress type) {
            this.type = type;
            notifyUpdates(this, Type.STARTED);
        }

        void progress(long value, long endValue) {
            progress(value, 0, value, endValue);
        }

        void progress(long counter, long startOffset, long value, long endOffset) {
            // keep this method short so that it can be inlined
            if (counter % 100000 == 0) {
                doProgress(value, startOffset, endOffset);
            }
        }

        @Override
        public void close() {
            notifyUpdates(this, Type.FINISHED);
        }

        private void doProgress(long value, long startOffset, long endOffset) {
            this.value = value;
            this.endOffset = endOffset;
            this.startOffset = startOffset;
            notifyUpdates(this, Type.PROGRESS);
        }

        long getValue() {
            return value;
        }

        long getStartOffset() {
            return startOffset;
        }

        long getEndOffset() {
            return endOffset;
        }
    }
}
