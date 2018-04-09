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

package simple.cpu;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;


/**
 *
 * @author ehucka
 */
public class Measure {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static PrintStream ps;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of Measure
     */
    public Measure() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void empty() {
    }

    public static void main(String[] args) {
        System.out.println("Application started: " + System.currentTimeMillis());

        try {
            ps = new PrintStream(new FileOutputStream(args[0]));

            Measure m = new Measure();
            m.test();
            ps.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        System.out.println("Application finished: " + System.currentTimeMillis());
    }

    public void run10() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }
    }

    public void run100() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 100) {
            ;
        }
    }

    public void run1000() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 1000) {
            ;
        }
    }

    public void run1000Call1() {
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 1000) {
            ;
        }
    }

    public void run1000Call4() {
        empty();
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 1000) {
            ;
        }

        empty();
        empty();
    }

    public void run100Call1() {
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 100) {
            ;
        }
    }

    public void run100Call4() {
        empty();
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 100) {
            ;
        }

        empty();
        empty();
    }

    //a method call
    public void run10Call1() {
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }
    }

    //4 calls
    public void run10Call4() {
        empty();
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        empty();
        empty();
    }

    public void run20() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 20) {
            ;
        }
    }

    public void run200() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 200) {
            ;
        }
    }

    public void run2000() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 2000) {
            ;
        }
    }

    public void run2000Call1() {
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 2000) {
            ;
        }
    }

    public void run2000Call4() {
        empty();
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 2000) {
            ;
        }

        empty();
        empty();
    }

    public void run200Call1() {
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 200) {
            ;
        }
    }

    public void run200Call4() {
        empty();
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 200) {
            ;
        }

        empty();
        empty();
    }

    public void run20Call1() {
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 20) {
            ;
        }
    }

    public void run20Call4() {
        empty();
        empty();

        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 20) {
            ;
        }

        empty();
        empty();
    }

    public void run4000by10() {
        long time;

        for (int i = 0; i < 400; i++) {
            time = System.currentTimeMillis();

            while ((System.currentTimeMillis() - time) < 10) {
                ;
            }
        }
    }

    public void run4000by100() {
        long time;

        for (int i = 0; i < 40; i++) {
            time = System.currentTimeMillis();

            while ((System.currentTimeMillis() - time) < 100) {
                ;
            }
        }
    }

    public void run4000by1000() {
        long time;

        for (int i = 0; i < 4; i++) {
            time = System.currentTimeMillis();

            while ((System.currentTimeMillis() - time) < 1000) {
                ;
            }
        }
    }

    public void test() {
        ps.println("Method;Ideal Time;Measured time");

        long time = System.currentTimeMillis();
        int count = 20;

        for (int i = 0; i < count; i++) {
            run10();
        }

        time = System.currentTimeMillis() - time;

        double val = time / (double) count;
        ps.println("run10;10;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 20;

        for (int i = 0; i < count; i++) {
            run20();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run20;20;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 10;

        for (int i = 0; i < count; i++) {
            run100();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run100;100;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 10;

        for (int i = 0; i < count; i++) {
            run200();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run200;200;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 5;

        for (int i = 0; i < count; i++) {
            run1000();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run1000;1000;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 5;

        for (int i = 0; i < count; i++) {
            run2000();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run2000;2000;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 4;

        for (int i = 0; i < count; i++) {
            run4000by10();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run4000by10;4000;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 4;

        for (int i = 0; i < count; i++) {
            run4000by100();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run4000by100;4000;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 4;

        for (int i = 0; i < count; i++) {
            run4000by1000();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run4000by1000;4000;" + String.valueOf(val));

        //******************************************************************
        time = System.currentTimeMillis();
        count = 20;

        for (int i = 0; i < count; i++) {
            run10Call1();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run10Call1;10;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 20;

        for (int i = 0; i < count; i++) {
            run20Call1();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run20Call1;20;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 10;

        for (int i = 0; i < count; i++) {
            run100Call1();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run100Call1;100;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 10;

        for (int i = 0; i < count; i++) {
            run200Call1();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run200Call1;200;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 5;

        for (int i = 0; i < count; i++) {
            run1000Call1();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run1000Call1;1000;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 5;

        for (int i = 0; i < count; i++) {
            run2000Call1();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run2000Call1;2000;" + String.valueOf(val));
        //******************************************************************
        time = System.currentTimeMillis();
        count = 20;

        for (int i = 0; i < count; i++) {
            run10Call4();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run10Call4;10;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 20;

        for (int i = 0; i < count; i++) {
            run20Call4();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run20Call4;20;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 10;

        for (int i = 0; i < count; i++) {
            run100Call4();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run100Call4;100;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 10;

        for (int i = 0; i < count; i++) {
            run200Call4();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run200Call4;200;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 5;

        for (int i = 0; i < count; i++) {
            run1000Call4();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run1000Call4;1000;" + String.valueOf(val));

        time = System.currentTimeMillis();
        count = 5;

        for (int i = 0; i < count; i++) {
            run2000Call4();
        }

        time = System.currentTimeMillis() - time;
        val = time / (double) count;
        ps.println("run2000Call4;2000;" + String.valueOf(val));

        //******************************************************************
    }

    protected static String complete(String s, int chars) {
        StringBuffer sb = new StringBuffer(chars);
        int tot = chars - s.length();
        sb.append(s);

        for (int i = 0; i < tot; i++) {
            sb.append(" ");
        }

        return sb.substring(0, chars);
    }
}
