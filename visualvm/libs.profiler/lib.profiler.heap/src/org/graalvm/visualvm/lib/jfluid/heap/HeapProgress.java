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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author Tomas Hurka
 */
public final class HeapProgress {

    public static final int PROGRESS_MAX = 1000;
    private static ThreadLocal<ModelInfo> progressThreadLocal = new ThreadLocal<>();
    private static Map<Long,ModelInfo> progresses = Collections.synchronizedMap(new HashMap<>());

    private HeapProgress() {
    }

    public static long getProgressId() {
        ModelInfo info = progressThreadLocal.get();

        if (info == null) {
            info = new ModelInfo();
            progressThreadLocal.set(info);
            progresses.put(info.progressId, info);
        }
        return info.progressId;
    }

    public static int getProgressValue(long progressId) {
        ModelInfo info = progresses.get(progressId);
        if (info != null) {
            return info.value;
        }
        return -1;
     }

    static void progress(long counter, long startOffset, long value, long endOffset) {
        // keep this method short so that it can be inlined
        if (counter % 100000 == 0) {
            progress(value, endOffset, startOffset);
        }
    }

    static void progress(long value, long endValue) {
        progress(value,0,value,endValue);
    }

    private static void progress(final long value, final long endOffset, final long startOffset) {
        ModelInfo info = progressThreadLocal.get();
        if (info != null) {
            if (info.level>info.divider) {
                info.divider = info.level;
            }
            long val = PROGRESS_MAX*(value - startOffset)/(endOffset - startOffset);
            int modelVal = (int) (info.offset + val/info.divider);
            info.value = modelVal;
        }
    }

    private static int levelAdd(ModelInfo info, int diff) {
        info.level+=diff;
        return info.level;
    }

    static void progressStart() {
        ModelInfo info = progressThreadLocal.get();
        if (info != null) {
            levelAdd(info, 1);
        }
    }

    static void progressFinish() {
        ModelInfo info = progressThreadLocal.get();
        if (info != null) {
            int level = levelAdd(info, -1);

            assert level >= 0;
            if (level == 0) {
                progressThreadLocal.remove();
                progresses.remove(info.progressId);
            }
            info.offset = info.value;
        }
    }
    
    private static class ModelInfo {
        private static final AtomicLong PROGRESS_ID = new AtomicLong(0);

        private final long progressId;
        private int level;
        private int divider;
        private int offset;
        private int value;

        private ModelInfo() {
            progressId = PROGRESS_ID.incrementAndGet();
        }
    }
}
