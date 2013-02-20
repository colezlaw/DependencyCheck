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
package org.codesecure.dependencycheck.data.nvdcve;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.codesecure.dependencycheck.Engine;
import org.codesecure.dependencycheck.analyzer.AnalysisException;
import org.codesecure.dependencycheck.analyzer.AnalysisPhase;
import org.codesecure.dependencycheck.dependency.Dependency;
import org.codesecure.dependencycheck.dependency.Vulnerability;
import org.codesecure.dependencycheck.dependency.Identifier;

/**
 * NvdCveAnalyzer is a utility class that takes a project dependency and
 * attempts to discern if there is an associated CVEs. It uses the the
 * identifiers found by other analyzers to lookup the CVE data.
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class NvdCveAnalyzer implements org.codesecure.dependencycheck.analyzer.Analyzer {

    /**
     * The maximum number of query results to return.
     */
    static final int MAX_QUERY_RESULTS = 100;
    /**
     * The CVE Index.
     */
    protected CveDB cveDB = null;

    /**
     * Opens the data source.
     *
     * @throws SQLException thrown when there is a SQL Exception
     * @throws IOException thrown when there is an IO Exception
     * @throws DatabaseException thrown when there is a database exceptions
     */
    public void open() throws SQLException, IOException, DatabaseException {
        cveDB = new CveDB();
        cveDB.open();
    }

    /**
     * Closes the data source.
     */
    public void close() {
        cveDB.close();
        cveDB = null;
    }

    /**
     * Returns the status of the data source - is the database open.
     *
     * @return true or false.
     */
    public boolean isOpen() {
        return (cveDB != null);
    }

    /**
     * Ensures that the CVE Database is closed.
     *
     * @throws Throwable when a throwable is thrown.
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (isOpen()) {
            close();
        }
    }

    /**
     * Analyzes a dependency and attempts to determine if there are any CPE
     * identifiers for this dependency.
     *
     * @param dependency The Dependency to analyze
     * @param engine The analysis engine
     * @throws AnalysisException is thrown if there is an issue analyzing the
     * dependency
     */
    public void analyze(Dependency dependency, Engine engine) throws AnalysisException {
        for (Identifier id : dependency.getIdentifiers()) {
            if ("cpe".equals(id.getType())) {
                try {
                    String value = id.getValue();
                    List<Vulnerability> vulns = cveDB.getVulnerabilities(value);
                    for (Vulnerability v : vulns) {
                        dependency.addVulnerability(v);
                    }
                } catch (DatabaseException ex) {
                    throw new AnalysisException(ex);
                }
            }
        }
    }

    /**
     * Returns true because this analyzer supports all dependency types.
     *
     * @return true.
     */
    public Set<String> getSupportedExtensions() {
        return null;
    }

    /**
     * Returns the name of this analyzer.
     *
     * @return the name of this analyzer.
     */
    public String getName() {
        return "NVD CVE Analyzer";
    }

    /**
     * Returns true because this analyzer supports all dependency types.
     *
     * @param extension the file extension of the dependency being analyzed.
     * @return true.
     */
    public boolean supportsExtension(String extension) {
        return true;
    }

    /**
     * Returns the analysis phase that this analyzer should run in.
     *
     * @return the analysis phase that this analyzer should run in.
     */
    public AnalysisPhase getAnalysisPhase() {
        return AnalysisPhase.FINDING_ANALYSIS;
    }

    /**
     * Opens the NVD CVE Lucene Index.
     *
     * @throws Exception is thrown if there is an issue opening the index.
     */
    public void initialize() throws Exception {
        this.open();
    }
}
