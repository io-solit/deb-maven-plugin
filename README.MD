# Maven Debian Plugin

Maven plugin create .deb packages, creating control file, copyright file
and transitively copying runtime dependencies.

Plugin is platform-independent and does not require additional external tools.

Requires java 8 and maven 3.3

## Goals:
* `pupulate` - transitively copy runtime dependencies to a specified directory
* `copyright` - create a machine readable copyright file
* `package` - create a .deb file with control file

## Details:

### `populate` goal

During this goal execution plugin will traverse all runtime dependencies of this project possibly applying
filters to exclude some ot them.

Full configuration:
```xml
    <configuration>
        <dependencyDir></dependencyDir>
        <packageName>${project.artifactId}</packageName>
        <stageDir>${project.build.directory}/deb</stageDir>
        <traverseExclusions>
            <dependencies>
                <dependency>
                    <groupId></groupId>
                    <artifactId></artifactId>
                    <version></version>
                    <type></type>
                    <classifier></classifier>
                </dependency>
            </dependencies>
            <pattern></pattern>
        </traverseExclusions>
        <packageExclusions>
            <dependencies>
                <dependency>
                    <groupId></groupId>
                    <artifactId></artifactId>
                    <version></version>
                    <type></type>
                    <classifier></classifier>
                </dependency>
            </dependencies>
            <pattern></pattern>
        </packageExclusions>
    </configuration>
```

*   `dependencyDir` - directory to copy dependencies to; if unspecified `<stageDir>/usr/share/<packageName>`
    will be used
*   `packageName` - name of a debian package; default is project artifact id; may be set using `deb.name`
    system property
*   `stageDir` - directory to stage files before package creation; default is ${project.build.directory}/deb
*   `traverseExclusions` - filter to exclude dependencies transitively from being processed; if dependency
    matches a filter it is excluded and it's transitive dependencies are __not__ processed
    *   `dependencies` - list of standard maven dependencies; only specified parameters are checked during
        filtering: specifying only a version will exclude all artifacts of that version
    *   `pattern` - a regular expression to check artifact string (groupId:artifactId:version\[:classifier\])
        against
*   `packageExclusions` - filter to exclude dependencies from being processed; if dependency
    matches a filter it is excluded but it's transitive dependencies are still processed
    *   `dependencies` - list of standard maven dependencies; only specified parameters are checked during
        filtering: specifying only a version will exclude all artifacts of that version
    *   `pattern` - a regular expression to check artifact string (groupId:artifactId:version\[:classifier\])
        against

### `copyright` goal

Creates a machine-readable copyright file according to
[specification](https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/). May include copyright for
dependencies copied into a package

Copyright information may be specified in a configuration or extracted from a pom file if no
configuration present.

Copyright info is build based on a `copyrightPatterns` parameter and gathered from a pom file if no
`copyrightPatterns` specified:
*   copyright is constructed from a copyright symbol, <code>inceptionYear</code> (if present), current
    year and holders:
    1.  project organisation, if present
    2.  otherwise, project developers if present
    3.  otherwise, project contributors if present
    4.  otherwise <code>user.name</code> system property is used
*   for project license `mainLicence` parameter is used if specified, otherwise first of project licenses
    is used; if no licenses present, project considered to be in a public domain.

If `dependencyCopyrights` parameter is set to true, all dependencies filtered by `traversalExclusions`
and `packageExclusions` are grouped by their copyright and licenses and included as a separate Files
paragraph. Copyright and license strings are constructed the same way as for this project

Full configuration:
```xml
    <configuration>
        <upstreamName></upstreamName>
        <upstreamContact></upstreamContact>
        <source></source>
        <copyrightDisclaimer></copyrightDisclaimer>
        <copyrightComment></copyrightComment>
        <mainLicence></mainLicence>
        <copyrightText></copyrightText>
        <copyrightPatterns>
            <copyrightPattern>
                <files></files>
                <copyright></copyright>
                <licence></licence>
                <licenceContent></licenceContent>
                <comment></comment>
            </copyrightPattern>
        </copyrightPatterns>
        <licesnces>
            <licence>
                <name></name>
                <file></file>
                <comment></comment>
            </licence>
        </licesnces>
        <copyrightFile></copyrightFile>
        <dependencyCopyrights></dependencyCopyrights>
        <dependencyDir></dependencyDir>
        <packageName>${project.artifactId}</packageName>
        <stageDir>${project.build.directory}/deb</stageDir>
        <traverseExclusions>
            <dependencies>
                <dependency>
                    <groupId></groupId>
                    <artifactId></artifactId>
                    <version></version>
                    <type></type>
                    <classifier></classifier>
                </dependency>
            </dependencies>
            <pattern></pattern>
        </traverseExclusions>
        <packageExclusions>
            <dependencies>
                <dependency>
                    <groupId></groupId>
                    <artifactId></artifactId>
                    <version></version>
                    <type></type>
                    <classifier></classifier>
                </dependency>
            </dependencies>
            <pattern></pattern>
        </packageExclusions>
    </configuration>
```
*   `upstreamName` - upstream name of deb package
*   `upstreamContact` - comma separated set of upstream contacts of deb package
*   `source` - url of upstream source code
*   `copyrightDisclaimer` - disclaimer (no liability clause for instance)
*   `copyrightComment` - comment for a copyright
*   `mainLicence` - licence to use for the project as a whole
*   `copyrightText` - copyright for the project as a whole
*   `copyrightPatterns` - file patterns to specify copyright for; each pattern includes:
    *   `files` - mandatory set of glob patterns for a file
    *   `copyright` - mandatory copyright string eg *Copyright 2018, Some Guy*
    *   `licence` - mandatory name of license
    *   `licenceContent` - an optional licence text
    *   `comment` - an optional comment
*   `licesnces` - licences to include into a copyright file; each licence consists of
    *   `name` - mandatory license name
    *   `file` - mandatory file, containing licence full text
    *   `comment` - optional comment
*   `copyrightFile` - File to wright copyright to. If not specified `<stageDir>/usr/share/doc/<packageName>/copyright`
    is used
*   `dependencyCopyrights` - whether to include copyrights for gathered dependencies
*   `dependencyDir` - directory to copy dependencies to; if unspecified `<stageDir>/usr/share/<packageName>`
    will be used
*   `packageName` - name of a debian package; default is project artifact id; may be set using `deb.name`
    system property
*   `stageDir` - directory to stage files before package creation; default is ${project.build.directory}/deb
*   `traverseExclusions` - filter to exclude dependencies transitively from being processed; if dependency
    matches a filter it is excluded and it's transitive dependencies are __not__ processed
    *   `dependencies` - list of standard maven dependencies; only specified parameters are checked during
        filtering: specifying only a version will exclude all artifacts of that version
    *   `pattern` - a regular expression to check artifact string (groupId:artifactId:version\[:classifier\])
        against
*   `packageExclusions` - filter to exclude dependencies from being processed; if dependency
    matches a filter it is excluded but it's transitive dependencies are still processed
    *   `dependencies` - list of standard maven dependencies; only specified parameters are checked during
        filtering: specifying only a version will exclude all artifacts of that version
    *   `pattern` - a regular expression to check artifact string (groupId:artifactId:version\[:classifier\])
        against

### Package goal

Create deb package with files situated in a stage dir.

This goal:
*   creates control file based on configuration
*   copies control files to a control archive, following present symlinks
*   writes md5sums of present data files
*   copies data files from a stage dir to a data archive, following present symlinks
*   creates symbolic links based on a configuration

```xml
    <configuration>
        <maintainerScripts>preinst,postinst,prerm,postrm,config</maintainerScripts>
        <stageDir>${project.build.directory}/deb</stageDir>
        <controlDir>${project.build.directory}/control</controlDir>
        <target></target>
        <packageName>${project.artifactId}</packageName>
        <version>${project.version}</version>
        <revision></revision>
        <maintainer>${project.developers[0].name}</maintainer>
        <maintainerEmail>${project.developers[0].email}</maintainerEmail>
        <architecture>all</architecture>
        <synopsis>${project.name}</synopsis>
        <description>${project.description}</description>
        <homepage>${project.url}</homepage>
        <permissions>
            <permission>
                <permissions></permissions>
                <include></include>
                <exclude></exclude>
            </permission>
        </permissions>
        <packageAttributes>
            <section></section>
            <priority></priority>
            <source></source>
            <essential></essential>
            <builtUsing></builtUsing>
            <depends></depends>
            <preDepends></preDepends>
            <recommends></recommends>
            <suggests></suggests>
            <enhances></enhances>
            <breaks></breaks>
            <conflicts></conflicts>
            <provides></provides>
        </packageAttributes>
        <symbolicLinks>
            <symbolicLink>
                <name></name>
                <linkDestination></linkDestination>
            </symbolicLink>
        </symbolicLinks>
    </configuration>
```
*   `maintainerScripts` - names of control files to treat as maintainer scripts
*   `stageDir` - directory to stage file to be packaged as a deb file
*   `controlDir` - directory to stage package control files
*   `target` - result .deb file; if not specified
    `${project.build.directory}/<packageName>_<version>-<revision>_<architecture>.deb` is used
*   `packageName` - name of debian package
*   `version` - upstream part of a package version
*   `revision` - revision part of a package version
*   `maintainer` - package maintainer's name
*   `maintainerEmail` - package maintainer's email address
*   `architecture` - package architecture
*   `synopsis` - short one-line package description, human readable name
*   `description` - detailed package description
*   `homepage` - project home page
*   `permissions` - permission changes to apply to packaged files:
    *   `permissions` - octal permission (eg 600)
    *   `include` - list of glob patterns to apply permissions change to (if omitted, all files assumed)
    *   `exclude` - list of glob patterns to exclude from permission change, excludes have
        higher priority than includes
*   `packageAttributes` - optional package control fields
*   `symbolicLinks` - symbolic links to create in a package data files; all fields should be specified
    for every link:
    *   `name` - name of a link file
    *   `linkDestination` - name of a destination file (file man not exist)