def sanitize(s: String): String = {
  s.split(" ", 2) match{
    case Array(prefix, suffix) if prefix.forall(c => c.isDigit || c == '.') =>
      (prefix.split(" |-|\\.", -1) ++ suffix.split(" |-", -1)).map(_.filter(_.isLetterOrDigit)).mkString("-").toLowerCase
    case _ => s.split(" |-", -1).map(_.filter(_.isLetterOrDigit)).mkString("-").toLowerCase
  }

}