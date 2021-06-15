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
 * Memory1.java
 *
 * Created on July 25, 2005, 4:16 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package simple.memory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 *
 * @author ehucka
 */
public class Memory1 {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static ArrayList storage2 = new ArrayList();

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    ArrayList storage;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Memory1 */
    public Memory1() {
        storage = new ArrayList();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void add() {
        storage.add(new Bean());
        storage2.add(new Bean());
    }

    public void clear() {
        storage.clear();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(">>app: start: " + System.currentTimeMillis());

        //wait for profiler
        /*try {
           BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
           br.readLine();
           } catch (Exception ex) {}
           //wait for the first measuring
           try {
               Thread.sleep(4000);
           } catch (Exception e) {}*/
        int[] cnts = new int[] { 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1 };
        Memory1 memory = new Memory1();

        for (int i = 0; i < cnts.length; i++) {
            /*try {
               Thread.sleep(200);
               } catch (Exception e) {}*/
            for (int b = 0; b < cnts[i]; b++) {
                memory.add();
            }

            try {
                Thread.sleep(200);
            } catch (Exception e) {
            }

            //memory.clear();
            System.gc();
        }

        System.out.println(">>app: end: " + System.currentTimeMillis());
    }
}
