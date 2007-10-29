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
public class Methods2 {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of WaitingTest */
    public Methods2() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("start app: " + System.currentTimeMillis());

        Methods2 test = new Methods2();

        for (int i = 0; i < 4; i++) {
            test.method0();
            test.method9();
            test.method99();
            test.method999();
        }

        System.out.println("end app: " + System.currentTimeMillis());

        /*
           //generation
           int methods=1000;
           System.out.println("public void method0() {\nlong time=System.currentTimeMillis();");
           System.out.println("while ((System.currentTimeMillis()-time) < 10);\n}\n");
           for (int i = 1; i < methods; i++) {
               System.out.println("public void method"+i+"() {\nmethod"+(i-1)+"();\n}\n");
           }*/
    }

    public void method0() {
        long time = System.currentTimeMillis();

        while ((System.currentTimeMillis() - time) < 400) {
            ;
        }
    }

    public void method1() {
        method0();
    }

    public void method10() {
        method9();
    }

    public void method100() {
        method99();
    }

    public void method101() {
        method100();
    }

    public void method102() {
        method101();
    }

    public void method103() {
        method102();
    }

    public void method104() {
        method103();
    }

    public void method105() {
        method104();
    }

    public void method106() {
        method105();
    }

    public void method107() {
        method106();
    }

    public void method108() {
        method107();
    }

    public void method109() {
        method108();
    }

    public void method11() {
        method10();
    }

    public void method110() {
        method109();
    }

    public void method111() {
        method110();
    }

    public void method112() {
        method111();
    }

    public void method113() {
        method112();
    }

    public void method114() {
        method113();
    }

    public void method115() {
        method114();
    }

    public void method116() {
        method115();
    }

    public void method117() {
        method116();
    }

    public void method118() {
        method117();
    }

    public void method119() {
        method118();
    }

    public void method12() {
        method11();
    }

    public void method120() {
        method119();
    }

    public void method121() {
        method120();
    }

    public void method122() {
        method121();
    }

    public void method123() {
        method122();
    }

    public void method124() {
        method123();
    }

    public void method125() {
        method124();
    }

    public void method126() {
        method125();
    }

    public void method127() {
        method126();
    }

    public void method128() {
        method127();
    }

    public void method129() {
        method128();
    }

    public void method13() {
        method12();
    }

    public void method130() {
        method129();
    }

    public void method131() {
        method130();
    }

    public void method132() {
        method131();
    }

    public void method133() {
        method132();
    }

    public void method134() {
        method133();
    }

    public void method135() {
        method134();
    }

    public void method136() {
        method135();
    }

    public void method137() {
        method136();
    }

    public void method138() {
        method137();
    }

    public void method139() {
        method138();
    }

    public void method14() {
        method13();
    }

    public void method140() {
        method139();
    }

    public void method141() {
        method140();
    }

    public void method142() {
        method141();
    }

    public void method143() {
        method142();
    }

    public void method144() {
        method143();
    }

    public void method145() {
        method144();
    }

    public void method146() {
        method145();
    }

    public void method147() {
        method146();
    }

    public void method148() {
        method147();
    }

    public void method149() {
        method148();
    }

    public void method15() {
        method14();
    }

    public void method150() {
        method149();
    }

    public void method151() {
        method150();
    }

    public void method152() {
        method151();
    }

    public void method153() {
        method152();
    }

    public void method154() {
        method153();
    }

    public void method155() {
        method154();
    }

    public void method156() {
        method155();
    }

    public void method157() {
        method156();
    }

    public void method158() {
        method157();
    }

    public void method159() {
        method158();
    }

    public void method16() {
        method15();
    }

    public void method160() {
        method159();
    }

    public void method161() {
        method160();
    }

    public void method162() {
        method161();
    }

    public void method163() {
        method162();
    }

    public void method164() {
        method163();
    }

    public void method165() {
        method164();
    }

    public void method166() {
        method165();
    }

    public void method167() {
        method166();
    }

    public void method168() {
        method167();
    }

    public void method169() {
        method168();
    }

    public void method17() {
        method16();
    }

    public void method170() {
        method169();
    }

    public void method171() {
        method170();
    }

    public void method172() {
        method171();
    }

    public void method173() {
        method172();
    }

    public void method174() {
        method173();
    }

    public void method175() {
        method174();
    }

    public void method176() {
        method175();
    }

    public void method177() {
        method176();
    }

    public void method178() {
        method177();
    }

    public void method179() {
        method178();
    }

    public void method18() {
        method17();
    }

    public void method180() {
        method179();
    }

    public void method181() {
        method180();
    }

    public void method182() {
        method181();
    }

    public void method183() {
        method182();
    }

    public void method184() {
        method183();
    }

    public void method185() {
        method184();
    }

    public void method186() {
        method185();
    }

    public void method187() {
        method186();
    }

    public void method188() {
        method187();
    }

    public void method189() {
        method188();
    }

    public void method19() {
        method18();
    }

    public void method190() {
        method189();
    }

    public void method191() {
        method190();
    }

    public void method192() {
        method191();
    }

    public void method193() {
        method192();
    }

    public void method194() {
        method193();
    }

    public void method195() {
        method194();
    }

    public void method196() {
        method195();
    }

    public void method197() {
        method196();
    }

    public void method198() {
        method197();
    }

    public void method199() {
        method198();
    }

    public void method2() {
        method1();
    }

    public void method20() {
        method19();
    }

    public void method200() {
        method199();
    }

    public void method201() {
        method200();
    }

    public void method202() {
        method201();
    }

    public void method203() {
        method202();
    }

    public void method204() {
        method203();
    }

    public void method205() {
        method204();
    }

    public void method206() {
        method205();
    }

    public void method207() {
        method206();
    }

    public void method208() {
        method207();
    }

    public void method209() {
        method208();
    }

    public void method21() {
        method20();
    }

    public void method210() {
        method209();
    }

    public void method211() {
        method210();
    }

    public void method212() {
        method211();
    }

    public void method213() {
        method212();
    }

    public void method214() {
        method213();
    }

    public void method215() {
        method214();
    }

    public void method216() {
        method215();
    }

    public void method217() {
        method216();
    }

    public void method218() {
        method217();
    }

    public void method219() {
        method218();
    }

    public void method22() {
        method21();
    }

    public void method220() {
        method219();
    }

    public void method221() {
        method220();
    }

    public void method222() {
        method221();
    }

    public void method223() {
        method222();
    }

    public void method224() {
        method223();
    }

    public void method225() {
        method224();
    }

    public void method226() {
        method225();
    }

    public void method227() {
        method226();
    }

    public void method228() {
        method227();
    }

    public void method229() {
        method228();
    }

    public void method23() {
        method22();
    }

    public void method230() {
        method229();
    }

    public void method231() {
        method230();
    }

    public void method232() {
        method231();
    }

    public void method233() {
        method232();
    }

    public void method234() {
        method233();
    }

    public void method235() {
        method234();
    }

    public void method236() {
        method235();
    }

    public void method237() {
        method236();
    }

    public void method238() {
        method237();
    }

    public void method239() {
        method238();
    }

    public void method24() {
        method23();
    }

    public void method240() {
        method239();
    }

    public void method241() {
        method240();
    }

    public void method242() {
        method241();
    }

    public void method243() {
        method242();
    }

    public void method244() {
        method243();
    }

    public void method245() {
        method244();
    }

    public void method246() {
        method245();
    }

    public void method247() {
        method246();
    }

    public void method248() {
        method247();
    }

    public void method249() {
        method248();
    }

    public void method25() {
        method24();
    }

    public void method250() {
        method249();
    }

    public void method251() {
        method250();
    }

    public void method252() {
        method251();
    }

    public void method253() {
        method252();
    }

    public void method254() {
        method253();
    }

    public void method255() {
        method254();
    }

    public void method256() {
        method255();
    }

    public void method257() {
        method256();
    }

    public void method258() {
        method257();
    }

    public void method259() {
        method258();
    }

    public void method26() {
        method25();
    }

    public void method260() {
        method259();
    }

    public void method261() {
        method260();
    }

    public void method262() {
        method261();
    }

    public void method263() {
        method262();
    }

    public void method264() {
        method263();
    }

    public void method265() {
        method264();
    }

    public void method266() {
        method265();
    }

    public void method267() {
        method266();
    }

    public void method268() {
        method267();
    }

    public void method269() {
        method268();
    }

    public void method27() {
        method26();
    }

    public void method270() {
        method269();
    }

    public void method271() {
        method270();
    }

    public void method272() {
        method271();
    }

    public void method273() {
        method272();
    }

    public void method274() {
        method273();
    }

    public void method275() {
        method274();
    }

    public void method276() {
        method275();
    }

    public void method277() {
        method276();
    }

    public void method278() {
        method277();
    }

    public void method279() {
        method278();
    }

    public void method28() {
        method27();
    }

    public void method280() {
        method279();
    }

    public void method281() {
        method280();
    }

    public void method282() {
        method281();
    }

    public void method283() {
        method282();
    }

    public void method284() {
        method283();
    }

    public void method285() {
        method284();
    }

    public void method286() {
        method285();
    }

    public void method287() {
        method286();
    }

    public void method288() {
        method287();
    }

    public void method289() {
        method288();
    }

    public void method29() {
        method28();
    }

    public void method290() {
        method289();
    }

    public void method291() {
        method290();
    }

    public void method292() {
        method291();
    }

    public void method293() {
        method292();
    }

    public void method294() {
        method293();
    }

    public void method295() {
        method294();
    }

    public void method296() {
        method295();
    }

    public void method297() {
        method296();
    }

    public void method298() {
        method297();
    }

    public void method299() {
        method298();
    }

    public void method3() {
        method2();
    }

    public void method30() {
        method29();
    }

    public void method300() {
        method299();
    }

    public void method301() {
        method300();
    }

    public void method302() {
        method301();
    }

    public void method303() {
        method302();
    }

    public void method304() {
        method303();
    }

    public void method305() {
        method304();
    }

    public void method306() {
        method305();
    }

    public void method307() {
        method306();
    }

    public void method308() {
        method307();
    }

    public void method309() {
        method308();
    }

    public void method31() {
        method30();
    }

    public void method310() {
        method309();
    }

    public void method311() {
        method310();
    }

    public void method312() {
        method311();
    }

    public void method313() {
        method312();
    }

    public void method314() {
        method313();
    }

    public void method315() {
        method314();
    }

    public void method316() {
        method315();
    }

    public void method317() {
        method316();
    }

    public void method318() {
        method317();
    }

    public void method319() {
        method318();
    }

    public void method32() {
        method31();
    }

    public void method320() {
        method319();
    }

    public void method321() {
        method320();
    }

    public void method322() {
        method321();
    }

    public void method323() {
        method322();
    }

    public void method324() {
        method323();
    }

    public void method325() {
        method324();
    }

    public void method326() {
        method325();
    }

    public void method327() {
        method326();
    }

    public void method328() {
        method327();
    }

    public void method329() {
        method328();
    }

    public void method33() {
        method32();
    }

    public void method330() {
        method329();
    }

    public void method331() {
        method330();
    }

    public void method332() {
        method331();
    }

    public void method333() {
        method332();
    }

    public void method334() {
        method333();
    }

    public void method335() {
        method334();
    }

    public void method336() {
        method335();
    }

    public void method337() {
        method336();
    }

    public void method338() {
        method337();
    }

    public void method339() {
        method338();
    }

    public void method34() {
        method33();
    }

    public void method340() {
        method339();
    }

    public void method341() {
        method340();
    }

    public void method342() {
        method341();
    }

    public void method343() {
        method342();
    }

    public void method344() {
        method343();
    }

    public void method345() {
        method344();
    }

    public void method346() {
        method345();
    }

    public void method347() {
        method346();
    }

    public void method348() {
        method347();
    }

    public void method349() {
        method348();
    }

    public void method35() {
        method34();
    }

    public void method350() {
        method349();
    }

    public void method351() {
        method350();
    }

    public void method352() {
        method351();
    }

    public void method353() {
        method352();
    }

    public void method354() {
        method353();
    }

    public void method355() {
        method354();
    }

    public void method356() {
        method355();
    }

    public void method357() {
        method356();
    }

    public void method358() {
        method357();
    }

    public void method359() {
        method358();
    }

    public void method36() {
        method35();
    }

    public void method360() {
        method359();
    }

    public void method361() {
        method360();
    }

    public void method362() {
        method361();
    }

    public void method363() {
        method362();
    }

    public void method364() {
        method363();
    }

    public void method365() {
        method364();
    }

    public void method366() {
        method365();
    }

    public void method367() {
        method366();
    }

    public void method368() {
        method367();
    }

    public void method369() {
        method368();
    }

    public void method37() {
        method36();
    }

    public void method370() {
        method369();
    }

    public void method371() {
        method370();
    }

    public void method372() {
        method371();
    }

    public void method373() {
        method372();
    }

    public void method374() {
        method373();
    }

    public void method375() {
        method374();
    }

    public void method376() {
        method375();
    }

    public void method377() {
        method376();
    }

    public void method378() {
        method377();
    }

    public void method379() {
        method378();
    }

    public void method38() {
        method37();
    }

    public void method380() {
        method379();
    }

    public void method381() {
        method380();
    }

    public void method382() {
        method381();
    }

    public void method383() {
        method382();
    }

    public void method384() {
        method383();
    }

    public void method385() {
        method384();
    }

    public void method386() {
        method385();
    }

    public void method387() {
        method386();
    }

    public void method388() {
        method387();
    }

    public void method389() {
        method388();
    }

    public void method39() {
        method38();
    }

    public void method390() {
        method389();
    }

    public void method391() {
        method390();
    }

    public void method392() {
        method391();
    }

    public void method393() {
        method392();
    }

    public void method394() {
        method393();
    }

    public void method395() {
        method394();
    }

    public void method396() {
        method395();
    }

    public void method397() {
        method396();
    }

    public void method398() {
        method397();
    }

    public void method399() {
        method398();
    }

    public void method4() {
        method3();
    }

    public void method40() {
        method39();
    }

    public void method400() {
        method399();
    }

    public void method401() {
        method400();
    }

    public void method402() {
        method401();
    }

    public void method403() {
        method402();
    }

    public void method404() {
        method403();
    }

    public void method405() {
        method404();
    }

    public void method406() {
        method405();
    }

    public void method407() {
        method406();
    }

    public void method408() {
        method407();
    }

    public void method409() {
        method408();
    }

    public void method41() {
        method40();
    }

    public void method410() {
        method409();
    }

    public void method411() {
        method410();
    }

    public void method412() {
        method411();
    }

    public void method413() {
        method412();
    }

    public void method414() {
        method413();
    }

    public void method415() {
        method414();
    }

    public void method416() {
        method415();
    }

    public void method417() {
        method416();
    }

    public void method418() {
        method417();
    }

    public void method419() {
        method418();
    }

    public void method42() {
        method41();
    }

    public void method420() {
        method419();
    }

    public void method421() {
        method420();
    }

    public void method422() {
        method421();
    }

    public void method423() {
        method422();
    }

    public void method424() {
        method423();
    }

    public void method425() {
        method424();
    }

    public void method426() {
        method425();
    }

    public void method427() {
        method426();
    }

    public void method428() {
        method427();
    }

    public void method429() {
        method428();
    }

    public void method43() {
        method42();
    }

    public void method430() {
        method429();
    }

    public void method431() {
        method430();
    }

    public void method432() {
        method431();
    }

    public void method433() {
        method432();
    }

    public void method434() {
        method433();
    }

    public void method435() {
        method434();
    }

    public void method436() {
        method435();
    }

    public void method437() {
        method436();
    }

    public void method438() {
        method437();
    }

    public void method439() {
        method438();
    }

    public void method44() {
        method43();
    }

    public void method440() {
        method439();
    }

    public void method441() {
        method440();
    }

    public void method442() {
        method441();
    }

    public void method443() {
        method442();
    }

    public void method444() {
        method443();
    }

    public void method445() {
        method444();
    }

    public void method446() {
        method445();
    }

    public void method447() {
        method446();
    }

    public void method448() {
        method447();
    }

    public void method449() {
        method448();
    }

    public void method45() {
        method44();
    }

    public void method450() {
        method449();
    }

    public void method451() {
        method450();
    }

    public void method452() {
        method451();
    }

    public void method453() {
        method452();
    }

    public void method454() {
        method453();
    }

    public void method455() {
        method454();
    }

    public void method456() {
        method455();
    }

    public void method457() {
        method456();
    }

    public void method458() {
        method457();
    }

    public void method459() {
        method458();
    }

    public void method46() {
        method45();
    }

    public void method460() {
        method459();
    }

    public void method461() {
        method460();
    }

    public void method462() {
        method461();
    }

    public void method463() {
        method462();
    }

    public void method464() {
        method463();
    }

    public void method465() {
        method464();
    }

    public void method466() {
        method465();
    }

    public void method467() {
        method466();
    }

    public void method468() {
        method467();
    }

    public void method469() {
        method468();
    }

    public void method47() {
        method46();
    }

    public void method470() {
        method469();
    }

    public void method471() {
        method470();
    }

    public void method472() {
        method471();
    }

    public void method473() {
        method472();
    }

    public void method474() {
        method473();
    }

    public void method475() {
        method474();
    }

    public void method476() {
        method475();
    }

    public void method477() {
        method476();
    }

    public void method478() {
        method477();
    }

    public void method479() {
        method478();
    }

    public void method48() {
        method47();
    }

    public void method480() {
        method479();
    }

    public void method481() {
        method480();
    }

    public void method482() {
        method481();
    }

    public void method483() {
        method482();
    }

    public void method484() {
        method483();
    }

    public void method485() {
        method484();
    }

    public void method486() {
        method485();
    }

    public void method487() {
        method486();
    }

    public void method488() {
        method487();
    }

    public void method489() {
        method488();
    }

    public void method49() {
        method48();
    }

    public void method490() {
        method489();
    }

    public void method491() {
        method490();
    }

    public void method492() {
        method491();
    }

    public void method493() {
        method492();
    }

    public void method494() {
        method493();
    }

    public void method495() {
        method494();
    }

    public void method496() {
        method495();
    }

    public void method497() {
        method496();
    }

    public void method498() {
        method497();
    }

    public void method499() {
        method498();
    }

    public void method5() {
        method4();
    }

    public void method50() {
        method49();
    }

    public void method500() {
        method499();
    }

    public void method501() {
        method500();
    }

    public void method502() {
        method501();
    }

    public void method503() {
        method502();
    }

    public void method504() {
        method503();
    }

    public void method505() {
        method504();
    }

    public void method506() {
        method505();
    }

    public void method507() {
        method506();
    }

    public void method508() {
        method507();
    }

    public void method509() {
        method508();
    }

    public void method51() {
        method50();
    }

    public void method510() {
        method509();
    }

    public void method511() {
        method510();
    }

    public void method512() {
        method511();
    }

    public void method513() {
        method512();
    }

    public void method514() {
        method513();
    }

    public void method515() {
        method514();
    }

    public void method516() {
        method515();
    }

    public void method517() {
        method516();
    }

    public void method518() {
        method517();
    }

    public void method519() {
        method518();
    }

    public void method52() {
        method51();
    }

    public void method520() {
        method519();
    }

    public void method521() {
        method520();
    }

    public void method522() {
        method521();
    }

    public void method523() {
        method522();
    }

    public void method524() {
        method523();
    }

    public void method525() {
        method524();
    }

    public void method526() {
        method525();
    }

    public void method527() {
        method526();
    }

    public void method528() {
        method527();
    }

    public void method529() {
        method528();
    }

    public void method53() {
        method52();
    }

    public void method530() {
        method529();
    }

    public void method531() {
        method530();
    }

    public void method532() {
        method531();
    }

    public void method533() {
        method532();
    }

    public void method534() {
        method533();
    }

    public void method535() {
        method534();
    }

    public void method536() {
        method535();
    }

    public void method537() {
        method536();
    }

    public void method538() {
        method537();
    }

    public void method539() {
        method538();
    }

    public void method54() {
        method53();
    }

    public void method540() {
        method539();
    }

    public void method541() {
        method540();
    }

    public void method542() {
        method541();
    }

    public void method543() {
        method542();
    }

    public void method544() {
        method543();
    }

    public void method545() {
        method544();
    }

    public void method546() {
        method545();
    }

    public void method547() {
        method546();
    }

    public void method548() {
        method547();
    }

    public void method549() {
        method548();
    }

    public void method55() {
        method54();
    }

    public void method550() {
        method549();
    }

    public void method551() {
        method550();
    }

    public void method552() {
        method551();
    }

    public void method553() {
        method552();
    }

    public void method554() {
        method553();
    }

    public void method555() {
        method554();
    }

    public void method556() {
        method555();
    }

    public void method557() {
        method556();
    }

    public void method558() {
        method557();
    }

    public void method559() {
        method558();
    }

    public void method56() {
        method55();
    }

    public void method560() {
        method559();
    }

    public void method561() {
        method560();
    }

    public void method562() {
        method561();
    }

    public void method563() {
        method562();
    }

    public void method564() {
        method563();
    }

    public void method565() {
        method564();
    }

    public void method566() {
        method565();
    }

    public void method567() {
        method566();
    }

    public void method568() {
        method567();
    }

    public void method569() {
        method568();
    }

    public void method57() {
        method56();
    }

    public void method570() {
        method569();
    }

    public void method571() {
        method570();
    }

    public void method572() {
        method571();
    }

    public void method573() {
        method572();
    }

    public void method574() {
        method573();
    }

    public void method575() {
        method574();
    }

    public void method576() {
        method575();
    }

    public void method577() {
        method576();
    }

    public void method578() {
        method577();
    }

    public void method579() {
        method578();
    }

    public void method58() {
        method57();
    }

    public void method580() {
        method579();
    }

    public void method581() {
        method580();
    }

    public void method582() {
        method581();
    }

    public void method583() {
        method582();
    }

    public void method584() {
        method583();
    }

    public void method585() {
        method584();
    }

    public void method586() {
        method585();
    }

    public void method587() {
        method586();
    }

    public void method588() {
        method587();
    }

    public void method589() {
        method588();
    }

    public void method59() {
        method58();
    }

    public void method590() {
        method589();
    }

    public void method591() {
        method590();
    }

    public void method592() {
        method591();
    }

    public void method593() {
        method592();
    }

    public void method594() {
        method593();
    }

    public void method595() {
        method594();
    }

    public void method596() {
        method595();
    }

    public void method597() {
        method596();
    }

    public void method598() {
        method597();
    }

    public void method599() {
        method598();
    }

    public void method6() {
        method5();
    }

    public void method60() {
        method59();
    }

    public void method600() {
        method599();
    }

    public void method601() {
        method600();
    }

    public void method602() {
        method601();
    }

    public void method603() {
        method602();
    }

    public void method604() {
        method603();
    }

    public void method605() {
        method604();
    }

    public void method606() {
        method605();
    }

    public void method607() {
        method606();
    }

    public void method608() {
        method607();
    }

    public void method609() {
        method608();
    }

    public void method61() {
        method60();
    }

    public void method610() {
        method609();
    }

    public void method611() {
        method610();
    }

    public void method612() {
        method611();
    }

    public void method613() {
        method612();
    }

    public void method614() {
        method613();
    }

    public void method615() {
        method614();
    }

    public void method616() {
        method615();
    }

    public void method617() {
        method616();
    }

    public void method618() {
        method617();
    }

    public void method619() {
        method618();
    }

    public void method62() {
        method61();
    }

    public void method620() {
        method619();
    }

    public void method621() {
        method620();
    }

    public void method622() {
        method621();
    }

    public void method623() {
        method622();
    }

    public void method624() {
        method623();
    }

    public void method625() {
        method624();
    }

    public void method626() {
        method625();
    }

    public void method627() {
        method626();
    }

    public void method628() {
        method627();
    }

    public void method629() {
        method628();
    }

    public void method63() {
        method62();
    }

    public void method630() {
        method629();
    }

    public void method631() {
        method630();
    }

    public void method632() {
        method631();
    }

    public void method633() {
        method632();
    }

    public void method634() {
        method633();
    }

    public void method635() {
        method634();
    }

    public void method636() {
        method635();
    }

    public void method637() {
        method636();
    }

    public void method638() {
        method637();
    }

    public void method639() {
        method638();
    }

    public void method64() {
        method63();
    }

    public void method640() {
        method639();
    }

    public void method641() {
        method640();
    }

    public void method642() {
        method641();
    }

    public void method643() {
        method642();
    }

    public void method644() {
        method643();
    }

    public void method645() {
        method644();
    }

    public void method646() {
        method645();
    }

    public void method647() {
        method646();
    }

    public void method648() {
        method647();
    }

    public void method649() {
        method648();
    }

    public void method65() {
        method64();
    }

    public void method650() {
        method649();
    }

    public void method651() {
        method650();
    }

    public void method652() {
        method651();
    }

    public void method653() {
        method652();
    }

    public void method654() {
        method653();
    }

    public void method655() {
        method654();
    }

    public void method656() {
        method655();
    }

    public void method657() {
        method656();
    }

    public void method658() {
        method657();
    }

    public void method659() {
        method658();
    }

    public void method66() {
        method65();
    }

    public void method660() {
        method659();
    }

    public void method661() {
        method660();
    }

    public void method662() {
        method661();
    }

    public void method663() {
        method662();
    }

    public void method664() {
        method663();
    }

    public void method665() {
        method664();
    }

    public void method666() {
        method665();
    }

    public void method667() {
        method666();
    }

    public void method668() {
        method667();
    }

    public void method669() {
        method668();
    }

    public void method67() {
        method66();
    }

    public void method670() {
        method669();
    }

    public void method671() {
        method670();
    }

    public void method672() {
        method671();
    }

    public void method673() {
        method672();
    }

    public void method674() {
        method673();
    }

    public void method675() {
        method674();
    }

    public void method676() {
        method675();
    }

    public void method677() {
        method676();
    }

    public void method678() {
        method677();
    }

    public void method679() {
        method678();
    }

    public void method68() {
        method67();
    }

    public void method680() {
        method679();
    }

    public void method681() {
        method680();
    }

    public void method682() {
        method681();
    }

    public void method683() {
        method682();
    }

    public void method684() {
        method683();
    }

    public void method685() {
        method684();
    }

    public void method686() {
        method685();
    }

    public void method687() {
        method686();
    }

    public void method688() {
        method687();
    }

    public void method689() {
        method688();
    }

    public void method69() {
        method68();
    }

    public void method690() {
        method689();
    }

    public void method691() {
        method690();
    }

    public void method692() {
        method691();
    }

    public void method693() {
        method692();
    }

    public void method694() {
        method693();
    }

    public void method695() {
        method694();
    }

    public void method696() {
        method695();
    }

    public void method697() {
        method696();
    }

    public void method698() {
        method697();
    }

    public void method699() {
        method698();
    }

    public void method7() {
        method6();
    }

    public void method70() {
        method69();
    }

    public void method700() {
        method699();
    }

    public void method701() {
        method700();
    }

    public void method702() {
        method701();
    }

    public void method703() {
        method702();
    }

    public void method704() {
        method703();
    }

    public void method705() {
        method704();
    }

    public void method706() {
        method705();
    }

    public void method707() {
        method706();
    }

    public void method708() {
        method707();
    }

    public void method709() {
        method708();
    }

    public void method71() {
        method70();
    }

    public void method710() {
        method709();
    }

    public void method711() {
        method710();
    }

    public void method712() {
        method711();
    }

    public void method713() {
        method712();
    }

    public void method714() {
        method713();
    }

    public void method715() {
        method714();
    }

    public void method716() {
        method715();
    }

    public void method717() {
        method716();
    }

    public void method718() {
        method717();
    }

    public void method719() {
        method718();
    }

    public void method72() {
        method71();
    }

    public void method720() {
        method719();
    }

    public void method721() {
        method720();
    }

    public void method722() {
        method721();
    }

    public void method723() {
        method722();
    }

    public void method724() {
        method723();
    }

    public void method725() {
        method724();
    }

    public void method726() {
        method725();
    }

    public void method727() {
        method726();
    }

    public void method728() {
        method727();
    }

    public void method729() {
        method728();
    }

    public void method73() {
        method72();
    }

    public void method730() {
        method729();
    }

    public void method731() {
        method730();
    }

    public void method732() {
        method731();
    }

    public void method733() {
        method732();
    }

    public void method734() {
        method733();
    }

    public void method735() {
        method734();
    }

    public void method736() {
        method735();
    }

    public void method737() {
        method736();
    }

    public void method738() {
        method737();
    }

    public void method739() {
        method738();
    }

    public void method74() {
        method73();
    }

    public void method740() {
        method739();
    }

    public void method741() {
        method740();
    }

    public void method742() {
        method741();
    }

    public void method743() {
        method742();
    }

    public void method744() {
        method743();
    }

    public void method745() {
        method744();
    }

    public void method746() {
        method745();
    }

    public void method747() {
        method746();
    }

    public void method748() {
        method747();
    }

    public void method749() {
        method748();
    }

    public void method75() {
        method74();
    }

    public void method750() {
        method749();
    }

    public void method751() {
        method750();
    }

    public void method752() {
        method751();
    }

    public void method753() {
        method752();
    }

    public void method754() {
        method753();
    }

    public void method755() {
        method754();
    }

    public void method756() {
        method755();
    }

    public void method757() {
        method756();
    }

    public void method758() {
        method757();
    }

    public void method759() {
        method758();
    }

    public void method76() {
        method75();
    }

    public void method760() {
        method759();
    }

    public void method761() {
        method760();
    }

    public void method762() {
        method761();
    }

    public void method763() {
        method762();
    }

    public void method764() {
        method763();
    }

    public void method765() {
        method764();
    }

    public void method766() {
        method765();
    }

    public void method767() {
        method766();
    }

    public void method768() {
        method767();
    }

    public void method769() {
        method768();
    }

    public void method77() {
        method76();
    }

    public void method770() {
        method769();
    }

    public void method771() {
        method770();
    }

    public void method772() {
        method771();
    }

    public void method773() {
        method772();
    }

    public void method774() {
        method773();
    }

    public void method775() {
        method774();
    }

    public void method776() {
        method775();
    }

    public void method777() {
        method776();
    }

    public void method778() {
        method777();
    }

    public void method779() {
        method778();
    }

    public void method78() {
        method77();
    }

    public void method780() {
        method779();
    }

    public void method781() {
        method780();
    }

    public void method782() {
        method781();
    }

    public void method783() {
        method782();
    }

    public void method784() {
        method783();
    }

    public void method785() {
        method784();
    }

    public void method786() {
        method785();
    }

    public void method787() {
        method786();
    }

    public void method788() {
        method787();
    }

    public void method789() {
        method788();
    }

    public void method79() {
        method78();
    }

    public void method790() {
        method789();
    }

    public void method791() {
        method790();
    }

    public void method792() {
        method791();
    }

    public void method793() {
        method792();
    }

    public void method794() {
        method793();
    }

    public void method795() {
        method794();
    }

    public void method796() {
        method795();
    }

    public void method797() {
        method796();
    }

    public void method798() {
        method797();
    }

    public void method799() {
        method798();
    }

    public void method8() {
        method7();
    }

    public void method80() {
        method79();
    }

    public void method800() {
        method799();
    }

    public void method801() {
        method800();
    }

    public void method802() {
        method801();
    }

    public void method803() {
        method802();
    }

    public void method804() {
        method803();
    }

    public void method805() {
        method804();
    }

    public void method806() {
        method805();
    }

    public void method807() {
        method806();
    }

    public void method808() {
        method807();
    }

    public void method809() {
        method808();
    }

    public void method81() {
        method80();
    }

    public void method810() {
        method809();
    }

    public void method811() {
        method810();
    }

    public void method812() {
        method811();
    }

    public void method813() {
        method812();
    }

    public void method814() {
        method813();
    }

    public void method815() {
        method814();
    }

    public void method816() {
        method815();
    }

    public void method817() {
        method816();
    }

    public void method818() {
        method817();
    }

    public void method819() {
        method818();
    }

    public void method82() {
        method81();
    }

    public void method820() {
        method819();
    }

    public void method821() {
        method820();
    }

    public void method822() {
        method821();
    }

    public void method823() {
        method822();
    }

    public void method824() {
        method823();
    }

    public void method825() {
        method824();
    }

    public void method826() {
        method825();
    }

    public void method827() {
        method826();
    }

    public void method828() {
        method827();
    }

    public void method829() {
        method828();
    }

    public void method83() {
        method82();
    }

    public void method830() {
        method829();
    }

    public void method831() {
        method830();
    }

    public void method832() {
        method831();
    }

    public void method833() {
        method832();
    }

    public void method834() {
        method833();
    }

    public void method835() {
        method834();
    }

    public void method836() {
        method835();
    }

    public void method837() {
        method836();
    }

    public void method838() {
        method837();
    }

    public void method839() {
        method838();
    }

    public void method84() {
        method83();
    }

    public void method840() {
        method839();
    }

    public void method841() {
        method840();
    }

    public void method842() {
        method841();
    }

    public void method843() {
        method842();
    }

    public void method844() {
        method843();
    }

    public void method845() {
        method844();
    }

    public void method846() {
        method845();
    }

    public void method847() {
        method846();
    }

    public void method848() {
        method847();
    }

    public void method849() {
        method848();
    }

    public void method85() {
        method84();
    }

    public void method850() {
        method849();
    }

    public void method851() {
        method850();
    }

    public void method852() {
        method851();
    }

    public void method853() {
        method852();
    }

    public void method854() {
        method853();
    }

    public void method855() {
        method854();
    }

    public void method856() {
        method855();
    }

    public void method857() {
        method856();
    }

    public void method858() {
        method857();
    }

    public void method859() {
        method858();
    }

    public void method86() {
        method85();
    }

    public void method860() {
        method859();
    }

    public void method861() {
        method860();
    }

    public void method862() {
        method861();
    }

    public void method863() {
        method862();
    }

    public void method864() {
        method863();
    }

    public void method865() {
        method864();
    }

    public void method866() {
        method865();
    }

    public void method867() {
        method866();
    }

    public void method868() {
        method867();
    }

    public void method869() {
        method868();
    }

    public void method87() {
        method86();
    }

    public void method870() {
        method869();
    }

    public void method871() {
        method870();
    }

    public void method872() {
        method871();
    }

    public void method873() {
        method872();
    }

    public void method874() {
        method873();
    }

    public void method875() {
        method874();
    }

    public void method876() {
        method875();
    }

    public void method877() {
        method876();
    }

    public void method878() {
        method877();
    }

    public void method879() {
        method878();
    }

    public void method88() {
        method87();
    }

    public void method880() {
        method879();
    }

    public void method881() {
        method880();
    }

    public void method882() {
        method881();
    }

    public void method883() {
        method882();
    }

    public void method884() {
        method883();
    }

    public void method885() {
        method884();
    }

    public void method886() {
        method885();
    }

    public void method887() {
        method886();
    }

    public void method888() {
        method887();
    }

    public void method889() {
        method888();
    }

    public void method89() {
        method88();
    }

    public void method890() {
        method889();
    }

    public void method891() {
        method890();
    }

    public void method892() {
        method891();
    }

    public void method893() {
        method892();
    }

    public void method894() {
        method893();
    }

    public void method895() {
        method894();
    }

    public void method896() {
        method895();
    }

    public void method897() {
        method896();
    }

    public void method898() {
        method897();
    }

    public void method899() {
        method898();
    }

    public void method9() {
        method8();
    }

    public void method90() {
        method89();
    }

    public void method900() {
        method899();
    }

    public void method901() {
        method900();
    }

    public void method902() {
        method901();
    }

    public void method903() {
        method902();
    }

    public void method904() {
        method903();
    }

    public void method905() {
        method904();
    }

    public void method906() {
        method905();
    }

    public void method907() {
        method906();
    }

    public void method908() {
        method907();
    }

    public void method909() {
        method908();
    }

    public void method91() {
        method90();
    }

    public void method910() {
        method909();
    }

    public void method911() {
        method910();
    }

    public void method912() {
        method911();
    }

    public void method913() {
        method912();
    }

    public void method914() {
        method913();
    }

    public void method915() {
        method914();
    }

    public void method916() {
        method915();
    }

    public void method917() {
        method916();
    }

    public void method918() {
        method917();
    }

    public void method919() {
        method918();
    }

    public void method92() {
        method91();
    }

    public void method920() {
        method919();
    }

    public void method921() {
        method920();
    }

    public void method922() {
        method921();
    }

    public void method923() {
        method922();
    }

    public void method924() {
        method923();
    }

    public void method925() {
        method924();
    }

    public void method926() {
        method925();
    }

    public void method927() {
        method926();
    }

    public void method928() {
        method927();
    }

    public void method929() {
        method928();
    }

    public void method93() {
        method92();
    }

    public void method930() {
        method929();
    }

    public void method931() {
        method930();
    }

    public void method932() {
        method931();
    }

    public void method933() {
        method932();
    }

    public void method934() {
        method933();
    }

    public void method935() {
        method934();
    }

    public void method936() {
        method935();
    }

    public void method937() {
        method936();
    }

    public void method938() {
        method937();
    }

    public void method939() {
        method938();
    }

    public void method94() {
        method93();
    }

    public void method940() {
        method939();
    }

    public void method941() {
        method940();
    }

    public void method942() {
        method941();
    }

    public void method943() {
        method942();
    }

    public void method944() {
        method943();
    }

    public void method945() {
        method944();
    }

    public void method946() {
        method945();
    }

    public void method947() {
        method946();
    }

    public void method948() {
        method947();
    }

    public void method949() {
        method948();
    }

    public void method95() {
        method94();
    }

    public void method950() {
        method949();
    }

    public void method951() {
        method950();
    }

    public void method952() {
        method951();
    }

    public void method953() {
        method952();
    }

    public void method954() {
        method953();
    }

    public void method955() {
        method954();
    }

    public void method956() {
        method955();
    }

    public void method957() {
        method956();
    }

    public void method958() {
        method957();
    }

    public void method959() {
        method958();
    }

    public void method96() {
        method95();
    }

    public void method960() {
        method959();
    }

    public void method961() {
        method960();
    }

    public void method962() {
        method961();
    }

    public void method963() {
        method962();
    }

    public void method964() {
        method963();
    }

    public void method965() {
        method964();
    }

    public void method966() {
        method965();
    }

    public void method967() {
        method966();
    }

    public void method968() {
        method967();
    }

    public void method969() {
        method968();
    }

    public void method97() {
        method96();
    }

    public void method970() {
        method969();
    }

    public void method971() {
        method970();
    }

    public void method972() {
        method971();
    }

    public void method973() {
        method972();
    }

    public void method974() {
        method973();
    }

    public void method975() {
        method974();
    }

    public void method976() {
        method975();
    }

    public void method977() {
        method976();
    }

    public void method978() {
        method977();
    }

    public void method979() {
        method978();
    }

    public void method98() {
        method97();
    }

    public void method980() {
        method979();
    }

    public void method981() {
        method980();
    }

    public void method982() {
        method981();
    }

    public void method983() {
        method982();
    }

    public void method984() {
        method983();
    }

    public void method985() {
        method984();
    }

    public void method986() {
        method985();
    }

    public void method987() {
        method986();
    }

    public void method988() {
        method987();
    }

    public void method989() {
        method988();
    }

    public void method99() {
        method98();
    }

    public void method990() {
        method989();
    }

    public void method991() {
        method990();
    }

    public void method992() {
        method991();
    }

    public void method993() {
        method992();
    }

    public void method994() {
        method993();
    }

    public void method995() {
        method994();
    }

    public void method996() {
        method995();
    }

    public void method997() {
        method996();
    }

    public void method998() {
        method997();
    }

    public void method999() {
        method998();
    }
}
