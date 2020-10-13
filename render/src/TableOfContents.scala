package render

import scalatags.Text.all._
object TableOfContents{
  def main(args: Array[String]): Unit = {
    val toc = upickle.default.read[TableOfContents](os.read(os.pwd / "in.json"))
    os.write(os.pwd / "toc-summary.html", toc.summary().render)
    os.write(os.pwd / "toc-part-1.html", toc.part(0).render)
    os.write(os.pwd / "toc-part-2.html", toc.part(1).render)
    os.write(os.pwd / "toc-part-3.html", toc.part(2).render)
    os.write(os.pwd / "toc-part-4.html", toc.part(3).render)
  }
  implicit val rw: upickle.default.ReadWriter[TableOfContents] = upickle.default.macroRW

  val paddedTd = td(
    paddingTop := 2,
    paddingBottom := 2,
    paddingLeft := 0,
    paddingRight := 0
  )

  def chapterHref(webPreview: Boolean,
                  partNames: Seq[String],
                  num: String, chapterTitle: String, partNum: Int) = {
    if (webPreview && !Set("1", "2", "3", "4", "5").contains(num)){
      util.Util.sanitize(s"Part ${Shared.partNumerals(partNum)} ${partNames(partNum)}") +
        ".html#" +
        util.Util.sanitize(s"Chapter $num: $chapterTitle")
    }else{
      util.Util.sanitize(s"Chapter $num: $chapterTitle") + ".html"
    }
  }
}

case class TableOfContents(groupedChapters: Seq[(Seq[Seq[(String, String, Int)]], Int, Int)],
                           pageLinks: Boolean,
                           partNames: Seq[String],
                           forewordPageNum: Int,
                           conclusionPageNum: Int,
                           preview: Boolean,
                           webPreview: Boolean,
                           web: Boolean,
                           pageNumbers: Boolean) {
  import TableOfContents.paddedTd
  def pageLinkCell(page0: Int, notInSample: Boolean) = {
    val page = math.abs(page0)
    paddedTd(textAlign.right)(
      if (notInSample) "(not in sample) ",
      if (!pageNumbers) frag()
      else if (pageLinks) a(href := s"page-link://$page")(page + 1)
      else page + 1
    )
  }

  val colorGrey = color := "grey"
  def summary() = frag(
    h1(textAlign.center)(
      "Table of Contents",
      a(
        href := s"https://localhost/section-header/ Table of Contents".replace(" ", "%20"),
        width := 1,
        height := 1,
        display.`inline-block`
      ),
    ),
    hr,
    table(
      cls := "table table-borderless table-sm",
      width := "100%",
      css("page-break-inside") := "avoid",
      marginBottom := 15
    )(
      tr(
        paddedTd(h3("Foreword")),
        pageLinkCell(forewordPageNum, false)(verticalAlign.middle)
      )
    ),
    for ((chapters, partNum, partStartPage) <- groupedChapters) yield frag(
      table(
        cls := "table table-borderless table-sm",
        width := "100%",
        css("page-break-inside") := "avoid",
        marginBottom := 15
      )(
        tr(
          paddedTd(
            h3(
              if (web) {
                a(href := (util.Util.sanitize(s"Part ${Shared.partNumerals(partNum)} ${partNames(partNum)}") + ".html"))(
                  Shared.partNumerals(partNum)
                )
              } else Shared.partNumerals(partNum),
              " ", partNames(partNum)
            )
          ),
          pageLinkCell(partStartPage, false)(verticalAlign.middle)
        ),

        for (chapter <- chapters) yield {
          val (num, chapterTitle, chapterPage) = chapter(0)
          tr(
            paddedTd(
              if (web) a(href := chapterHref(num, chapterTitle, partNum))(num)
              else num,
              " ", chapterTitle
            ),
            if (num.toInt > 5 && preview) modifier(colorGrey, pageLinkCell(chapterPage, true))
            else pageLinkCell(chapterPage, false)
          )
        }
      )
    ),
    table(
      cls := "table table-borderless table-sm",
      width := "100%",
      css("page-break-inside") := "avoid",
      marginBottom := 0
    )(
      tr(
        paddedTd(h3("Conclusion")),
        pageLinkCell(conclusionPageNum, false)(verticalAlign.middle)
      )
    )
  )

  def chapterHref(num: String, chapterTitle: String, partNum: Int) = {
    TableOfContents.chapterHref(webPreview, partNames, num, chapterTitle, partNum)
  }

  def sectionHref(num: String, chapterTitle: String, sectionLabel: String) = {
    num match{
      case "1" | "2" | "3" | "4" | "5" =>
        Some("chapter-" + util.Util.sanitize(s"$num $chapterTitle") + ".html#section-" + util.Util.sanitize(sectionLabel))
      case _ if !preview && !webPreview =>
        Some("chapter-" + util.Util.sanitize(s"$num $chapterTitle") + ".html#section-" + util.Util.sanitize(sectionLabel))
      case _ => None
    }
  }

  def part(n: Int) = {
    val (chapters, partNum, partStartPage) = groupedChapters(n)

    val headerTitle = s"Part ${Shared.partNumerals(partNum)} ${partNames(partNum)}"
    frag(
      h2(css("page-break-before") := "always", id := util.Util.sanitize(headerTitle))(
        headerTitle
      ),
      hr(borderColor := "white"),
      for (chapter <- chapters) yield frag(
        table(
          cls := "table table-borderless table-sm",
          width := "100%",
          css("page-break-inside") := "avoid",
          marginBottom := 12
        )(
          for (((nums, name, page), subChapterNum) <- chapter.zipWithIndex) yield {
            val (chapterRhs, sectionRhs) =
              if (n == 0 || !preview) (pageLinkCell(page, false), pageLinkCell(page, false))
              else (
                modifier(pageLinkCell(page, true), colorGrey),
                modifier(td, colorGrey),
              )

            val chapterNum = chapter.head._1
            val chapterTitle = chapter.head._2
            if (subChapterNum != 0)  tr(
              paddedTd(
                sectionHref(chapterNum, chapterTitle, nums + " " + name) match{
                  case Some(dest) if web => a(href := dest)(nums)
                  case _ => nums
                },
                " ", name
              ),
              sectionRhs
            ) else tr(borderBottom := "1px solid rgb(225, 225, 225)")(
              paddedTd(b(
                if (web) a(href := chapterHref(nums, chapterTitle, partNum))(nums)
                else nums,
                " ", name
              )),
              chapterRhs
            )
          }
        )
      )
    )
  }
}