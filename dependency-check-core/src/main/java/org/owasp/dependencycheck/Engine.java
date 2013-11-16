/*
 * This file is part of dependency-check-core.
 *
 * Dependency-check-core is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Dependency-check-core is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * dependency-check-core. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2012 Jeremy Long. All Rights Reserved.
 */
package org.owasp.dependencycheck;

import java.util.EnumMap;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.owasp.dependencycheck.analyzer.AnalysisException;
import org.owasp.dependencycheck.analyzer.AnalysisPhase;
import org.owasp.dependencycheck.analyzer.Analyzer;
import org.owasp.dependencycheck.analyzer.AnalyzerService;
import org.owasp.dependencycheck.analyzer.CPEAnalyzer;
import org.owasp.dependencycheck.data.CachedWebDataSource;
import org.owasp.dependencycheck.data.NoDataException;
import org.owasp.dependencycheck.data.UpdateException;
import org.owasp.dependencycheck.data.UpdateService;
import org.owasp.dependencycheck.data.cpe.CpeIndexReader;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.utils.FileUtils;
import org.owasp.dependencycheck.utils.InvalidSettingException;
import org.owasp.dependencycheck.utils.Settings;

/**
 * Scans files, directories, etc. for Dependencies. Analyzers are loaded and
 * used to process the files found by the scan, if a file is encountered and an
 * Analyzer is associated with the file type then the file is turned into a
 * dependency.
 *
 * @author Jeremy Long (jeremy.long@owasp.org)
 */
public class Engine {

    /**
     * The list of dependencies.
     */
    private final List<Dependency> dependencies = new ArrayList<Dependency>();
    /**
     * A Map of analyzers grouped by Analysis phase.
     */
    private final EnumMap<AnalysisPhase, List<Analyzer>> analyzers =
            new EnumMap<AnalysisPhase, List<Analyzer>>(AnalysisPhase.class);
    /**
     * A set of extensions supported by the analyzers.
     */
    private final Set<String> extensions = new HashSet<String>();

    /**
     * Creates a new Engine.
     */
    public Engine() {
        boolean autoUpdate = true;
        try {
            autoUpdate = Settings.getBoolean(Settings.KEYS.AUTO_UPDATE);
        } catch (InvalidSettingException ex) {
            Logger.getLogger(Engine.class.getName()).log(Level.FINE, "Invalid setting for auto-update; using true.");
        }
        if (autoUpdate) {
            doUpdates();
        }
        loadAnalyzers();
    }

    /**
     * Creates a new Engine.
     *
     * @param autoUpdate indicates whether or not data should be updated from
     * the Internet
     * @deprecated This function should no longer be used; the autoupdate flag
     * should be set using:
     * <code>Settings.setBoolean(Settings.KEYS.AUTO_UPDATE, value);</code>
     */
    @Deprecated
    public Engine(boolean autoUpdate) {
        if (autoUpdate) {
            doUpdates();
        }
        loadAnalyzers();
    }

    /**
     * Loads the analyzers specified in the configuration file (or system
     * properties).
     */
    private void loadAnalyzers() {

        for (AnalysisPhase phase : AnalysisPhase.values()) {
            analyzers.put(phase, new ArrayList<Analyzer>());
        }

        final AnalyzerService service = AnalyzerService.getInstance();
        final Iterator<Analyzer> iterator = service.getAnalyzers();
        while (iterator.hasNext()) {
            final Analyzer a = iterator.next();
            analyzers.get(a.getAnalysisPhase()).add(a);
            if (a.getSupportedExtensions() != null) {
                extensions.addAll(a.getSupportedExtensions());
            }
        }
    }

    /**
     * Get the List of the analyzers for a specific phase of analysis.
     *
     * @param phase the phase to get the configured analyzers.
     * @return the analyzers loaded
     */
    public List<Analyzer> getAnalyzers(AnalysisPhase phase) {
        return analyzers.get(phase);
    }

    /**
     * Get the dependencies identified.
     *
     * @return the dependencies identified
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Scans an array of files or directories. If a directory is specified, it
     * will be scanned recursively. Any dependencies identified are added to the
     * dependency collection.
     *
     * @since v0.3.2.5
     *
     * @param paths an array of paths to files or directories to be analyzed.
     */
    public void scan(String[] paths) {
        for (String path : paths) {
            final File file = new File(path);
            scan(file);
        }
    }

    /**
     * Scans a given file or directory. If a directory is specified, it will be
     * scanned recursively. Any dependencies identified are added to the
     * dependency collection.
     *
     * @param path the path to a file or directory to be analyzed.
     */
    public void scan(String path) {
        final File file = new File(path);
        scan(file);
    }

    /**
     * Scans an array of files or directories. If a directory is specified, it
     * will be scanned recursively. Any dependencies identified are added to the
     * dependency collection.
     *
     * @since v0.3.2.5
     *
     * @param files an array of paths to files or directories to be analyzed.
     */
    public void scan(File[] files) {
        for (File file : files) {
            scan(file);
        }
    }

    /**
     * Scans a list of files or directories. If a directory is specified, it
     * will be scanned recursively. Any dependencies identified are added to the
     * dependency collection.
     *
     * @since v0.3.2.5
     *
     * @param files a set of paths to files or directories to be analyzed.
     */
    public void scan(Set<File> files) {
        for (File file : files) {
            scan(file);
        }
    }

    /**
     * Scans a list of files or directories. If a directory is specified, it
     * will be scanned recursively. Any dependencies identified are added to the
     * dependency collection.
     *
     * @since v0.3.2.5
     *
     * @param files a set of paths to files or directories to be analyzed.
     */
    public void scan(List<File> files) {
        for (File file : files) {
            scan(file);
        }
    }

    /**
     * Scans a given file or directory. If a directory is specified, it will be
     * scanned recursively. Any dependencies identified are added to the
     * dependency collection.
     *
     * @since v0.3.2.4
     *
     * @param file the path to a file or directory to be analyzed.
     */
    public void scan(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else {
                scanFile(file);
            }
        }
    }

    /**
     * Recursively scans files and directories. Any dependencies identified are
     * added to the dependency collection.
     *
     * @param dir the directory to scan.
     */
    protected void scanDirectory(File dir) {
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    scanDirectory(f);
                } else {
                    scanFile(f);
                }
            }
        }
    }

    /**
     * Scans a specified file. If a dependency is identified it is added to the
     * dependency collection.
     *
     * @param file The file to scan.
     */
    protected void scanFile(File file) {
        if (!file.isFile()) {
            final String msg = String.format("Path passed to scanFile(File) is not a file: %s. Skipping the file.", file.toString());
            Logger.getLogger(Engine.class.getName()).log(Level.FINE, msg);
            return;
        }
        final String fileName = file.getName();
        final String extension = FileUtils.getFileExtension(fileName);
        if (extension != null) {
            if (extensions.contains(extension)) {
                final Dependency dependency = new Dependency(file);
                dependencies.add(dependency);
            }
        } else {
            final String msg = String.format("No file extension found on file '%s'. The file was not analyzed.",
                    file.toString());
            Logger.getLogger(Engine.class.getName()).log(Level.FINEST, msg);
        }
    }

    /**
     * Runs the analyzers against all of the dependencies.
     */
    public void analyzeDependencies() {
        //need to ensure that data exists
        try {
            ensureDataExists();
        } catch (NoDataException ex) {
            String msg = String.format("%n%n%s%n%nUnable to continue dependency-check analysis.", ex.getMessage());
            Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, msg);
            Logger.getLogger(Engine.class.getName()).log(Level.FINE, null, ex);
            return;
        }

        //phase one initialize
        for (AnalysisPhase phase : AnalysisPhase.values()) {
            final List<Analyzer> analyzerList = analyzers.get(phase);
            for (Analyzer a : analyzerList) {
                try {
                    final String msg = String.format("Initializing %s", a.getName());
                    Logger.getLogger(Engine.class.getName()).log(Level.FINE, msg);
                    a.initialize();
                } catch (Exception ex) {
                    final String msg = String.format("Exception occurred initializing %s.", a.getName());
                    Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, msg);
                    Logger.getLogger(Engine.class.getName()).log(Level.INFO, null, ex);
                    try {
                        a.close();
                    } catch (Exception ex1) {
                        Logger.getLogger(Engine.class.getName()).log(Level.FINEST, null, ex1);
                    }
                }
            }
        }

        // analysis phases
        for (AnalysisPhase phase : AnalysisPhase.values()) {
            final List<Analyzer> analyzerList = analyzers.get(phase);

            for (Analyzer a : analyzerList) {
                /* need to create a copy of the collection because some of the
                 * analyzers may modify it. This prevents ConcurrentModificationExceptions.
                 * This is okay for adds/deletes because it happens per analyzer.
                 */
                final String msg = String.format("Begin Analyzer '%s'", a.getName());
                Logger.getLogger(Engine.class.getName()).log(Level.FINE, msg);
                final Set<Dependency> dependencySet = new HashSet<Dependency>();
                dependencySet.addAll(dependencies);
                for (Dependency d : dependencySet) {
                    final String msgFile = String.format("Begin Analysis of '%s'", d.getActualFilePath());
                    Logger.getLogger(Engine.class.getName()).log(Level.FINE, msgFile);
                    if (a.supportsExtension(d.getFileExtension())) {
                        try {
                            a.analyze(d, this);
                        } catch (AnalysisException ex) {
                            d.addAnalysisException(ex);
                        }
                    }
                }
            }
        }

        //close/cleanup
        for (AnalysisPhase phase : AnalysisPhase.values()) {
            final List<Analyzer> analyzerList = analyzers.get(phase);
            for (Analyzer a : analyzerList) {
                final String msg = String.format("Closing Analyzer '%s'", a.getName());
                Logger.getLogger(Engine.class.getName()).log(Level.FINE, msg);
                try {
                    a.close();
                } catch (Exception ex) {
                    Logger.getLogger(Engine.class.getName()).log(Level.FINEST, null, ex);
                }
            }
        }
    }

    /**
     * Cycles through the cached web data sources and calls update on all of
     * them.
     */
    private void doUpdates() {
        final UpdateService service = UpdateService.getInstance();
        final Iterator<CachedWebDataSource> iterator = service.getDataSources();
        while (iterator.hasNext()) {
            final CachedWebDataSource source = iterator.next();
            try {
                source.update();
            } catch (UpdateException ex) {
                Logger.getLogger(Engine.class.getName()).log(Level.WARNING,
                        "Unable to update Cached Web DataSource, using local data instead. Results may not include recent vulnerabilities.");
                Logger.getLogger(Engine.class.getName()).log(Level.FINE,
                        String.format("Unable to update details for %s", source.getClass().getName()), ex);
            }
        }
    }

    /**
     * Returns a full list of all of the analyzers. This is useful for reporting
     * which analyzers where used.
     *
     * @return a list of Analyzers
     */
    public List<Analyzer> getAnalyzers() {
        final List<Analyzer> ret = new ArrayList<Analyzer>();
        for (AnalysisPhase phase : AnalysisPhase.values()) {
            final List<Analyzer> analyzerList = analyzers.get(phase);
            ret.addAll(analyzerList);
        }
        return ret;
    }

    /**
     * Checks all analyzers to see if an extension is supported.
     *
     * @param ext a file extension
     * @return true or false depending on whether or not the file extension is
     * supported
     */
    public boolean supportsExtension(String ext) {
        if (ext == null) {
            return false;
        }
        for (AnalysisPhase phase : AnalysisPhase.values()) {
            final List<Analyzer> analyzerList = analyzers.get(phase);
            for (Analyzer a : analyzerList) {
                if (a.getSupportedExtensions() != null && a.supportsExtension(ext)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks the CPE Index to ensure documents exists. If none exist a
     * NoDataException is thrown.
     *
     * @throws NoDataException thrown if no data exists in the CPE Index
     */
    private void ensureDataExists() throws NoDataException {
        CpeIndexReader cpe = null;
        boolean noDataExists = false;
        try {
            cpe = new CpeIndexReader();
            cpe.open();
            if (cpe.numDocs() <= 0) {
                noDataExists = true;
            }
        } catch (IOException ex) {
            noDataExists = true;
        } catch (NullPointerException ex) {
            noDataExists = true;
        } finally {
            if (cpe != null) {
                cpe.close();
            }
        }
        if (noDataExists) {
            throw new NoDataException("No data exists in the data store. Please check that you are able to connect "
                    + "to the Internet and re-run dependency-check. If the problem persists determine whether you need "
                    + "to set a proxy url and port.\\n\\nIf you are unable to solve this problem please contact the mailing "
                    + "list for help: dependency-check@googlegroups.com");

        }
    }
}
