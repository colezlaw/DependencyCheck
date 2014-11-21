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
package org.owasp.dependencycheck.analyzer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.data.central.CentralSearch;
import org.owasp.dependencycheck.data.nexus.MavenArtifact;
import org.owasp.dependencycheck.dependency.Confidence;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.utils.InvalidSettingException;
import org.owasp.dependencycheck.utils.Settings;

/**
 * Analyzer which will attempt to locate a dependency, and the GAV information, by querying Central for the dependency's
 * SHA-1 digest.
 *
 * @author colezlaw
 */
public class CentralAnalyzer extends AbstractFileTypeAnalyzer {

    /**
     * The logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CentralAnalyzer.class.getName());

    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "Central Analyzer";

    /**
     * The phase in which this analyzer runs.
     */
    private static final AnalysisPhase ANALYSIS_PHASE = AnalysisPhase.INFORMATION_COLLECTION;

    /**
     * The types of files on which this will work.
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = newHashSet("jar");

    /**
     * The analyzer should be disabled if there are errors, so this is a flag to determine if such an error has
     * occurred.
     */
    private boolean errorFlag = false;

    /**
     * The searcher itself.
     */
    private CentralSearch searcher;

    /**
     * Field indicating if the analyzer is enabled.
     */
    private final boolean enabled = checkEnabled();

    /**
     * Determine whether to enable this analyzer or not.
     *
     * @return whether the analyzer should be enabled
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Determines if this analyzer is enabled.
     *
     * @return <code>true</code> if the analyzer is enabled; otherwise <code>false</code>
     */
    private boolean checkEnabled() {
        boolean retval = false;

        try {
            if (Settings.getBoolean(Settings.KEYS.ANALYZER_CENTRAL_ENABLED)) {
                if (!Settings.getBoolean(Settings.KEYS.ANALYZER_NEXUS_ENABLED)
                        || NexusAnalyzer.DEFAULT_URL.equals(Settings.getString(Settings.KEYS.ANALYZER_NEXUS_URL))) {
                    LOGGER.fine("Enabling the Central analyzer");
                    retval = true;
                } else {
                    LOGGER.info("Nexus analyzer is enabled, disabling the Central Analyzer");
                }
            } else {
                LOGGER.info("Central analyzer disabled");
            }
        } catch (InvalidSettingException ise) {
            LOGGER.warning("Invalid setting. Disabling the Central analyzer");
        }
        return retval;
    }

    /**
     * Initializes the analyzer once before any analysis is performed.
     *
     * @throws Exception if there's an error during initialization
     */
    @Override
    public void initializeFileTypeAnalyzer() throws Exception {
        LOGGER.fine("Initializing Central analyzer");
        LOGGER.fine(String.format("Central analyzer enabled: %s", isEnabled()));
        if (isEnabled()) {
            final String searchUrl = Settings.getString(Settings.KEYS.ANALYZER_CENTRAL_URL);
            LOGGER.fine(String.format("Central Analyzer URL: %s", searchUrl));
            searcher = new CentralSearch(new URL(searchUrl));
        }
    }

    /**
     * Returns the analyzer's name.
     *
     * @return the name of the analyzer
     */
    @Override
    public String getName() {
        return ANALYZER_NAME;
    }

    /**
     * Returns the key used in the properties file to to reference the analyzer's enabled property.
     *
     * @return the analyzer's enabled property setting key.
     */
    @Override
    protected String getAnalyzerEnabledSettingKey() {
        return Settings.KEYS.ANALYZER_CENTRAL_ENABLED;
    }

    /**
     * Returns the analysis phase under which the analyzer runs.
     *
     * @return the phase under which the analyzer runs
     */
    @Override
    public AnalysisPhase getAnalysisPhase() {
        return ANALYSIS_PHASE;
    }

    /**
     * Returns the extensions for which this Analyzer runs.
     *
     * @return the extensions for which this Analyzer runs
     */
    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    /**
     * Performs the analysis.
     *
     * @param dependency the dependency to analyze
     * @param engine the engine
     * @throws AnalysisException when there's an exception during analysis
     */
    @Override
    public void analyzeFileType(Dependency dependency, Engine engine) throws AnalysisException {
        if (errorFlag || !isEnabled()) {
            return;
        }

        try {
            final List<MavenArtifact> mas = searcher.searchSha1(dependency.getSha1sum());
            final Confidence confidence = mas.size() > 1 ? Confidence.HIGH : Confidence.HIGHEST;
            for (MavenArtifact ma : mas) {
                LOGGER.fine(String.format("Central analyzer found artifact (%s) for dependency (%s)", ma.toString(), dependency.getFileName()));
                dependency.addAsEvidence("central", ma, confidence);
            }
        } catch (IllegalArgumentException iae) {
            LOGGER.info(String.format("invalid sha1-hash on %s", dependency.getFileName()));
        } catch (FileNotFoundException fnfe) {
            LOGGER.fine(String.format("Artifact not found in repository: '%s", dependency.getFileName()));
        } catch (IOException ioe) {
            LOGGER.log(Level.FINE, "Could not connect to Central search", ioe);
            errorFlag = true;
        }
    }
}
