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
 * Consumer.java
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
public class Consumer extends Thread {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Data cubbyhole;
    private int number;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public Consumer(Data c, int number) {
        cubbyhole = c;
        this.number = number;
        setName("Consumer " + number);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void run() {
        int value = 0;

        while (true) {
            value = cubbyhole.get(number);

            long time = System.currentTimeMillis();

            while ((System.currentTimeMillis() - time) < value) {
                ;
            }
        }
    }
}
