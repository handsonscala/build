
import java.awt.RenderingHints
import java.awt.image.BufferedImage

import $ivy.`com.atlassian.commonmark:commonmark:0.13.1`
import $ivy.`com.atlassian.commonmark:commonmark-ext-gfm-tables:0.13.1`
import $ivy.`com.atlassian.commonmark:commonmark-ext-autolink:0.13.1`
import $ivy.`com.lihaoyi::scalatags:0.9.1`
import $ivy.`guru.nidi:graphviz-java:0.15.0`
import $ivy.`org.apache.pdfbox:pdfbox:2.0.18`
import $ivy.`org.jsoup:jsoup:1.13.1`
import org.jsoup._
import $file.renderPdf
import $file.renderHtml
import $file.markdown
import $file.distribution
import javax.script.ScriptEngine
import mill._
import scalalib._
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.rendering.{ImageType, PDFRenderer}
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.common.PDRectangle

val sampleChapterNumbers = Seq(1, 2, 3, 4, 5)

val romanNumerals = Seq("I", "II", "III", "IV", "V")

val partNumbersAndNames: Seq[(Int, String, os.Path)] = interp.watchValue(
  os.list(os.pwd).flatMap{ p =>
    p.last match {case s"$num - $name" => Some((num.toInt, name, p)) case _ => None}
  }
)

val chapters0 = partNumbersAndNames.map(_._3).flatMap(os.list(_)).flatMap { p =>
  p.last match {
    case s"$num - $name" => Some((num.toInt, name, p))
    case _ => None
  }
}

val partNumberLookup = partNumbersAndNames.map{case (num, name, p) => (num, (name, p))}.toMap

val partNumeralNames = partNumbersAndNames.map{case (num, name, p) => (romanNumerals(num - 1) + " " + name)}

val chapters = chapters0.sortBy(_._1)

val chapterLookup = chapters.map{case (number, name, path) => (number, (name, path))}.toMap

val sheetUrls = Seq(
  "https://maxcdn.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css"  -> "bootstrap.min.css",
  "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/themes/prism.min.css" -> "prism.min.css",
  "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/diff-highlight/prism-diff-highlight.css" -> "prism-diff-highlight.css",

)


def baseCss = T.source(millSourcePath / "resources" / "base.css")

def printCss = T.source(millSourcePath / "resources" / "print.css")
def screenCss = T.source(millSourcePath / "resources" / "screen.css")

def epubCss = T.source(millSourcePath / "resources" / "epub.css")
def htmlCss = T.source(millSourcePath / "resources" / "html.css")
def pdfCss = T.source(millSourcePath / "resources" / "pdf.css")

def webResources = T.source(millSourcePath / "resources" / "web")

def baseSheets = T{
  val downloaded =
    for ((sheetUrl, sheetName) <- sheetUrls)
    yield (sheetName, requests.get(sheetUrl).text())
  downloaded ++ Seq("base.css" -> os.read(baseCss().path))
}

val screenSheets = T{
  baseSheets() ++ Seq("screen.css" -> os.read(screenCss().path))
}

val printSheets = T{
  baseSheets() ++ Seq("print.css" -> os.read(printCss().path))
}
val epubSheets = T{
  screenSheets() ++ Seq("epub.css" -> os.read(epubCss().path), "html.css" -> os.read(htmlCss().path))
}


def scripts = T{

  val scripts = Seq(
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/prism.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/diff-highlight/prism-diff-highlight.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-sql.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-json.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-clike.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-java.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-markup.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-markdown.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-scala.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-javascript.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-diff.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/components/prism-bash.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/diff-highlight/prism-diff-highlight.min.js",
    "https://cdnjs.cloudflare.com/ajax/libs/prism/1.17.1/plugins/keep-markup/prism-keep-markup.min.js"
  )
  for (script <- scripts) yield {
    println("Downloading " + script)
    (script, requests.get(script).text())
  }
}

def engine = T.worker{
  val engine = new javax.script.ScriptEngineManager().getEngineByName("nashorn")
  engine.eval("var self = void 0; var document = void 0")
  for ((scriptName, scriptCode) <- scripts()) {
    println("Nashorn Evaluating " + scriptName)
    engine.eval(scriptCode)
  }
  engine
}

def highlight(engine: ScriptEngine, nodeType: String, s: String) = synchronized{
  engine.eval(s"""
        Prism.highlight(
          ${ujson.write(s)},
          ${
    if (nodeType.startsWith("diff-")) "Prism.languages.diff"
    else s"Prism.languages[${ujson.write(nodeType)}]"
  },
          ${ujson.write(nodeType)}
        )
      """).toString
    .replace("<span", """<code""")
    .replace("</span>", """</code>""")
}
val expectedTableOfContentsPageCount = 6
val expectedCoverPageCount = 2
val expectedForewordPageCount = 4
val expectedFrontPageCount =
  expectedTableOfContentsPageCount +
  expectedCoverPageCount +
  expectedForewordPageCount


trait HtmlToPdfModule extends Module{
  def marginMM: Float
  def htmlContent: T[String]
  def resources: T[PathRef]
  def images: T[PathRef]

  def sheets: T[Seq[(String, String)]]
  def html = T{
    if (os.exists(resources().path)) os.copy(resources().path, T.dest / "resources")
    if (os.exists(images().path)) os.copy(images().path, T.dest / "images")

    (renderHtml.renderHtmlPage(sheets(), "rendered", htmlContent()), PathRef(T.dest))
  }
  def pdf0 = T{
    renderPdfWorker().apply(millModuleSegments.render, html()._1.path, T.dest / "body.pdf", marginMM)
  }
  def pdf = T{
    val headersPerPage = renderPdf.getPdfOutline(pdf0().path)
    renderPdf.RenderedOutput(pdf0(), headersPerPage.toSeq)
  }
  def pageCount = T{
    PDDocument.load(os.read.bytes(pdf().pdf.path)).getNumberOfPages
  }
}

def reviewer = T.input{ sys.env.get("REVIEWER") }

trait BookModule extends Module{
  def hasCoverPage: Boolean
  def pageLinks: Boolean
  def sheets: T[Seq[(String, String)]]
  def web: Boolean
  def preview: Boolean
  def webPreview: Boolean
  def marginMM: Float
  def pageNumbers: Boolean
  def spineOffsetMM: Option[Float]
  def chapterPdfs = T.sequence(part.items.map(_._2.chapterPdf)).map(_.flatten)

  def chapterStartPages = T.sequence(part.items.map(_._2.chapterStartPages)).map(_.flatten)

  def chapterHtmls = T.sequence(part.items.map(_._2.chapterHtmls)).map(_.flatten)

  def chapterOutlines = T.sequence(part.items.map(_._2.chapterOutlines)).map(_.flatten)

  val allChapterLookup = part.items.flatMap(_._2.chapterLookup).toMap

  object part extends mill.Cross[PartModule](partNumbersAndNames.map(_._1.toString):_*)
  class PartModule(partNumber: String) extends Module{

    def partStartPageOffset = part.itemMap.get(List((partNumber.toInt-1).toString)) match {
      case None => T{ expectedFrontPageCount }
      case Some(prev) => T{ prev.chapter.items.last._2.endPageOffset() }
    }

    def chapterPdf = front.pdf.zip(T.sequence(chapter.items.map(_._2.pdf)))
      .map{case (f, cs) => f.pdf +: cs}

    def chapterHtmls = T.sequence(chapter.items.map(t =>
      t._2.cover.htmlContent.zip(t._2.body.htmlContent.map(Some(_)))
    ))

    def chapterOutlines = T.sequence(chapter.items.map(_._2.chapterOutline))

    def chapterStartPages = T.sequence(chapter.items.map(_._2.startPageOffset))

    def examples = T.sequence(chapter.items.map(_._2.examples)).map(_.flatten)

    def resources = T.sequence(chapter.items.map(_._2.resources))

    def images = T.sequence(chapter.items.map(_._2.images))

    def htmlBodyAndPreviews = T.sequence(
      chapter.items.map(t => t._2.htmlBodyAndPreview.map(x => (t._2.number, x)))
    )

    def millSourcePath = partNumberLookup(partNumber.toInt)._2
    val chapters0 = interp.watchValue(
      os.list(millSourcePath).flatMap{ p =>
        p.last match {case s"$num - $name" => Some((num.toInt, name, p)) case _ => None}
      }
    )

    val chapters = chapters0.sortBy(_._1)

    val chapterLookup = chapters.map{case (number, name, path) => (number, (name, path))}.toMap
    def source = T.source(millSourcePath / "part.md")
    object front extends HtmlToPdfModule{
      def sheets = BookModule.this.sheets
      def resources = PathRef(T.dest)
      def images = PathRef(T.dest)
      def marginMM = BookModule.this.marginMM
//      def startPageOffset = partStartPageOffset

      def htmlContent = T{
        val partName = partNumberLookup(partNumber.toInt)._1

        os.write(
          T.dest / "in.json",
          ujson.Obj(
            "partNum" -> (partNumber.toInt - 1),
            "partName" -> partName,
            "chapters" -> upickle.default.writeJs(
              T.sequence(
                chapter.items.map(t =>
                  t._2.startPageOffset.map(p => (t._2.number, t._2.name, p))
                )
              )()
            ),
            "descriptionHtmlString" ->
              markdown.makeSimpleRenderer().render(
                markdown.makeParser().parse(os.read(source().path))
              ),
            "pageNumbers" -> pageNumbers,
            "web" -> web,
            "webPreview" -> webPreview,
            "pageLinks" -> pageLinks
          )
        )
        mill.modules.Jvm.runSubprocess(
          "render.PartFront",
          render.runClasspath().map(_.path),
          workingDir = T.dest
        )
        os.read(T.dest / "page.html")
      }
    }

    object chapter extends mill.Cross[ChapterModule](chapters.map(_._1.toString):_*)
    class ChapterModule(number0: String) extends Module{
      def millSourcePath = chapterLookup(number0.toInt)._2
      val number = number0.toInt
      val (name, path0) = chapterLookup(number)
      def source = T.source(path0 / "chapter.md")
      def resources = T.source(path0 / "resources")
      def images = T.source(path0 / "images")
      def examples = T.sources(
        os.list(path0)
          .filter(_.last.contains(" - "))
          .map(PathRef(_))
      )

      def startPageOffset: T[Int] = chapter.itemMap.get(List((number-1).toString)) match {
        case None => partStartPageOffset.map(_ + 2)
        case Some(prev) => prev.endPageOffset
      }

      def endPageOffset = T{
        val fillerPageCount = if (body.pageCount() % 2 == 1) 0 else 1
        if (preview && partNumber != "1") startPageOffset() + 2
        else startPageOffset() + cover.pageCount() + body.pageCount() + fillerPageCount
      }

      def sheets = T{ BookModule.this.sheets() }

      def htmlBodyAndPreview = T{
        val (codeSnippets, preview, bodyContent) = markdown.renderMarkdown(
          number,
          os.read(source().path),
          highlight(engine(), _, _),
          allChapterLookup.mapValues(_._1).to(Map),
          web,
          partNumeralNames.contains,
          exampleInfoMap
        )
        (preview, bodyContent, codeSnippets)
      }

      def chapterOutline = T{
        if (preview && partNumber != "1") Seq(Seq(number0 -> name) ++ body.pdf().outline.flatten)
        else Seq(Seq(number0 -> name)) ++ body.pdf().outline
      }
      object cover extends ChapterHtmlToPdfModule{
        def htmlContent = T{
          renderHtml.renderChapterCover(
            number,
            chapterOutline().zipWithIndex.flatMap{
              case (titles, pageNum) =>
                titles.collect{
                  case (numbering, title) if numbering.split('.').length == 2 =>
                    (numbering, title, (pageNum + startPageOffset()))
                }
            },
            chapterLookup(number)._1,
            htmlBodyAndPreview()._1,
            web,
            pageLinks,
            preview || webPreview,
            pageNumbers
          )
        }
      }
      object body extends ChapterHtmlToPdfModule{
        def htmlContent = T{
          import scalatags.Text.all._
          ChapterModule.this.htmlBodyAndPreview()._2 +
          div(cls := "alert alert-primary", css("page-break-inside") := "avoid")(
            b(
              s"Discuss Chapter $number online at ",
              a(href := s"https://www.handsonscala.com/discuss/$number")(
                s"https://www.handsonscala.com/discuss/$number"
              )
            )
          )
        }
      }

      trait ChapterHtmlToPdfModule extends HtmlToPdfModule{
        def sheets = BookModule.this.sheets
        def resources = T{ ChapterModule.this.resources() }
        def images = T{ ChapterModule.this.images() }
        def marginMM = BookModule.this.marginMM
      }

      def pdf = T{
        renderPdf.mergePdfs(
          if (preview && partNumber != "1") Seq(cover.pdf().pdf.path, filler.pdf().pdf.path)
          else if (body.pageCount() % 2 == 1) Seq(cover.pdf().pdf.path, body.pdf().pdf.path)
          else Seq(cover.pdf().pdf.path, body.pdf().pdf.path, filler.pdf().pdf.path),
          T.dest / "chapter.pdf"
        )
        PathRef(T.dest / "chapter.pdf")
      }
    }


  }
  object filler extends BookHtmlToPdfModule{
    def htmlContent = T{
      import scalatags.Text.all.{name => _, _}
      frag().render
    }
  }
  object conclusion extends BookHtmlToPdfModule{
    def source = T.source(os.pwd / "Blurbs" / "conclusion.md")
    def htmlContent = T{
      os.write(
        T.dest / "in.json",
        ujson.Obj(
          "web" -> web,
          "partNum" -> 4,
          "partName" -> "Conclusion",
          "chapters" -> ujson.Arr(),
          "descriptionHtmlString" ->
            markdown.makeSimpleRenderer().render(
              markdown.makeParser().parse(os.read(source().path))
            ),
          "pageNumbers" -> pageNumbers,
          "webPreview" -> webPreview,
          "pageLinks" -> pageLinks
        )
      )
      mill.modules.Jvm.runSubprocess(
        "render.PartFront",
        render.runClasspath().map(_.path),
        workingDir = T.dest
      )
      os.read(T.dest / "page.html")
    }
  }
  trait BookHtmlToPdfModule extends HtmlToPdfModule {
    def sheets = BookModule.this.sheets
    def resources = PathRef(T.dest)
    def images = PathRef(T.dest)
    def marginMM = BookModule.this.marginMM
  }
  def renderedEarlyPages = T{
    os.write(
      T.dest / "in.json",
      ujson.Obj(
        "authorsNote" ->
          markdown.makeSimpleRenderer().render(
            markdown.makeParser().parse(os.read(authorsNoteMd().path))
          ),
        "frontMatter" ->
          markdown.makeSimpleRenderer().render(
            markdown.makeParser().parse(os.read(frontMatterMd().path))
          ),
        "foreword" ->
          markdown.makeSimpleRenderer().render(
            markdown.makeParser().parse(os.read(forewordMd().path))
          ),
        "metadataNewPage" -> (!pageLinks)

      )
    )
    mill.modules.Jvm.runSubprocess(
      "render.FrontPage",
      render.runClasspath().map(_.path),
      workingDir = T.dest
    )
    Tuple3(
      PathRef(T.dest / "metadata.html"),
      PathRef(T.dest / "foreword.html"),
      PathRef(T.dest / "authors-note.html")
    )
  }
  def authorsNoteMd = T.source(os.pwd / "Blurbs" / "authors-note.md")
  def frontMatterMd = T.source(os.pwd / "Blurbs" / "front-matter.md")
  def forewordMd = T.source(os.pwd / "Blurbs" / "foreword.md")

  object foreword extends BookHtmlToPdfModule{
    def htmlContent = T{
      val (metadataHtml, forewordHtml, authorsNoteHtml) = renderedEarlyPages()
      os.read(forewordHtml.path)
    }
  }
  object authorsNote extends BookHtmlToPdfModule{
    def htmlContent = T{
      val (metadataHtml, forewordHtml, authorsNoteHtml) = renderedEarlyPages()
      os.read(authorsNoteHtml.path)
    }
  }

  object front extends BookHtmlToPdfModule{
    def pdf = T{

      val sup = super.pdf()
      val out = T.dest / "front.pdf"

      if (!hasCoverPage) os.copy(sup.pdf.path, out)
      else {
        val image = T.dest / "scaled-front-cover.png"
        scaleImage(
          ImageIO.read((os.pwd / "cover" / "Front.png").toIO),
          900,
          image
        )
        val doc = PDDocument.load(os.read.bytes(sup.pdf.path))
        val mediaBox = doc.getPage(0).getMediaBox
        val page = new PDPage(mediaBox)
        doc.getPages.insertBefore(page, doc.getPage(0))
        import org.apache.pdfbox.pdmodel.PDPageContentStream
        import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject


        val pdImage = PDImageXObject.createFromFile(image.toString, doc)

        val contents = new PDPageContentStream(doc, page)

        contents.drawImage(pdImage, -1, -1, mediaBox.getWidth + 2, mediaBox.getHeight + 2)
        if (preview) {
          contents.setNonStrokingColor(242, 70, 68); //reddish text
          val font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
          val fontSize = 24
          val str = "Free Chapters"
          val textWidth = (font.getStringWidth(str) / 1000.0f) * fontSize
          contents.beginText();
          contents.setFont(font, fontSize);
          contents.newLineAtOffset((mediaBox.getWidth - textWidth) / 2, 27)
          contents.drawString(str);
          contents.endText();
        }
        contents.close()

        doc.save(out.toIO)
      }

      renderPdf.RenderedOutput(PathRef(out), sup.outline)
    }

    def htmlContent = T{
      val (metadataHtml, forewordHtml, authorsNoteHtml) = renderedEarlyPages()
      os.read(metadataHtml.path)
    }
  }

  def flattenedChapters = T{
    chapterOutlines().zip(chapterStartPages()).map{ case (chapterPageTitles, startPage) =>
      chapterPageTitles.zipWithIndex.flatMap{ case (pageTitles, p) =>
        pageTitles.map { case (k, v) => (k, v, p + startPage)}
      }
    }
  }
  object tableOfContents extends BookHtmlToPdfModule{
    def rendered = T{
      val partStartPages = T.sequence(part.items.map(_._2.partStartPageOffset))()
      val groupedChapters: Seq[(Seq[Seq[(String, String, Int)]], Int, Int)] = flattenedChapters()
        .map(_.filter { case (nums, name, page) => nums.split('.').length <= 2 && name != "Conclusion"})
        .grouped(5)
        .zipWithIndex
        .toSeq
        .zip(partStartPages)
        .map{
          case ((chapters, idx), startPage) => (chapters, idx, startPage)
        }

      os.write(
        T.dest / "in.json",
        ujson.Obj(
          "groupedChapters" -> upickle.default.writeJs(groupedChapters),
          "pageLinks" -> pageLinks,
          "partNames" -> upickle.default.writeJs(partNumbersAndNames.map(_._2)),
          "conclusionPageNum" -> part("4").chapter("20").endPageOffset(),
          "forewordPageNum" -> (expectedTableOfContentsPageCount + expectedCoverPageCount),
          "preview" -> preview,
          "webPreview" -> webPreview,
          "web" -> web,
          "pageNumbers" -> pageNumbers
        )
      )

      mill.modules.Jvm.runSubprocess(
        "render.TableOfContents",
        render.runClasspath().map(_.path),
        workingDir = T.dest
      )

      Tuple5(
        PathRef(T.dest / "toc-summary.html"),
        PathRef(T.dest / "toc-part-1.html"),
        PathRef(T.dest / "toc-part-2.html"),
        PathRef(T.dest / "toc-part-3.html"),
        PathRef(T.dest / "toc-part-4.html")
      )
    }
    def htmlContent = T{
      val (summary, toc1, toc2, toc3, toc4) = rendered()
      os.read(summary.path) +
      os.read(toc1.path) +
      os.read(toc2.path) +
      os.read(toc3.path) +
      os.read(toc4.path)
    }
  }

  def unmangled = T{
    val fillerOpt = if (tableOfContents.pageCount() % 2 == 0) Nil else Seq(filler.pdf().pdf.path)

    val seqed: Seq[PathRef] = chapterPdfs()
    renderPdf.mergePdfs(
      Seq(front.pdf().pdf.path) ++
      Seq(tableOfContents.pdf().pdf.path) ++
      fillerOpt ++
      Seq(foreword.pdf().pdf.path) ++
      Seq(filler.pdf().pdf.path) ++
      Seq(authorsNote.pdf().pdf.path) ++
      Seq(filler.pdf().pdf.path) ++
      seqed.map(_.path) ++
      Seq(conclusion.pdf().pdf.path),
      T.dest / bookName
    )
  }

  def finalPdfHeaders = T{
    for {
      (headers, pageNum) <- renderPdf.getPdfOutline(unmangled().path).zipWithIndex.toSeq
      (headerNum, headerName) <- headers
    } yield (headerNum, headerName, pageNum)
  }
  def testLinks = T{
    renderPdf.checkExternalLinks(pdf().path)
  }
  def testOutline = T{
    renderPdf.checkPdfOutline(unmangled().path, finalPdfHeaders())
  }

  def pdf = T{
    renderPdf.finalizePdf(unmangled().path, T.dest / bookName, finalPdfHeaders(), marginMM, spineOffsetMM)

    PathRef(T.dest / bookName)
  }

  def bookName: String = millModuleSegments.value.last.pathSegments.last  + ".pdf"

  val examples = T.sequence(part.items.map(_._2.examples))

  val resources = T.sequence(part.items.map(_._2.resources)).map(_.flatten)

  val images = T.sequence(part.items.map(_._2.images)).map(_.flatten)

  val codeSnippets = T.sequence(
    part.items.map{case (_, p) => p.htmlBodyAndPreviews.map(_.map(t => t._1.toInt -> t._2._3))}
  ).map(_.flatten)

}

def frontCoverPdf = T{


  val out = T.dest / "frontCover.pdf"

  val doc = new PDDocument()
  val page = new PDPage(new PDRectangle(16.2f * 72, 9.5f * 72))
  doc.getPages.add(page)
  import org.apache.pdfbox.pdmodel.PDPageContentStream
  import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

  val pdImage = PDImageXObject.createFromFile((os.pwd / "cover" / "Cover JPEG.jpg").toString, doc)

  val contents = new PDPageContentStream(doc, page)

  contents.drawImage(pdImage, 0, 0, 16.2f * 72, 9.5f * 72)

  contents.close()

  doc.save(out.toIO)

  PathRef(out)
}


trait BookDist extends Module{
  def preview: Boolean
  object color extends BookModule{
    def hasCoverPage = true
    def pageLinks = true
    def sheets = screenSheets() ++ Seq("pdf.css" -> os.read(pdfCss().path))
    def web = false
    def preview = BookDist.this.preview
    def webPreview = false
    def marginMM = 17.5f
    def pageNumbers = true
    def spineOffsetMM = None
  }

  object compact extends BookModule{
    def hasCoverPage = true
    def pageLinks = true
    def sheets = screenSheets() ++ Seq("pdf.css" -> os.read(pdfCss().path))
    def web = false
    def preview = BookDist.this.preview
    def webPreview = false
    def pageNumbers = true
    def marginMM = 2.5f
    def spineOffsetMM = None
  }

  object print extends BookModule{
    def hasCoverPage = false
    def pageLinks = false
    def sheets = printSheets() ++ Seq("pdf.css" -> os.read(pdfCss().path))
    def web = false
    def preview = BookDist.this.preview
    def webPreview = false
    def pageNumbers = true
    def marginMM = 15
    def spineOffsetMM = Some(5)
  }

  object epub extends RenderEpubModule{
    def hasCoverPage = true
    def pageLinks = false
    def sheets = epubSheets()
    def web = true
    def preview = BookDist.this.preview
    def webPreview = false
    def pageNumbers = false
    def marginMM = 17.5f
    def showCoverImage = true
    def spineOffsetMM = None
  }
  object kindle extends RenderEpubModule{
    def hasCoverPage = true
    def pageLinks = false
    def sheets = epubSheets()
    def web = true
    def preview = BookDist.this.preview
    def webPreview = false
    def pageNumbers = false
    def marginMM = 17.5f
    def showCoverImage = false
    def spineOffsetMM = None
  }

  def all = T{
    os.copy(color.pdf().path, T.dest / "hands-on-scala-programming.pdf")
    os.copy(compact.pdf().path, T.dest / "hands-on-scala-programming-compact.pdf")
    os.copy(epub.epub().path, T.dest / "hands-on-scala-programming.epub")
    os.copy(kindle.kindle().path, T.dest / "hands-on-scala-programming.mobi")
    PathRef(T.dest)
  }

  def test = T{
    color.testLinks()
//    compact.testLinks()
    color.testOutline()
    compact.testOutline()
    epub.testEpub()
  }
}

object web extends BookModule{
  def hasCoverPage = true
  def pageLinks = false
  def sheets = screenSheets() ++ Seq("html.css" -> os.read(htmlCss().path))
  def web = true
  def preview = false
  def webPreview = true
  def pageNumbers = true
  def marginMM = 17.5f
  def spineOffsetMM = None
}

trait RenderEpubModule extends BookModule{
  def showCoverImage: Boolean
  def epub = T{
    val output = T.dest / "hands-on-scala-programming.epub"
    for((name, code) <- epubSheets()) os.write(T.dest / name, code)
    val images =
      for (res <- dist.color.images(); if os.exists(res.path); p <- os.list(res.path))
        yield{
          val image = T.dest / "images" / p.last
          os.copy(p, image, createFolders = true)
          image
        }

    val sheetsList = sheets()
    val (metadataFile, forewordFile, authorsNoteFile) = renderedEarlyPages()
    os.write(
      T.dest / "in.json",
      ujson.Obj(
        "coverImage0" -> (if (showCoverImage) ujson.Arr((os.pwd / "cover" / "Front.png").toString) else ujson.Arr()),
        "images0" -> images.map(_.toString),
        "dest0" -> output.toString,
        "stylesheets0" -> ujson.Arr.from(epubSheets().map(t => (T.dest / t._1).toString)),
        "chapters" -> ujson.Arr.from(
          for(((cover, bodyOpt), i) <- chapterHtmls().zipWithIndex)
            yield renderHtml.renderHtmlPage0(
              sheetsList,
              if (!preview || i < 5) cover + bodyOpt.map("""<div style="page-break-before: always" />""" + _).getOrElse("")
              else cover
            )
        ),
        "tableOfContents" -> renderHtml.renderHtmlPage0(sheetsList, tableOfContents.htmlContent()),
        "metadata" -> renderHtml.renderHtmlPage0(sheetsList, os.read(metadataFile.path)),
        "foreword" -> renderHtml.renderHtmlPage0(sheetsList, os.read(forewordFile.path)),
        "authorsNote" -> renderHtml.renderHtmlPage0(sheetsList, os.read(authorsNoteFile.path)),
        "chapterNames" -> chapters.map(_._2),
        "partCovers" -> ujson.Arr(
          renderHtml.renderHtmlPage0(sheetsList, part("1").front.htmlContent()),
          renderHtml.renderHtmlPage0(sheetsList, part("2").front.htmlContent()),
          renderHtml.renderHtmlPage0(sheetsList, part("3").front.htmlContent()),
          renderHtml.renderHtmlPage0(sheetsList, part("4").front.htmlContent())
        ),
        "conclusion" -> renderHtml.renderHtmlPage0(sheetsList, conclusion.htmlContent())
      )
    )
    mill.modules.Jvm.runSubprocess(
      build.epub.finalMainClass(),
      build.epub.runClasspath().map(_.path),
      build.epub.forkArgs(),
      build.epub.forkEnv(),
      Array.empty[String],
      workingDir = T.dest
    )
    PathRef(output)
  }
  def unpacked = T{
    os.proc("unzip", epub().path, "-d", T.dest).call()
    PathRef(T.dest)
  }

  def testEpub = T{
    val res = os.proc("java", "-jar", epubcheck().path, epub().path)
      .call(mergeErrIntoOut = true, check = false)
    os.write(
      T.dest / "log.txt",
      res.out.text()
    )
    val lines = os.read.lines(T.dest / "log.txt")
    val filtered = lines
      .filter(!_.contains("Error while parsing file: Duplicate "))
      .filter(!_.contains("""attribute "charset" not allowed here"""))
      .filter(!_.contains("""element "opf:guide" incomplete; missing required element "opf:reference""""))
      //      .filter(!_.contains(""" The file 'OEBPS/cover.png' does not appear to match the media type image/png, as specified in the OPF file."""))
      .filter(_.startsWith("ERROR"))
    os.write(T.dest / "filtered.txt", filtered.mkString("\n"))
    if (filtered.nonEmpty) mill.api.Result.Failure("Validation Failed", Some(filtered))
    else mill.api.Result.Success(filtered)
  }

  def kindle = T{
    os.copy(epub().path, T.dest / epub().path.last)
    val res = os.proc(kindlegen().path / "kindlegen", T.dest / epub().path.last, "-o", "book.mobi")
      .call(cwd = T.dest, check = false)


    os.write(T.dest / "log.txt", res.out.text())
    if (!res.out.text().contains("Mobi file built with WARNINGS!")) {
      mill.api.Result.Failure("Mobi generation Failed", Some(PathRef(T.dest / "log.txt")))
    }
    else mill.api.Result.Success(PathRef(T.dest / "book.mobi"))
  }
  def kindleSingle = T{
    val (renderedFile, renderedFolder) = part("1").chapter("1").body.html()
    for(f <- os.list(renderedFolder.path)) os.copy(f, T.dest / f.last)
    os.proc(kindlegen().path / "kindlegen", T.dest / "rendered.html", "-o", "rendered.mobi")
      .call(stdout = os.Inherit, cwd = T.dest)

    PathRef(T.dest / "rendered.mobi")
  }


}

object sample extends BookDist{
  def preview = true
}
object dist extends BookDist{
  def preview = false
}

def pdfize = T.source(os.pwd / "pdfize.js")

def puppeteer = T.persistent{
  os.proc("npm", "install", "puppeteer@4.0.1").call(cwd = T.dest, stderr = os.Pipe)
  PathRef(T.dest)
}
def renderPdfWorker = T.worker{
  for(p <- os.list(puppeteer().path)) os.copy.over(p, T.dest / p.last)
  val localPdfize = T.dest / pdfize().path.last

  os.proc("pkill", "-f", "pdfize.js").call(check = false, stdout = os.Inherit)
  os.proc("pkill", "-f", "puppeteer").call(check = false, stdout = os.Inherit)
  os.copy.over(pdfize().path, localPdfize)

  val proc = os.proc(localPdfize).spawn(cwd = T.dest)

  val line = proc.stdout.buffered.readLine()
  assert(line == "Spawned", "Unexpected worker response: " + line)


  val results = new java.util.concurrent.ConcurrentHashMap[String, scala.concurrent.Promise[Unit]]()

  val resultThread = new Thread(() =>
    while (proc.stdout.readLine() match {
      case s"Done $key" =>
        results.get(key).success(())
        true
      case null =>
        println("renderPdfWorker subprocess died")
        false
      case s =>
        println("UNKNOWN RESPONSE " + s)
        false
    })()
  )
  resultThread.setDaemon(true)
  resultThread.start()

  (key: String, htmlFile: os.Path, outputFile: os.Path, marginMM: Float) => {
    val promise = scala.concurrent.Promise[Unit]
    results.put(key, promise)
    synchronized {
      proc.stdin.writeLine(ujson.Arr(key, htmlFile.toString, outputFile.toString, marginMM).render())
      proc.stdin.flush()
    }
    scala.concurrent.Await.result(promise.future, scala.concurrent.duration.Duration.Inf)
    results.remove(key)
    PathRef(outputFile)
  }

}

def uploadGithub() = T.command{
  distribution.uploadGithub(
    dist.color.examples().flatten.map(_.path),
    dist.color.resources().map(_.path),
    dist.color.codeSnippets(),
    T.dest,
    chapters.map(_._2),
    partNumeralNames
  )
}

object util extends ScalaModule {
  def scalaVersion = "2.12.10"
}

object render extends ScalaModule {
  def scalaVersion = "2.12.10"
  def moduleDeps = Seq(util)
  def ivyDeps = Agg(
    ivy"com.lihaoyi::scalatags:0.9.1",
    ivy"com.lihaoyi::os-lib:0.7.1",
    ivy"com.lihaoyi::upickle:1.2.0"
  )
}


def scaleImage(image: BufferedImage,
               height0: Int,
               dest: os.Path) = {

  val height = height0 * 2
  val width = image.getWidth * height / image.getHeight

  val scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

  val graphics = scaled.createGraphics()
  graphics.setRenderingHint(
    RenderingHints.KEY_INTERPOLATION,
    RenderingHints.VALUE_INTERPOLATION_BICUBIC
  )

  graphics.drawImage(image, 0, 0, width, height, null)

  javax.imageio.ImageIO.write(scaled, "PNG", dest.toIO)
}

def epubcheckZip = T.source(os.pwd / "tools" / "epubcheck-4.2.2.zip")
def kindlegenZip = T.source(os.pwd / "tools" / "KindleGen_Mac_i386_v2_9.zip")
def epubcheck = T{
  os.proc("unzip", epubcheckZip().path, "-d", T.dest).call()
  PathRef(T.dest / "epubcheck-4.2.2" / "epubcheck.jar")
}

def kindlegen = T{
  os.proc("unzip", kindlegenZip().path, "-d", T.dest).call()
  PathRef(T.dest)

}
object epub extends ScalaModule {
  def scalaVersion = "2.12.10"
  def moduleDeps = Seq(util)
  def ivyDeps = Agg(
    ivy"com.lihaoyi::scalatags:0.9.1",
    ivy"com.lihaoyi::os-lib:0.7.1",
    ivy"com.lihaoyi::upickle:1.2.0",
    ivy"com.positiondev.epublib:epublib-core:3.1"
  )
}

object site extends ScalaModule {
  def scalaVersion = "2.12.10"
  def moduleDeps = Seq(util)
  def ivyDeps = Agg(
    ivy"com.lihaoyi::scalatags:0.9.1",
    ivy"com.lihaoyi::os-lib:0.7.1",
    ivy"com.lihaoyi::pprint:0.5.9",
    ivy"com.lihaoyi::upickle:1.2.0",
  )

  def prepareFiles = T{
    val e = engine()
    for (res <- os.list(webResources().path)) {
      res.ext match {
        case "xml" | "scala" => os.write(T.dest / res.last, highlight(e, "scala", os.read(res)))
        case "dot" => os.write(T.dest / (res.last + ".svg"), markdown.renderGraphviz(os.read(res), true))
        case _ => os.copy(res, T.dest / res.last)
      }
    }

    for ((name, contents) <- screenSheets() ++ Seq("html.css" -> os.read(pdfCss().path))) {
      os.write(T.dest / name, contents)
    }
    os.makeDir(T.dest / "resources")
    for (res <- web.resources().take(5); if os.exists(res.path); p <- os.list(res.path)) {
      os.copy.into(p, T.dest / "resources")
    }
    os.makeDir(T.dest / "images")
    for (res <- web.images().take(5); if os.exists(res.path); p <- os.list(res.path)) {
      os.copy.into(p, T.dest / "images")
    }

    scaleImage(
      ImageIO.read((web.part("1").chapter("2").images().path / "intellij-indexed.png").toIO),
      325,
      T.dest / "intellij-indexed.png"
    )
    scaleImage(
      ImageIO.read((web.part("1").chapter("2").images().path / "vscode-metals-popup.png").toIO),
      325,
      T.dest / "vscode-metals-popup.png"
    )

    os.copy(sample.color.pdf().path, T.dest/ "hands-on-scala-programming-sample.pdf")
    os.copy(sample.compact.pdf().path, T.dest/ "hands-on-scala-programming-compact-sample.pdf")
    os.copy(sample.epub.epub().path, T.dest/ "hands-on-scala-programming-sample.epub")
    os.copy(sample.kindle.kindle().path, T.dest/ "hands-on-scala-programming-sample.mobi")
    PathRef(T.dest)
  }
  def local = T{
    for (p <- os.list(prepareFiles().path)) {
      os.copy.into(p, T.dest)
    }
    val (metadataHtml, forewordHtml, authorsNoteHtml) = web.renderedEarlyPages()
    val (tocSummary, tocPart1, tocPart2, tocPart3, tocPart4) = web.tableOfContents.rendered()
    os.write(
      T.dest / "args.json",
      ujson.Obj(
        "metadataPages" -> ujson.Arr(
          os.read(metadataHtml.path)
        ),
        "tableOfContentsPages" -> ujson.Arr(
          os.read(tocSummary.path),
          os.read(tocPart1.path),
          os.read(tocPart2.path),
          os.read(tocPart3.path),
          os.read(tocPart4.path)
        ),

        "forewordPages" -> ujson.Arr(
          os.read(forewordHtml.path),
          os.read(authorsNoteHtml.path),
        ),
        "partCovers" -> ujson.Arr(
          web.part("1").front.htmlContent(),
          web.part("2").front.htmlContent(),
          web.part("3").front.htmlContent(),
          web.part("4").front.htmlContent()
        ),
        "freeChapters" -> upickle.default.writeJs(
          web.chapterHtmls()
            .collect{case (coverHtml, Some(bodyHtml)) => (coverHtml, bodyHtml)}
            .zipWithIndex
            .take(5)
            .map{ case ((preview, content), i) =>
              (i + 1, chapterLookup(i+1)._1, preview, content)
            }
        ),
        "freeOutlines" -> upickle.default.writeJs(
          web.chapterOutlines().map(
            _.flatten.collect{case (nums, name) if nums.split('.').length == 2 =>
              s"$nums $name"
            }
          )

        ),
        "previews" -> upickle.default.writeJs(
          chapters.zip(web.chapterHtmls()).map{
            case ((i, n, p), (coverHtml, bodyHtmlOpt)) => (i, n, coverHtml)
          }
        )
      )
    )
    mill.modules.Jvm.runSubprocess(
      "site.Main",
      runClasspath().map(_.path),
      forkArgs(),
      forkEnv(),
      workingDir = T.dest
    )

    PathRef(T.dest)
  }

  def publish() = T.command{
    for (item <- os.list(local().path)) os.copy.into(item, T.ctx().dest)

    os.write(T.ctx().dest / 'CNAME, "www.handsonscala.com")
    os.proc('git, 'init).call(cwd = T.ctx().dest, stdout = os.Inherit, stderr = os.Inherit)
    os.proc('git, 'add, "-A", ".").call(cwd = T.ctx().dest, stdout = os.Inherit, stderr = os.Inherit)
    os.proc('git, 'commit, "-am", "first commit").call(cwd = T.ctx().dest, stdout = os.Inherit, stderr = os.Inherit)
    os.proc('git, 'remote, 'add, 'origin, "git@github.com:lihaoyi/hands-on-scala-website.git").call(cwd = T.ctx().dest, stdout = os.Inherit, stderr = os.Inherit)
    os.proc('git, 'push, "-uf", 'origin, 'master).call(cwd = T.ctx().dest, stdout = os.Inherit, stderr = os.Inherit)
  }
}

def zip = T{
  PathRef(distribution.zip(dist.color.pdf().path, dist.print.pdf().path, reviewer(), T.dest))
}
val exampleInfo = for{
  (crossValues, partModule) <- dist.color.part.items
  (crossValues, chapterModule) <- partModule.chapter.items
  sub <- os.list(chapterModule.millSourcePath)
  Array(exNum, exName) <- Seq(sub.last.split(" - "))
} yield (s"${chapterModule.number}.$exNum", (chapterModule.number, exNum.toInt), sub)

val exampleInfoKeys = exampleInfo.sortBy(_._2).map(_._1)
val exampleInfoMap = exampleInfo.map(t => (t._1, t._3)).toMap
object example extends mill.Cross[ExampleModule](exampleInfoKeys:_*)
class ExampleModule(key: String) extends Module{
  def sources = T.source(exampleInfoMap(key))
  def test = T{ distribution.test(sources().path, T.dest) }
}

def reviewSample = T{
  os.copy(dist.color.pdf().path, T.dest / "color.pdf")
  os.copy(dist.print.pdf().path, T.dest / "grey.pdf")
  (PathRef(T.dest / "color.pdf"), PathRef(T.dest / "grey.pdf"))
}
