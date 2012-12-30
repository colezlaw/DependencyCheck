package org.codesecure.dependencycheck.dependency;
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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codesecure.dependencycheck.data.cpe.Entry;

/**
 * A record containing information about vulnerable software. This
 * is referenced from a vulnerability.
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class VulnerableSoftware extends Entry implements Serializable {

    private static final long serialVersionUID = 307319490326651052L;

    /**
     * Parse a CPE entry from the cpe string repesentation
     *
     * @param cpe a cpe entry (e.g. cpe:/a:vendor:software:version)
     */
    public void setCpe(String cpe) {
        try {
            parseName(cpe);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(VulnerableSoftware.class.getName()).log(Level.SEVERE, null, ex);
            setName(cpe);
        }
    }

    /**
     * If present, indicates that previous version are vulnerable
     */
    protected String previousVersion = null;

    /**
     * Indicates if previous versions of this software are vulnerable
     *
     * @return if previous versions of this software are vulnerable
     */
    public boolean hasPreviousVersion() {
        return previousVersion == null;
    }

    /**
     * Get the value of previousVersion
     *
     * @return the value of previousVersion
     */
    public String getPreviousVersion() {
        return previousVersion;
    }

    /**
     * Set the value of previousVersion
     *
     * @param previousVersion new value of previousVersion
     */
    public void setPreviousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
    }

        @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final VulnerableSoftware other = (VulnerableSoftware) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 83 * hash + (this.previousVersion != null ? this.previousVersion.hashCode() : 0);
        return hash;
    }
}
