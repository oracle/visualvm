/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.oql.repository.api;

import java.util.List;
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
public class OQLQueryRepositoryTest {
    private OQLQueryRepository instance;

    public OQLQueryRepositoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = OQLQueryRepository.getInstance();
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of listCategories() method, of class OQLQueryBrowser.
     * Matching for any category
     */
    @Test
    public void testListAllCategories() {
        System.out.println("listAllCategories");

        List<?> result = instance.listCategories();
        assertTrue(!result.isEmpty());
    }

    /**
     * Test of listCategories(pattern) method, of class OQLQueryBrowser.
     * Matching for an existing pattern
     */
    @Test
    public void testListMatchingCategories() {
        System.out.println("listMatchingCategories");
        String pattern = "Sam.*";
        List<?> result = instance.listCategories(pattern);
        assertEquals(1, result.size());
    }

    /**
     * Test of listCategories(pattern) method, of class OQLQueryBrowser.
     * Matching for a nonexisting pattern
     */
    @Test
    public void testListNonMatchingCategories() {
        System.out.println("listNonMatchingCategories");
        String pattern = "[0-9]+";
        List<?> result = instance.listCategories(pattern);
        assertEquals(0, result.size());
    }

    /**
     * Test of listQueries() method, of class OQLQueryBrowser
     * Listing all queries available
     */
    @Test
    public void testListAllQueries() {
        System.out.println("listAllQueries");
        List<?> result = instance.listQueries();
        assertEquals(11, result.size());
    }

    /**
     * Test of listQueries(OQLQueryCategory) method, of class OQLQueryBrowser
     * Listing all queries available for certain category
     */
    @Test
    public void testListAllCategoryQueries() {
        System.out.println("listAllCategoryQueries");
        OQLQueryCategory category = instance.listCategories().get(0);
        List<?> result = instance.listQueries(category);
        assertEquals(4, result.size());
    }

    /**
     * Test of listQueries(String) method, of class OQLQueryBrowser
     * Listing all queries available matching the given pattern
     */
    @Test
    public void testListAllMatchingQueries() {
        System.out.println("listAllMatchingQueries");
        List<?> result = instance.listQueries(".+?allocated.*");
        assertEquals(2, result.size());
    }

    /**
     * Test of listQueries(OQLQueryCategory, String) method, of class OQLQueryBrowser
     * Listing all queries available for certain category matching the given pattern
     */
    @Test
    public void testListMatchingCategoryQueries() {
        System.out.println("listMatchingCategoryQueries");
        OQLQueryCategory category = instance.listCategories().get(0);
        List<?> result = instance.listQueries(category, ".+?allocated.*");
        assertEquals(2, result.size());
    }
}
