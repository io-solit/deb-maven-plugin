# Markdown processing for man pages

This file describes how different markdown elements are translated into a roff man page.

The way markdown is handled is heavily influenced by a [`ronn`][ronn] utility written by Ryan Tomayko
(available as [ruby-ronn][ronn-package] package on debian) and meant to be generally compatible with it.
The way `ronn` handles source markdown files is described in [ronn-format][ronn-format-man].

Markdown is parsed according to [commonmark specification][commonmark-spec], so any sequence of characters
will be a valid document, and therefore will translate to a manpage. However such man page will not always
comply with requirements, so some care should be taken when writing man pages.

## Man header

Man page specification requires each man page to have `NAME` section which gives
a short description for topic covered in that man page. This line is used by man-db
to build a database of topics utilized by `apropos` and other commands.

Additionally every man page should contain a header specifying it's name, section, date,
source and manual, where
* name - is a topic name
* section - is a man section from 1 to 8
* date - man page generation date
* source - package providing a page
* manual - a name of the manual

The preferred way to provide this information for the generation process is to
include specially formatted heading into markdown file. This heading should
follow the format below:

```
name(1) -- short, single-sentence description
=============================================
```

Heading may also be given using `#` markdown syntax, but it should be a level one
heading.  For example heading for ls utility may look the following way:
```
# ls(1) -- list directory contents
```

When processing the source file plugin will ignore any content before this man heading
and will give the appropriate warning message if such content is present.

If source file does not contain level one header that satisfies this format, plugin
may try to deduce the correct name and section of the man page based on name and path
of a source file. For instance file `ls.1.md` or `man1/ls.md` will be assumed to
contain a man page with name `ls` from section `1`. However short description for files
without proper header can not be deduced, so a stub made from a package name and a
section name will be used

## Section headings

Other first level markdown headers and second level markdown headings will be
translated to a section headings. Markdown headers of level three and below
will be translated as subsection headings.

Any inline markup of section and subsection headings, including links, images
inline code and emphasis will be disregarded. Section headings will be additionally
converted to upper case.

## Inline markup

*   ` ``backticks`` `: will be displayed in <code><b>monospaced boldface</b></code>,
    but since terminal is always monospaced, it's usually seen as just <b>bold</b>.
    Used for code, flags, commands, and noun-like things.
*   `*stars*` and `**double-stars**`: will be displayed in <b>bold</b>.
*   `<anglequotes>`: is interpreted as inline html by markdown and translates as
    <i>italic</i> which displays as <u>underline</u> in terminal. Used for
    user-specified arguments, variables, or user input
*   `_underbars_` and `__double underbars__`:translates as
    <i>italic</i> which displays as <u>underline</u> in terminal
    Used for literal option values.

Here is grep's DESCRIPTION section represented in markdown:

    `Grep` searches the named input <FILE> (or standard input if
    no files are named, or the file name `-` is given) for lines
    containing a match to the given <PATTERN>. By default, `grep`
    prints the matching lines.

## Lists

Translation process supports both ordered and unordered markdown lists.

Ordered lists will be always numbered with arabic numerals (`1.`, `2.`, `3.` and so on),
and unordered list will use bullets for list marked with asterisk or em-dashes for list
marked with minus.

Nesting is supported, that is other lists, quotes, paragraphs, code blocks may be placed
inside a list item

## Definition list

Definition lists are useful when describing things like program options, where
a short term is followed by detailed explanations.

The definition list syntax is compatible with markdown's unordered list syntax
but requires that the first line of each list item be terminated with a colon
"`:`" character. The contents of the first line is the `term`; subsequent lines
may be comprised of multiple paragraphs, code blocks, standard lists, and nested
definition lists.

The colon must be the last character in  a term line, immediately followed by a
line break. It should not be enclosed into an emphasis or followed by a whitespace

If unordered list includes items that may be treated as definition list items and
ordinary items simultaneously, such list will be divided into several lists, each
of which will contain items of one type.

An example definition list, taken from `test`'s *DESCRIPTION* section:

     The following primaries are used to construct expressions:

       * `-b` <file>:
         True if <file> exists and is a block special file.

       * `-c` <file>:
         True if _file_ exists and is a character special file.

       * `-d` <file>:
         True if file exists and is a directory.

## Links and images

Both links and images are translated as roff links. In terminal such links are displayed
as text followed by URL enclosed in parenthesis.

If link or image contains no text in brackets, but title is present, that title will be used.
In case no title and no text is present, link url will be used.

## Code blocks and HTML blocks

Both code blocks (fenced and indented) and html blocks are treated as an examples. They will be displayed
with no filling in <code><b>bold monospace</b></code> usually visible as just <b>bold</b> in terminals.

Inner content of HTML blocks is not processed, and will be displayed as is.

## Quotes

Quotes are supported and displayed as indented content without further modification. Content may be nested
inside quotes including lists, examples and another quotes.

## Additional validation

[Man page specification][man-spec]  suggests a list of standard section included in manpage, and states
that `SYNOPSIS`, `DESCRIPTION` and `SEE ALSO` section should be present. Additionally it recommends
to place standard section in a specific order.

Plugin will check if man page follows this suggestion and issue a warning if not.

[ronn]: https://github.com/rtomayko/ronn
[ronn-package]: https://packages.debian.org/stable/ruby-ronn
[ronn-format-man]: https://manpages.debian.org/stable/ruby-ronn/ronn-format.7.en.html
[commonmark-spec]: https://spec.commonmark.org/0.28/
[man-spec]: https://manpages.debian.org/stable/manpages/man.7.en.html