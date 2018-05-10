/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 *
 * Contributor(s):
 */
package org.graalvm.visualvm.lib.jfluid.server;

/**
 *
 * @author Tomas Hurka
 */
class InstrumentConstructorTest {

    private static final boolean DEBUG = Boolean.getBoolean("org.graalvm.visualvm.lib.jfluid.server.InstrumentConstructorTest");

    static boolean test() {
        try {
            return new TestClassLoader().test();
        } catch (InstantiationException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private static class TestClassLoader extends ClassLoader {
    /*
        Classfile org/graalvm/visualvm/lib/jfluid/ConstructorTest.class
          Last modified May 8, 2018; size 609 bytes
          MD5 checksum 6796b1dd2e3cb95970ea0a811e633bef
          Compiled from "ConstructorTest.java"
        public class org.graalvm.visualvm.lib.jfluid.ConstructorTest
          minor version: 0
          major version: 51
          flags: (0x0021) ACC_PUBLIC, ACC_SUPER
          this_class: #2                          // org/graalvm/visualvm/lib/jfluid/ConstructorTest
          super_class: #3                         // java/lang/Object
          interfaces: 0, fields: 0, methods: 1, attributes: 1
        Constant pool:
           #1 = Methodref          #3.#13         // java/lang/Object."<init>":()V
           #2 = Class              #14            // org/graalvm/visualvm/lib/jfluid/ConstructorTest
           #3 = Class              #15            // java/lang/Object
           #4 = Utf8               <init>
           #5 = Utf8               ()V
           #6 = Utf8               Code
           #7 = Utf8               LineNumberTable
           #8 = Utf8               LocalVariableTable
           #9 = Utf8               this
          #10 = Utf8               Lorg/graalvm/visualvm/lib/jfluid/ConstructorTest;
          #11 = Utf8               SourceFile
          #12 = Utf8               ConstructorTest.java
          #13 = NameAndType        #4:#5          // "<init>":()V
          #14 = Utf8               org/graalvm/visualvm/lib/jfluid/ConstructorTest
          #15 = Utf8               java/lang/Object
          #16 = Methodref          #17.#19        // org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr.methodEntry:(C)V
          #17 = Class              #18            // org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr
          #18 = Utf8               org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr
          #19 = NameAndType        #20:#21        // methodEntry:(C)V
          #20 = Utf8               methodEntry
          #21 = Utf8               (C)V
          #22 = Methodref          #17.#23        // org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr.methodExit:(C)V
          #23 = NameAndType        #24:#21        // methodExit:(C)V
          #24 = Utf8               methodExit
          #25 = Methodref          #17.#26        // org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr.profilePointHit:(C)V
          #26 = NameAndType        #27:#21        // profilePointHit:(C)V
          #27 = Utf8               profilePointHit
          #28 = Methodref          #17.#29        // org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr.rootMethodEntry:(C)V
          #29 = NameAndType        #30:#21        // rootMethodEntry:(C)V
          #30 = Utf8               rootMethodEntry
          #31 = Utf8               StackMapTable
          #32 = Class              #33            // java/lang/Throwable
          #33 = Utf8               java/lang/Throwable
        {
          public org.graalvm.visualvm.lib.jfluid.ConstructorTest();
            descriptor: ()V
            flags: (0x0001) ACC_PUBLIC
            Code:
              stack=4, locals=2, args_size=1
                 0: sipush        1
                 3: invokestatic  #28                 // Method org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr.rootMethodEntry:(C)V
                 6: nop
                 7: nop
                 8: aload_0
                 9: invokespecial #1                  // Method java/lang/Object."<init>":()V
                12: sipush        1
                15: invokestatic  #22                 // Method org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr.methodExit:(C)V
                18: nop
                19: nop
                20: return
                21: astore_1
                22: sipush        1
                25: invokestatic  #22                 // Method org/graalvm/visualvm/lib/jfluid/server/ProfilerRuntimeCPUFullInstr.methodExit:(C)V
                28: aload_1
                29: athrow
              Exception table:
                 from    to  target type
                     0    21    21   any
              LineNumberTable:
                line 4: 0
              LocalVariableTable:
                Start  Length  Slot  Name   Signature
                    8      13     0  this   Lorg/graalvm/visualvm/lib/jfluid/ConstructorTest;
              StackMapTable: number_of_entries = 1
                frame_type = 255 // full_frame
                  offset_delta = 21
                  locals = [ top ]
                  stack = [ class java/lang/Throwable ]
        }
        SourceFile: "ConstructorTest.java"

        */
        // od -t u1 ConstructorTest.class | awk '{for (i=2; i<=NF; i++) { val=$i; if (val>127) val-=256 ; printf ("%d, ", val); } printf "\n"}'

        private byte[] classBytes = new byte[]{-54, -2, -70, -66, 0, 0, 0, 51, 0, 34, 10, 0, 3, 0, 13, 7, 
            0, 14, 7, 0, 15, 1, 0, 6, 60, 105, 110, 105, 116, 62, 1, 0, 
            3, 40, 41, 86, 1, 0, 4, 67, 111, 100, 101, 1, 0, 15, 76, 105, 
            110, 101, 78, 117, 109, 98, 101, 114, 84, 97, 98, 108, 101, 1, 0, 18, 
            76, 111, 99, 97, 108, 86, 97, 114, 105, 97, 98, 108, 101, 84, 97, 98, 
            108, 101, 1, 0, 4, 116, 104, 105, 115, 1, 0, 49, 76, 111, 114, 103, 
            47, 103, 114, 97, 97, 108, 118, 109, 47, 118, 105, 115, 117, 97, 108, 118, 
            109, 47, 108, 105, 98, 47, 106, 102, 108, 117, 105, 100, 47, 67, 111, 110, 
            115, 116, 114, 117, 99, 116, 111, 114, 84, 101, 115, 116, 59, 1, 0, 10, 
            83, 111, 117, 114, 99, 101, 70, 105, 108, 101, 1, 0, 20, 67, 111, 110, 
            115, 116, 114, 117, 99, 116, 111, 114, 84, 101, 115, 116, 46, 106, 97, 118, 
            97, 12, 0, 4, 0, 5, 1, 0, 47, 111, 114, 103, 47, 103, 114, 97, 
            97, 108, 118, 109, 47, 118, 105, 115, 117, 97, 108, 118, 109, 47, 108, 105, 
            98, 47, 106, 102, 108, 117, 105, 100, 47, 67, 111, 110, 115, 116, 114, 117, 
            99, 116, 111, 114, 84, 101, 115, 116, 1, 0, 16, 106, 97, 118, 97, 47, 
            108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 10, 0, 17, 0, 19, 
            7, 0, 18, 1, 0, 66, 111, 114, 103, 47, 103, 114, 97, 97, 108, 118, 
            109, 47, 118, 105, 115, 117, 97, 108, 118, 109, 47, 108, 105, 98, 47, 106, 
            102, 108, 117, 105, 100, 47, 115, 101, 114, 118, 101, 114, 47, 80, 114, 111, 
            102, 105, 108, 101, 114, 82, 117, 110, 116, 105, 109, 101, 67, 80, 85, 70, 
            117, 108, 108, 73, 110, 115, 116, 114, 12, 0, 20, 0, 21, 1, 0, 11, 
            109, 101, 116, 104, 111, 100, 69, 110, 116, 114, 121, 1, 0, 4, 40, 67, 
            41, 86, 10, 0, 17, 0, 23, 12, 0, 24, 0, 21, 1, 0, 10, 109, 
            101, 116, 104, 111, 100, 69, 120, 105, 116, 10, 0, 17, 0, 26, 12, 0, 
            27, 0, 21, 1, 0, 15, 112, 114, 111, 102, 105, 108, 101, 80, 111, 105, 
            110, 116, 72, 105, 116, 10, 0, 17, 0, 29, 12, 0, 30, 0, 21, 1, 
            0, 15, 114, 111, 111, 116, 77, 101, 116, 104, 111, 100, 69, 110, 116, 114, 
            121, 1, 0, 13, 83, 116, 97, 99, 107, 77, 97, 112, 84, 97, 98, 108, 
            101, 7, 0, 33, 1, 0, 19, 106, 97, 118, 97, 47, 108, 97, 110, 103, 
            47, 84, 104, 114, 111, 119, 97, 98, 108, 101, 0, 33, 0, 2, 0, 3, 
            0, 0, 0, 0, 0, 1, 0, 1, 0, 4, 0, 5, 0, 1, 0, 6, 
            0, 0, 0, 99, 0, 4, 0, 2, 0, 0, 0, 30, 17, 0, 1, -72, 
            0, 28, 0, 0, 42, -73, 0, 1, 17, 0, 1, -72, 0, 22, 0, 0, 
            -79, 76, 17, 0, 1, -72, 0, 22, 43, -65, 0, 1, 0, 0, 0, 21, 
            0, 21, 0, 0, 0, 3, 0, 7, 0, 0, 0, 6, 0, 1, 0, 0, 
            0, 4, 0, 8, 0, 0, 0, 12, 0, 1, 0, 8, 0, 13, 0, 9, 
            0, 10, 0, 0, 0, 31, 0, 0, 0, 13, 0, 1, -1, 0, 21, 0, 
            1, 0, 0, 1, 7, 0, 32, 0, 1, 0, 11, 0, 0, 0, 2, 0, 
            12,};

        private boolean test() throws InstantiationException, IllegalAccessException {
            if (DEBUG) {
                System.err.println("ConstructorTest Class size:"+classBytes.length);
            }
            Class cls = defineClass("org.graalvm.visualvm.lib.jfluid.ConstructorTest", classBytes, 0, classBytes.length);
            if (DEBUG) {
                System.err.println("ConstructorTest Class load:"+cls);
            }
            try {
                cls.getConstructors();
            } catch (VerifyError ve) {
                if (DEBUG) {
                    ve.printStackTrace();
                }
                return false;
            }
            return true;
        }
    }

}
