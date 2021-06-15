/*
 * Copyright (c) 1997, 2009, Oracle and/or its affiliates. All rights reserved.
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

package simple.cpu;

public class WaitingTest {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final Object mutex = new Object();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    long run = 0;
    long sleep = 0;
    long wait = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public WaitingTest() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("Start app: " + System.currentTimeMillis());

        WaitingTest test = new WaitingTest();

        for (int i = 0; i < 2; i++) {
            test.method1000();
            System.out.println("sleep: " + test.sleep);
            System.out.println("wait: " + test.wait);
            System.out.println("run: " + test.run);
        }

        System.out.println("Finish app: " + System.currentTimeMillis());
    }

    public void method1000() {
        sleep = 0;
        run = 0;
        wait = 0;

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 1000) {
            ;
        }

        run += (System.currentTimeMillis() - time);
        time = System.currentTimeMillis();

        synchronized (mutex) {
            try {
                mutex.wait(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        wait += (System.currentTimeMillis() - time);
        time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 1000) {
            ;
        }

        run += (System.currentTimeMillis() - time);
        time = System.currentTimeMillis();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }

        sleep += (System.currentTimeMillis() - time);
    }
}
