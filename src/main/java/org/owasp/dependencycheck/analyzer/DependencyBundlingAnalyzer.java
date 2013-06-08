/*
 * This file is part of Dependency-Check.
 *
 * Dependency-Check is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Dependency-Check is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Dependency-Check. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.analyzer;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.utils.DependencyVersion;
import org.owasp.dependencycheck.utils.DependencyVersionUtil;

/**
 * <p>This analyzer ensures dependencies that should be grouped together, to
 * remove excess noise from the report, are grouped. An example would be Spring,
 * Spring Beans, Spring MVC, etc. If they are all for the same version and have
 * the same relative path then these should be grouped into a single dependency
 * under the core/main library.</p>
 * <p>Note, this grouping only works on dependencies with identified CVE
 * entries</p>
 *
 * @author Jeremy Long (jeremy.long@owasp.org)
 */
public class DependencyBundlingAnalyzer extends AbstractAnalyzer implements Analyzer {

    //<editor-fold defaultstate="collapsed" desc="Constants and Member Variables">
    /**
     * A pattern for obtaining the first part of a filename.
     */
    private static final Pattern STARTING_TEXT_PATTERN = Pattern.compile("^[a-zA-Z]*");
    /**
     * a flag indicating if this analyzer has run. This analyzer only runs once.
     */
    private boolean analyzed = false;
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="All standard implmentation details of Analyzer">
    /**
     * The set of file extensions supported by this analyzer.
     */
    private static final Set<String> EXTENSIONS = null;
    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "Dependency Bundling Analyzer";
    /**
     * The phase that this analyzer is intended to run in.
     */
    private static final AnalysisPhase ANALYSIS_PHASE = AnalysisPhase.PRE_FINDING_ANALYSIS;
    /**
     * Returns a list of file EXTENSIONS supported by this analyzer.
     * @return a list of file EXTENSIONS supported by this analyzer.
     */
    public Set<String> getSupportedExtensions() {
        return EXTENSIONS;
    }

    /**
     * Returns the name of the analyzer.
     * @return the name of the analyzer.
     */
    public String getName() {
        return ANALYZER_NAME;
    }

    /**
     * Returns whether or not this analyzer can process the given extension.
     * @param extension the file extension to test for support
     * @return whether or not the specified file extension is supported by this
     * analyzer.
     */
    public boolean supportsExtension(String extension) {
        return true;
    }

    /**
     * Returns the phase that the analyzer is intended to run in.
     * @return the phase that the analyzer is intended to run in.
     */
    public AnalysisPhase getAnalysisPhase() {
        return ANALYSIS_PHASE;
    }
    //</editor-fold>

    /**
     * Analyzes a set of dependencies. If they have been found to have the same
     * base path and the same set of identifiers they are likely related. The
     * related dependencies are bundled into a single reportable item.
     *
     * @param ignore this analyzer ignores the dependency being analyzed
     * @param engine the engine that is scanning the dependencies
     * @throws AnalysisException is thrown if there is an error reading the JAR
     * file.
     */
    public void analyze(Dependency ignore, Engine engine) throws AnalysisException {
        if (!analyzed) {
            analyzed = true;
            final Set<Dependency> dependenciesToRemove = new HashSet<Dependency>();
            final ListIterator<Dependency> mainIterator = engine.getDependencies().listIterator();
            //for (Dependency nextDependency : engine.getDependencies()) {
            while (mainIterator.hasNext()) {
                final Dependency dependency = mainIterator.next();
                if (mainIterator.hasNext()) {
                    final ListIterator<Dependency> subIterator = engine.getDependencies().listIterator(mainIterator.nextIndex());
                    while (subIterator.hasNext()) {
                        final Dependency nextDependency = subIterator.next();

                        if (identifiersMatch(dependency, nextDependency)
                                && hasSameBasePath(dependency, nextDependency)
                                && fileNameMatch(dependency, nextDependency)) {

                            if (isCore(dependency, nextDependency)) {
                                dependency.addRelatedDependency(nextDependency);
                                //move any "related dependencies" to the new "parent" dependency
                                final Iterator<Dependency> i = nextDependency.getRelatedDependencies().iterator();
                                while (i.hasNext()) {
                                    dependency.addRelatedDependency(i.next());
                                    i.remove();
                                }
                                dependenciesToRemove.add(nextDependency);
                            } else {
                                if (isCore(nextDependency, dependency)) {
                                    nextDependency.addRelatedDependency(dependency);
                                    //move any "related dependencies" to the new "parent" dependency
                                    final Iterator<Dependency> i = dependency.getRelatedDependencies().iterator();
                                    while (i.hasNext()) {
                                        nextDependency.addRelatedDependency(i.next());
                                        i.remove();
                                    }
                                    dependenciesToRemove.add(dependency);
                                }
                            }
                        }
                    }
                }
            }
            //removing dependencies here as ensuring correctness and avoiding ConcurrentUpdateExceptions
            // was difficult because of the inner iterator.
            for (Dependency d : dependenciesToRemove) {
                engine.getDependencies().remove(d);
            }
        }
    }

    /**
     * Attempts to trim a maven repo to a common base path. This is typically
     * [drive]\[repo_location]\repository\[path1]\[path2].
     *
     * @param path the path to trim
     * @return a string representing the base path.
     */
    private String getBaseRepoPath(final String path) {
        int pos = path.indexOf("repository" + File.separator) + 11;
        if (pos < 0) {
            return path;
        }
        int tmp = path.indexOf(File.separator, pos);
        if (tmp <= 0) {
            return path;
        }
        if (tmp > 0) {
            pos = tmp + 1;
        }
        tmp = path.indexOf(File.separator, pos);
        if (tmp > 0) {
            pos = tmp + 1;
        }
        return path.substring(0, pos);
    }

    /**
     * Returns true if the file names (and version if it exists) of the two
     * dependencies are sufficiently similiar.
     * @param dependency1 a dependency2 to compare
     * @param dependency2 a dependency2 to compare
     * @return true if the identifiers in the two supplied dependencies are equal
     */
    private boolean fileNameMatch(Dependency dependency1, Dependency dependency2) {
        if (dependency1 == null || dependency1.getFileName() == null
                || dependency2 == null || dependency2.getFileName() == null) {
            return false;
        }
        final String fileName1 = dependency1.getFileName();
        final String fileName2 = dependency2.getFileName();
        //version check
        final DependencyVersion version1 = DependencyVersionUtil.parseVersionFromFileName(fileName1);
        final DependencyVersion version2 = DependencyVersionUtil.parseVersionFromFileName(fileName2);
        if (version1 != null && version2 != null) {
            if (!version1.equals(version2)) {
                return false;
            }
        }
        //filename check
        final Matcher match1 = STARTING_TEXT_PATTERN.matcher(fileName1);
        final Matcher match2 = STARTING_TEXT_PATTERN.matcher(fileName2);
        if (match1.find() && match2.find()) {
            return match1.group().equals(match2.group());
        }

        return false;
    }

    /**
     * Returns true if the identifiers in the two supplied dependencies are equal.
     * @param dependency1 a dependency2 to compare
     * @param dependency2 a dependency2 to compare
     * @return true if the identifiers in the two supplied dependencies are equal
     */
    private boolean identifiersMatch(Dependency dependency1, Dependency dependency2) {
        if (dependency1 == null || dependency1.getIdentifiers() == null
                || dependency2 == null || dependency2.getIdentifiers() == null) {
            return false;
        }
        return dependency1.getIdentifiers().size() > 0
                && dependency2.getIdentifiers().equals(dependency1.getIdentifiers());
    }

    /**
     * Determines if the two dependencies have the same base path.
     * @param dependency1 a Dependency object
     * @param dependency2 a Dependency object
     * @return true if the base paths of the dependencies are identical
     */
    private boolean hasSameBasePath(Dependency dependency1, Dependency dependency2) {
        if (dependency1 == null || dependency2 == null) {
            return false;
        }
        final File lFile = new File(dependency1.getFilePath());
        String left = lFile.getParent();
        final File rFile = new File(dependency2.getFilePath());
        String right = rFile.getParent();
        if (left == null) {
            if (right == null) {
                return true;
            }
            return false;
        }
        if (left.equalsIgnoreCase(right)) {
            return true;
        }
        if (left.matches(".*[/\\\\]repository[/\\\\].*") && right.matches(".*[/\\\\]repository[/\\\\].*")) {
            left = getBaseRepoPath(left);
            right = getBaseRepoPath(right);
        }
        return left.equalsIgnoreCase(right);
    }

    /**
     * This is likely a very broken attempt at determining if the 'left'
     * dependency is the 'core' library in comparison to the 'right' library.
     *
     * TODO - consider splitting on /\._-\s/ and checking if all of one side is fully contained in the other
     *  With the exception of the word "core". This might work even on groups when we don't have a CVE.
     *
     * @param left the dependency to test
     * @param right the dependency to test against
     * @return a boolean indicating whether or not the left dependency should be
     * considered the "core" version.
     */
    private boolean isCore(Dependency left, Dependency right) {
        final String leftName = left.getFileName().toLowerCase();
        final String rightName = right.getFileName().toLowerCase();

        if (rightName.contains("core") && !leftName.contains("core")) {
            return false;
        } else if (!rightName.contains("core") && leftName.contains("core")) {
            return true;
        } else {
            //TODO should we be splitting the name on [-_(.\d)+] and seeing if the
            //  parts are contained in the other side?
            if (leftName.length() > rightName.length()) {
                return false;
            }
            return true;
        }
    }
}
