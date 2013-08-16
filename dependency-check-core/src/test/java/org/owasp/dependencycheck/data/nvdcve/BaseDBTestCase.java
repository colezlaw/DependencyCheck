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
package org.owasp.dependencycheck.data.nvdcve;

import org.owasp.dependencycheck.data.cpe.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import junit.framework.TestCase;
import org.owasp.dependencycheck.utils.Settings;

/**
 *
 * @author Jeremy Long (jeremy.long@owasp.org)
 */
public abstract class BaseDBTestCase extends TestCase {

    protected final static int BUFFER_SIZE = 2048;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ensureDBExists();
    }

    protected static File getDataDirectory(Class clazz) throws IOException {
        final File dataDirectory = Settings.getFile(Settings.KEYS.CVE_DATA_DIRECTORY, clazz);
        return dataDirectory;
    }

    public static void ensureDBExists() throws Exception {
        ensureDBExists(BaseDBTestCase.class);
    }

    public static void ensureDBExists(Class clazz) throws Exception {
        String indexPath = getDataDirectory(clazz).getAbsolutePath();
        java.io.File f = new File(indexPath);
        if (!f.exists() || (f.isDirectory() && f.listFiles().length == 0)) {
            f.mkdirs();
            FileInputStream fis = null;
            ZipInputStream zin = null;
            try {
                File path = new File(clazz.getClassLoader().getResource("db.cve.zip").getPath());
                fis = new FileInputStream(path);
                zin = new ZipInputStream(new BufferedInputStream(fis));
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    FileOutputStream fos = null;
                    BufferedOutputStream dest = null;
                    try {
                        File o = new File(indexPath, entry.getName());
                        o.createNewFile();
                        fos = new FileOutputStream(o, false);
                        dest = new BufferedOutputStream(fos, BUFFER_SIZE);
                        byte data[] = new byte[BUFFER_SIZE];
                        int count;
                        while ((count = zin.read(data, 0, BUFFER_SIZE)) != -1) {
                            dest.write(data, 0, count);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(BaseDBTestCase.class.getName()).log(Level.FINEST, null, ex);
                    } finally {
                        try {
                            if (dest != null) {
                                dest.flush();
                                dest.close();
                            }
                        } catch (Throwable ex) {
                            Logger.getLogger(BaseDBTestCase.class.getName()).log(Level.FINEST, null, ex);
                        }
                        try {
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (Throwable ex) {
                            Logger.getLogger(BaseDBTestCase.class.getName()).log(Level.FINEST, null, ex);
                        }
                    }
                }
            } finally {
                try {
                    if (zin != null) {
                        zin.close();
                    }
                } catch (Throwable ex) {
                    Logger.getLogger(BaseDBTestCase.class.getName()).log(Level.FINEST, null, ex);
                }
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Throwable ex) {
                    Logger.getLogger(BaseDBTestCase.class.getName()).log(Level.FINEST, null, ex);
                }
            }
        }
    }
}
