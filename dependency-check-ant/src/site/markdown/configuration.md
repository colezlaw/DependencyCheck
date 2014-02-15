Configuration
====================
To configure the dependency-check task you can add it to a target and include a
file based [resource collection](http://ant.apache.org/manual/Types/resources.html#collection)
such as a [FileSet](http://ant.apache.org/manual/Types/fileset.html), [DirSet](http://ant.apache.org/manual/Types/dirset.html),
or [FileList](http://ant.apache.org/manual/Types/filelist.html) that includes
the project's dependencies.

```xml
<target name="dependency-check" description="Dependency-Check Analysis">
    <dependency-check applicationname="Hello World"
                      reportoutputdirectory="${basedir}"
                      reportformat="ALL">

        <fileset dir="lib">
            <include name="**/*.jar"/>
        </fileset>
    </dependency-check>
</target>
```
The following table lists the configurable properties:

Property              | Description | Requirement | Default Value
----------------------|-------------|-------------|------------
ApplicationName       | The name of the application to use in the generated report. | Required |
ReportFormat          | The format of the report to be generated. Allowed values are: HTML, XML, VULN, or ALL. The default value is HTML.| Optional |
ReportOutputDirectory | The directory where dependency-check will store data used for analysis. Defaults to the current working directory. | Optional |
FailBuildOn           | If set and a CVE is found that is greater then the specified value the build will fail. The default value is 11 which means that the build will not fail. Valid values are 0-11. | Optional |
AutoUpdate            | If set to false the NVD CVE data is not automatically updated. Setting this to false could result in false negatives. However, this may be required in some environments. The default value is true. | Optional |
DataDirectory         | The directory where dependency-check will store data used for analysis. Defaults to a folder called, called 'dependency-check-data', that is in the same directory as the dependency-check-ant jar file was installed in. *It is not recommended to change this.* | Optional |
LogFile               | The file path to write verbose logging information. | Optional |
SuppressionFile       | An XML file conforming to the suppression schema that suppresses findings; this is used to hide [false positives](../suppression.html). | Optional |
ProxyUrl              | Defines the proxy used to connect to the Internet. | Optional |
ProxyPort             | Defines the port for the proxy. | Optional |
ProxyUsername         | Defines the proxy user name. | Optional |
ProxyPassword         | Defines the proxy password. | Optional |
ConnectionTimeout     | The connection timeout used when downloading data files from the Internet. | Optional |
nexusAnalyzerEnabled  | The connection timeout used when downloading data files from the Internet. | Optional |
nexusUrl              | The connection timeout used when downloading data files from the Internet. | Optional |
databaseDriverName    | The name of the database driver. Example: org.h2.Driver. | Optional |
databaseDriverPath    | The path to the database driver JAR file; only used if the driver is not in the class path. | Optional |
connectionString      | The connection string used to connect to the database. | Optional |
databaseUser          | The username used when connecting to the database. | Optional | dcuser
databasePassword      | The password used when connecting to the database. | Optional |
zipExtensions         | A comma-separated list of additional file extensions to be treated like a ZIP file, the contents will be extracted and analyzed. | Optional
cveUrl12Modified      | URL for the modified CVE 1.2 | Optional | http://nvd.nist.gov/download/nvdcve-modified.xml
cveUrl20Modified      | URL for the modified CVE 2.0 | Optional | http://static.nvd.nist.gov/feeds/xml/cve/nvdcve-2.0-modified.xml
cveUrl12Base          | Base URL for each year's CVE 1.2, the %d will be replaced with the year | Optional | http://nvd.nist.gov/download/nvdcve-%d.xml
cveUrl20Base          | Base URL for each year's CVE 2.0, the %d will be replaced with the year | Optional | http://static.nvd.nist.gov/feeds/xml/cve/nvdcve-2.0-%d.xml
