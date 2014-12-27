/*
 * This file is part of dependency-check-maven.
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
package org.owasp.dependencycheck.maven;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.utils.Settings;

/**
 * Maven Plugin that checks the project dependencies to see if they have any known published vulnerabilities.
 *
 * @author Jeremy Long <jeremy.long@owasp.org>
 */
@Mojo(
        name = "check",
        defaultPhase = LifecyclePhase.COMPILE,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresOnline = true
)
public class CheckMojo extends BaseDependencyCheckMojo {

    /**
     * Logger field reference.
     */
    private static final Logger LOGGER = Logger.getLogger(CheckMojo.class.getName());

    /**
     * Returns whether or not a the report can be generated.
     *
     * @return <code>true</code> if the report can be generated; otherwise <code>false</code>
     */
    @Override
    public boolean canGenerateReport() {
        boolean isCapable = false;
        for (Artifact a : getProject().getArtifacts()) {
            if (!excludeFromScan(a)) {
                isCapable = true;
                break;
            }
        }
        return isCapable;
    }

    /**
     * Executes the dependency-check engine on the project's dependencies and generates the report.
     *
     * @throws MojoExecutionException thrown if there is an exception executing the goal
     * @throws MojoFailureException thrown if dependency-check is configured to fail the build
     */
    @Override
    public void runCheck() throws MojoExecutionException, MojoFailureException {
        final Engine engine;
        try {
            engine = initializeEngine();
        } catch (DatabaseException ex) {
            LOGGER.log(Level.FINE, "Database connection error", ex);
            throw new MojoExecutionException("An exception occured connecting to the local database. Please see the log file for more details.", ex);
        }
        scanArtifacts(getProject(), engine);
        if (engine.getDependencies().isEmpty()) {
            LOGGER.info("No dependencies were identified that could be analyzed by dependency-check");
        } else {
            engine.analyzeDependencies();
            writeReports(engine, getProject(), getCorrectOutputDirectory());
            writeDataFile(engine.getDependencies());
            showSummary(engine.getDependencies());
            checkForFailure(engine.getDependencies());
        }
        engine.cleanup();
        Settings.cleanup();
    }

    /**
     * Returns the report name.
     *
     * @param locale the location
     * @return the report name
     */
    public String getName(Locale locale) {
        return "dependency-check";
    }

    /**
     * Gets the description of the Dependency-Check report to be displayed in the Maven Generated Reports page.
     *
     * @param locale The Locale to get the description for
     * @return the description
     */
    public String getDescription(Locale locale) {
        return "Generates a report providing details on any published vulnerabilities within project dependencies. "
                + "This report is a best effort and may contain false positives and false negatives.";
    }

}
