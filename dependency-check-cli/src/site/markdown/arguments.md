Command Line Arguments
====================

The following table lists the command line arguments:

Short  | Argument Name         | Parameter       | Description | Requirement
-------|-----------------------|-----------------|-------------|------------
 \-a   | \-\-app               | \<name\>        | The name of the application being scanned. This is a required argument. | Required
 \-c   | \-\-connectiontimeout | \<timeout\>     | The connection timeout (in milliseconds) to use when downloading resources. | Optional
 \-d   | \-\-data              | \<path\>        | The location of the data directory used to store persistent data. This option should generally not be set. | Optional
 \-f   | \-\-format            | \<format\>      | The output format to write to (XML, HTML, VULN, ALL). The default is HTML. | Required
 \-h   | \-\-help              |                 | Print the help message. | Optional
 \-l   | \-\-log               | \<file\>        | The file path to write verbose logging information. | Optional
 \-n   | \-\-noupdate          |                 | Disables the automatic updating of the CPE data. | Optional
 \-o   | \-\-out               | \<folder\>      | The folder to write reports to. This defaults to the current directory. | Optional
 \-p   | \-\-proxyport         | \<port\>        | The proxy port to use when downloading resources. | Optional
       | \-\-proxypass         | \<pass\>        | The proxy password to use when downloading resources. | Optional
       | \-\-proxyuser         | \<user\>        | The proxy username to use when downloading resources. | Optional
 \-s   | \-\-scan              | \<path\>        | The path to scan \- this option can be specified multiple times. | Required
       | \-\-suppression       | \<file\>        | The file path to the suppression XML file; used to suppress [false positives](../suppression.html). | Optional
 \-u   | \-\-proxyurl          | \<url\>         | The proxy url to use when downloading resources. | Optional
 \-v   | \-\-version           |                 | Print the version information. | Optional
       | \-\-advancedHelp      |                 | Print the advanced help message. | Optional
       | \-\-connectionString  | \<connStr\>     | The connection string to the database. | Optional
       | \-\-dbDriverName      | \<driver\>      | The database driver name. | Optional
       | \-\-dbDriverPath      | \<path\>        | The path to the database driver; note, this does not need to be set unless the JAR is outside of the class path. | Optional
       | \-\-dbPassword        | \<password\>    | The password for connecting to the database. | Optional
       | \-\-dbUser            | \<user\>        | The username used to connect to the database. | Optional
       | \-\-disableNexus      |                 | Disable the Nexus Analyzer. | Optional
       | \-\-nexus             | \<url\>         | The url to the Nexus Server. | Optional
       | \-\-nexusUsesProxy    | \<true\|false\> | Whether or not the defined proxy should be used when connecting to Nexus. | Optional
       | \-\-zipExtensions     | \<strings\>     | A comma-separated list of additional file extensions to be treated like a ZIP file, the contents will be extracted and analyzed. | Optional
       | \-\-pathToMono        | \<path\>        | The path to Mono for .NET Assembly analysis on non-windows systems. | Optional