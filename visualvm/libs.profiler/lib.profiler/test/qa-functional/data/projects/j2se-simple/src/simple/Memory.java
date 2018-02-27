/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package simple;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 *
 * @author ehucka
 */
public class Memory {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    ArrayList list = new ArrayList();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Memory */
    public Memory() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void getData() {
        for (int i = 0; i < 100; i++) {
            list.add(new Data());
        }
    }

    public void get1000() {
        long[] l = new long[100];
        list.add(l);
    }

    public void get500() {
        int[] d = new int[1000];
        list.add(d);
    }

    public static void main(String[] args) {
        System.out.println(">>app: start: " + System.currentTimeMillis());

        //wait for profiler
        /*try {
           BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
           br.readLine();
           } catch (Exception ex) {}
           //wait for the first measuring
           try {
               Thread.sleep(3000);
           } catch (Exception e) {}*/
        Memory m = new Memory();
        int cycle = 20;

        while (cycle > 0) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
            }

            m.get1000();
            m.get500();
            m.getData();
            cycle--;
        }

        System.out.println(">>app: end: " + System.currentTimeMillis());
    }
}
