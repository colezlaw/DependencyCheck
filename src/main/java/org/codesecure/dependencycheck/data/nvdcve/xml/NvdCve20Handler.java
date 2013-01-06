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
package org.codesecure.dependencycheck.data.nvdcve.xml;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.CorruptIndexException;
import org.codesecure.dependencycheck.data.cpe.Index;
import org.codesecure.dependencycheck.data.nvdcve.CveDB;
import org.codesecure.dependencycheck.data.nvdcve.DatabaseException;
import org.codesecure.dependencycheck.dependency.Reference;
import org.codesecure.dependencycheck.dependency.Vulnerability;
import org.codesecure.dependencycheck.dependency.VulnerableSoftware;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX Handler that will parse the NVD CVE XML (schema version 2.0).
 *
 * @author Jeremy Long (jeremy.long@gmail.com)
 */
public class NvdCve20Handler extends DefaultHandler {

    private static final String CURRENT_SCHEMA_VERSION = "2.0";
    private Element current = new Element();
    StringBuilder nodeText = null;
    Vulnerability vulnerability = null;
    Reference reference = null;
    boolean hasApplicationCpe = false;

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        current.setNode(qName);
        if (current.isEntryNode()) {
            hasApplicationCpe = false;
            vulnerability = new Vulnerability();
            vulnerability.setName(attributes.getValue("id"));
        } else if (current.isVulnProductNode()) {
            nodeText = new StringBuilder(100);
        } else if (current.isVulnReferencesNode()) {
            String lang = attributes.getValue("xml:lang");
            if ("en".equals(lang)) {
                reference = new Reference();
            } else {
                reference = null;
            }
        } else if (reference != null && current.isVulnReferenceNode()) {
            reference.setUrl(attributes.getValue("href"));
            nodeText = new StringBuilder(130);
        } else if (reference != null && current.isVulnSourceNode()) {
            nodeText = new StringBuilder(30);
        } else if (current.isVulnSummaryNode()) {
            nodeText = new StringBuilder(500);
        } else if (current.isNVDNode()) {
            String nvdVer = attributes.getValue("nvd_xml_version");
            if (!CURRENT_SCHEMA_VERSION.equals(nvdVer)) {
                throw new SAXNotSupportedException("Schema version " + nvdVer + " is not supported");
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (nodeText != null) {
            nodeText.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        current.setNode(qName);
        if (current.isEntryNode()) {
            if (hasApplicationCpe) {
                try {
                    saveEntry(vulnerability);
                } catch (DatabaseException ex) {
                    throw new SAXException(ex);
                } catch (CorruptIndexException ex) {
                    throw new SAXException(ex);
                } catch (IOException ex) {
                    throw new SAXException(ex);
                }
            }
            vulnerability = null;
        } else if (current.isVulnProductNode()) {
            String cpe = nodeText.toString();
            if (cpe.startsWith("cpe:/a:")) {
                hasApplicationCpe = true;
                vulnerability.addVulnerableSoftware(cpe);
            }
            nodeText = null;
        } else if (reference != null && current.isVulnReferencesNode()) {
            vulnerability.addReference(reference);
            reference = null;
        } else if (reference != null && current.isVulnReferenceNode()) {
            reference.setName(nodeText.toString());
            nodeText = null;
        } else if (reference != null && current.isVulnSourceNode()) {
            reference.setSource(nodeText.toString());
            nodeText = null;
        } else if (current.isVulnSummaryNode()) {
            vulnerability.setDescription(nodeText.toString());
            nodeText = null;
        }
    }
    private CveDB cveDB = null;

    /**
     * Sets the cveDB
     * @param db a reference to the CveDB
     */
    public void setCveDB(CveDB db) {
        cveDB = db;
    }
    /**
     * A list of CVE entries and associated VulnerableSoftware entries that contain
     * previous entries.
     */
    private Map<String, List<VulnerableSoftware>> prevVersionVulnMap = null;

    /**
     * Sets the prevVersionVulnMap.
     * @param map the map of vulnerable software with previous versions being vulnerable
     */
    public void setPrevVersionVulnMap(Map<String, List<VulnerableSoftware>> map) {
        prevVersionVulnMap = map;
    }

    /**
     * Saves a vulnerability to the CVE Database. This is a callback method
     * called by the Sax Parser Handler {@link org.codesecure.dependencycheck.data.nvdcve.xml.NvdCve20Handler}.
     *
     * @param vuln the vulnerability to store in the database
     * @throws DatabaseException thrown if there is an error writing to the database
     * @throws CorruptIndexException is thrown if the CPE Index is corrupt
     * @throws IOException thrown if there is an IOException with the CPE Index
     */
    public void saveEntry(Vulnerability vuln) throws DatabaseException, CorruptIndexException, IOException {
        if (cveDB == null) {
            return;
        }
        String cveName = vuln.getName();
        if (prevVersionVulnMap.containsKey(cveName)) {
            List<VulnerableSoftware> vulnSoftware = prevVersionVulnMap.get(cveName);
            for (VulnerableSoftware vs : vulnSoftware) {
                vuln.updateVulnerableSoftware(vs);
            }
        }
        for (VulnerableSoftware vs : vuln.getVulnerableSoftware()) {
            if (cpeIndex != null) {
                cpeIndex.saveEntry(vs);
            }
        }
        cveDB.updateVulnerability(vuln);
    }
    private Index cpeIndex = null;

    /**
     * Sets the cpe index
     * @param index the CPE Lucene Index
     */
    void setCpeIndex(Index index) {
        cpeIndex = index;
    }

    // <editor-fold defaultstate="collapsed" desc="The Element Class that maintains state information about the current node">
    /**
     * A simple class to maintain information about the current element while
     * parsing the NVD CVE XML.
     */
    protected static class Element {

        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String NVD = "nvd";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String ENTRY = "entry";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_PRODUCT = "vuln:product";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_REFERNCES = "vuln:references";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_SOURCE = "vuln:source";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_REFERNCE = "vuln:reference";
        /**
         * A node type in the NVD CVE Schema 2.0
         */
        public static final String VULN_SUMMARY = "vuln:summary";
        private String node = null;

        /**
         * Gets the value of node
         *
         * @return the value of node
         */
        public String getNode() {
            return this.node;
        }

        /**
         * Sets the value of node
         *
         * @param node new value of node
         */
        public void setNode(String node) {
            this.node = node;
        }

        /**
         * Checks if the handler is at the NVD node
         *
         * @return true or false
         */
        public boolean isNVDNode() {
            return NVD.equals(node);
        }

        /**
         * Checks if the handler is at the ENTRY node
         *
         * @return true or false
         */
        public boolean isEntryNode() {
            return ENTRY.equals(node);
        }

        /**
         * Checks if the handler is at the VULN_PRODUCT node
         *
         * @return true or false
         */
        public boolean isVulnProductNode() {
            return VULN_PRODUCT.equals(node);
        }

        /**
         * Checks if the handler is at the REFERENCES node
         *
         * @return true or false
         */
        public boolean isVulnReferencesNode() {
            return VULN_REFERNCES.equals(node);
        }

        /**
         * Checks if the handler is at the REFERENCE node
         *
         * @return true or false
         */
        public boolean isVulnReferenceNode() {
            return VULN_REFERNCE.equals(node);
        }

        /**
         * Checks if the handler is at the VULN_SOURCE node
         *
         * @return true or false
         */
        public boolean isVulnSourceNode() {
            return VULN_SOURCE.equals(node);
        }

        /**
         * Checks if the handler is at the VULN_SUMMARY node
         *
         * @return true or false
         */
        public boolean isVulnSummaryNode() {
            return VULN_SUMMARY.equals(node);
        }
    }
    // </editor-fold>
}
