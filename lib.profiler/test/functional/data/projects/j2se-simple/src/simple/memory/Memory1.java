/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
