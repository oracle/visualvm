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
 * Region.java
 *
 * Created on November 1, 2005, 4:02 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package simple.cpu;


/**
 *
 * @author ehucka
 */
public class Region {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Region */
    public Region() {
        run100();
        run100();
        run100();
        run1000();
        run2000();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Start application: " + System.currentTimeMillis());

        AnotherThread tt = new AnotherThread();
        tt.start();

        Region r = new Region();

        try {
            tt.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        System.out.println("Finish application: " + System.currentTimeMillis());
    }

    public void run100() {
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < 100) {
            ;
        }
    }

    public void run1000() {
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < 1000) {
            ;
        }
    }

    public void run2000() {
        for (int i = 0; i < 20; i++) {
            run100();
        }
    }
}
