Configuration
====================
The following properties can be set on the dependency-check-maven plugin.

Property             | Description                        | Default Value
---------------------|------------------------------------|------------------
autoUpdate           | Sets whether auto-updating of the NVD CVE/CPE data is enabled. It is not recommended that this be turned to false. | true
externalReport       | When using as a Site plugin this parameter sets whether or not the external report format should be used. | false
outputDirectory      | The location to write the report(s). Note, this is not used if generating the report as part of a `mvn site` build | 'target'
failBuildOnCVSS      | Specifies if the build should be failed if a CVSS score above a specified level is identified. The default is 11 which means since the CVSS scores are 0-10, by default the build will never fail.         | 11
format               | The report format to be generated (HTML, XML, VULN, ALL). This configuration option has no affect if using this within the Site plugin unless the externalReport is set to true. | HTML
logFile              | The file path to write verbose logging information. | &nbsp;
suppressionFile      | The file path to the XML suppression file \- used to suppress [false positives](../suppression.html) | &nbsp;
connectionTimeout    | The Connection Timeout.            | &nbsp;
nexusAnalyzerEnabled | Sets whether Nexus Analyzer will be used. | &nbsp;
nexusUrl             | Defines the Nexus URL. | &nbsp;
nexusUsesProxy       | Whether or not the defined proxy should be used when connecting to Nexus. | true
databaseDriverName   | The name of the database driver. Example: org.h2.Driver. | &nbsp;
databaseDriverPath   | The path to the database driver JAR file; only used if the driver is not in the class path. | &nbsp;
connectionString     | The connection string used to connect to the database. | &nbsp;
databaseUser         | The username used when connecting to the database. | &nbsp;
databasePassword     | The password used when connecting to the database. | &nbsp;
zipExtensions        | A comma-separated list of additional file extensions to be treated like a ZIP file, the contents will be extracted and analyzed. | &nbsp;
skipTestScope        | Should be skip analysis for artifacts with Test Scope | true
skipProvidedScope    | Should be skip analysis for artifacts with Provided Scope | false
skipRuntimeScope     | Should be skip analysis for artifacts with Runtime Scope | false
dataDirectory        | Data directory to hold SQL CVEs contents. This should generally not be changed. | &nbsp;
cveUrl12Modified     | URL for the modified CVE 1.2 | http://nvd.nist.gov/download/nvdcve-modified.xml
cveUrl20Modified     | URL for the modified CVE 2.0 | http://static.nvd.nist.gov/feeds/xml/cve/nvdcve-2.0-modified.xml
cveUrl12Base         | Base URL for each year's CVE 1.2, the %d will be replaced with the year | http://nvd.nist.gov/download/nvdcve-%d.xml
cveUrl20Base         | Base URL for each year's CVE 2.0, the %d will be replaced with the year | http://static.nvd.nist.gov/feeds/xml/cve/nvdcve-2.0-%d.xml
pathToMono           | The path to Mono for .NET assembly analysis on non-windows systems | &nbsp;


Deprecated Properties
====================
The following properties have been deprecated. These can stell be set in
the dependency-check-maven plugin's configuration. However, future versions
will remove these properties. Instead using these properties you should
use [Maven's settings](https://maven.apache.org/settings.html#Proxies) to
configure a proxy.

Property             | Description                        | Default Value
---------------------|------------------------------------|------------------
proxyUrl             | The Proxy URL.                     | &nbsp;
proxyPort            | The Proxy Port.                    | &nbsp;
proxyUsername        | Defines the proxy user name.       | &nbsp;
proxyPassword        | Defines the proxy password.        | &nbsp;
