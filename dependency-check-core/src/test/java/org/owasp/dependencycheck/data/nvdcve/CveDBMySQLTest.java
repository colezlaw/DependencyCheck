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
 * Copyright (c) 2014 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.data.nvdcve;

import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;

/**
 *
 * @author Jeremy Long
 */
public class CveDBMySQLTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Pretty useless tests of open, commit, and close methods, of class CveDB.
     */
    @Test
    public void testOpen() throws DatabaseException {
        try {
            CveDB instance = new CveDB();
            instance.open();
            instance.close();
        } catch (DatabaseException ex) {
            System.out.println("Unable to connect to the My SQL database; verify that the db server is running and that the schema has been generated");
            throw ex;
        }
    }

    /**
     * Test of getCPEs method, of class CveDB.
     */
    @Test
    public void testGetCPEs() throws Exception {
        CveDB instance = new CveDB();
        try {
            String vendor = "apache";
            String product = "struts";
            instance.open();
            Set<VulnerableSoftware> result = instance.getCPEs(vendor, product);
            assertTrue("Has data been loaded into the MySQL DB? if not consider using the CLI to populate it", result.size() > 5);
        } catch (Exception ex) {
            System.out.println("Unable to access the My SQL database; verify that the db server is running and that the schema has been generated");
            throw ex;
        } finally {
            instance.close();
        }
    }

    /**
     * Test of getVulnerabilities method, of class CveDB.
     */
    @Test
    public void testGetVulnerabilities() throws Exception {
        String cpeStr = "cpe:/a:apache:struts:2.1.2";
        CveDB instance = new CveDB();
        try {
            instance.open();
            List result = instance.getVulnerabilities(cpeStr);
            assertTrue(result.size() > 5);
        } catch (Exception ex) {
            System.out.println("Unable to access the My SQL database; verify that the db server is running and that the schema has been generated");
            throw ex;
        } finally {
            instance.close();
        }
    }
}
