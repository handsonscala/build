package site

import scalatags.Text.all._

object Chapter{
  def render(rawContents: Seq[(String, Boolean)],
             freeChapters: Seq[String],
             chapterName: String,
             chapterOutline: Seq[String]) = {
    Util.pageChrome(
      s"Hands-on Scala: ${chapterName.split(": ").last}",
      ".",

      frag(
        div(
          cls := "chapter-left-col",
          display.flex,
          alignItems.center,
          flexDirection.column,
          height := "100%",
          overflowY.scroll
        )(
          div(width := 360, maxWidth := "100%")(
            Util.renderFreeChapterList(freeChapters, chapterName),
            hr,
            div(cls := "list-group")(
              for (chapterLabel <- chapterOutline) yield {
                val prefix = if (chapterLabel.contains('.')) "section-" else ""
                val suffix = util.Util.sanitize(chapterLabel)
                a(
                  cls := s"list-group-item list-group-item-action",
                  padding := 8,
                  href := s"#$prefix$suffix"
                )(
                  chapterLabel
                )
              },
            )
          )
        ),
        div(display.flex)(
          div(cls := "chapter-right-col"),
          div(flexGrow := 1, maxWidth := "100%")(
            for((rawContent, narrowBottom) <- rawContents) yield div(
              maxWidth := 720,
              backgroundColor := "white",
              paddingBottom := (if (narrowBottom) 25 else 100),
              paddingTop := 25,
              marginLeft.auto,
              marginRight.auto,
              marginTop := 25,
              fontSize := 13,
              position.relative,
              boxShadow := "0px 0px 25px rgba(0, 0, 0, 0.5)",
              marginBottom := 50,
              cls := "book-page"
            )(
               div(marginLeft.auto, marginRight.auto, maxWidth := 650, padding := 10)(
                raw(rawContent),
              )
            )
          ),
        )
      ),
      freeChapters,
      fixedHeader = true
    )

  }
}