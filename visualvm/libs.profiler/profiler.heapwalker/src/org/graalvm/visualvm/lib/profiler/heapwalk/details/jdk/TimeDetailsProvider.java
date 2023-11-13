/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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


package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import java.time.Instant;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public final class TimeDetailsProvider extends DetailsProvider.Basic {
    private static final String INSTANT_MASK = "java.time.Instant"; // NOI18N

    public TimeDetailsProvider() {
        super(INSTANT_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        if (INSTANT_MASK.equals(className)) {
            Instant instant = getInstant(instance);
            if (instant != null) return instant.toString();
        }
        return null;
    }

    static Instant getInstant(Object instant) {
        if (instant instanceof Instance) {
            Instance instantObj = (Instance) instant;
            long seconds = DetailsUtils.getLongFieldValue(instantObj, "seconds", -1);     // NOI18N
            int nanos = DetailsUtils.getIntFieldValue(instantObj, "nanos", -1);      // NOI18N

            if (seconds != -1 && nanos != -1) {
                return Instant.ofEpochSecond(seconds, nanos);
            }
        }
        return null;
    }

}
