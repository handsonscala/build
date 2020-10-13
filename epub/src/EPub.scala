package epub

import nl.siegmann.epublib.domain.{Author, Book, Resource}
import nl.siegmann.epublib.epub.EpubWriter
import scalatags.Text.all._
object EPub{
  def main(args: Array[String]): Unit = {
    os.write(
      os.pwd / "page.html",
      upickle.default.read[EPub](os.read(os.pwd / "in.json")).run().render
    )
  }
  implicit val rw: upickle.default.ReadWriter[EPub] = upickle.default.macroRW
}
case class EPub(coverImage0: Option[String],
                dest0: String,
                stylesheets0: Seq[String],
                images0: Seq[String],
                chapters: Seq[String],
                metadata: String,
                foreword: String,
                authorsNote: String,
                chapterNames: Seq[String],
                tableOfContents: String,
                partCovers: Seq[String],
                conclusion: String) {
  val dest = os.Path(dest0)
  val coverImage = coverImage0.map(os.Path(_))
  val stylesheets = stylesheets0.map(os.Path(_))
  val images = images0.map(os.Path(_))
  def run() = {


    val book = new Book

    // Set the title
    book.getMetadata.addTitle("Hands-on Scala Programming")

    // Add an Author
    book.getMetadata.addAuthor(new Author("Haoyi", "Li"))

    // Set cover image
    for(c <- coverImage) book.setCoverImage(new Resource(os.read.bytes(c), "cover.png"))

    // Add Chapters
    def addSection(title: String, contents: String) = {
      book.addSection(title, new Resource(contents.getBytes, util.Util.sanitize(title) + ".html"))
    }
    addSection("Metadata", metadata)
    addSection("Chapter Table of Contents", tableOfContents)
    addSection("Foreword", foreword)
    addSection("Author's Note", authorsNote)

    val partGroupedChapters = chapters
      .zipWithIndex
      .zip(chapterNames)
      .grouped(5)
      .zip(partCovers.toIterator)
      .zipWithIndex

    for(((partChapters, partContents), partIndex) <- partGroupedChapters){
      addSection(util.Util.parts(partIndex), partContents)
      for(((chapter, i), chapterName) <- partChapters){
        addSection(s"Chapter ${i + 1}: $chapterName", chapter)
      }
    }

    addSection("Conclusion", conclusion)

    // Add css file
    for(stylesheet <- stylesheets){
      book.getResources.add(new Resource(os.read.bytes(stylesheet), stylesheet.last))
    }
    for(image <- images){
      book.getResources.add(new Resource(os.read.bytes(image), "images/" + image.last))
    }
//
//    // Add Chapter 2
//    val chapter2 = book.addSection("Second Chapter", new Nothing(classOf[Nothing].getResourceAsStream("/book1/chapter2.html"), "chapter2.html"))
//
//    // Add image used by Chapter 2
//    book.getResources.add(new Nothing(classOf[Nothing].getResourceAsStream("/book1/flowers_320x240.jpg"), "flowers.jpg"))
//
//    // Add Chapter2, Section 1
//    book.addSection(chapter2, "Chapter 2, section 1", new Nothing(classOf[Nothing].getResourceAsStream("/book1/chapter2_1.html"), "chapter2_1.html"))
//
//    // Add Chapter 3
//    book.addSection("Conclusion", new Nothing(classOf[Nothing].getResourceAsStream("/book1/chapter3.html"), "chapter3.html"))

    // Create EpubWriter
    val epubWriter = new EpubWriter

    // Write the Book as Epub
    val out = os.write.outputStream(dest)
    try epubWriter.write(book, out)
    finally out.close()
  }
}
