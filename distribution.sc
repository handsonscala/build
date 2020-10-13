import $file.util, util._
def uploadGithub(examplePaths: Seq[os.Path],
                 resources: Seq[os.Path],
                 codeSnippets: Seq[(Int, Seq[(String, String)])],
                 dest: os.Path,
                 chapterNames: Seq[String],
                 partNames: Seq[String]) = {
  val dests = for (examplePath <- examplePaths) yield {
    val Array(chapterNum, chapterName) = (examplePath / os.up).last.split(" - ")
    val Array(exampleNum, exampleName) = examplePath.last.split(" - ")
    val exampleDest = dest / "examples" / s"$chapterNum.$exampleNum - $exampleName"
    os.copy(examplePath, exampleDest, createFolders = true)
    val readmeLines = os.read.lines(exampleDest / "readme.md")
    os.remove(exampleDest / "readme.md")
    (exampleDest, chapterNum.toInt, exampleNum.toInt, readmeLines)
  }

  val exampleBaseUrl = "https://github.com/handsonscala/handsonscala/tree/v1/examples"

  val diffLinks = collection.mutable.ArrayDeque.empty[(String, String)]
  val mangled = for((exampleDest, chapterNum, exampleNum, readmeLines) <- dests) yield{
    val diffIndex = readmeLines.indexOf("```diff")
    if (diffIndex == -1) (exampleDest, readmeLines.mkString("\n"))
    else {

      val diffSource = readmeLines(diffIndex + 1) match{
        case s"$sourceChapter.$sourceExample - $sourceName" => s"$sourceChapter.$sourceExample - $sourceName"
        case s"$sourceExample - $sourceName" => s"$chapterNum.$sourceExample - $sourceName"
      }
      diffLinks.append((diffSource, exampleDest.last))
      pprint.log(exampleDest / os.up)
      pprint.log(diffSource)
      pprint.log(exampleDest.last)

      val stdout =
        os.proc("git", "diff", "--minimal", "--ignore-space-change", "--no-index", diffSource, exampleDest.last)
          .call(cwd = exampleDest / os.up, check = false)
          .out
          .lines()

      val url = s"$exampleBaseUrl/$diffSource"
      val prefix = Seq(s"## Upstream Example: [${diffSource}](${url.replace(" ", "%20")}):", "Diff:")
      val readmeIndex = stdout.indexWhere(l => l.contains("diff --git") && l.contains("readme.md"))
      pprint.log(readmeIndex)
      val filtered = readmeIndex match {
        case -1 => stdout
        case n =>
          val replacedLength = stdout.indexWhere(_.contains("diff --git"), n + 1) match {
            case -1 => stdout.length - n
            case end => end - n
          }
          pprint.log(replacedLength)
          stdout.patch(n, Nil, replacedLength)
      }

      (exampleDest, readmeLines.patch(diffIndex, prefix ++ Seq("```diff") ++ filtered, 2).mkString("\n"))
    }
  }
  for((exampleDest, mangledReadme) <- mangled){
    val links =
      for((upstream, downstream) <- diffLinks if upstream == exampleDest.last)
      yield s"[$downstream]($exampleBaseUrl/${downstream.replace(" ", "%20")})"

    val linksSnippet =
      if (links.isEmpty) ""
      else "## Downstream Examples\n\n" + links.map("- " + _).mkString("\n")

    os.write.over(
      exampleDest / "readme.md",
      s"# Example ${exampleDest.last}\n" +
      mangledReadme + "\n" +
      linksSnippet
    )

  }

  for (resourceRoot <- resources if os.exists(resourceRoot)) {
    val Array(chapterNum, chapterName) = (resourceRoot / os.up).last.split(" - ")
    os.copy(
      resourceRoot,
      dest / "resources" / s"$chapterNum",
      createFolders = true
    )
  }
  for ((chapterNumber, snippets) <- codeSnippets) {
    for (((snippet, nodeType), i) <- snippets.zipWithIndex) {
      os.write(dest / "snippets" / s"$chapterNumber.${i+1}.$nodeType", snippet, createFolders = true)
    }
  }
  os.write(
    dest / "LICENSE",
    """License
      |=======
      |
      |The MIT License (MIT)
      |
      |Copyright (c) 2020 Li Haoyi (haoyi.sg@gmail.com)
      |
      |Permission is hereby granted, free of charge, to any person obtaining a copy
      |of this software and associated documentation files (the "Software"), to deal
      |in the Software without restriction, including without limitation the rights
      |to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
      |copies of the Software, and to permit persons to whom the Software is
      |furnished to do so, subject to the following conditions:
      |
      |The above copyright notice and this permission notice shall be included in
      |all copies or substantial portions of the Software.
      |
      |THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
      |IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
      |FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
      |AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
      |LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
      |FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
      |DEALINGS IN THE SOFTWARE.""".stripMargin
  )

  os.write(
    dest / "readme.md",
    os.read(os.pwd / "Blurbs" / "github-readme.md") +
    chapterNames
      .zipWithIndex
      .grouped(5)
      .zipWithIndex
      .flatMap{case (group, groupIndex) =>
        val partSuffix = if (groupIndex== 0) "" else "-preview"
        Seq(s"## [Part ${partNames(groupIndex)}](https://www.handsonscala.com/part-${sanitize(partNames(groupIndex))}$partSuffix.html)") ++
        group.flatMap{case (name, i0) =>
          val i = i0 + 1
          val relevantExamples = dests.filter(_._1.last.startsWith(i + "."))

          val chapterUrl =
            if (i <= 5) s"https://www.handsonscala.com/chapter-$i-${sanitize(name)}.html"
            else s"https://www.handsonscala.com/part-${sanitize(partNames(groupIndex))}-preview.html#chapter-$i-${sanitize(name)}"
          Seq(s"### [Chapter $i: $name]($chapterUrl)") ++
          relevantExamples
            .sortBy(_._3)
            .map{ case (p, _, _, readmeLines) =>
              val readmeSummaryLines = readmeLines.takeWhile(_.nonEmpty)
              s"- [${p.last}]($exampleBaseUrl/${p.last.replace(" ", "%20")}): " + readmeSummaryLines.mkString(" ")
            }
        }
      }
      .mkString("\n")
  )

  os.proc("git", "init").call(cwd = dest, stdout = os.Inherit)
  os.proc("git", "add", "-A").call(cwd = dest, stdout = os.Inherit)
  os.proc("git", "commit", "-am", "update").call(cwd = dest, stdout = os.Inherit)
  val remote = "git@github.com:handsonscala/handsonscala.git"
  os.proc("git", "push", remote, "head:v1", "-f").call(cwd = dest, stdout = os.Inherit)

}

def zip(colorPdf: os.Path, greyPdf: os.Path, reviewer: Option[String], dest: os.Path) = {
  os.copy(colorPdf, dest / s"hands-on-scala-${reviewer.getOrElse("")}-color.pdf")
  os.copy(greyPdf, dest / s"hands-on-scala-${reviewer.getOrElse("")}-grey.pdf")
  os.proc(
    "zip", dest / "hands-on-scala.zip",
    s"hands-on-scala-${reviewer.getOrElse("")}-color.pdf",
    s"hands-on-scala-${reviewer.getOrElse("")}-grey.pdf"
  ).call(cwd = dest)
  dest / "hands-on-scala.zip"
}
def test(sources: os.Path, dest: os.Path) = {
  for(src <- os.walk(sources)){
    if (src.ext == "sc" || src.ext == "scala"){
      if (os.read(src).last != '\n') throw new Exception(s"$src has no trailing newline")
      if (os.read(src).endsWith("\n\n")) throw new Exception(s"$src has two trailing newlines")
      if (os.read(src).contains("\n\n\n")) throw new Exception(s"$src has three consecutive newlines")
    }
  }
  os.copy.over(sources, dest)
  if (os.exists(dest / "readme.md")) {
    val cmds = os.read.lines(dest / "readme.md")
      .dropWhile(_ != "```bash")
      .drop(1)
      .takeWhile(_ != "```")

    for (cmd <- cmds) {
      pprint.log(cmd)
      os.proc("bash", "-c", cmd)
        .call(cwd = dest, stdout = os.Inherit, env = sys.env + ("COMPILE_ONLY" -> "true"))
    }
  }
}