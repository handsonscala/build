val raw = javax.imageio.ImageIO.read(os.read.inputStream(os.pwd / "cover" / "Mockup.jpg"))
val blanked = javax.imageio.ImageIO.read(os.read.inputStream(os.pwd /  "cover" / "Blanked.png"))
val model = raw.getColorModel
val width = raw.getWidth
val height = raw.getHeight
val originalColor = new java.awt.Color(raw.getRGB(0, 0))
val colorScale = 1.0 / (originalColor.getRed + originalColor.getGreen + originalColor.getBlue)
val queue = collection.mutable.Queue((0, 0))

val cooked = new java.awt.image.BufferedImage(
  raw.getWidth,
  raw.getHeight,
  java.awt.image.BufferedImage.TYPE_4BYTE_ABGR
)

val max = raw.getHeight * raw.getWidth
var n = 0
val seen = new collection.mutable.BitSet()
while (queue.nonEmpty) {
  val (currX, currY) = queue.dequeue()
  n += 1
  if (n % 1000 == 0) println(n + "/" + max)
  val currColor = new java.awt.Color(raw.getRGB(currX, currY))

  val cRed = currColor.getRed
  val cGreen = currColor.getGreen
  val cBlue = currColor.getBlue
  val alpha = 1.0 - (cRed + cGreen + cBlue) * colorScale

  cooked.setRGB(
    currX,
    currY,
    new java.awt.Color(0, 0, 0, math.max(0, (alpha * 255).toInt)).getRGB
  )
  def propagate(dx: Int, dy: Int) = {
    val (nextX, nextY) = (currX + dx, currY + dy)

    if (nextX >= 0 && nextY >= 0 && nextX < width && nextY < height) {


      val seenIndex = nextY * width + nextX
      if (!seen.contains(seenIndex)) {
        val nextColor = new java.awt.Color(raw.getRGB(nextX, nextY))
        seen.add(seenIndex)
        val blankedColor = new java.awt.Color(blanked.getRGB(nextX, nextY))
        val nRed = nextColor.getRed
        val nGreen = nextColor.getGreen
        val nBlue = nextColor.getBlue
        if (math.abs(cBlue - nBlue) < 30 &&
            math.abs(cRed - nRed) < 30 &&
            math.abs(cGreen - nGreen) < 30 &&
            math.abs(nBlue - nGreen) < 30 &&
            math.abs(nGreen - nRed) < 30 &&
            math.abs(nRed - nBlue) < 30 &&
            (blankedColor.getRed < 220 || blankedColor.getBlue > 40)) {

          queue.enqueue((nextX, nextY))
        }
      }
    }
  }

  propagate(-1, 0)
  propagate(1, 0)
  propagate(0, -1)
  propagate(0, 1)
}
n = 0
var i = 0
while (i < width * height) {
  val x = i % width
  val y = i / width
  n += 1
  if (n % 1000 == 0) println(n + "/" + max)
  if (!seen(y * width + x)) cooked.setRGB(x, y, raw.getRGB(x, y))
  i += 1
}

javax.imageio.ImageIO.write(cooked, "png", (os.pwd / "cooked.png").toIO)