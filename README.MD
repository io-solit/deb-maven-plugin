# Deb Maven Plugin

Maven plugin for creating .deb packages, creating control file, copyright file
and transitively copying runtime dependencies.

Plugin is platform-independent and does not require additional external tools.

Requires java 8 and maven 3.3

Plugin is available from central maven repository. To use it, simply add the following to a 
`build/plugins` section of the pom file. 

```xml
<plugin>
    <groupId>io.solit.maven</groupId>
    <artifactId>deb-maven-plugin</artifactId>
    <version>0.5</version>
</plugin>
```

## Goals:
* `pupulate` - transitively copy runtime dependencies to a specified directory
* `copyright` - create a machine readable copyright file
* `man` - compile a man pages from markdown source files
* `changelog` - copy and/or convert changelog to debian format
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
[specification][mr-copyright]. May include copyright for
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
    
### Man goal

Goal generates linux man pages from markdown source. Generation process is similar to one used by 
[`ruby-ronn`][ronn] utility.

Goal processes every file in source directory with `.md` extension (case-insensitive), and converts it to a 
man page.
Files with other extensions are silently ignored.

Each processed file must contain a level one header specifying man title, section and short description used 
by man-db to
generate apropos database. File content before header which follows this format will be ignored

For instance, the following code will create man page with title `foo` in section `5` with short description 
`bar baz qux`

```
# foo(5) - bar baz qux
======================
```

Other headers in file will be turned to a sections of man file, if header of level one or two, and to a 
subheader for other levels.

Generation supports, lists (including nested ones), definition lists, code blocks, quotes, emphasis and 
other markdown feature described in
[commonmark][comonmark] markdown specification.

For further details on markdown processing see [man-markdown.md](man-markdown.md)

```xml
<configuration>
    <manSourceDirectory>${project.basedir}/src/deb/doc</manSourceDirectory>
    <manDestinationDirectory></manDestinationDirectory>
    <deduceManTitle>true</deduceManTitle>
    <strictManParsing>false</strictManParsing>
    <manSource></manSource>
    <manName></manName>
    <packageName>${project.artifactId}</packageName>
    <stageDir>${project.build.directory}/deb</stageDir>
    <sourceEncoding>${project.build.sourceEncoding}</sourceEncoding>
</configuration>
```
*   `manSourceDirectory` - directory to read markdown files for man page generation.  
    Only files with `.md` (case-insensitive) will be processed.  
    Subdirectory structure will be preserved, with exception for `.md` files in directory itself. Such files
    will be placed to a directory named `man[sectionNumber]`  
    Defaults to `${project.basedir}/src/deb/doc`
*   `manDestinationDirectory` - Directory to write generated man pages to.  
    Subdirectory structure of source directory will be preserved, with exception for `.md` files in directory
    itself. Such files will be placed to a directory named `man[sectionNumber]`  
    If not specified `<stageDir>/usr/share/man` will be used.
*   `deduceManTitle` - Whether to try to create man title and description, if no proper heading found inside 
    markdown file.  
    Title will be deduced the following way:
    -   if file name matches `title.[number].md` pattern, then <code>title</code> is used as man title,
        and `[number]` as man section
    -   else if file is in directory `man[number]`, than file name (without extension) is used as man title and,
        `[number]` as man section
    -   otherwise file name (without extension) is used as title and section is assumed to be `1`
    
    For short description required by man page specification combination of title and section name will be used
*   `strictManParsing` - Treat parser and validation warnings as failures and abort generation.
*   `manSource` - manual sources specified in a manual header. Package name will be used by default.
*   `manName` - manual name specified in a manual header.
*   `packageName` - name of debian package
*   `stageDir` - stage directory, containing files to be included into a deb package
*   `sourceEncoding` - encoding used to read source files

### Changelog goal

This goal creates and/or copies a changelog file as requested by debian policy
    
If `copyOriginalChangelog` is set to `true`, `upstreamChangelogSource` file
will be gzipped and copied into `changelogDestinationDirectory` as *changelog.gz*

If `convertToDebianChangelog` is set, plugin will try to parse `changelogSource` as
markdown file written according to [keepachangelog.com][keep-a-changelog] recommendations and
write a changelog in a debian format as *changelog.Debian.gz*

Details on conversion process can be found in [keepachangelog-markdown.md](keepachangelog-markdown.md)

```xml
<configuration>
    <copyOriginalChangelog>false</copyOriginalChangelog>
    <convertToDebianChangelog>true</convertToDebianChangelog>
    <appendCurrentVersionChangeSet>false</appendCurrentVersionChangeSet>
    <version>${project.version}</version>
    <revision></revision>
    <changelogSource>${project.basedir}/changelog.md</changelogSource>
    <upstreamChangelogSource>${project.basedir}/changelog.md</upstreamChangelogSource>
    <changelogDestinationDirectory></changelogDestinationDirectory>
    <stageDir>${project.build.directory}/deb</stageDir>
    <packageName>${project.artifactId}</packageName>
    <maintainer>${project.developers[0].name}</maintainer>
    <maintainerEmail>${project.developers[0].email}</maintainerEmail>
    <targetDistribution>stable</targetDistribution>
    <sourceEncoding>${project.build.sourceEncoding}</sourceEncoding>
</configuration>
```
*   `copyOriginalChangelog` - Whether to copy a gzipped original changelog to a destination folder
*   `convertToDebianChangelog` - Whether to try parsing changelog and converting it to a form
    specified by debian policy
*   `appendCurrentVersionChangeSet` - if current version does not match latest version in a changelog, 
    and this option is set to true, plugin will add a yanked change set to a generated changelog
*   `version` - upstream part of a current package version
*   `revision` - revision part of a current  package version
*   `changelogSource` - A source file for debian changelog
*   `upstreamChangelogSource` - A source file for upstream changelog
*   `changelogDestinationDirectory` - Directory to write changelog to. If not specified
    `<stageDir>/usr/share/doc/<packageName>` will be used
*   `stageDir` - Stage directory, containing files to be included into a deb package
*   `packageName` - Name of debian package
*   `maintainer` - Package maintainer's name
*   `maintainerEmail` - Package maintainer's email address
*   `targetDistribution` - A distribution to write to changelog
*   `sourceEncoding` - Encoding used to read source file

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

[mr-copyright]: https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
[ronn]: https://manpages.debian.org/stretch/ruby-ronn/ronn-format.7.en.html
[comonmark]: https://spec.commonmark.org/0.28/
[keep-a-changelog]: https://keepachangelog.com/en/1.0.0/