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

package com.sun.tools.visualvm.core.scheduler;

import java.util.concurrent.TimeUnit;

/**
 * This class {@linkplain TimeUnit} and provides easy conversion and comparation
 * between various units
 * @author Jaroslav Bachorik
 */
public class Quantum {
    final public static Quantum SUSPENDED = new Quantum(TimeUnit.NANOSECONDS, -1);

    /**
     * Read-only field - the {@linkplain TimeUnit} used in the Quantum
     */
    final public TimeUnit unit;
    /**
     * Read-only field - the interval
     */
    final public int interval;

    /**
     * Sets-up a new quantum
     * @param unit The {@linkplain TimeUnit} to use
     * @param interval The interval
     */
    public Quantum(TimeUnit unit, int interval) {
        this.unit = unit;
        this.interval = interval;
    }

    /**
     * Factory method - creates a new quantum given the number of seconds
     * @param interval The number of seconds
     * @return Returns new instance of {@linkplain Quantum}
     */
    public static Quantum seconds(int interval) {
        return new Quantum(TimeUnit.SECONDS, interval);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Quantum other = (Quantum) obj;
        
        if (this.getNanos() != other.getNanos()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (int)(getNanos() % 321721);
    }
    
    private long getNanos() {
        long multiplier = 1;
        switch(unit) {
            case MICROSECONDS: {
                multiplier = 1000;
                break;
            }
            case MILLISECONDS: {
                multiplier = 1000 * 1000;
                break;
            }
            case SECONDS: {
                multiplier = 1000 * 1000 * 1000;
                break;
            }
            case MINUTES: {
                multiplier = 60 * 1000 * 1000 * 1000;
                break;
            }
            case HOURS: {
                multiplier = 60 * 60 * 1000 * 1000 * 1000;
                break;
            }
            case DAYS: {
                multiplier = 24 * 60 * 1000 * 1000 * 1000;
                break;
            }
        }
        return interval * multiplier;
    }

    @Override
    public String toString() {
        return interval + unit.toString();
    }
}
