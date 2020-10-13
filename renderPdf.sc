import mill._
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.{PDNamedDestination, PDPageXYZDestination}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.{PDDocumentOutline, PDOutlineItem, PDOutlineNode}
import org.apache.pdfbox.util.Matrix

case class RenderedOutput(pdf: PathRef,
                          outline: Seq[Seq[(String, String)]])
implicit val renderedOutputRW: upickle.default.ReadWriter[RenderedOutput] = upickle.default.macroRW

def getPdfOutline(src: os.Path) = {
  import collection.JavaConverters._
  val doc = org.apache.pdfbox.pdmodel.PDDocument.load(src.toIO)
  try doc.getPages.asScala.map(p =>
    p.getAnnotations.asScala.collect{
      case a: org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink =>
        a.getAction match {
          case ac: org.apache.pdfbox.pdmodel.interactive.action.PDActionURI =>
            ac.getURI match {
              case s"https://localhost/section-header/$rest" =>
                val Array(numbering, title) = rest.replace("%20", " ").split(" ", 2)
                Some((numbering, title))
              case _ => None
            }

          case _ => None
        }
    }.flatten.toSeq
  ) finally doc.close()
}

def foreachOutlineChild(item: PDOutlineNode)(f: PDOutlineItem => Unit) = {
  var child = item.getFirstChild
  while (child != null) {
    val next = child.getNextSibling
    f(child)
    child = next
  }
}

def updatePdfPageOffsets(inputs: Seq[os.Path]) = {
  var currentPageOffset: Int = 0
  for (input <- inputs) {
    val pdf: PDDocument = PDDocument.load(os.read.bytes(input))
    val catalog = pdf.getDocumentCatalog
    val outline: PDDocumentOutline = catalog.getDocumentOutline
    def rec(item: PDOutlineItem): Unit = {
      val dest = item.getDestination match {
        case named: PDNamedDestination =>
          catalog
            .getDests
            .getDestination(named.getNamedDestination)
            .asInstanceOf[PDPageXYZDestination]
        case dest: PDPageXYZDestination => dest
      }
      dest.setPageNumber(dest.getPageNumber + currentPageOffset + 1)

      item.setDestination(dest)

      foreachOutlineChild(item)(rec)
    }
    if (outline != null) foreachOutlineChild(outline)(rec)
    currentPageOffset += pdf.getNumberOfPages
    pdf.save(input.toIO)
  }
}

def mergePdfs(inputs: Seq[os.Path], outPath: os.Path)
             (implicit ctx: mill.api.Ctx.Dest): PathRef = {
  val merger = new org.apache.pdfbox.multipdf.PDFMergerUtility
  for (input <- inputs) merger.addSource(input.toIO)
  val out = os.write.outputStream(outPath)
  try {
    merger.setDestinationStream(out)
    merger.mergeDocuments()
  } finally out.close()
  PathRef(outPath)
}

def checkExternalLinks(source: os.Path) = {
  val doc = org.apache.pdfbox.pdmodel.PDDocument.load(os.read.bytes(source))
  import collection.JavaConverters._
  val optFuturesIter =
    for {
      (page, i) <- doc.getPages.iterator().asScala.zipWithIndex
      annot <- page.getAnnotations.asScala
    } yield {
      val l = annot.asInstanceOf[org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink]
      val a = l.getAction.asInstanceOf[org.apache.pdfbox.pdmodel.interactive.action.PDActionURI]
      if (a == null) None
      else {
        a.getURI match{
          case s"page-link://$rest" => None
          case s"https://localhost/$rest" => None
          case s"mailto:$rest" => None
          case url =>
            println(s"checking URI page $i $url")
            def getSleep(sleep: Int) = {
              if (sleep != 0) println(s"Retrying after $sleep ms")
              Thread.sleep(sleep)
              requests.get(url, check = false)
            }
            val resp = getSleep(0)
            val resp2 = if (resp.is2xx) resp else getSleep(5000)
            val resp3 = if (resp2.is2xx) resp2 else getSleep(10000)
            val resp4 = if (resp3.is2xx) resp2 else getSleep(20000)
            println(resp4.statusCode)
            Some((i, url, resp4))
        }
      }
    }
  val responses = optFuturesIter.flatten.toList

  val failures = responses.filter(!_._3.is2xx)
  assert(failures.isEmpty, failures.map{ case (i, url, resp) => (i, url, resp.statusCode)}.mkString("\n"))
}

def checkPdfOutline(source: os.Path,
                    headerPages: Seq[(String, String, Int)]) = {
  import org.apache.pdfbox.text.PDFTextStripper
  val stripper =  new PDFTextStripper()

  val pdf: PDDocument = PDDocument.load(os.read.bytes(source))
  createPdfOutline(pdf, headerPages)
  val catalog = pdf.getDocumentCatalog
  val outline: PDDocumentOutline = catalog.getDocumentOutline
  val missing = collection.mutable.ArrayDeque.empty[(String, Int)]
  def rec(item: PDOutlineItem): Unit = {
    val pageNum = item.getDestination.asInstanceOf[PDPageXYZDestination].getPageNumber
    stripper.setStartPage(pageNum + 1)
    stripper.setEndPage(pageNum + 1)
    val txt = stripper.getText(pdf).replace("\n", " ")
    if (!txt.contains(item.getTitle)){
      missing.append(item.getTitle -> pageNum)
    }
    foreachOutlineChild(item)(rec)
  }
  foreachOutlineChild(outline)(rec)
  assert(missing.isEmpty, missing.mkString("\n"))
}

def createPdfOutline(doc: PDDocument,
                     headerPages: Seq[(String, String, Int)]) = {
  val chapterSubsectionPages = headerPages.map{ case (nums, name, v) => (nums, v)}.toMap


  import collection.JavaConverters._
  for (page <- doc.getPages.iterator().asScala; annot <- page.getAnnotations.asScala) {
    val l = annot.asInstanceOf[org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink]
    val a = l.getAction.asInstanceOf[org.apache.pdfbox.pdmodel.interactive.action.PDActionURI]
    if (a != null) {
      def mangleLinkOpt(handler: PartialFunction[String, Int]) = {
        handler.lift(a.getURI).map{ linkPageNumber =>
          l.setAction(null)
          val dest = new PDPageXYZDestination()

          dest.setPageNumber(linkPageNumber)
          l.setDestination(dest)
        }
      }
      mangleLinkOpt{case s"page-link://$rest" => rest.toInt}
      mangleLinkOpt{case s"https://localhost/section-link/$rest" => chapterSubsectionPages(rest)}
    }
  }
  val outline = new PDDocumentOutline()

  outline.openNode()
  val outlineItemStack = collection.mutable.ArrayDeque.empty[PDOutlineItem]
  for((num, name, page) <- headerPages){
    val depth = if (num == "") 0 else num.split('.').length
    val outlineItem = new PDOutlineItem()
    val dest = new PDPageXYZDestination()
    dest.setPageNumber(page)
    outlineItem.setDestination(dest)
    outlineItem.setTitle(if (num == "") name else num + " " + name)

    if (depth == 0) outline.addLast(outlineItem)
    else {
      while (depth <= outlineItemStack.length) outlineItemStack.removeLast()
      outlineItemStack.lastOption match{
        case None => outline.addLast(outlineItem)
        case Some(last) => last.addLast(outlineItem)
      }
      outlineItemStack.append(outlineItem)
    }
  }

  doc.getDocumentCatalog.setDocumentOutline(outline)
  doc
}

def createPdfFooter(doc: PDDocument,
                    headerPages: Seq[(String, String, Int)],
                    marginMM: Float,
                    spineOffsetMM: Option[Float]) = {
  import org.apache.pdfbox.pdmodel.PDPageContentStream
  import org.apache.pdfbox.pdmodel.font.PDType0Font
  val flatHeaderPages = headerPages.filter(!_._1.contains('.'))
  val pointsPerMM = 72 / 25.4F
  val marginUnits = marginMM * pointsPerMM
  for (pageNum <- Range(0, doc.getNumberOfPages)) {
    val page = doc.getPage(pageNum)
    for((headerNum, headerName, headerPage) <- flatHeaderPages.findLast(_._3 <= pageNum)) {
      val contentStream = new PDPageContentStream(
        doc,
        page,
        PDPageContentStream.AppendMode.APPEND,
        true,
        true
      )
      val font = PDType0Font.load(doc, os.read.inputStream(os.pwd / "resources" / "times-it.ttf"))


      val fontSize = 10
      contentStream.setFont(font, fontSize)


      if (headerPage != pageNum && headerNum != "") {
        contentStream.beginText()
        contentStream.newLineAtOffset(marginUnits, marginUnits)
        contentStream.showText(s"Chapter $headerNum $headerName")
        contentStream.endText()
      }
      if (headerName != "Table of Contents") {
        contentStream.beginText()
        val pageNumString = (pageNum + 1).toString
        val textWidth = (font.getStringWidth(pageNumString) / 1000.0f) * fontSize
        contentStream.newLineAtOffset(page.getMediaBox.getUpperRightX - textWidth - marginUnits, marginUnits)
        contentStream.showText(pageNumString)
        contentStream.endText()
      }
      contentStream.close()
    }

    for(offset <- spineOffsetMM) {
      val contentStream = new PDPageContentStream(
        doc,
        page,
        PDPageContentStream.AppendMode.PREPEND,
        false // compress
      );
      contentStream.transform(
        Matrix.getTranslateInstance((if (pageNum % 2 == 0) 1 else -1) * offset * pointsPerMM, 0)
      )
      contentStream.close()
    }

  }
}

def finalizePdf(source: os.Path,
                dest: os.Path,
                headerPages: Seq[(String, String, Int)],
                marginMM: Float,
                spineOffsetMM: Option[Float]) = {

  val doc = org.apache.pdfbox.pdmodel.PDDocument.load(os.read.bytes(source))
  try{
    createPdfOutline(doc, headerPages)
    createPdfFooter(doc, headerPages, marginMM, spineOffsetMM)
    doc.save(dest.toIO)
  }finally doc.close()
}
