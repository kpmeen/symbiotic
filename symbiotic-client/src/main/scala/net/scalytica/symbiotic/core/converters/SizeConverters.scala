package net.scalytica.symbiotic.core.converters

object SizeConverters {

  def toReadableSize(numBytes: Long): String = {
    val unit     = 1000
    val prefixes = "KMGTPE"
    if (numBytes < unit) s"$numBytes B"
    else {
      val exp = (Math.log(numBytes) / Math.log(unit)).toInt
      f"${numBytes / Math.pow(unit, exp)}%.1f ${prefixes.charAt(exp - 1)}%sB"
    }
  }

  def toReadableSize(numBytes: String): String =
    toReadableSize(numBytes.toLong)

}
