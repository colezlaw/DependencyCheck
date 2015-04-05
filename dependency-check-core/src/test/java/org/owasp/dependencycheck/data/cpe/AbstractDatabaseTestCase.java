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
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.data.cpe;

import org.junit.Before;
import org.owasp.dependencycheck.BaseTest;
import org.owasp.dependencycheck.data.nvdcve.BaseDBTestCase;

/**
 * An abstract database test case that is used to ensure the H2 DB exists prior to performing tests that utilize the
 * data contained within.
 *
 * @author Jeremy Long
 */
public abstract class AbstractDatabaseTestCase extends BaseTest {

    @Before
    public void setUp() throws Exception {
        BaseDBTestCase.ensureDBExists();
    }

}
