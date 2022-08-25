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

package org.graalvm.visualvm.api.caching;

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
public class CacheImplTest {
    private Cache<String, String> nullCache;
    private Cache<String, String> constantCache;

    private boolean cacheMissed = false;

    public CacheImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        nullCache = CacheFactory.getInstance().softMapCache(new EntryFactory<String, String>() {
            @Override
            public Entry<String> createEntry(String key) {
                cacheMissed = true;
                return null;
            }
        });
        constantCache = CacheFactory.getInstance().softMapCache(new EntryFactory<String, String>() {
            @Override
            public Entry<String> createEntry(String key) {
                cacheMissed = true;
                return new Entry<>("test");
            }
        });
    }

    @After
    public void tearDown() {
        nullCache = null;
        constantCache = null;
    }

    /**
     * Test of retrieveObject method, of class AbstractCache.
     */
    @Test
    public void testRetrieveObjectNoResolver() {
        System.out.println("retrieveObject - no resolver");
        String key = "key";
        String result = nullCache.retrieveObject(key);
        assertNull(result);
    }

    /**
     * Test of retrieveObject method, of class AbstractCache.
     */
    @Test
    public void testRetrieveObjectConstantResolver() {
        System.out.println("retrieveObject - constant resolver");
        String key = "key";
        String expected = "test";
        String result = constantCache.retrieveObject(key);
        assertTrue(cacheMissed);
        assertEquals(expected, result);
    }

    /**
     * Test of retrieveObject method, of class AbstractCache.
     */
    @Test
    public void testRetrieveObjectCached() {
        System.out.println("retrieveObject - cached");
        String key = "key";
        String expected = "test";
        constantCache.setTTL(500);
        String result = constantCache.retrieveObject(key);
        assertTrue(cacheMissed);
        cacheMissed = false;
        result = constantCache.retrieveObject(key);
        assertFalse(cacheMissed);
        
        assertEquals(expected, result);
    }

    /**
     * Test of retrieveObject method, of class AbstractCache.
     */
    @Test
    public void testRetrieveObjectEvicted() throws Exception {
        System.out.println("retrieveObject - evicted");
        String key = "key";
        String expected = "test";
        constantCache.setTTL(500);
        String result = constantCache.retrieveObject(key);
        assertTrue(cacheMissed);
        cacheMissed = false;
        Thread.sleep(1000);
        result = constantCache.retrieveObject(key);
        assertTrue(cacheMissed);

        assertEquals(expected, result);
    }

    /**
     * Test of invalidateObject method, of class AbstractCache.
     */
    @Test
    public void testInvalidateObject() {
        System.out.println("invalidateObject");
        String key = "key";
        String expResult = "test";

        constantCache.retrieveObject(key);
        cacheMissed = false;
        constantCache.invalidateObject(key);
        String result = constantCache.retrieveObject(key);
        assertTrue(cacheMissed);
        assertEquals(expResult, result);
    }

}