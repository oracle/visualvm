/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.tools.visualvm.api.caching;

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
                return new Entry<String>("test");
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