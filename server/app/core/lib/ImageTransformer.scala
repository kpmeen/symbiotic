/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.lib

import org.slf4j.LoggerFactory

import scala.util.Try
import javax.imageio.ImageIO.{read, write}
import java.awt.image.BufferedImage
import java.io.File.createTempFile

object ImageTransformer {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def resizeImage(f: java.io.File, width: Int, height: Int): Option[java.io.File] = {
    Try {
      val image = read(f)
      val imgType = {
        if (image.getType == 0) BufferedImage.TYPE_INT_ARGB
        else image.getType
      }
      val resized = new BufferedImage(width, height, imgType)
      val g = resized.createGraphics()
      g.drawImage(image, 0, 0, width, height, null)
      g.dispose()

      val resizedFile = createTempFile("resized", "avatar")
      if (write(resized, "png", resizedFile)) Some(resizedFile)
      else None
    }.recover {
      case t: Throwable =>
        logger.warn("Unable to resize avatar image", t)
        None
    }.get
  }
}
