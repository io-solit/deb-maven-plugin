# Markdown processing for changelog

`changelog` plugin goal can process source changelog file assuming it 
is a markdown file written according to [keep a changelog][keep-a-changelog] 
recommendations. 

Debian policy specifies that changelog is a series of entries (here called change sets)
containing package name, version, date, distribution, maintainer contacts, urgency and 
changes details. Since not all of this info is available from a markdown changelog files, plugin will 
assume some values by default, while providing a way to override it.

## Finding change sets

Keep a changelog suggests that each release should be described as a section starting with
level 2 markdown header containing version and release date, like so:
```
## [version] - release-date
```

Parser will try to find such headers assuming that version can consist of alphanumeric characters,
`.`, `~`, `+`, `:` and `-`, and release-date is a ISO-8601 formatted local date, zoned date, local date time
or zoned date time, for instance:
```
## [2:4.5.12+dfsg-2+deb9u1] - 2017-11-20T23:24:53UTC+0100
```

Change set will include everything below such header and until end of file or any next header of level one or two.
Changes will be copied into a result file with minimal modification, preserving markdown syntax.

Changelog content not included into any change set is discarded, as well as change sets with no changes.
Exception to that is a yanked change sets.  Such  change set will be included even if it does not contain any
changes. Yanked change set header should end with `[YANKED]` mark, like so:
```
## [4.1.1.162-3] - 2018-01-23 [YANKED]
```

## Change set details

By default for every change set package name, distribution and maintainer contacts are taken from goal configuration,
and urgency is set to medium. Parser provides a mechanism to alter change set properties via change set content.

Any change set that contains level three header `security` will have urgency set to `high`.

Additionally it is possible to override default values by adding a level third header `Release` into a change set
followed  by a bullet list containing colon separated key-value pairs:
*   key `urgency`, value is one of urgency levels (`low`, `medium`, `high`, `emergency`, `critical`) followed
    by optional comment in a parenthesis.
*   key `maintainer`, value is a maintainer name followed by email possibly in angle brackets
*   key `distribution`, value is a distribution name

Example:
```
### Release
*   urgency: emergency (update now!)
*   maintainer: bob <bob@example.com>
*   distribution: stable-security
```

The `Release` header and this list typically will not be copied into a change set. However if some other content is
present between `Release` header and next header of any level, it will be copied into a change set as well as a
header. If list contains some items that are not key-value pair, have unknown key, or do not follow a format expected
for that key, both list and header will be included into a change set.

## Additional processing

If no change sets were found during changelog parsing, such changelog is discarded entirely.

Change sets in a result changelog will be ordered by date from latest on top to earliest at the bottom.


[keep-a-changelog]: https://keepachangelog.com/en/1.0.0/