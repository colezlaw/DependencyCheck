/*
 * This file is part of dependency-check-core.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2013 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.data.nvdcve;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.dependencycheck.data.update.NvdCveInfo;

/**
 *
 * @author Jeremy Long <jeremy.long@owasp.org>
 */
public class DatabasePropertiesTest {

    public DatabasePropertiesTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of isEmpty method, of class DatabaseProperties.
     */
    @Test
    public void testIsEmpty() throws Exception {
        CveDB cveDB = new CveDB();
        cveDB.open();
        DatabaseProperties instance = cveDB.getDatabaseProperties();
        boolean expResult = false;
        boolean result = instance.isEmpty();
        //no exception means the call worked... whether or not it is empty depends on if the db is new
        //assertEquals(expResult, result);
        cveDB.close();
    }

    /**
     * Test of save method, of class DatabaseProperties.
     */
    @Test
    public void testSave() throws Exception {
        NvdCveInfo updatedValue = new NvdCveInfo();
        String key = "test";
        long expected = 1337;
        updatedValue.setId(key);
        updatedValue.setTimestamp(expected);
        CveDB cveDB = new CveDB();
        cveDB.open();
        DatabaseProperties instance = cveDB.getDatabaseProperties();
        instance.save(updatedValue);
        //reload the properties
        cveDB.close();
        cveDB = new CveDB();
        cveDB.open();
        instance = cveDB.getDatabaseProperties();
        cveDB.close();
        long results = Long.parseLong(instance.getProperty("lastupdated." + key));
        assertEquals(expected, results);
    }

    /**
     * Test of getProperty method, of class DatabaseProperties.
     */
    @Test
    public void testGetProperty_String_String() throws Exception {
        String key = "doesn't exist";
        String defaultValue = "default";
        CveDB cveDB = new CveDB();
        cveDB.open();
        DatabaseProperties instance = cveDB.getDatabaseProperties();
        cveDB.close();
        String expResult = "default";
        String result = instance.getProperty(key, defaultValue);
        assertEquals(expResult, result);
    }
}
