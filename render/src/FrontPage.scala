package render
import TableOfContents.paddedTd
import scalatags.Text.all._
object FrontPage{
  def main(args: Array[String]): Unit = {
    val frontPage = upickle.default.read[FrontPage](os.read(os.pwd / "in.json"))
    os.write(os.pwd / "metadata.html", frontPage.renderMetadata().render)
    os.write(os.pwd / "foreword.html", frontPage.renderForeword().render)
    os.write(os.pwd / "authors-note.html", frontPage.renderAuthorsNote().render)
  }
  implicit val rw: upickle.default.ReadWriter[FrontPage] = upickle.default.macroRW
}
case class FrontPage(authorsNote: String,
                     foreword: String,
                     frontMatter: String,
                     metadataNewPage: Boolean) {
  def page(title: String, txt: String) = frag(
    div(maxWidth := 600, marginLeft.auto, marginRight.auto)(
      div(height := 100, width := 100),
      if (title != "") frag(
        h1(textAlign.center, id := util.Util.sanitize(title))(
          title,
          a(
            href := s"https://localhost/section-header/ $title".replace(" ", "%20"),
            width := 1,
            height := 1,
            display.`inline-block`
          )
        ),
        hr
      ),
      raw(txt)
    ),
  )
  def renderMetadata() = {
    val internalCover =
      if (!metadataNewPage) frag()
      else div(
        div(width := 1, height := 356),
        h1(textAlign.center)("Hands-on Scala Programming"),
        hr,
        table(cls := "table table-borderless table-sm", width := "100%")(
          tr(paddedTd("Author"), paddedTd(textAlign.right)("Li Haoyi")),
          tr(paddedTd("Published"), paddedTd(textAlign.right)("1 June 2020")),
          tr(paddedTd("Website"), paddedTd(textAlign.right)("www.handsonscala.com")),
          tr(paddedTd("ISBN"), paddedTd(textAlign.right)("978-981-14-5693-0"))
        ),
        hr,
        css("page-break-after") := "always"
      )
    frag(internalCover, page("", frontMatter))
  }

  def renderForeword() = page("Foreword", foreword)
  def renderAuthorsNote() = page("Author's Note", authorsNote)

}