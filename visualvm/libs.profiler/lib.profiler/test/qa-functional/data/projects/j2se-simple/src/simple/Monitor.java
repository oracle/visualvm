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

/*
 * Monitor.java
 *
 * Created on June 14, 2005, 12:20 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package simple;


/**
 *
 * @author ehucka
 */
public class Monitor {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of Monitor
     */
    public Monitor() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Data data = new Data();
        Consumer c1 = new Consumer(data, 1);
        Producer p1 = new Producer(data, 1);
        p1.start();
        c1.start();

        try {
            Thread.sleep(5000);

            Consumer c2 = new Consumer(data, 2);
            Producer p2 = new Producer(data, 2);
            p2.start();
            c2.start();
            p1.join();
            p2.join();
        } catch (InterruptedException e) {
        }
    }
}
