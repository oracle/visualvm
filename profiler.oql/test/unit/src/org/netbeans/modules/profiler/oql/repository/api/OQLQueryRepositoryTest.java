/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.oql.repository.api;

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
        
        List result = instance.listCategories();
        assertTrue(result.size() > 0);
    }

    /**
     * Test of listCategories(pattern) method, of class OQLQueryBrowser.
     * Matching for an existing pattern
     */
    @Test
    public void testListMatchingCategories() {
        System.out.println("listMatchingCategories");
        String pattern = "Sam.*";
        List result = instance.listCategories(pattern);
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
        List result = instance.listCategories(pattern);
        assertEquals(0, result.size());
    }

    /**
     * Test of listQueries() method, of class OQLQueryBrowser
     * Listing all queries available
     */
    @Test
    public void testListAllQueries() {
        System.out.println("listAllQueries");
        List result = instance.listQueries();
        assertEquals(4, result.size());
    }

    /**
     * Test of listQueries(OQLQueryCategory) method, of class OQLQueryBrowser
     * Listing all queries available for certain category
     */
    @Test
    public void testListAllCategoryQueries() {
        System.out.println("listAllCategoryQueries");
        OQLQueryCategory category = instance.listCategories().get(0);
        List result = instance.listQueries(category);
        assertEquals(4, result.size());
    }

    /**
     * Test of listQueries(String) method, of class OQLQueryBrowser
     * Listing all queries available matching the given pattern
     */
    @Test
    public void testListAllMatchingQueries() {
        System.out.println("listAllMatchingQueries");
        List result = instance.listQueries(".+?allocated.*");
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
        List result = instance.listQueries(category, ".+?allocated.*");
        assertEquals(2, result.size());
    }
}