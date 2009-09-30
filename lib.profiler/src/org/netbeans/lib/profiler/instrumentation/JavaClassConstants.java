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

package org.netbeans.lib.profiler.instrumentation;


/**
 * Various constants that may be used in a binary class file.
 *
 * @author Misha Dmitriev
 */
public interface JavaClassConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    /* Class file constants */
    public static final int JAVA_MAGIC = -889275714;
    public static final int JAVA_MAJOR_VERSION = 48;
    public static final int JAVA_MINOR_VERSION = 0;
    public static final int JAVA_MIN_MAJOR_VERSION = 45;
    public static final int JAVA_MIN_MINOR_VERSION = 3;
    public static final int DEFAULT_MAJOR_VERSION = 46;
    public static final int DEFAULT_MINOR_VERSION = 0;

    /* Constant pool entries tag constants */
    public static final int CONSTANT_Utf8 = 1;
    public static final int CONSTANT_Unicode = 2;
    public static final int CONSTANT_Integer = 3;
    public static final int CONSTANT_Float = 4;
    public static final int CONSTANT_Long = 5;
    public static final int CONSTANT_Double = 6;
    public static final int CONSTANT_Class = 7;
    public static final int CONSTANT_String = 8;
    public static final int CONSTANT_Fieldref = 9;
    public static final int CONSTANT_Methodref = 10;
    public static final int CONSTANT_InterfaceMethodref = 11;
    public static final int CONSTANT_NameAndType = 12;

    /* Opcodes */
    static final int opc_try = -3;
    static final int opc_dead = -2;
    static final int opc_label = -1;
    static final int opc_nop = 0;
    static final int opc_aconst_null = 1;
    static final int opc_iconst_m1 = 2;
    static final int opc_iconst_0 = 3;
    static final int opc_iconst_1 = 4;
    static final int opc_iconst_2 = 5;
    static final int opc_iconst_3 = 6;
    static final int opc_iconst_4 = 7;
    static final int opc_iconst_5 = 8;
    static final int opc_lconst_0 = 9;
    static final int opc_lconst_1 = 10;
    static final int opc_fconst_0 = 11;
    static final int opc_fconst_1 = 12;
    static final int opc_fconst_2 = 13;
    static final int opc_dconst_0 = 14;
    static final int opc_dconst_1 = 15;
    static final int opc_bipush = 16;
    static final int opc_sipush = 17;
    static final int opc_ldc = 18;
    static final int opc_ldc_w = 19;
    static final int opc_ldc2_w = 20;
    static final int opc_iload = 21;
    static final int opc_lload = 22;
    static final int opc_fload = 23;
    static final int opc_dload = 24;
    static final int opc_aload = 25;
    static final int opc_iload_0 = 26;
    static final int opc_iload_1 = 27;
    static final int opc_iload_2 = 28;
    static final int opc_iload_3 = 29;
    static final int opc_lload_0 = 30;
    static final int opc_lload_1 = 31;
    static final int opc_lload_2 = 32;
    static final int opc_lload_3 = 33;
    static final int opc_fload_0 = 34;
    static final int opc_fload_1 = 35;
    static final int opc_fload_2 = 36;
    static final int opc_fload_3 = 37;
    static final int opc_dload_0 = 38;
    static final int opc_dload_1 = 39;
    static final int opc_dload_2 = 40;
    static final int opc_dload_3 = 41;
    static final int opc_aload_0 = 42;
    static final int opc_aload_1 = 43;
    static final int opc_aload_2 = 44;
    static final int opc_aload_3 = 45;
    static final int opc_iaload = 46;
    static final int opc_laload = 47;
    static final int opc_faload = 48;
    static final int opc_daload = 49;
    static final int opc_aaload = 50;
    static final int opc_baload = 51;
    static final int opc_caload = 52;
    static final int opc_saload = 53;
    static final int opc_istore = 54;
    static final int opc_lstore = 55;
    static final int opc_fstore = 56;
    static final int opc_dstore = 57;
    static final int opc_astore = 58;
    static final int opc_istore_0 = 59;
    static final int opc_istore_1 = 60;
    static final int opc_istore_2 = 61;
    static final int opc_istore_3 = 62;
    static final int opc_lstore_0 = 63;
    static final int opc_lstore_1 = 64;
    static final int opc_lstore_2 = 65;
    static final int opc_lstore_3 = 66;
    static final int opc_fstore_0 = 67;
    static final int opc_fstore_1 = 68;
    static final int opc_fstore_2 = 69;
    static final int opc_fstore_3 = 70;
    static final int opc_dstore_0 = 71;
    static final int opc_dstore_1 = 72;
    static final int opc_dstore_2 = 73;
    static final int opc_dstore_3 = 74;
    static final int opc_astore_0 = 75;
    static final int opc_astore_1 = 76;
    static final int opc_astore_2 = 77;
    static final int opc_astore_3 = 78;
    static final int opc_iastore = 79;
    static final int opc_lastore = 80;
    static final int opc_fastore = 81;
    static final int opc_dastore = 82;
    static final int opc_aastore = 83;
    static final int opc_bastore = 84;
    static final int opc_castore = 85;
    static final int opc_sastore = 86;
    static final int opc_pop = 87;
    static final int opc_pop2 = 88;
    static final int opc_dup = 89;
    static final int opc_dup_x1 = 90;
    static final int opc_dup_x2 = 91;
    static final int opc_dup2 = 92;
    static final int opc_dup2_x1 = 93;
    static final int opc_dup2_x2 = 94;
    static final int opc_swap = 95;
    static final int opc_iadd = 96;
    static final int opc_ladd = 97;
    static final int opc_fadd = 98;
    static final int opc_dadd = 99;
    static final int opc_isub = 100;
    static final int opc_lsub = 101;
    static final int opc_fsub = 102;
    static final int opc_dsub = 103;
    static final int opc_imul = 104;
    static final int opc_lmul = 105;
    static final int opc_fmul = 106;
    static final int opc_dmul = 107;
    static final int opc_idiv = 108;
    static final int opc_ldiv = 109;
    static final int opc_fdiv = 110;
    static final int opc_ddiv = 111;
    static final int opc_irem = 112;
    static final int opc_lrem = 113;
    static final int opc_frem = 114;
    static final int opc_drem = 115;
    static final int opc_ineg = 116;
    static final int opc_lneg = 117;
    static final int opc_fneg = 118;
    static final int opc_dneg = 119;
    static final int opc_ishl = 120;
    static final int opc_lshl = 121;
    static final int opc_ishr = 122;
    static final int opc_lshr = 123;
    static final int opc_iushr = 124;
    static final int opc_lushr = 125;
    static final int opc_iand = 126;
    static final int opc_land = 127;
    static final int opc_ior = 128;
    static final int opc_lor = 129;
    static final int opc_ixor = 130;
    static final int opc_lxor = 131;
    static final int opc_iinc = 132;
    static final int opc_i2l = 133;
    static final int opc_i2f = 134;
    static final int opc_i2d = 135;
    static final int opc_l2i = 136;
    static final int opc_l2f = 137;
    static final int opc_l2d = 138;
    static final int opc_f2i = 139;
    static final int opc_f2l = 140;
    static final int opc_f2d = 141;
    static final int opc_d2i = 142;
    static final int opc_d2l = 143;
    static final int opc_d2f = 144;
    static final int opc_i2b = 145;
    static final int opc_i2c = 146;
    static final int opc_i2s = 147;
    static final int opc_lcmp = 148;
    static final int opc_fcmpl = 149;
    static final int opc_fcmpg = 150;
    static final int opc_dcmpl = 151;
    static final int opc_dcmpg = 152;
    static final int opc_ifeq = 153;
    static final int opc_ifne = 154;
    static final int opc_iflt = 155;
    static final int opc_ifge = 156;
    static final int opc_ifgt = 157;
    static final int opc_ifle = 158;
    static final int opc_if_icmpeq = 159;
    static final int opc_if_icmpne = 160;
    static final int opc_if_icmplt = 161;
    static final int opc_if_icmpge = 162;
    static final int opc_if_icmpgt = 163;
    static final int opc_if_icmple = 164;
    static final int opc_if_acmpeq = 165;
    static final int opc_if_acmpne = 166;
    static final int opc_goto = 167;
    static final int opc_jsr = 168;
    static final int opc_ret = 169;
    static final int opc_tableswitch = 170;
    static final int opc_lookupswitch = 171;
    static final int opc_ireturn = 172;
    static final int opc_lreturn = 173;
    static final int opc_freturn = 174;
    static final int opc_dreturn = 175;
    static final int opc_areturn = 176;
    static final int opc_return = 177;
    static final int opc_getstatic = 178;
    static final int opc_putstatic = 179;
    static final int opc_getfield = 180;
    static final int opc_putfield = 181;
    static final int opc_invokevirtual = 182;
    static final int opc_invokespecial = 183;
    static final int opc_invokestatic = 184;
    static final int opc_invokeinterface = 185;
    static final int opc_xxxunusedxxx = 186;
    static final int opc_new = 187;
    static final int opc_newarray = 188;
    static final int opc_anewarray = 189;
    static final int opc_arraylength = 190;
    static final int opc_athrow = 191;
    static final int opc_checkcast = 192;
    static final int opc_instanceof = 193;
    static final int opc_monitorenter = 194;
    static final int opc_monitorexit = 195;
    static final int opc_wide = 196;
    static final int opc_multianewarray = 197;
    static final int opc_ifnull = 198;
    static final int opc_ifnonnull = 199;
    static final int opc_goto_w = 200;
    static final int opc_jsr_w = 201;
    static final int opc_breakpoint = 202;
    public static final int[] opc_length = {
                                               1, // opc_nop
    1, // opc_aconst_null              = 1;
    1, // opc_iconst_m1                = 2;
    1, // opc_iconst_0                 = 3;
    1, // opc_iconst_1                 = 4;
    1, // opc_iconst_2                 = 5;
    1, // opc_iconst_3                 = 6;
    1, // opc_iconst_4                 = 7;
    1, // opc_iconst_5                 = 8;
    1, // opc_lconst_0                 = 9;
    1, // opc_lconst_1                 = 10;
    1, // opc_fconst_0                 = 11;
    1, // opc_fconst_1                 = 12;
    1, // opc_fconst_2                 = 13;
    1, // opc_dconst_0                 = 14;
    1, // opc_dconst_1                 = 15;
    2, // opc_bipush
    3, // opc_sipush
    2, // opc_ldc
    3, // opc_ldc_w
    3, // opc_ldc2_w
    2, // opc_iload
    2, // opc_lload
    2, // opc_fload
    2, // opc_dload
    2, // opc_aload
    1, // opc_iload_0                  = 26;
    1, // opc_iload_1                  = 27;
    1, // opc_iload_2                  = 28;
    1, // opc_iload_3                  = 29;
    1, // opc_lload_0                  = 30;
    1, // opc_lload_1                  = 31;
    1, // opc_lload_2                  = 32;
    1, // opc_lload_3                  = 33;
    1, // opc_fload_0                  = 34;
    1, // opc_fload_1                  = 35;
    1, // opc_fload_2                  = 36;
    1, // opc_fload_3                  = 37;
    1, // opc_dload_0                  = 38;
    1, // opc_dload_1                  = 39;
    1, // opc_dload_2                  = 40;
    1, // opc_dload_3                  = 41;
    1, // opc_aload_0                  = 42;
    1, // opc_aload_1                  = 43;
    1, // opc_aload_2                  = 44;
    1, // opc_aload_3                  = 45;
    1, // opc_iaload                   = 46;
    1, // opc_laload                   = 47;
    1, // opc_faload                   = 48;
    1, // opc_daload                   = 49;
    1, // opc_aaload                   = 50;
    1, // opc_baload                   = 51;
    1, // opc_caload                   = 52;
    1, // opc_saload                   = 53;
    2, // opc_istore
    2, // opc_lstore
    2, // opc_fstore
    2, // opc_dstore
    2, // opc_astore
    1, // opc_istore_0                 = 59;
    1, // opc_istore_1                 = 60;
    1, // opc_istore_2                 = 61;
    1, // opc_istore_3                 = 62;
    1, // opc_lstore_0                 = 63;
    1, // opc_lstore_1                 = 64;
    1, // opc_lstore_2                 = 65;
    1, // opc_lstore_3                 = 66;
    1, // opc_fstore_0                 = 67;
    1, // opc_fstore_1                 = 68;
    1, // opc_fstore_2                 = 69;
    1, // opc_fstore_3                 = 70;
    1, // opc_dstore_0                 = 71;
    1, // opc_dstore_1                 = 72;
    1, // opc_dstore_2                 = 73;
    1, // opc_dstore_3                 = 74;
    1, // opc_astore_0                 = 75;
    1, // opc_astore_1                 = 76;
    1, // opc_astore_2                 = 77;
    1, // opc_astore_3                 = 78;
    1, // opc_iastore                  = 79;
    1, // opc_lastore                  = 80;
    1, // opc_fastore                  = 81;
    1, // opc_dastore                  = 82;
    1, // opc_aastore                  = 83;
    1, // opc_bastore                  = 84;
    1, // opc_castore                  = 85;
    1, // opc_sastore                  = 86;
    1, // opc_pop                      = 87;
    1, // opc_pop2                     = 88;
    1, // opc_dup                      = 89;
    1, // opc_dup_x1                   = 90;
    1, // opc_dup_x2                   = 91;
    1, // opc_dup2                     = 92;
    1, // opc_dup2_x1                  = 93;
    1, // opc_dup2_x2                  = 94;
    1, // opc_swap                     = 95;
    1, // opc_iadd                     = 96;
    1, // opc_ladd                     = 97;
    1, // opc_fadd                     = 98;
    1, // opc_dadd                     = 99;
    1, // opc_isub                     = 100;
    1, // opc_lsub                     = 101;
    1, // opc_fsub                     = 102;
    1, // opc_dsub                     = 103;
    1, // opc_imul                     = 104;
    1, // opc_lmul                     = 105;
    1, // opc_fmul                     = 106;
    1, // opc_dmul                     = 107;
    1, // opc_idiv                     = 108;
    1, // opc_ldiv                     = 109;
    1, // opc_fdiv                     = 110;
    1, // opc_ddiv                     = 111;
    1, // opc_irem                     = 112;
    1, // opc_lrem                     = 113;
    1, // opc_frem                     = 114;
    1, // opc_drem                     = 115;
    1, // opc_ineg                     = 116;
    1, // opc_lneg                     = 117;
    1, // opc_fneg                     = 118;
    1, // opc_dneg                     = 119;
    1, // opc_ishl                     = 120;
    1, // opc_lshl                     = 121;
    1, // opc_ishr                     = 122;
    1, // opc_lshr                     = 123;
    1, // opc_iushr                    = 124;
    1, // opc_lushr                    = 125;
    1, // opc_iand                     = 126;
    1, // opc_land                     = 127;
    1, // opc_ior                      = 128;
    1, // opc_lor                      = 129;
    1, // opc_ixor                     = 130;
    1, // opc_lxor                     = 131;
    3, // opc_iinc
    1, // opc_i2l                      = 133;
    1, // opc_i2f                      = 134;
    1, // opc_i2d                      = 135;
    1, // opc_l2i                      = 136;
    1, // opc_l2f                      = 137;
    1, // opc_l2d                      = 138;
    1, // opc_f2i                      = 139;
    1, // opc_f2l                      = 140;
    1, // opc_f2d                      = 141;
    1, // opc_d2i                      = 142;
    1, // opc_d2l                      = 143;
    1, // opc_d2f                      = 144;
    1, // opc_i2b                      = 145;
    1, // opc_i2c                      = 146;
    1, // opc_i2s                      = 147;
    1, // opc_lcmp                     = 148;
    1, // opc_fcmpl                    = 149;
    1, // opc_fcmpg                    = 150;
    1, // opc_dcmpl                    = 151;
    1, // opc_dcmpg                    = 152;
    3, // opc_ifeq
    3, // opc_ifne
    3, // opc_iflt
    3, // opc_ifge
    3, // opc_ifgt
    3, // opc_ifle
    3, // opc_if_icmpeq
    3, // opc_if_icmpne
    3, // opc_if_icmplt
    3, // opc_if_icmpge
    3, // opc_if_icmpgt
    3, // opc_if_icmple
    3, // opc_if_acmpeq
    3, // opc_if_acmpne
    3, // opc_goto
    3, // opc_jsr
    2, // opc_ret
    0, // opc_tableswitch - variable length, handled specially
    0, // opc_lookupswitch - variable length, handled specially
    1, // opc_ireturn                  = 172;
    1, // opc_lreturn                  = 173;
    1, // opc_freturn                  = 174;
    1, // opc_dreturn                  = 175;
    1, // opc_areturn                  = 176;
    1, // opc_return                   = 177;
    3, // opc_getstatic
    3, // opc_putstatic
    3, // opc_getfield
    3, // opc_putfield
    3, // opc_invokevirtual
    3, // opc_invokespecial
    3, // opc_invokestatic
    5, // opc_invokeinterface
    0, // opc_xxxunusedxxx
    3, // opc_new
    2, // opc_newarray
    3, // opc_anewarray
    1, // opc_arraylength              = 190;
    1, // opc_athrow                   = 191;
    3, // opc_checkcast
    3, // opc_instanceof
    1, // opc_monitorenter             = 194;
    1, // opc_monitorexit              = 195;
    0, // opc_wide - special handling
    4, // opc_multianewarray
    3, // opc_ifnull
    3, // opc_ifnonnull
    5, // opc_goto_w
    5, // opc_jsr_w
    0, // opc_breakpoint
                                           };

    /* Primitive type array codes (used by opc_newarray opcode) */
    public static final int T_BOOLEAN = 4; // Z
    public static final int T_CHAR = 5; // C
    public static final int T_FLOAT = 6; // F
    public static final int T_DOUBLE = 7; // D
    public static final int T_BYTE = 8; // B
    public static final int T_SHORT = 9; // S
    public static final int T_INT = 10; // I
    public static final int T_LONG = 11; // J
    public static final String[] PRIMITIVE_ARRAY_TYPE_NAMES = {
                                                                  null, null, null, null, "[Z", // NOI18N
    "[C", // NOI18N
    "[F", // NOI18N
    "[D", // NOI18N
    "[B", // NOI18N
    "[S", // NOI18N
    "[I", // NOI18N
    "[J" // NOI18N
                                                              };
}
