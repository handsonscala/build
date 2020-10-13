import $file.util, util._
import mill._
import scalatags.Text.all._

def renderChapterCover(number: Int,
                       outlineTitles: Seq[(String, String, Int)],
                       chapterTitle: String,
                       previewHtml: String,
                       web: Boolean,
                       pageLinks: Boolean,
                       preview: Boolean,
                       pageNumbers: Boolean) = {
  val paddedTd = td(
    paddingTop := 2,
    paddingBottom := 2,
    paddingLeft := 0,
    paddingRight := 0
  )
  frag(
    h1(textAlign.center)(span(fontSize := 120)(number)),
    h1(textAlign.center)(
      chapterTitle,
      a(
        href := s"https://localhost/section-header/$number $chapterTitle".replace(" ", "%20"),
        width := 1,
        height := 1,
        display.`inline-block`
      )
    ),
    hr,
    table(cls := "table table-borderless table-sm", width := "100%")(
      for ((nums, name, p) <- outlineTitles if name != "Conclusion")
      yield {
        if (web) tr(
          if (number > 5 && preview) paddedTd(nums, " ", name)
          else paddedTd(a(href := s"#section-${sanitize(nums + " " + name)}")(nums), " ", name),
          paddedTd(textAlign.right)(if (pageNumbers) p)
        ) else tr(
          paddedTd(nums, " ", name),
          if (number > 5 && preview) modifier(color := "grey", paddedTd(textAlign.right)("(not in sample)"))
          else if (pageLinks) paddedTd(textAlign.right)(a(href := s"page-link://$p")(p + 1))
          else paddedTd(textAlign.right)(p + 1)
        )
      }
    ),
    hr,
    raw(previewHtml)
  ).render
}
def renderHtmlPage0(sheets: Seq[(String, String)],
                    pageContent: String) = {

  """<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">""" +
  html(xmlns := "http://www.w3.org/1999/xhtml")(
    head(
      tag("title")("Page Title"),
      meta(charset := "utf-8"),
      for ((sheetName, sheetCode) <- sheets)
        yield link(href := sheetName, rel := "stylesheet", `type` := "text/css")
    ),
    body(div(cls := "book-page")(raw(pageContent)))
  )

}
def renderHtmlPage(sheets: Seq[(String, String)],
                   outputName: String,
                   pageContent: String)
                  (implicit ctx: mill.api.Ctx.Dest)= {

  for ((sheetName, sheetCode) <- sheets) {
    os.write(ctx.dest / os.SubPath(sheetName), sheetCode, createFolders = true)
  }
  os.write(
    ctx.dest / s"$outputName.html",
    renderHtmlPage0(sheets, pageContent)
  )

  PathRef(ctx.dest / s"$outputName.html")
}
