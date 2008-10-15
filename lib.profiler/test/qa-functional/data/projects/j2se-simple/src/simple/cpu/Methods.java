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

package simple.cpu;


/**
 *
 * @author ehucka
 */
public class Methods {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of WaitingTest */
    public Methods() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("start app: " + System.currentTimeMillis());

        Methods test = new Methods();

        for (int i = 0; i < 2; i++) {
            test.method0();
        }

        System.out.println("end app: " + System.currentTimeMillis());

        /*
           //generation
           int count=100;
           for (int i=0;i < count;i++) {
               System.out.println("    public void method"+i+"() {");
               System.out.println("        long time=System.currentTimeMillis();");
               System.out.println("        while ((System.currentTimeMillis()-time) < 10);");
               if (i < (count-1))
                   System.out.println("        method"+(i+1)+"();");
               System.out.println("    }\n");
           }*/
    }

    public void method0() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method1();
    }

    public void method1() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method2();
    }

    public void method10() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method11();
    }

    public void method11() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method12();
    }

    public void method12() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method13();
    }

    public void method13() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method14();
    }

    public void method14() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method15();
    }

    public void method15() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method16();
    }

    public void method16() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method17();
    }

    public void method17() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method18();
    }

    public void method18() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method19();
    }

    public void method19() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method20();
    }

    public void method2() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method3();
    }

    public void method20() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method21();
    }

    public void method21() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method22();
    }

    public void method22() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method23();
    }

    public void method23() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method24();
    }

    public void method24() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method25();
    }

    public void method25() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method26();
    }

    public void method26() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method27();
    }

    public void method27() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method28();
    }

    public void method28() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method29();
    }

    public void method29() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method30();
    }

    public void method3() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method4();
    }

    public void method30() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method31();
    }

    public void method31() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method32();
    }

    public void method32() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method33();
    }

    public void method33() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method34();
    }

    public void method34() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method35();
    }

    public void method35() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method36();
    }

    public void method36() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method37();
    }

    public void method37() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method38();
    }

    public void method38() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method39();
    }

    public void method39() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method40();
    }

    public void method4() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method5();
    }

    public void method40() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method41();
    }

    public void method41() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method42();
    }

    public void method42() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method43();
    }

    public void method43() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method44();
    }

    public void method44() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method45();
    }

    public void method45() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method46();
    }

    public void method46() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method47();
    }

    public void method47() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method48();
    }

    public void method48() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method49();
    }

    public void method49() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method50();
    }

    public void method5() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method6();
    }

    public void method50() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method51();
    }

    public void method51() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method52();
    }

    public void method52() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method53();
    }

    public void method53() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method54();
    }

    public void method54() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method55();
    }

    public void method55() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method56();
    }

    public void method56() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method57();
    }

    public void method57() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method58();
    }

    public void method58() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method59();
    }

    public void method59() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method60();
    }

    public void method6() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method7();
    }

    public void method60() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method61();
    }

    public void method61() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method62();
    }

    public void method62() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method63();
    }

    public void method63() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method64();
    }

    public void method64() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method65();
    }

    public void method65() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method66();
    }

    public void method66() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method67();
    }

    public void method67() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method68();
    }

    public void method68() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method69();
    }

    public void method69() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method70();
    }

    public void method7() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method8();
    }

    public void method70() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method71();
    }

    public void method71() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method72();
    }

    public void method72() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method73();
    }

    public void method73() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method74();
    }

    public void method74() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method75();
    }

    public void method75() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method76();
    }

    public void method76() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method77();
    }

    public void method77() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method78();
    }

    public void method78() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method79();
    }

    public void method79() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method80();
    }

    public void method8() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method9();
    }

    public void method80() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method81();
    }

    public void method81() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method82();
    }

    public void method82() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method83();
    }

    public void method83() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method84();
    }

    public void method84() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method85();
    }

    public void method85() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method86();
    }

    public void method86() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method87();
    }

    public void method87() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method88();
    }

    public void method88() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method89();
    }

    public void method89() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method90();
    }

    public void method9() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method10();
    }

    public void method90() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method91();
    }

    public void method91() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method92();
    }

    public void method92() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method93();
    }

    public void method93() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method94();
    }

    public void method94() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method95();
    }

    public void method95() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method96();
    }

    public void method96() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method97();
    }

    public void method97() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method98();
    }

    public void method98() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }

        method99();
    }

    public void method99() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 10) {
            ;
        }
    }
}
