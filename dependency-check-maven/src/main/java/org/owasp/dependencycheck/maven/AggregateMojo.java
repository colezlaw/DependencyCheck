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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.owasp.dependencycheck.analyzer.DependencyBundlingAnalyzer;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.utils.Settings;

/**
 * Maven Plugin that checks project dependencies and the dependencies of all child modules to see if they have any known published
 * vulnerabilities.
 *
 * @author Jeremy Long <jeremy.long@owasp.org>
 */
@Mojo(
        name = "aggregate",
        defaultPhase = LifecyclePhase.SITE,
        aggregator = true,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresOnline = true
)
public class AggregateMojo extends BaseDependencyCheckMojo {

    /**
     * Logger field reference.
     */
    private static final Logger LOGGER = Logger.getLogger(AggregateMojo.class.getName());

    /**
     * Executes the aggregate dependency-check goal. This runs dependency-check and generates the subsequent reports.
     *
     * @throws MojoExecutionException thrown if there is ane exception running the mojo
     * @throws MojoFailureException thrown if dependency-check is configured to fail the build
     */
    @Override
    public void runCheck() throws MojoExecutionException, MojoFailureException {
        final Engine engine = generateDataFile();

        if (getProject() == getReactorProjects().get(getReactorProjects().size() - 1)) {
            final Map<MavenProject, Set<MavenProject>> children = buildAggregateInfo();

            for (MavenProject current : getReactorProjects()) {
                final File outputDir = getCorrectOutputDirectory(current);
                if (outputDir == null) { //dc was never run on this project. write the ser to the target.
                    engine.getDependencies().clear();
                    engine.resetFileTypeAnalyzers();
                    scanArtifacts(current, engine);
                    engine.analyzeDependencies();
                    final File target = new File(current.getBuild().getOutputDirectory()).getParentFile();
                    writeDataFile(current, target, engine.getDependencies());
                    showSummary(current, engine.getDependencies());
                }
            }

            for (MavenProject current : getReactorProjects()) {
                List<Dependency> dependencies = readDataFile(current);
                final List<MavenProject> childProjects = getAllChildren(current, children);

                //check for orchestration build - execution root with no children or dependencies
                if ((dependencies == null || dependencies.isEmpty()) && childProjects.isEmpty() && current.isExecutionRoot()) {
                    engine.getDependencies().clear();
                    engine.resetFileTypeAnalyzers();
                    for (MavenProject mod : getReactorProjects()) {
                        scanArtifacts(mod, engine);
                    }
                    engine.analyzeDependencies();
                } else {
                    if (dependencies == null) {
                        dependencies = new ArrayList<Dependency>();
                    }
                    for (MavenProject reportOn : childProjects) {
                        final List<Dependency> childDeps = readDataFile(reportOn);
                        if (childDeps != null && !childDeps.isEmpty()) {
                            dependencies.addAll(childDeps);
                        }
                    }
                    engine.getDependencies().clear();
                    engine.getDependencies().addAll(dependencies);
                    final DependencyBundlingAnalyzer bundler = new DependencyBundlingAnalyzer();
                    try {
                        bundler.analyze(null, engine);
                    } catch (AnalysisException ex) {
                        LOGGER.log(Level.WARNING, "An error occured grouping the dependencies; duplicate entries may exist in the report", ex);
                        LOGGER.log(Level.FINE, "Bundling Exception", ex);
                    }
                }
                File outputDir = getCorrectOutputDirectory(current);
                if (outputDir == null) {
                    //in some regards we shouldn't be writting this, but we are anyway.
                    //we shouldn't write this because nothing is configured to generate this report.
                    outputDir = new File(current.getBuild().getOutputDirectory()).getParentFile();
                }
                writeReports(engine, current, outputDir);
            }
        }
        engine.cleanup();
        Settings.cleanup();
    }

    /**
     * Returns a list containing all the recursive, non-pom children of the given project, never <code>null</code>.
     *
     * @param project the parent project to collect the child project references
     * @param childMap a map of the parent-child relationships
     * @return a list of child projects
     */
    protected List<MavenProject> getAllChildren(MavenProject project, Map<MavenProject, Set<MavenProject>> childMap) {
        final Set<MavenProject> children = childMap.get(project);
        if (children == null) {
            return Collections.emptyList();
        }
        final List<MavenProject> result = new ArrayList<MavenProject>();
        for (MavenProject child : children) {
            if (isMultiModule(child)) {
                result.addAll(getAllChildren(child, childMap));
            } else {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * Test if the project has pom packaging
     *
     * @param mavenProject Project to test
     * @return <code>true</code> if it has a pom packaging; otherwise <code>false</code>
     */
    protected boolean isMultiModule(MavenProject mavenProject) {
        return "pom".equals(mavenProject.getPackaging());
    }

    /**
     * Builds the parent-child map.
     *
     * @return a map of the parent/child relationships
     */
    private Map<MavenProject, Set<MavenProject>> buildAggregateInfo() {
        final Map<MavenProject, Set<MavenProject>> parentChildMap = new HashMap<MavenProject, Set<MavenProject>>();
        for (MavenProject proj : getReactorProjects()) {
            Set<MavenProject> depList = parentChildMap.get(proj.getParent());
            if (depList == null) {
                depList = new HashSet<MavenProject>();
                parentChildMap.put(proj.getParent(), depList);
            }
            depList.add(proj);
        }
        return parentChildMap;
    }

    /**
     * Runs dependency-check's Engine and writes the serialized dependencies to disk.
     *
     * @return the Engine used to execute dependency-check
     * @throws MojoExecutionException thrown if there is an exception running the mojo
     * @throws MojoFailureException thrown if dependency-check is configured to fail the build if severe CVEs are identified.
     */
    protected Engine generateDataFile() throws MojoExecutionException, MojoFailureException {
        final Engine engine;
        try {
            engine = initializeEngine();
        } catch (DatabaseException ex) {
            LOGGER.log(Level.FINE, "Database connection error", ex);
            throw new MojoExecutionException("An exception occured connecting to the local database. Please see the log file for more details.", ex);
        }
        scanArtifacts(getProject(), engine);
        engine.analyzeDependencies();
        writeDataFile(getProject(), null, engine.getDependencies());
        showSummary(getProject(), engine.getDependencies());
        checkForFailure(engine.getDependencies());
        return engine;
    }

    @Override
    public boolean canGenerateReport() {
        return true; //aggregate always returns true for now - we can look at a more complicated/acurate solution later
    }

    /**
     * Returns the report name.
     *
     * @param locale the location
     * @return the report name
     */
    public String getName(Locale locale) {
        return "dependency-check:aggregate";
    }

    /**
     * Gets the description of the Dependency-Check report to be displayed in the Maven Generated Reports page.
     *
     * @param locale The Locale to get the description for
     * @return the description
     */
    public String getDescription(Locale locale) {
        return "Generates an aggregate report of all child Maven projects providing details on any "
                + "published vulnerabilities within project dependencies. This report is a best "
                + "effort and may contain false positives and false negatives.";
    }
}
