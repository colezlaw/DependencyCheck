/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.owasp.dependencycheck.data.update;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.owasp.dependencycheck.data.UpdateException;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.data.nvdcve.NvdCve12Handler;
import org.owasp.dependencycheck.data.nvdcve.NvdCve20Handler;
import org.owasp.dependencycheck.dependency.VulnerableSoftware;
import org.xml.sax.SAXException;

/**
 *
 * @author Jeremy Long (jeremy.long@owasp.org)
 */
public class ProcessTask implements Callable<ProcessTask> {

    private UpdateException exception = null;

    /**
     * Get the value of exception
     *
     * @return the value of exception
     */
    public UpdateException getException() {
        return exception;
    }

    /**
     * Set the value of exception
     *
     * @param exception new value of exception
     */
    public void setException(UpdateException exception) {
        this.exception = exception;
    }
    private final CveDB cveDB;
    private final CallableDownloadTask filePair;
    private final DataStoreMetaInfo properties;

    public ProcessTask(final CveDB cveDB, final DataStoreMetaInfo properties, final CallableDownloadTask filePair) {
        this.cveDB = cveDB;
        this.filePair = filePair;
        this.properties = properties;
    }

    @Override
    public ProcessTask call() throws Exception {
        try {
            processFiles();
        } catch (UpdateException ex) {
            this.exception = ex;
        }
        return this;
    }

    /**
     * Imports the NVD CVE XML File into the Lucene Index.
     *
     * @param file the file containing the NVD CVE XML
     * @param oldVersion contains the file containing the NVD CVE XML 1.2
     * @throws ParserConfigurationException is thrown if there is a parser
     * configuration exception
     * @throws SAXException is thrown if there is a SAXException
     * @throws IOException is thrown if there is a IO Exception
     * @throws SQLException is thrown if there is a SQL exception
     * @throws DatabaseException is thrown if there is a database exception
     * @throws ClassNotFoundException thrown if the h2 database driver cannot be
     * loaded
     */
    protected void importXML(File file, File oldVersion) throws ParserConfigurationException,
            SAXException, IOException, SQLException, DatabaseException, ClassNotFoundException {

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();

        final NvdCve12Handler cve12Handler = new NvdCve12Handler();
        saxParser.parse(oldVersion, cve12Handler);
        final Map<String, List<VulnerableSoftware>> prevVersionVulnMap = cve12Handler.getVulnerabilities();

        final NvdCve20Handler cve20Handler = new NvdCve20Handler();
        cve20Handler.setCveDB(cveDB);
        cve20Handler.setPrevVersionVulnMap(prevVersionVulnMap);
        saxParser.parse(file, cve20Handler);
    }

    private void processFiles() throws UpdateException {
        String msg = String.format("Processing Started for NVD CVE - %s", filePair.getNvdCveInfo().getId());
        Logger.getLogger(StandardUpdateTask.class.getName()).log(Level.INFO, msg);
        try {
            importXML(filePair.getFirst(), filePair.getSecond());
            cveDB.commit();
            properties.save(filePair.getNvdCveInfo());
        } catch (FileNotFoundException ex) {
            throw new UpdateException(ex);
        } catch (ParserConfigurationException ex) {
            throw new UpdateException(ex);
        } catch (SAXException ex) {
            throw new UpdateException(ex);
        } catch (IOException ex) {
            throw new UpdateException(ex);
        } catch (SQLException ex) {
            throw new UpdateException(ex);
        } catch (DatabaseException ex) {
            throw new UpdateException(ex);
        } catch (ClassNotFoundException ex) {
            throw new UpdateException(ex);
        } finally {
            filePair.cleanup();
        }
        msg = String.format("Processing Complete for NVD CVE - %s", filePair.getNvdCveInfo().getId());
        Logger.getLogger(StandardUpdateTask.class.getName()).log(Level.INFO, msg);
    }
}
