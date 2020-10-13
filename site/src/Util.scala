package site
object Util{
  import scalatags.Text.all._, scalatags.Text.tags2
  import java.time.LocalDate

  implicit val localDateRw: upickle.default.ReadWriter[java.time.LocalDate] =
    upickle.default.readwriter[Long].bimap[java.time.LocalDate](_.toEpochDay, java.time.LocalDate.ofEpochDay(_))

  def pageChrome(pageTitle: String,
                 unNesting: String,
                 contents: Frag,
                 freeChapters: Seq[String],
                 fixedHeader: Boolean) = {
    val sheets = Seq(
      "bootstrap.min.css",
      "prism.min.css",
      "prism-diff-highlight.css",
      "base.css",
      "screen.css"
    )

    val introToScalaLabel = "Intro to Scala (Free Chapters)"
    doctype("html")(
      html(overflowX.hidden)(
        head(
          meta(charset := "utf-8"),
          for (sheet <- sheets)
          yield link(href := sheet, rel := "stylesheet", `type` := "text/css" ),
          tags2.title(pageTitle),
          script(src:="https://code.jquery.com/jquery-3.4.1.slim.min.js"),
          script(src:="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js"),
          script(src:="https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js"),
          script(
            attr("async") := "async",
            src := "https://www.googletagmanager.com/gtag/js?id=UA-27464920-8"
          ),
          script(raw("""
            window.dataLayer = window.dataLayer || [];
            function gtag() {dataLayer.push(arguments);}
            gtag('js', new Date());
            gtag('config', 'UA-27464920-8');
          """)),
          meta(name:="viewport", content:="initial-scale = 1.0,maximum-scale = 1.0"),
          tag("style")(
            """
              |@media (max-width: 992px) {
              |  .chapter-left-col{
              |    position: relative;
              |    width: 100%;
              |    padding-top: 25px;
              |  }
              |  .chapter-right-col{
              |
              |  }
              |  .chapter-nav-bar{
              |
              |  }
              |  .chapter-nav-bar-2{
              |    display: none;
              |  }
              |  .index-image {
              |    margin-right: -125px;
              |  }
              |}
              |@media (min-width: 992px) {
              |  .chapter-left-col{
              |    position: fixed;
              |    width: 420px;
              |    padding-top: 75px;
              |  }
              |  .chapter-right-col{
              |    width: 30%;
              |    min-width: 420px;
              |  }
              |  .chapter-nav-bar{
              |    position: fixed;
              |  }
              |}
              |.book-page svg{
              |  max-width: 100%;
              |}
              |""".stripMargin)
        ),
        body(overflowX.hidden, margin := 0, fontSize := 14, cls := "text-dark bg-light")(

          if (fixedHeader)tag("nav")(
            cls := "navbar navbar-expand-xl navbar-dark bg-dark chapter-nav-bar-2",
            position.fixed, width := "100%", height := 56
          ),

          tag("nav")(
            cls := s"navbar navbar-expand-lg navbar-dark bg-dark ${if (fixedHeader) " chapter-nav-bar" else ""}",
            if (!fixedHeader) width := "100%",
            if (fixedHeader) zIndex := 10
          )(
            a(cls := "navbar-brand", href := s"$unNesting/index.html")(
              img(src := "logo-transparent.png", height := 24, marginRight := 16),

              "Hands-on Scala"
            ),
            button(
              cls := "navbar-toggler",
              `type` := "button",
              attr("data-toggle") := "collapse",
              attr("data-target") := "#navbarNavDropdown",
              attr("aria-controls") := "navbarNavDropdown",
              attr("aria-expanded") := "false",
              attr("aria-label") := "Toggle navigation",
            )(
              span(cls := "navbar-toggler-icon")
            ),
            div(cls := "collapse navbar-collapse", id := "navbarNavDropdown")(
              ul(cls := "navbar-nav")(
                li(cls := "navbar-item")(
                  a(cls := "nav-link", href := s"$unNesting/chat.html")(
                    "Chat"
                  ),
                ),
                li(cls := "navbar-item")(
                  a(cls := "nav-link", href := s"$unNesting/discuss/index.html")(
                    "Discuss"
                  ),
                ),
                li(cls := "navbar-item")(
                  a(cls := "nav-link", href := "https://github.com/handsonscala/handsonscala")(
                    "Online Materials"
                  ),
                ),
                if (!fixedHeader) li(cls := "nav-item dropdown")(
                  a(
                    cls := "nav-link dropdown-toggle",
                    href := "#",
                    id := "navbarDropdownMenuLink",
                    role := "button",
                    attr("data-toggle") := "dropdown",
                    attr("aria-haspopup") := "true",
                    attr("aria-expanded") := "false"
                  )(
                    introToScalaLabel
                  ),
                  div(cls := "dropdown-menu", attr("aria-labelledby") := "navbarDropdownMenuLink",
                    for (label <- freeChapters)
                    yield a(
                      cls := "dropdown-item",
                      href := s"$unNesting/${Util.chapterHtmlName(label)}")(
                      label
                    ),
                    //                    a(
                    //                      cls := "dropdown-item",
                    //                      href := s"$unNesting/hands-on-scala-free-chapters.pdf",
                    //                      "Chapters 1-5 (Free PDF Download)"
                    //                    )
                  )
                )
              )
            ),
          ),
          contents
        )
      )
    )
  }

  def renderFreeChapterList(freeChapters: Seq[String], activeLabel: String) = {
    div(cls := "list-group")(
      for (label <- freeChapters) yield {
        val url = chapterHtmlName(label)
        val activeCls = if (label == activeLabel) "active" else ""

        a(
          cls := s"list-group-item list-group-item-action $activeCls",
          padding := 8,
          href := url
        )(
          label
        )
      }/*,
      a(
        cls := "list-group-item list-group-item-action",
        href := s"./hands-on-scala-free-chapters.pdf",
        "Chapters 1-5 (Free PDF Download)"
      )*/
    )
  }
  def chapterHtmlName(chapterName: String) = util.Util.sanitize(chapterName) + ".html"

  def renderAdjacentLink(next: Boolean, name: String) = {
    a(href := s"${util.Util.sanitize(name)}.html")(
      if (next) frag(name, " ", i(cls:="fa fa-arrow-right" , aria.hidden:=true))
      else frag(i(cls:="fa fa-arrow-left" , aria.hidden:=true), " ", name)
    )
  }
}