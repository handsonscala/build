package site
import scalatags.Text.all._
object Index{
  def render(freeChapters: Seq[String],
             previews: Seq[(Int, String, String)]) = {
    def row(frags: Frag*) = div(cls := "row")(
      for(f <- frags) yield div(
        cls := "index-col col-lg", display.flex, alignItems.center, justifyContent.center,
        paddingBottom := 20, paddingTop := 20
      )(
        f
      )
    )
    def container(f: Frag*) = {
      div(cls := "container-md")(f)
    }
    def darkContainer(f: Frag*) = {
      div(cls := "bg-dark text-light")(div(cls := "container-md")(f))
    }

    Util.pageChrome(
      "Hands-on Scala Programming",
      ".",
      frag(
        container(
          row(
            div(zIndex := 5)(

              h1("Hands-on Scala Programming"),
              p(cls := "text-dark")(
                i("Hands-on Scala"),
                """
                teaches you how to use the Scala programming language in a practical,
                project-based fashion. This book is designed to quickly teach an
                existing programmer everything needed to go from "hello world" to
                building production applications like interactive websites, parallel web
                crawlers, and distributed systems in Scala. In the process you will learn
                how to use the Scala language to solve challenging problems in an elegant
                and intuitive manner.
                """
              ),
              p(cls := "text-dark")(
                i("Hands-on Scala"),
                """
                is available now as an E-Book in PDF, EPub, and Mobi formats,
                or as a Paperback on Amazon.
                """
              ),
              div(
                textAlign.center,
                display.flex,
                alignItems.center,
                justifyContent.spaceAround,
              )(
                script(src := "https://gumroad.com/js/gumroad.js"),
                span(boxShadow := "0px 0px 25px rgba(0, 0, 0, 0.5)")(
                  a(
                    cls := "gumroad-button",
                    role := "button",
                    href := "https://gum.co/DNJPR"
                  )(
                    "Buy Hands-on Scala E-Book"
                  )
                )
              ),
              div(
                textAlign.center,
                display.flex,
                alignItems.center,
                justifyContent.spaceAround,
              )(
                a(
                  cls := "btn btn-primary",
                  role := "button",
                  href := "https://www.amazon.com/dp/9811456933",
                  margin := 10
                )(
                  "Buy Hands-on Scala Paperback"
                ),
              ),

            ),
            div(height := 400)(
              img(
                display.block,
                marginLeft.auto,
                marginRight.auto,
                cls := "index-image",
                src := "SplashShot.JPG",
                height := 400
              ),
            )
          ),
          blockquote(
            cls := "blockquote",
            maxWidth := 700,
            fontSize := 16,
            marginLeft.auto,
            marginRight.auto
          )(i(
            """
            Hands-On Scala is the best way to learn about writing Scala in this
            simple and straightforward manner, and a great resource for getting
            things done using the Scala ecosystem.
            """,
            footer(cls := "blockquote-footer", fontSize := 14)(
              a(href := "foreword.html")(
                "Foreword by Martin Odersky, creator of the Scala language"
              )
            )
          )),
          row(
            frag(
              blockquote(cls := "blockquote", fontSize := 16)(i(
                """
                I helped review some of this book - excellent work by
                @li_haoyi; if you're looking to get into Scala, recommended!
                """,
                footer(cls := "blockquote-footer", fontSize := 14)(
                  a(href := "https://twitter.com/alexallain/status/1266793072991498240")(
                    "Alex Allain, author of Jumping into C++"
                  )
                )
              )),
            ),
            frag(
              blockquote(cls := "blockquote", fontSize := 16)(i(
                """
                Fantastic book! I got the privilege to review it, and I can honestly
                say it made me a better engineer! Tour de force by @li_haoyi
                """,
                footer(cls := "blockquote-footer", fontSize := 14)(
                  a(href := "https://twitter.com/themitak/status/1266812051013300225")(
                    "Dimitar Simeonov, entrepreneur"
                  )
                )
              )),
            ),
            frag(
              blockquote(cls := "blockquote", fontSize := 16)(i(
                """
                This will be great! @li_haoyi
                asked me to read an early draft. Very pragmatic. Great examples.
                """,
                br,
                br,
                footer(cls := "blockquote-footer", fontSize := 14)(
                  a(href := "https://twitter.com/deanwampler/status/1251897586841268226")(
                    "Dean Wampler, author of Programming Scala"
                  )
                )
              )),
            ),
          )

        ),
        darkContainer(
          div(width := 20, height := 20),
          row(
            div(boxShadow := "0px 0px 25px rgba(0, 0, 0, 0.5)")(
              raw(os.read(os.pwd / "pipeline-syncer.dot.svg"))
            ),
            div(
              h2("Focused on Real-world Projects"),
              p(cls := "text-white-50")(
                i("Hands-on Scala"),
                """
                is designed for professional developers who need to get up to speed
                using Scala in production. This book dives straight into use cases:
                you will write interactive websites, network file synchronizers,
                parallel web crawlers, data migration tools, and much more.
                Every chapter not only teaches language concepts, but also walks you
                through a use case that the author has worked on professionally
                and will be a valuable addition to your developer toolbox.
                """
              )
            )
          ),
          hr,
          row(
            div(width := "100%", marginTop := -25, height := 325, position.relative)(
              img(src := "intellij-indexed.png", height := 325),
              img(src := "vscode-metals-popup.png", height := 325, marginTop := -275, marginLeft := 50),
            ),
            div(
              h2("Beyond the Scala Language"),
              p(cls := "text-white-50")(
                """
                Knowing the language alone isn't enough to go to production:
                """,
                i("Hands-on Scala"),
                """
                introduces the reader to the ecosystem of
                editors, build tools, web frameworks, database libraries, everything
                necessary to do real work using Scala. You will finish this book
                having all the necessary building blocks to be productive using
                Scala in production.
                """
              )
            )
          )(cls := "flex-row-reverse"),
          hr,
          row(
            img(
              src := "TerminalLightTango.png",
              height := 350,
              marginLeft := -50,
              marginBottom := -50
            ),
            div(
              h2("Code First"),
              p(cls := "text-white-50")(
                i("Hands-on Scala"),
                """
                starts and ends with working code. The concepts you learn in this
                book are backed up by over 120 executable code examples that
                demonstrate the concepts in action, and every chapter ends
                with a set of exercises with complete executable solutions. More than
                just a source of knowledge, """,
                i("Hands-on Scala"),
                """'s wide variety
                of working code examples also serve as a cookbook you can use to
                kickstart any project you may work on in future.
                """
              )
            ),
          ),
//          row(
//            div(
//              h2("Beautifully Typeset"),
//              p(cls := "text-white-50")(
//                i("Hands-on Scala"),
//                """
//                makes use of all available tools to make reading it a joy: colors,
//                syntax highlighting, and print-optimized page layouts. Far from the
//                sterile black-and-white text of traditional programming books,
//                """,
//                i("Hands-on Scala"),
//                """'s content comes alive on the page, making your experience learning
//                Scala delightful and engaging.
//                """
//              )
//            ),
//            img(src := "PaperbackBalanced.jpg", height := 375),
//          ),
          div(width := 20, height := 20),
        ),

        container(
          h1(marginTop := 30, textAlign.center, "Why Scala?"),
          hr,
          row(
            pre(code(raw(os.read(os.pwd / "requests.scala")))),
            div(
              h2("A Compiled Language that feels Dynamic"),
              p(cls := "text-dark")(
                """
                Python's convenience with Go's performance and scalability,
                Scala gives you the best of both worlds. Scala's conciseness
                makes rapid prototyping a joy, while its optimizing compiler and
                fast JVM runtime provides great performance to support your
                heaviest production workloads.
                """
              )
            )
          ),
          hr,
          row(
            pre(code(raw(os.read(os.pwd / "compileError.scala")))),
            div(
              h2("Easy Safety and Correctness"),
              p(cls := "text-dark")(
                "Tired of fighting ", code("TypeError"), "s and ", code("NullPointerException"),
                """s in
                production? Scala's functional programming style and
                type-checking compiler helps rule out entire classes of bugs
                and defects, saving you time and effort you can instead spend
                developing features for your users.
                """
              )
            )
          )(cls := "flex-row-reverse"),
          hr,
          row(
            pre(code(raw(os.read(os.pwd / "dependencies.xml")))),
            div(
              h2("A Broad and Deep Ecosystem"),
              p(cls := "text-dark")(
                """
                The Scala programming language gives you access to the vast Java ecosystem:
                runtimes, libraries, profilers, package repositories, all battle-tested and
                a single install away. No matter what you are building, with Scala you
                will find everything you need to take your idea to production.
                """
              )
            )
          ),
        ),

        darkContainer(
          h1(paddingTop := 30, textAlign.center, "About the Author"),
          hr(marginBottom := 0),
          row(
            img(height := 230, src := "headshot.jpg"),
            div(
              p(cls := "text-white-50")(
                """
                Li Haoyi graduated from MIT with a degree in Computer Science and Engineering,
                and since then has been a major contributor to the Scala community. His open
                source projects have over 10,000 stars on Github, and are downloaded over
                7,000,000 times a month. Haoyi has used Scala professionally to build
                distributed backend systems, programming languages, high-performance web
                applications, and much more.
                """
              ),
              p(cls := "text-white-50")(
                """
                Haoyi writes a blog about Scala and other technical topics at
                """,
                a(href := "https://www.lihaoyi.com/")("www.lihaoyi.com")
              ),
            )
          )(cls := "flex-row-reverse"),
          div(width := 20, height := 20),
        ),

        container(
          h1(marginTop := 30, textAlign.center, "Table of Contents"),
          hr(marginBottom := 0),
          div(cls := "row")(
            div(cls := "col-lg-auto", paddingTop := 30, paddingBottom := 30, display.flex, alignItems.center, justifyContent.center)(
              div(
                h2(textAlign.center)("Chapter Listing"),
                div(cls := "list-group", id := "list-tab", role := "tablist")(
                  for ((chapterNum, chapterName, chapterPreviewHtml) <- previews) yield a(
                    cls := s"list-group-item list-group-item-action ${if (chapterNum == 1) "active" else ""}",
                    id := s"list-chapter-$chapterNum-list",
                    attr("data-toggle") := "list",
                    href := s"#list-chapter-$chapterNum",
                    role := "tab",
                    padding := 8,
                    attr("aria-controls") := s"chapter-$chapterNum"
                  )(
                    "Chapter ", chapterNum, ": ", chapterName
                  ),
                )
              )

            ),
            div(cls := "col-lg", paddingTop := 30, paddingBottom := 30)(
              div(cls := "tab-content", id := "nav-tabContent", zIndex := 1)(
                for ((chapterNum, chapterName, chapterPreviewHtml) <- previews) yield div(
                  cls := s"tab-pane ${if (chapterNum == 1) "active" else ""}",
                  id := s"list-chapter-$chapterNum",
                  role := "tabpanel",
                  attr("aria-labelledby") := s"list-chapter-$chapterNum",
                )(
                  div(
                    position.absolute, maxWidth := 720, height := 900,
                    backgroundColor := "white",
                    boxShadow := "0px 0px 25px rgba(0, 0, 0, 0.5)",
                    css("transform") := "rotate(5deg)",
                    zIndex := -20
                  ),
                  div(
                    position.absolute, maxWidth := 720, height := 900,
                    backgroundColor := "white",
                    boxShadow := "0px 0px 25px rgba(0, 0, 0, 0.5)",
                    css("transform") := "rotate(2deg)",
                    zIndex := -10
                  ),
                  div(
                    fontSize := 13,
                    maxWidth := 720, minHeight := 900,
                    paddingTop := 25,
                    paddingBottom := 25,
                    backgroundColor := "white",
                    boxShadow := "0px 0px 25px rgba(0, 0, 0, 0.5)",
                    id := s"table-of-contents-$chapterNum-preview",
                  )(
                    div(
                      marginLeft.auto,
                      marginRight.auto,
                      maxWidth := 650,
                      padding := 10,
                      cls := "book-page"
                    )(
                      raw(chapterPreviewHtml),
                    )
                  ),
                  script(raw(s"""
                    document
                      .querySelectorAll("#table-of-contents-$chapterNum-preview a")
                      .forEach((x) => x.removeAttribute("href"))
                  """))
                ),
              )
            )
          ),
          row(
            div(boxShadow := "0px 0px 25px rgba(0, 0, 0, 0.5)")(
              raw(os.read(os.pwd / "chapter-graph.dot.svg")),
            ),
            div(
              h2(textAlign.center)("Chapter Organization"),
              p(cls := "text-dark")(
                "The chapters of ", i("Hands-on Scala"), " are broken down into four parts:"
              ),
              ul(
                for(partName <- util.Util.parts)
                yield li(a(href := (util.Util.sanitize(partName) + ".html"))(partName))
              ),
              p(cls := "text-dark")("""
                Each part of the book focuses on one particular aspect of using
                the Scala language. The chapters within each part build up to
                one or more projects that make use of what you learned throughout
                the preceding chapters.
              """),
              p(cls := "text-dark")("""
                The diagram on the left illustrates how the chapters are organized:
                which chapters depend on each other as prerequisites, and which chapters
                are independent. You can use this to chart your own path through
              """, i("Hands-on Scala"), """,
                focusing your attention on the chapters that cover topics
                you are most interested in.
              """
              )
            )
          ),
          div(height := 20, width := 20)
        ),
        darkContainer(
          div(height := 20, width := 20),
          row(
            div(
              display.flex, alignItems.center, justifyContent.center, flexDirection.column,
              backgroundColor := "white", boxShadow := "0px 0px 10px rgba(0, 0, 0, 0.5)"
            )(
              raw(os.read(os.pwd / "my-list.dot.svg")),
              div(height := 50, width := 50),
              pre(code(raw(os.read(os.pwd / "myList.scala")))),
            ),
            div(
              h2("Free Chapters: Intro to Scala"),
              p(cls := "text-white-50")(
                """
                The first 5 chapters of
                """,
                i("Hands-on Scala"),
                """
                are free to read, as a standalone introduction to the Scala
                language. This is an excellent way to learn the basics of the Scala
                language whether or not you are intending on purchasing the book.
                """
              ),
              p(cls := "text-white-50")(
                i("Introduction to Scala"),
                """
                is available online, or as free PDF, EPub or Mobi downloads.
                It takes you through setting up, basic language constructs, the Scala
                standard collections library, and finally lets you use Scala to implement
                some fun, well-known algorithms. If you are curious about the Scala
                language or curious about the kind of content that is in this book, feel
                free to start reading!
                """
              ),
              div(textAlign.center)(
                a(
                  cls := "btn btn-primary",
                  role := "button",
                  href := "part-i-introduction-to-scala.html",
                  margin := 10
                )(
                  "Intro to Scala (Web)"
                ),
              ),
              div(textAlign.center)(
                a(
                  cls := "btn btn-secondary",
                  role := "button",
                  href := "hands-on-scala-programming-sample.pdf",
                  margin := 10
                )(
                  "Free PDF"
                ),
                a(
                  cls := "btn btn-secondary",
                  role := "button",
                  href := "hands-on-scala-programming-compact-sample.pdf",
                  margin := 10
                )(
                  "Free Compact PDF"
                ),
                a(
                  cls := "btn btn-secondary",
                  role := "button",
                  href := "hands-on-scala-programming-sample.epub",
                  margin := 10
                )(
                  "EPub"
                ),
                a(
                  cls := "btn btn-secondary",
                  role := "button",
                  href := "hands-on-scala-programming-sample.mobi",
                  margin := 10
                )(
                  "Mobi"
                )
              )
            ),
          )(cls := "flex-row-reverse"),
          row(
            div(
              display.flex, alignItems.center, justifyContent.center, flexDirection.column,
              backgroundColor := "white", boxShadow := "0px 0px 10px rgba(0, 0, 0, 0.5)"
            )(
              img(cls := "index-image", src := "materials.png", height := 300),
            ),
            div(
              h2("Free Online Materials"),
              p(cls := "text-white-50")(
                i("Hands-on Scala"),
                """
                has a free online repository of supporting materials, including more than
                120 self-contained executable code examples. This repository forms a great
                reference for you to quickly look up examples of working code to accomplish
                common tasks in Scala.
                """,
              ),
              p(cls := "text-white-50")(
                """
                These examples cover everything from basic Scala syntax and standard library
                APIs, to simple filesystem operations, database access, HTTP servers and
                clients, programming language interpreters, and distributed file synchronizers.
                Each example is self-contained, and can be run and tested using the commands
                provided in its
                """,
                code("readme.md"),
                """. These form a great reference cookbook for anyone using Scala, whether
                or not they read the book, although following along with
                """,
                i("Hands-on Scala"),
                """
                will give you the best experience and help you get the most out of them."""
              ),
              div(
                textAlign.center,
                display.flex,
                alignItems.center,
                justifyContent.spaceAround,
              )(
                a(
                  cls := "btn btn-primary",
                  role := "button",
                  href := "https://github.com/handsonscala/handsonscala"
                )(
                  "Online Reference Materials"
                )
              )
            ),
          ),
          row(
            div(
              display.flex, alignItems.center, justifyContent.center, flexDirection.column,
              backgroundColor := "white", boxShadow := "0px 0px 10px rgba(0, 0, 0, 0.5)"
            )(
              img(cls := "index-image", src := "discuss.png", height := 200),
            ),
            div(
              h2("Learn Together"),
              p(cls := "text-white-50")(
                """
                You don't need to go alone!
                """,
                i("Hands-on Scala"),
                """
                has online discussion threads for every chapter, and a chat room
                for more interactive discussions. Get help from the author or compare
                notes with fellow learners, so you never need to get stuck.
                """,
                i("Hands-on Scala"),
                """'s online community of learners helps enrich your learning experience
                far beyond that of other books or tutorials.
                """
              ),
              div(textAlign.center)(
                a(
                  cls := "btn btn-primary",
                  role := "button",
                  href := "discuss/index.html",
                  margin := 10
                )(
                  "Discussion Threads"
                ),
                a(
                  cls := "btn btn-primary",
                  role := "button",
                  href := "chat.html",
                  margin := 10
                )(
                  "Live Chat"
                )
              )
            ),
          )(cls := "flex-row-reverse"),
          div(height := 20, width := 20)
        ),
        div(textAlign.center, padding := 5)(
          i(display.`inline-block`, padding := 5)("Hands-on Scala Programming"),
          " • ",
          i(display.`inline-block`, padding := 5)("Copyright (c) 2020 Li Haoyi (haoyi.sg@gmail.com)"),
          " • ",
          i(display.`inline-block`, padding := 5)("ISBN 978-981-14-5693-0"),
          " • ",
          i(display.`inline-block`, padding := 5)("First edition published June 1 2020"),
        )
      ),
      freeChapters,
      fixedHeader = false
    )
  }
}