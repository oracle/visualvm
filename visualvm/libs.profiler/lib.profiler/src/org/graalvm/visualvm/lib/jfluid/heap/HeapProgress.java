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

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SwingUtilities;


/**
 * @author Tomas Hurka
 */
public final class HeapProgress {

    public static final int PROGRESS_MAX = 1000;
    private static ThreadLocal<ModelInfo> progressThreadLocal = new ThreadLocal();
    static {
        Progress.register(new Progress.Listener() {
            @Override
            public void started(Progress.Handle h) {
                progressStart();
            }

            @Override
            public void progress(Progress.Handle h) {
                HeapProgress.progress(h.getValue(), h.getEndOffset(), h.getStartOffset());
            }

            @Override
            public void finished(Progress.Handle h) {
                progressFinish();
            }
        });
    }

    private HeapProgress() {
    }

    public static BoundedRangeModel getProgress() {
        ModelInfo info = progressThreadLocal.get();

        if (info == null) {
            info = new ModelInfo();
            progressThreadLocal.set(info);
        }
        return info.model;
    }

    private static void progress(final long value, final long endOffset, final long startOffset) {
        ModelInfo info = progressThreadLocal.get();
        if (info != null) {
            if (info.level>info.divider) {
                info.divider = info.level;
            }
            long val = PROGRESS_MAX*(value - startOffset)/(endOffset - startOffset);
            int modelVal = (int) (info.offset + val/info.divider);
            setValue(info.model, modelVal);
        }
    }

    private static int levelAdd(ModelInfo info, int diff) {
        info.level+=diff;
        return info.level;
    }

    private static void progressStart() {
        ModelInfo info = progressThreadLocal.get();
        if (info != null) {
            levelAdd(info, 1);
        }
    }

    private static void progressFinish() {
        ModelInfo info = progressThreadLocal.get();
        if (info != null) {
            int level = levelAdd(info, -1);

            assert level >= 0;
            if (level == 0) {
                progressThreadLocal.remove();
            }
            info.offset = info.model.getValue();
        }
    }

    private static void setValue(final BoundedRangeModel model, final int val) {
        if (SwingUtilities.isEventDispatchThread()) {
            model.setValue(val);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { model.setValue(val); }
            });
        }
    }

    private static class ModelInfo {
        private BoundedRangeModel model;
        private int level;
        private int divider;
        private int offset;

        private ModelInfo() {
            model = new DefaultBoundedRangeModel(0,0,0,PROGRESS_MAX);
        }
    }
}
