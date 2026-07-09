package chext.util

class Logger(val name: String) {
  private def print(tpe: String, identifier: String, lines: String*): Unit = {
    assert(tpe.length == 4)

    val lineHead0 = f"[ $tpe ] $name/$identifier : "
    val lineHead1 = " " * lineHead0.length

    lines.zipWithIndex.foreach { //
      case (msg, idx) =>
        if (idx == 0) println(lineHead0 + msg)
        else println(lineHead1 + msg)
    }
  }

  def info(identifier: String, lines: String*): Unit =
    print("INFO", identifier, lines: _*)

  def error(identifier: String, lines: String*): Unit =
    print("ERR ", identifier, lines: _*)

  def warn(identifier: String, lines: String*): Unit =
    print("WARN", identifier, lines: _*)
}
