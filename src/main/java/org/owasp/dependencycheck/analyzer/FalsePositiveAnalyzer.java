/*
 * This file is part of DependencyCheck.
 *
 * DependencyCheck is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * DependencyCheck is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * DependencyCheck. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck.analyzer;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.cpe.Entry;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.dependency.Identifier;
import org.owasp.dependencycheck.utils.InvalidSettingException;
import org.owasp.dependencycheck.utils.Settings;

/**
 * This analyzer attempts to remove some well known false positives -
 * specifically regarding the java runtime.
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class FalsePositiveAnalyzer extends AbstractAnalyzer {

    /**
     * The set of file extensions supported by this analyzer.
     */
    private static final Set<String> EXTENSIONS = null;
    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "False Positive Analyzer";
    /**
     * The phase that this analyzer is intended to run in.
     */
    private static final AnalysisPhase ANALYSIS_PHASE = AnalysisPhase.POST_IDENTIFIER_ANALYSIS;

    /**
     * Returns a list of file EXTENSIONS supported by this analyzer.
     *
     * @return a list of file EXTENSIONS supported by this analyzer.
     */
    public Set<String> getSupportedExtensions() {
        return EXTENSIONS;
    }

    /**
     * Returns the name of the analyzer.
     *
     * @return the name of the analyzer.
     */
    public String getName() {
        return ANALYZER_NAME;
    }

    /**
     * Returns whether or not this analyzer can process the given extension.
     *
     * @param extension the file extension to test for support
     * @return whether or not the specified file extension is supported by this
     * analyzer.
     */
    public boolean supportsExtension(String extension) {
        return true; //EXTENSIONS.contains(extension);
    }

    /**
     * Returns the phase that the analyzer is intended to run in.
     *
     * @return the phase that the analyzer is intended to run in.
     */
    public AnalysisPhase getAnalysisPhase() {
        return ANALYSIS_PHASE;
    }

    /**
     *
     *
     * @param dependency the dependency to analyze.
     * @param engine the engine that is scanning the dependencies
     * @throws AnalysisException is thrown if there is an error reading the JAR
     * file.
     */
    public void analyze(Dependency dependency, Engine engine) throws AnalysisException {
        removeJreEntries(dependency);
        boolean deepScan = false;
        try {
            deepScan = Settings.getBoolean(Settings.KEYS.PERFORM_DEEP_SCAN);
        } catch (InvalidSettingException ex) {
            Logger.getLogger(FalsePositiveAnalyzer.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!deepScan) {
            removeSpuriousCPE(dependency);
        }
    }

    /**
     * Intended to remove spurious CPE entries.
     *
     * @param dependency the dependency being analyzed
     */
    private void removeSpuriousCPE(Dependency dependency) {
        List<Identifier> ids = new ArrayList<Identifier>();
        ids.addAll(dependency.getIdentifiers());
        ListIterator<Identifier> mainItr = ids.listIterator();
        while (mainItr.hasNext()) {
            Identifier currentId = mainItr.next();
            Entry currentCpe = parseCpe(currentId.getType(), currentId.getValue());
            if (currentCpe == null) {
                continue;
            }
            ListIterator<Identifier> subItr = ids.listIterator(mainItr.nextIndex());
            while (subItr.hasNext()) {
                Identifier nextId = subItr.next();
                Entry nextCpe = parseCpe(nextId.getType(), nextId.getValue());
                if (nextCpe == null) {
                    continue;
                }
                if (currentCpe.getVendor().equals(nextCpe.getVendor())) {
                    if (currentCpe.getProduct().equals(nextCpe.getProduct())) {
                        // see if one is contained in the other.. remove the contained one from dependency.getIdentifier
                        String mainVersion = currentCpe.getVersion();
                        String nextVersion = nextCpe.getVersion();
                        if (mainVersion.length() < nextVersion.length()) {
                            if (nextVersion.startsWith(mainVersion)) {
                                //remove mainVersion
                                dependency.getIdentifiers().remove(currentId);
                            }
                        } else {
                            if (mainVersion.startsWith(nextVersion)) {
                                //remove nextVersion
                                dependency.getIdentifiers().remove(nextId);
                            }
                        }
                    } else {
                        if (currentCpe.getVersion().equals(nextCpe.getVersion())) {
                            //same vendor and version - but different products
                            // are we dealing with something like Axis & Axis2
                            String currentProd = currentCpe.getProduct();
                            String nextProd = nextCpe.getProduct();
                            if (currentProd.startsWith(nextProd)) {
                                dependency.getIdentifiers().remove(nextId);
                            }
                            if (nextProd.startsWith(currentProd)) {
                                dependency.getIdentifiers().remove(currentId);
                            }

                        }
                    }
                }
            }
        }

         /*
         * NOTE - don't remove the two different vendors.
         *
            currentCpe: currentCpe:/a:mortbay:jetty:4.2.27
            currentCpe: currentCpe:/a:mortbay_jetty:jetty:4.2
            currentCpe: currentCpe:/a:mortbay:jetty:4.2
         *
            Source   Name	            Value
            file     name                   org.mortbay.jetty
            Manifest Implementation-Vendor  Mort Bay Consulting, Pty. Ltd.
            Manifest Implementation-Version 4.2.27
         */

    }

    /**
     * Removes any CPE entries for the JDK/JRE unless the filename ends with
     * rt.jar
     *
     * @param dependency the dependency to remove JRE CPEs from
     */
    private void removeJreEntries(Dependency dependency) {
        final List<Identifier> identifiers = dependency.getIdentifiers();
        final Iterator<Identifier> itr = identifiers.iterator();
        while (itr.hasNext()) {
            final Identifier i = itr.next();
            if ((i.getValue().startsWith("cpe:/a:sun:java:")
                    || i.getValue().startsWith("cpe:/a:oracle:jre")
                    || i.getValue().startsWith("cpe:/a:oracle:jdk"))
                    && !dependency.getFileName().toLowerCase().endsWith("rt.jar")) {
                itr.remove();
            }
        }
    }

    private Entry parseCpe(String type, String value) {
        if (!"cpe".equals(type)) {
            return null;
        }
        Entry cpe = new Entry();
        try {
            cpe.parseName(value);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(FalsePositiveAnalyzer.class.getName()).log(Level.FINEST, null, ex);
            return null;
        }
        return cpe;
    }
}
