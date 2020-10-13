package render

import scalatags.Text.all._
import TableOfContents.paddedTd
object PartFront{
  def main(args: Array[String]): Unit = {
    os.write(
      os.pwd / "page.html",
      upickle.default.read[PartFront](os.read(os.pwd / "in.json")).run().render
    )
  }
  implicit val rw: upickle.default.ReadWriter[PartFront] = upickle.default.macroRW
}
case class PartFront(partNum: Int,
                     partName: String,
                     chapters: Seq[(Int, String, Int)],
                     descriptionHtmlString: String,
                     pageNumbers: Boolean,
                     web: Boolean,
                     webPreview: Boolean,
                     pageLinks: Boolean) {
  def run() = {
    val fullPartName = Shared.partNumerals.lift(partNum).map(n => s"Part $n: ").getOrElse("") + partName
    frag(
      div(height := 50, width := 100),
      a(
        href := s"https://localhost/section-header/ $fullPartName".replace(" ", "%20"),
        width := 1,
        height := 1,
        display.`inline-block`
      ),
      div(marginLeft.auto, marginRight.auto, width := 256, height := 256)(
        raw(os.read(os.resource / Shared.partSplashImages(partNum)))
      ),
      div(height := 50, width := 50),
      h1(textAlign.center)(fullPartName),
      hr,
      if (chapters.nonEmpty) frag(
        table(cls := "table table-borderless table-sm", width := "100%")(
          for ((chapterNum, chapterName, chapterPage) <- chapters) yield {
            tr(
              paddedTd(
                if (web) a(
                  href := TableOfContents.chapterHref(
                    webPreview,
                    Seq.fill(10)(partName),
                    chapterNum.toString,
                    chapterName,
                    partNum
                  )
                )(
                  chapterNum
                )
                else chapterNum,
                " ", chapterName
              ),
              paddedTd(textAlign.right)(
                if (!pageNumbers) frag()
                else if (web) chapterPage
                else if (pageLinks) a(href := s"page-link://$chapterPage")(chapterPage + 1)
                else chapterPage + 1
              )
            )
          }
        ),
        hr
      ),
      raw(descriptionHtmlString),
      if (partNum != 4) div(css("page-break-before") := "always", width := 10, height := 10)
    )
  }
}