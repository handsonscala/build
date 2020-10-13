import java.io.File
import $file.util, util._
import org.commonmark.node._
import org.commonmark.parser.{Parser, PostProcessor}
import org.commonmark.renderer.html.{CoreHtmlNodeRenderer, HtmlNodeRendererContext, HtmlNodeRendererFactory, HtmlRenderer}
import scalatags.Text.all._
import org.commonmark.ext.gfm.tables.{TableBlock, TablesExtension}
import org.commonmark.ext.gfm.tables.internal.TableHtmlNodeRenderer
import org.commonmark.ext.autolink.AutolinkExtension

def makeParser() = {
  Parser.builder
    .extensions(java.util.Arrays.asList(
      TablesExtension.create(),
      AutolinkExtension.create()
    ))
    .build
}
def makeSimpleRenderer() = {
  HtmlRenderer.builder()
    .nodeRendererFactory(new HtmlNodeRendererFactory {
      def create(context: HtmlNodeRendererContext) = new CoreHtmlNodeRenderer(context){
        override def visit(node: BlockQuote): Unit = {
            val printer = context.getWriter()
            printer.raw("""<blockquote class="blockquote" style="font-style: italic; font-size: 16px; padding-left: 10px">""")
            super.visit(node)
            printer.raw("""</blockquote>""")

        }
      }
    })
    .build()
}
def renderGraphviz(str: String, check: Boolean) = {
  val tmp = os.temp(
    str
      .replace("fontcolor=green", "fontcolor=\"#00bb00\"")
      .replace("fontcolor=\"green\"", "fontcolor=\"#00bb00\"")
      .replace("fillcolor=green", "fillcolor=\"#00ff00\"")
      .replace("fillcolor=\"green\"", "fillcolor=\"#00ff00\"")
      .replaceAll("(?<![a-zA-Z0-9_])color=red", "color=red,penwidth=2.5")
      .replaceAll("(?<![a-zA-Z0-9_])color=\"red\"", "color=red,penwidth=2.5")
      .replaceAll("(?<![a-zA-Z0-9_])color=green", "color=\"#00bb00\",penwidth=3")
      .replaceAll("(?<![a-zA-Z0-9_])color=\"green\"", "color=\"#00bb00\",penwidth=3")
      .replaceAll("(?<![a-zA-Z0-9_])color=blue", "color=blue,penwidth=2")
      .replaceAll("(?<![a-zA-Z0-9_])color=\"blue\"", "color=blue,penwidth=2")
      .replaceFirst(
        "\\{",
        """{
                   graph [fontname = "helvetica"];
                   node [fontname = "helvetica"];
                   edge [fontname = "helvetica"];"""
      )
  )
  os.proc("dot", tmp, "-Tsvg")
    .call(check = check)
    .out
    .text()
    .replaceFirst("pt", "px")
    .replaceFirst("pt", "px")
    .replace(
      """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
        |<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN"
        | "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">""".stripMargin,
      ""
    )
}
def renderMarkdown(chapterNumber: Int,
                   txt: String,
                   highlight: (String, String) => String,
                   chapterNames: Map[Int, String],
                   web: Boolean,
                   partExists: String => Boolean,
                   allCodeExamples: Map[String, os.Path]): (Seq[(String, String)], String, String)= {
  val lines = txt.split("\n", -1).zipWithIndex
  for ((line, i) <- lines) {
    if (line.endsWith(" ") && line != "> ") throw new Exception(
      s"Trailing whitespace Chapter $chapterNumber line ${i+1}"
    )
  }

  val ast = makeParser().parse(txt)
  val headerVisitor = new HeaderVisitor(chapterNumber)
  ast.accept(headerVisitor)
  val customRenderer = new CustomRenderer(
    chapterNumber, highlight, headerVisitor.headers.to(Seq),
    chapterNames, web, partExists, allCodeExamples
  )
  val renderer = HtmlRenderer
    .builder
    .nodeRendererFactory(
      new org.commonmark.renderer.html.HtmlNodeRendererFactory() {
        override def create(context: org.commonmark.renderer.html.HtmlNodeRendererContext) = {
          new TableHtmlNodeRenderer(context) {
            override protected def renderBlock(tableBlock: TableBlock): Unit = {
              val htmlWriter = context.getWriter()
              import collection.JavaConverters._
              htmlWriter.line
              htmlWriter.tag("table", Map("class" -> "table table-sm").asJava)
              renderChildren(tableBlock)
              htmlWriter.tag("/table")
              htmlWriter.line
            }

            private def renderChildren(parent: Node): Unit = {
              var node = parent.getFirstChild
              while (node != null) {
                val next = node.getNext
                context.render(node)
                node = next
              }
            }
          }
        }
      }
    )
    .extensions(java.util.Arrays.asList(TablesExtension.create()))
    .nodeRendererFactory(customRenderer)
    .build

  var child = ast.getFirstChild
  while (!child.isInstanceOf[ThematicBreak]) child = child.getNext

  val buffer = collection.mutable.Buffer.empty[Node]
  while (child != null) {
    val next = child.getNext
    child.unlink()
    buffer.append(child)
    child = next
  }

  val preview = renderer.render(ast)

  child = ast.getFirstChild

  while (child != null) {
    val next = child.getNext
    child.unlink()
    child = next
  }
  for (b <- buffer.drop(1)) ast.appendChild(b)
  val bodyText = renderer.render(ast)
  (customRenderer.codeSnippets.to(Seq), preview, bodyText)
}

class HeaderVisitor(chapterNumber: Int) extends AbstractVisitor{

  val headers = collection.mutable.Buffer.empty[(String, Int, Seq[Int])]
  val headerStack = collection.mutable.Buffer(chapterNumber)
  override def visit(node: Heading): Unit = {

    val id = node.getFirstChild.asInstanceOf[Text].getLiteral
    if (id != "%stub") {
      val prevLevel = headers.lastOption.fold(0)(_._2)

      if (node.getLevel > prevLevel) headerStack.append(0)
      for (i <- Range(node.getLevel, prevLevel)) headerStack.remove(headerStack.length - 1)

      headerStack(headerStack.length - 1) += 1

      headers.append((id, node.getLevel(), headerStack.toList))
    }
  }

}

class CustomRenderer(chapterNumber: Int,
                     highlight0: (String, String) => String,
                     headers: Seq[(String, Int, Seq[Int])],
                     chapterNames: Map[Int, String],
                     web: Boolean,
                     partExists: String => Boolean,
                     allCodeExamples: Map[String, os.Path]) extends HtmlNodeRendererFactory() {
  var headerIndex = 0
  var codeSnippets = collection.mutable.Buffer.empty[(String, String)]
  val maxCodeWidthStack = collection.mutable.Buffer(90)
  def maxCodeWidth = maxCodeWidthStack.last
  def highlight(nodeType: String, txt: String, validateScala: Boolean): String = {
    val highlighted = highlight0(if (nodeType == "jsonnet") "json" else nodeType, txt)

    if (nodeType == "jsonnet") {
      highlighted
        .replace("(", """<span class="token punctuation">(</span>""")
        .replace(")", """<span class="token punctuation">)</span>""")
        .replace(";", """<span class="token punctuation">;</span>""")
        .replace("+", """<span class="token operator">+</span>""")
        .replace(" = ", """ <span class="token operator">=</span> """)
        .replace("local", """<span class="token keyword">local</span>""")
        .replace("function", """<span class="token keyword">function</span>""")
    }
    else if (!nodeType.contains("scala")) highlighted
    else {
      if (validateScala) {
        val banned = Seq(
          "){", "}else", "else{", "match{", "if(", "for(",
          "while(", "try{", "}catch", "catch{", "={", ")="
        )
        for (b <- banned) assert(!txt.contains(b), txt + "\n" + "INVALID STRING: " + b)
      }
      highlightStringInterpolations(highlighted, highlight("scala", _, validateScala))
    }
  }

  def create(context: HtmlNodeRendererContext) = new CoreHtmlNodeRenderer(context) {
    val printer = context.getWriter()

    override def visit(document: Document): Unit = {
      super.visit(document)
      if (openHeader != 0) printer.raw("</div>")
    }
    override def visit(node: Link): Unit = {
      node.getDestination match {
        case s"resources/$shortDest" =>
          def webLinkBase(segment: String) =
            s"https://github.com/handsonscala/handsonscala/$segment/v1/resources/$chapterNumber"

          node.setDestination(s"${webLinkBase("blob")}/$shortDest")
          node.getFirstChild.asInstanceOf[Text].setLiteral(shortDest)
          super.visit(node)

          printer.raw(i(s" (${webLinkBase("tree")})").render)
        case s"#$rest" =>

          val numString = headers
            .collectFirst { case (name, depth, nums) if sanitize(name) == rest => nums.mkString(".") }
            .getOrElse(throw new Exception(rest))
          if (!web) node.setDestination(s"https://localhost/section-link/$numString")
          else node.setDestination(s"#section-${numString.replace('.', '-')}-$rest")
          super.visit(node)
          printer.raw(" (" + i(numString) + ")")

        case _ => super.visit(node)
      }


    }
    override def visit(node: Code): Unit = {
      val frag = if (node.getLiteral.startsWith("%")) node.getLiteral match {
        case s"%O($txt)" => i(cls := s"language-scala", fontFamily := "Times New Roman")(s"O($txt)")
        case s"%Snippet $txt" => i(s"Snippet $txt")
        case s"%Chapter $n: $txt" =>
          val expected = chapterNames(n.toInt)
          assert(txt == expected, s"$txt != $expected")
          b(i(s"Chapter $n: $txt"))
        case s"%Chapter $txt" => b(i(s"Chapter $txt"))
        case s"%code $txt" =>
          code(cls := s"language-text", lineHeight := "87.5%", fontSize := "87.5%")(
            txt
          )
        case s"%Part $txt" =>
          assert(partExists(txt), txt)
          b(s"Part $txt")
      }else{
        code(cls := s"language-scala", lineHeight := "87.5%", fontSize := "87.5%")(
          raw(highlight("scala", node.getLiteral, false))
        )
      }

      printer.raw(frag.render)
    }
    override def visit(node: BlockQuote): Unit = {

      val literalFirstParagraph = node.getFirstChild match{
        case p: Paragraph =>
          p.getFirstChild match{
            case t: Text if t.getLiteral.startsWith("%") => Some(t.getLiteral)
            case _ => None
          }
        case _ => None
      }
      def exampleLink(suffix: String) = {
        val label = if (suffix.contains('.')) suffix else chapterNumber + "." + suffix
        assert(
          allCodeExamples.contains(label.takeWhile(_ != ' ')),
          s"${label.takeWhile(_ != ' ')} not in ${allCodeExamples.keys.toList.sorted}"
        )

        val encodedUrl =
          "https://github.com/handsonscala/handsonscala/tree/v1/examples/" +
          label.replace(" ", "%20")

        (label, encodedUrl)
      }
      literalFirstParagraph match{
        case Some(s"%code-example $suffix/$subpath") =>
          val (label, encodedUrl) = exampleLink(suffix)
          val base = allCodeExamples.getOrElse(
            label.takeWhile(_ != ' '),
            throw new Exception("Unknown code example: " + label)
          )
          val path = base / os.SubPath(subpath)

          val lines = os.read.lines(path)
          val filteredLines = lines.indexWhere(_.endsWith("// Test Suite")) match{
            case -1 => lines
            case n =>
              val indent = lines(n).takeWhile(_ == ' ')
              val closing = lines.indexWhere(_.takeWhile(_ == ' ') == indent, n + 1)
              lines.patch(n, Nil, closing - n + 1)
          }
          renderCode(
            path.ext match{
              case "sc" => "scala"
              case "txt" => "output"
              case ext => ext
            },
            filteredLines.mkString("\n"),
            Some(subpath)
          )
        case Some(s"%example $suffix") =>
          val (label, encodedUrl) = exampleLink(suffix)
          printer.raw(
            div(
              cls := "alert alert-primary",
              css("page-break-inside") := "avoid"
            )(
              b("See example ", a(href := encodedUrl)(label))
            ).render
          )
        case Some(s"%exercise $suffix") =>
          val (label, encodedUrl) = exampleLink(suffix)

          printer.raw("""<div class="alert alert-primary" style="page-break-inside: avoid">""")

          var current = node.getFirstChild.getNext
          val exercise = new StrongEmphasis()
          val exerciseText = new Text()
          exerciseText.setLiteral("Exercise: ")
          exercise.appendChild(exerciseText)
          current.prependChild(exercise)
          while(current != null){
            context.render(current)
            current = current.getNext
          }
          printer.raw(b("See example ", a(href := encodedUrl)(label)).render)
          printer.raw("""</div>""")

        case Some(s"%horizontal-$rest") =>
          val (leftWidth, rightWidth, line) = rest match {
            case s"$n-no-line" => (Some(n.toInt), Some(100 - n.toInt), false)
            case n => (Some(n.toInt), Some(100 - n.toInt), true)
          }

          val borderless = if (line) "" else "border-top: none"
          printer.raw(s"""<table class="table table-sm"><tbody>""")

          def styledCell(cellWidth: String, loneChild: String, left: Boolean) = {
            val noPadding = if (left) "padding-left: 0" else "padding-right: 0"
            s"""<td style="height: 1px; position: relative; width: $cellWidth; $borderless; $noPadding" $loneChild>"""
          }
          var child = node.getFirstChild.getNext
          val loneChild0 = if (child.getNext == null || child.getNext.isInstanceOf[ThematicBreak]) "class=\"lone-child\"" else ""

          printer.raw("<tr>" + styledCell(leftWidth.fold("auto")(_+"%"), loneChild0, left = true))
          var first = true
          var n = 0
          val outerMaxCodeWidth = maxCodeWidth
          maxCodeWidthStack.append(outerMaxCodeWidth * leftWidth.getOrElse(50) / 100 - 1)
          while (child != null) {
            if (child.isInstanceOf[ThematicBreak]) {
              n += 1
              val loneChild1 = if (child.getNext.getNext == null || child.getNext.getNext.isInstanceOf[ThematicBreak]) "class=\"lone-child\"" else ""

              if (n % 2 == 0 && n > 0) {
                maxCodeWidthStack(maxCodeWidthStack.length - 1) = outerMaxCodeWidth * leftWidth.getOrElse(50) / 100 - 1
                printer.raw("</td></tr><tr>" + styledCell(leftWidth.fold("auto")(_+"%"), loneChild1, left = true))
              } else {
                maxCodeWidthStack(maxCodeWidthStack.length - 1) = outerMaxCodeWidth * rightWidth.getOrElse(50) / 100 - 1
                printer.raw("</td>" + styledCell(rightWidth.fold("auto")(_+"%"), loneChild1, left = false))
              }
            }
            else context.render(child)
            first = false
            child = child.getNext
          }
          printer.raw("</td>")
          maxCodeWidthStack.remove(maxCodeWidthStack.length - 1)
          printer.raw("</tr></tbody></table>")
          if (openHeader == 2) openHeader = 3

        case None =>
          printer.raw("""<blockquote class="blockquote" style="font-style: italic; font-size: 16px; padding-left: 10px">""")
          super.visit(node)
          printer.raw("""</blockquote>""")
      }
    }
    override def visit(node: FencedCodeBlock): Unit = {
      val lines = node.getLiteral.split("\n", -1)
      assert(lines.head != "", "LEADING NEWLINE\n" + node.getLiteral)
      val trailingSpaceLine = lines.indexWhere(_.endsWith(" "))
      if (trailingSpaceLine != -1) {
        throw new Exception(s"TRAILING SPACES at line $trailingSpaceLine\n" + pprint.apply(lines))
      }
      assert(lines(lines.length - 2) != "", "TRAILING NEWLINE\n" + node.getLiteral) // 2nd last line
      node.getInfo match {
        case "graphviz" | "graphviz-error" =>

          printer.raw(
            div(
              paddingTop := 5,
              paddingBottom := 5,
              textAlign.center,
              maxWidth := "100%",
              overflow.scroll
            )(
              raw(
                renderGraphviz(node.getLiteral, node.getInfo != "graphviz-error")
                  .replaceFirst("<svg ", "<svg style=\"max-width: 100%\" ")
              )
            ).render
          )

        case nodeType =>

          val txt = node.getLiteral
          val (strippedTxt, nameTagOpt) =
            if (!txt.startsWith("// ")) (txt, None)
            else (txt.linesIterator.drop(1).mkString("\n"), Some(txt.linesIterator.next().drop(3)))

          renderCode(nodeType, strippedTxt, nameTagOpt)

      }
    }
    def renderCode(nodeType: String, strippedTxt: String, nameTagOpt: Option[String]) = {
      val noBreak = strippedTxt.linesIterator.size < 25

      val suffixNodeType = nodeType match {
        case "" => "txt"
        case s => s.stripPrefix("diff-")
      }

      if (strippedTxt.linesIterator.exists(_.length > maxCodeWidth)) {
        throw new Exception("Code snippet line too long in:\n" + strippedTxt)
      }

      val strippedLineCount = strippedTxt.linesIterator.size

      val snippetNumberTagOpt =
        if (strippedLineCount <= 1) None
        else {
          codeSnippets.append(strippedTxt -> suffixNodeType)
          Some(s"$chapterNumber.${codeSnippets.size}.$suffixNodeType")
        }

      val lastLineLength = strippedTxt.linesIterator.toSeq.last.length
      val newLineSuffix =
        if (lastLineLength > maxCodeWidth - snippetNumberTagOpt.fold(0)(_.length + 2)) {
          Some(span(display.block, height := 14))
        }
        else None

      printer.raw(
        pre(position.relative, if (noBreak) css("page-break-inside") := "avoid")(
          for (nameTag <- nameTagOpt) yield frag(
            span(cls := "code-snippet-name-tag")(code(nameTag)),
            if (nameTag.length + strippedTxt.linesIterator.next.length > maxCodeWidth) "\n"
          ),
          if (nodeType == "") code(cls := s"language-$nodeType diff-highlight")(strippedTxt)
          else renderChunkifiedCode(nodeType, strippedTxt),
          for (snippetNumberTag <- snippetNumberTagOpt)
          yield span(cls := "code-snippet-number-tag")(
            a(href := s"https://github.com/handsonscala/handsonscala/blob/v1/snippets/${snippetNumberTag}")(
              s"</> $snippetNumberTag"
            )
          ),
          newLineSuffix
        ).render
      )

    }
    def renderChunkifiedCode(nodeType: String, txt: String) = {

      val chunks = collection.mutable.Buffer.empty[String]
      var index = -1
      val prefixes = Seq("@", "\\$", "pg>")

      val matrix = prefixes.flatMap(p => Seq(s"^$p ", s"^$p\n", s"^$p$$"))
      val regex0 = ("(?m)" + matrix.mkString("|"))
      val regex = regex0.r

      def append(start: Int) = {
        chunks.append(txt.slice(index, start))
      }
      while ({
        val matched = regex.findFirstMatchIn(txt.drop(index + 1))
        matched match {
          case None => false
          case Some(firstMatch) =>
            append(firstMatch.start + index + 1)

            index = firstMatch.start + index + 1
            true
        }
      })()

      append(txt.length)
      code(cls := s"language-$nodeType diff-highlight")(
        if (chunks.size == 1) {
          if (txt.linesIterator.exists(l => l.startsWith("+ ") || l.startsWith("- "))) {
            raw(highlight(nodeType, chunks(0), true))
          } else if (nodeType == "output") {
            i(chunks(0))
          } else if (nodeType == "error") {
            i(color := "red")(chunks(0))
          } else if (nodeType.startsWith("output-")) {
            i(raw(highlight(nodeType.stripPrefix("output-"), chunks(0), true)))
          } else {
            b(raw(highlight(nodeType, chunks(0), true)))
          }
        } else for (s <- chunks.to(Seq)) yield s.take(2) match {
          case "$ " | "$\n" | "$" => splitHighlight(nodeType, s, false)
          case "@ " | "@\n" | "@" => splitHighlight(nodeType, s, true)
          case _ =>
            s.take(4) match {
              case "pg> " | "pg>" | "pg>\n" => splitHighlight(nodeType, s, true)
              case _ => raw(highlight(nodeType, s, true))
            }
        }
      )
    }


    def splitHighlight(nodeType: String,
                       chunk: String,
                       highlightOutput: Boolean): Frag = {

      val lines = chunk.split("\n", -1)
      lines.drop(1).indexWhere(l => l != "" && !l.startsWith("  ")) match {
        case -1 => b(raw(highlight(nodeType, chunk, true)))
        case n =>
          val (before, after) = lines.splitAt(n + 1)
          frag(
            b(raw(highlight(nodeType, before.mkString("\n"), true))),
            "\n",
            if (!highlightOutput) i(after.mkString("\n"))
            else {
              val isFailure = after.exists(line =>
                line.endsWith("Exception") ||
                line.contains("Exception: ") ||
                line.contains("Exception:") ||
                line.contains("Error") ||
                line.contains("Compilation Failed")
              )
              if (isFailure) i(color := "red", after.mkString("\n"))
              else if (nodeType == "sql") raw(highlight(nodeType, after.mkString("\n"), false))
              else i(raw(highlight(nodeType, after.mkString("\n"), false)))
            }
          )
      }
    }

    var openHeader = 0

    override def visit(node: Heading): Unit = {

      val tag = "h" + node.getLevel()

      val id = node.getFirstChild.asInstanceOf[Text].getLiteral

      if (id == "%stub") {
        printer.raw(s"""<$tag style="opacity: 0">stub</$tag>""")
      } else {
        val headerStack = headers(headerIndex)._3

        val sanitizedId = sanitize(headerStack.mkString("-") + "-" + id)
        if (!web && node.getParent.isInstanceOf[Document] && openHeader == 0) {
          printer.raw(s"""<div style="page-break-inside: avoid">""")
          if(node.getLevel <= 3) openHeader = 1
          else openHeader = 2
        }
        printer.raw(s"""<$tag id="section-$sanitizedId">""")
        if (web) printer.raw(s"""<a href="#section-$sanitizedId">""")
        printer.raw(headerStack.mkString("."))
        if (web) printer.raw(s"""</a>""")
        printer.raw(" ")
        visitChildren(node)
        printer.raw(
          a(
            href := s"https://localhost/section-header/${headerStack.mkString(".")} $id".replace(" ", "%20"),
            width := 1,
            height := 1,
            display.`inline-block`
          ).render
        )
        printer.raw(s"</$tag>")

        headerIndex += 1
      }
    }

    override def visitChildren(parent: Node): Unit = {
      var node = parent.getFirstChild

      while (node != null) {
        val next = node.getNext
        if (!web && node.getParent.isInstanceOf[Document]) {
          openHeader match {
            case 0 => // do nothing
            case 1 =>
              node match {
                case h: Heading =>
                  val prevLevel = h.getPrevious.asInstanceOf[Heading].getLevel
                  assert(
                    h.getLevel > prevLevel,
                    s"Chapter $chapterNumber ${h.getFirstChild.toString} ${h.getLevel} ${h.getPrevious.getFirstChild.toString} ${prevLevel}"
                  )
                case _ => openHeader = 2
              }
            case 2 =>
              node match {
                case h: Heading =>
                  if (h.getLevel <= h.getPrevious.getPrevious.asInstanceOf[Heading].getLevel) {
                    printer.raw("</div>")
                    openHeader = 0
                  } else {
                    // do nothing
                  }

                case _ => openHeader = 3
              }
            case 3 =>
              printer.raw("</div>")
              openHeader = 0
          }
        }
        context.render(node)
        node = next
      }
    }
  }
}

def highlightStringInterpolations(highlightedHtml: String, highlight: String => String): String = {
  import org.jsoup._, collection.JavaConverters._

  val parsed = Jsoup.parse("<pre>\n" + highlightedHtml + "</pre>")
  parsed.outputSettings(new nodes.Document.OutputSettings().prettyPrint(false));

  val strings = parsed.select("code.token.string").asScala

  for{
    str <- strings
    if str.html().head == '"'
    if Option(str.previousSibling()).exists(_.outerHtml().endsWith("s"))
  } {

    import fastparse._, NoWhitespace._

    def plain[_: P] = P( CharsWhile(_ != '$') ).!.map(Left(_))
    def ident[_: P] = P( "$".! ~ (CharIn("a-zA-Z_") ~ CharsWhileIn("a-zA-Z0-9_", 0)).! ).map(Right(_))
    def bracketed[_: P] = P( "$".! ~ ("{" ~ CharsWhile(_ != '}') ~ "}").! ).map(Right(_))
    def splitter[_: P] = P( plain | ident | bracketed ).rep() ~ End
    val chunks = fastparse.parse(str.text(), splitter(_)).get.value

    chunks.foreach{
      case Left(s) =>
        val n = new nodes.Element("span")
        n.addClass("token")
        n.addClass("string")
        n.text(s)
        str.before(n)
      case Right((dollar, interpolated)) =>
        val n = new nodes.Element("span")
        n.addClass("token")
        n.addClass("operator")
        n.text("$")
        str.before(n)
        for (c <- Jsoup.parse(highlight(interpolated)).body().childNodes().asScala.toList) {
          str.before(c)
        }
    }
    str.remove()

  }

  parsed.body().child(0).html()
}
