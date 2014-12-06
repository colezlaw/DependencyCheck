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
package org.owasp.dependencycheck.data.update;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.data.nvdcve.DatabaseProperties;
import org.owasp.dependencycheck.data.update.exception.UpdateException;
import org.owasp.dependencycheck.utils.DateUtil;
import org.owasp.dependencycheck.utils.DependencyVersion;
import org.owasp.dependencycheck.utils.Settings;
import org.owasp.dependencycheck.utils.URLConnectionFactory;
import org.owasp.dependencycheck.utils.URLConnectionFailureException;

/**
 *
 * @author Jeremy Long <jeremy.long@owasp.org>
 */
public class EngineVersionCheck implements CachedWebDataSource {

    /**
     * Static logger.
     */
    private static final Logger LOGGER = Logger.getLogger(EngineVersionCheck.class.getName());
    /**
     * The property key indicating when the last version check occurred.
     */
    public static final String ENGINE_VERSION_CHECKED_ON = "VersionCheckOn";
    /**
     * The property key indicating when the last version check occurred.
     */
    public static final String CURRENT_ENGINE_RELEASE = "CurrentEngineRelease";
    /**
     * Reference to the Cve Database.
     */
    private CveDB cveDB = null;

    @Override
    public void update() throws UpdateException {
        try {
            openDatabase();
            final DatabaseProperties properties = cveDB.getDatabaseProperties();
            final long lastChecked = Long.parseLong(properties.getProperty(ENGINE_VERSION_CHECKED_ON, "0"));
            final long now = (new Date()).getTime();
            String updateToVersion = properties.getProperty(CURRENT_ENGINE_RELEASE, "");
            String currentVersion = Settings.getString(Settings.KEYS.APPLICATION_VERSION, "0.0.0");
            //check every 30 days if we know there is an update, otherwise check every 7 days
            int checkRange = 30;
            if (updateToVersion.isEmpty()) {
                checkRange = 7;
            }
            if (!DateUtil.withinDateRange(lastChecked, now, checkRange)) {
                final String currentRelease = getCurrentReleaseVersion();
                if (currentRelease != null) {
                    DependencyVersion v = new DependencyVersion(currentRelease);
                    if (v.getVersionParts() != null && v.getVersionParts().size() >= 3) {
                        if (!currentRelease.equals(updateToVersion)) {
                            properties.save(CURRENT_ENGINE_RELEASE, v.toString());
                        }
                        properties.save(ENGINE_VERSION_CHECKED_ON, Long.toString(now));
                        updateToVersion = v.toString();
                    }
                }
            }
            DependencyVersion running = new DependencyVersion(currentVersion);
            DependencyVersion released = new DependencyVersion(updateToVersion);
            if (running.compareTo(released) < 0) {
                final String msg = String.format("A new version of dependency-check is available. Consider updating to version %s.",
                        released.toString());
                LOGGER.warning(msg);
            }
        } catch (DatabaseException ex) {
            LOGGER.log(Level.FINE, "Database Exception opening databases to retrieve properties", ex);
            throw new UpdateException("Error occured updating database properties.");
        } finally {
            closeDatabase();
        }
    }

    /**
     * Opens the CVE and CPE data stores.
     *
     * @throws UpdateException thrown if a data store cannot be opened
     */
    protected final void openDatabase() throws DatabaseException {
        if (cveDB != null) {
            return;
        }
        cveDB = new CveDB();
        cveDB.open();
    }

    /**
     * Closes the CVE and CPE data stores.
     */
    protected void closeDatabase() {
        if (cveDB != null) {
            try {
                cveDB.close();
            } catch (Throwable ignore) {
                LOGGER.log(Level.FINEST, "Error closing the cveDB", ignore);
            }
        }
    }

    protected String getCurrentReleaseVersion() {
        HttpURLConnection conn = null;
        try {
            final String str = Settings.getString(Settings.KEYS.ENGINE_VERSION_CHECK_URL, "http://jeremylong.github.io/DependencyCheck/current.txt");
            final URL url = new URL(str);
            conn = URLConnectionFactory.createHttpURLConnection(url);
            conn.connect();
            if (conn.getResponseCode() != 200) {
                return null;
            }
            String releaseVersion = IOUtils.toString(conn.getInputStream(), "UTF-8");
            if (releaseVersion != null) {
                return releaseVersion.trim();
            }
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.FINE, "unable to retrieve current release version of dependency-check", ex);
        } catch (URLConnectionFailureException ex) {
            LOGGER.log(Level.FINE, "unable to retrieve current release version of dependency-check", ex);
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "unable to retrieve current release version of dependency-check", ex);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
}
