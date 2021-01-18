/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.api.caching.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Jaroslav Bachorik
 */
public class SoftReferenceExTest {

    public SoftReferenceExTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of equals method, of class SoftReferenceEx.
     */
    @Test
    public void testEqualsNonEmpty() {
        System.out.println("equals - non empty");
        SoftReferenceEx instance1 = new SoftReferenceEx("xxx");
        SoftReferenceEx instance2 = new SoftReferenceEx("xxx");
        // equals must yield the equals result of the reference
        assertEquals(instance2, instance1);
        // also, the hashcodes must be the same for equaling references
        assertEquals(instance2.hashCode(), instance1.hashCode());
    }

    /**
     * Test of equals method, of class SoftReferenceEx.
     */
    @Test
    public void testEqualsEmpty() {
        System.out.println("equals - empty");
        SoftReferenceEx instance1 = new SoftReferenceEx("xxx");
        SoftReferenceEx instance2 = new SoftReferenceEx(null);
        SoftReferenceEx instance3 = new SoftReferenceEx(null);
        // equals for "non-null"x"null" must yield FALSE
        assertFalse(instance2.equals(instance1));
        // equals for "null"x"null" must yield FALSE
        assertFalse(instance2.equals(instance3));
    }

    @Test
    public void testNotEqualsNonEmpty() {
        System.out.println("equals - non empty");
        SoftReferenceEx instance1 = new SoftReferenceEx("xxx");
        SoftReferenceEx instance2 = new SoftReferenceEx("yyy");
        // equals must yield the equals result of the reference
        assertFalse(instance2.equals(instance1));
        // also, the hashcodes must not be the same for non-equaling references
        assertFalse(instance2.hashCode() == instance1.hashCode());
    }
}