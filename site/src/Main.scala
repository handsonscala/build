package site
object Main{
  def main(args: Array[String]): Unit = {
    val args = ujson.read(os.read.stream(os.pwd / "args.json"))
    val freeChapters = upickle.default.read[Seq[(Int, String, String, String)]](args("freeChapters"))
    val freeOutlines = upickle.default.read[Seq[Seq[String]]](args("freeOutlines"))
    val previews = upickle.default.read[Seq[(Int, String, String)]](args("previews"))
    val partCovers = upickle.default.read[Seq[String]](args("partCovers"))
    val forewordPages = upickle.default.read[Seq[String]](args("forewordPages"))
    val metadataPages = upickle.default.read[Seq[String]](args("metadataPages"))
    val tableOfContentsPages = upickle.default.read[Seq[String]](args("tableOfContentsPages"))

    def chapterLabel(chapterNum: Int, chapterName: String) = {
      s"Chapter $chapterNum: $chapterName"
    }
    val freeChapterListing = {
      util.Util.parts.take(1) ++
      freeChapters.map { case (num, name, _, _) => chapterLabel(num, name)} ++
      util.Util.parts.drop(1)
    }
    val fullFreeListing = Seq("Table of Contents", "Foreword") ++ freeChapterListing
    val partsListing = freeChapterListing.filter(_.startsWith("Part "))
    os.write(
      os.pwd / "index.html",
      Index.render(fullFreeListing, previews)
    )
    import scalatags.Text.all._
    os.write(
      os.pwd / "chat.html",
      doctype("html")(
        html(
          head(
            meta(
              attr("http-equiv") := "refresh",
              attr("content") := "0; URL='https://gitter.im/handsonscala/community'"
            )
          )
        )
      )
    )
    os.write(
      os.pwd / "discuss" / "index.html",
      doctype("html")(
        html(
          head(
            meta(
              attr("http-equiv") := "refresh",
              attr("content") := "0; URL='https://github.com/handsonscala/handsonscala/issues?q=label%3Adiscussion'"
            )
          )
        )
      ),
      createFolders = true
    )
    for(i <- Range.inclusive(1, 20)){
      os.write(
        os.pwd / "discuss" / s"$i.html",
        doctype("html")(
          html(
            head(
              meta(
                attr("http-equiv") := "refresh",
                attr("content") := s"0; URL='https://github.com/handsonscala/handsonscala/issues/$i'"
              )
            )
          )
        ),
        createFolders = true
      )
    }

    os.write.over(
      os.pwd / "foreword.html",
      Chapter.render(
        forewordPages.map(_ -> false),
        fullFreeListing,
        "Foreword",
        Seq(
          "Author's Note",
          "Metadata",
        )
      )
    )
    os.write.over(
      os.pwd / "table-of-contents.html",
      Chapter.render(
        tableOfContentsPages.map(_ -> true) ++ metadataPages.map(_ -> true),
        fullFreeListing,
        "Table of Contents",
        util.Util.parts
      )
    )

    for (((chapterNum, chapterName, rawPreview, rawContent), chapterOutline) <- freeChapters.zip(freeOutlines)) {
      os.write(
        os.pwd / (Util.chapterHtmlName(chapterLabel(chapterNum, chapterName))),
        Chapter.render(
          Seq(rawPreview -> false, rawContent -> true),
          fullFreeListing,
          chapterLabel(chapterNum, chapterName),
          chapterOutline
        )
      )
    }

    for(((partLabel, partCover), i) <- partsListing.zip(partCovers).zipWithIndex) {
      val relevantPreviews = if(i == 0) Nil else previews.slice(5 * i, 5 * i + 5)

      os.write(
        os.pwd / Util.chapterHtmlName(partLabel),
        Chapter.render(
          if (relevantPreviews.isEmpty) Seq(partCover -> false)
          else {
            Seq(partCover -> false) ++
            relevantPreviews.map { case (num, name, txt) =>
              div(id := util.Util.sanitize(chapterLabel(num, name))) + txt -> false
            }
          },
          fullFreeListing,
          partLabel,
          relevantPreviews.map { case (num, name, txt) => s"Chapter $num: $name" },
        )
      )
    }
  }
}
