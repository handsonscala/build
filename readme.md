# Hands-on Scala Programming Build Pipeline

This repository contains the book rendering pipeline for the book
www.handsonscala.com. It contains the full contents of the first 5 chapters,
with the subsequent 15 chapters replaced by stubs. This repo contains markdown
sources and can generate "sample" (5 chapter) and "dist" (20 chapter) versions
of the book in PDF, EPub and Mobi formats, along with a static website
containing a Web/HTML sample of the book. Page numbers, table of contents, etc.
are all handled programmatically and can be tweaked as desired. The generated
PDFs can easily be fed into IngramSpark to produce paperback editions.

This repo can perform parallel and incremental builds of the book, re-rendering
a full PDF in less than 60 seconds and individual chapters in about 1 second. It
comes with a `-w` flag to watch inputs, allowing an extremely fast turnaround
time between editing a chapter and viewing the rendered PDF.

This repo contains a test suite that validates all the book's standalone code
examples and exercises, and does further validation on the book's internal and
external links to make sure that no links are broken.

This build pipeline is designed to support the book Hands-on Scala Programming,
and will likely need to be modified to support any other book. There are no
opaque libraries or opaque helper code: all the code necessary is included,
except for a small set of dependencies below. It is expected you will need to
fork this repository if you want to render your own e-books. Code quality is at
a level suitable for such a one-off use case, with some amount of messiness,
duplication, and cheap hacks that are correct enough for Hands-on Scala to look
as it does.

All versions of the book are generated using HTML/CSS rendered via Chromium (via
Google's Puppeteer library) giving ultimate flexibility in how you want the book
to look. Modifying the book is similar to modifying a HTML website, allowing you
to fully customize the styling and layout however you see fit. The principles
behind the build system are explored in *Hands-on Scala*'s *Chapter 10: Static
Build Pipelines*, which ends with an exercise to build a minimal version of such
a pipeline rendering HTML/CSS to PDFs using the same techniques used in this
repository to render the book itself.

As Hands-on Scala Programming is complete, I do not expected to be updating
these repository going forward. This repo is purely mean to serve as a template
for people to copy and modify, or as a reference, in case anyone else wants to
undertake a similar project in future.

## Dependencies

This repo assumes you are running on Mac OS-X, but can probably be updated to
run on Linux and other operating systems without too much difficulty. This repo
requires the following dependencies to be pre-installed on the system:

- The `bash` shell
- The `java` runtime
- The `dot` graphviz executable
- The `node` Javascript runtime and `npm` package manager

In the process, this repo also makes use of the following dependencies which
will be automatically downloaded as necessary:

- The [Mill](https://github.com/lihaoyi/mill) build tool, to manage the build
  pipeline
- [Commonmark-Java](https://github.com/atlassian/commonmark-java) to handle
  markdown parsing
- The [Scalatags](https://github.com/lihaoyi/scalatags) templating engine, to
  render the HTML version of the book
- [Bootstrap CSS](https://getbootstrap.com/) for styling
- The [Puppeteer](https://github.com/puppeteer/puppeteer) Javascript library,
  which uses Chromium to convert HTML web pages to PDFs
- [Apache PDFBox](https://pdfbox.apache.org/), for general PDF-manipulating
  utilities
- [EPubLib](https://github.com/psiegman/epublib), to handle construction of EPub
  files
- [Kindlegen](https://www.amazon.com/gp/feature.html?ie=UTF8&docId=1000765211)
  for generating Mobi files

## Usage

You can compile various formats of the book via the following command-line
commands:

```bash
./mill -i show dist.print.pdf # print-ready PDF, with tweaked colors and gutter margin
./mill -i show dist.color.pdf # normal color PDF
./mill -i show dist.compact.pdf # thin-margin color PDF, for easy reading on ipads etc.
./mill -i show dist.epub.epub # epub, for use in iBooks etc.
./mill -i show dist.kindle.kindle # Physical kindle version (doesnt look good on kindle app)
```

This repo also can generate 5-chapter preview versions of each format:

```bash
./mill -i show sample.print.pdf
./mill -i show sample.color.pdf
./mill -i show sample.compact.pdf
./mill -i show sample.epub.epub
./mill -i show sample.kindle.kindle
```

Note that the first time you run a command, it may take a while to download all
the necessary dependencies. Subsequent runs will be much faster, and changes
made to the book's sources will only result in the necessary parts of the book
being re-built.

The online website and web preview can be built via

```bash
./mill -i show site.local
```

Note that the website bundles sample copies of the book in each format, and thus
may take quite a while to build the first time or after any change that affects
the book contents. After the first build, the sample copies are cached and
building is relatively fast-ish.

The github repo https://github.com/handsonscala/handsonscala of snippets,
exectable example code, and exercise solutions can be updated via:

```bash
./mill -i show uploadGithub
```

### Parallelism

To run the pipeline in parallel over multiple cores, you can use:

```bash
./mill -i -j 8 dist.color.pdf
```

This spans 8 threads to run different parts of the pipeline in parallel

### Individual Chapters

Individual chapter PDFs can be rendered via

```bash
./mill -i show dist.color.part[1].chapter[5].pdf
```

If you need to debug the raw HTML before they get converted into PDFs, the HTML
for an individual chapter can be rendered via

```bash
./mill -i show dist.color.part[1].chapter[5].body.html
./mill -i show dist.color.part[1].chapter[5].cover.html
```

### Watch and Rebuild

When iterating on a particular chapter, you can speed up the turnaround time by
using the `-w` flag:

```bash
./mill -i -w  dist.color.part[1].chapter[5].pdf
```

This watches the input files and automatically incrementally re-builds that
chapter's PDF when any input file changes. You can use the same `-w` flag on any
of the other commands we saw above.

### Tests

The repository's test suite can be run via

```bash
./mill -i dist.test
./mill -i example[_].test
```

The first command above does some sanity checks (all external links return 200s,
epubcheck passes, internal-links refer to a page with the linked text), while
the second command runs the individual test suites for all the examples. Note
that the test suites aren't particularly hermetic, and some fiddling may be
necessary to get all the tests to pass. Note that the examples' test suites are
cached, so if one fails you can fix the issue and re-run the above command to
pick up where it left off.

## Sources

The book's text is stored as markdown files, in 20 chapters split over 4 parts:

- `1 - Introduction to Scala/`
- `2 - Local Development/`
- `3 - Web Services/`
- `4 - Program Design/`

Each part has a `part.md` file contains the introductory page or that part of
the book, as well as 5 chapters each with a `chapter.md` file containing the
chapter contents. The chapters also have folders containing the working code
examples, each of which has a `readme.md` file with a short description as well
as a bash command that can be used to test and exercise the example.

The markdown is enhanced with the following features:

### Inline Macros

    `%Part I Introduction to Scala`
    
    `%O(log n)`

### Block Macros

    > `%example 2 - Transforms`
    >
    > ...

    > `%horizontal-50`
    >
    > ...

### Custom Highlighters

    ```graphviz
    ...
    ```

    ```diff-scala
    ...
    ```

    ```output
    ...
    ```

These special forms are recognized at parse time and expanded into larger HTML
templates. The implementation of these can be found in the `markdown.sc`
