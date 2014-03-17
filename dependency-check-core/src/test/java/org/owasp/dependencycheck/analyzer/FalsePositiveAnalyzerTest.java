/*
 * Copyright 2014 OWASP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.owasp.dependencycheck.analyzer;

import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.dependency.Dependency;

/**
 *
 * @author Jeremy Long <jeremy.long@owasp.org>
 */
public class FalsePositiveAnalyzerTest {

    public FalsePositiveAnalyzerTest() {
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
     * Test of getName method, of class FalsePositiveAnalyzer.
     */
    @Test
    public void testGetName() {
        FalsePositiveAnalyzer instance = new FalsePositiveAnalyzer();
        String expResult = "False Positive Analyzer";
        String result = instance.getName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getAnalysisPhase method, of class FalsePositiveAnalyzer.
     */
    @Test
    public void testGetAnalysisPhase() {
        FalsePositiveAnalyzer instance = new FalsePositiveAnalyzer();
        AnalysisPhase expResult = AnalysisPhase.POST_IDENTIFIER_ANALYSIS;
        AnalysisPhase result = instance.getAnalysisPhase();
        assertEquals(expResult, result);
    }

    /**
     * Test of analyze method, of class FalsePositiveAnalyzer.
     */
    @Test
    public void testAnalyze() throws Exception {
        Dependency dependency = new Dependency();
        dependency.setFileName("pom.xml");
        dependency.addIdentifier("cpe", "cpe:/a:file:file:1.2.1", "http://some.org/url");
        Engine engine = null;
        FalsePositiveAnalyzer instance = new FalsePositiveAnalyzer();
        int before = dependency.getIdentifiers().size();
        instance.analyze(dependency, engine);
        int after = dependency.getIdentifiers().size();
        assertTrue(before > after);
    }

}
