package org.niohiki.quark.util

import java.awt.Font
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import scala.collection.mutable.HashMap
import scala.xml.Elem
import scala.xml.XML
import javax.imageio.ImageIO
import java.io.OutputStream

trait Log {
  def log(c: Char): Unit
}
object Resources {
  val quark_base_path = "/org/niohiki/quark/"
}
object DefaultResources extends Resources(Resources.quark_base_path) {
  def setBasePath(_base_path: String) = base_path = _base_path
}
class Resources(protected var base_path: String) {
  def image_path = "images/"
  def xml_path = "xml/"
  def font_path = "fonts/"
  def script_path = "scripts/"
  private val images = new HashMap[String, BufferedImage]
  private val fonts = new HashMap[String, Font]
  private val dummy_image = ImageIO.read(
    getClass.getResourceAsStream(Resources.quark_base_path + "/dummy_image.bmp"))
  private val dummy_font = new Font("Arial", Font.PLAIN, 1)
  def getResource(name: String): InputStream = {
    return new File("." + base_path + name) match {
      case f if f.exists => new FileInputStream(f)
      case f => getClass.getResourceAsStream(base_path + name)
    }
  }
  def getExternalResourceOutput(name: String): OutputStream = {
    val file = new File("." + base_path + name)
    file.getParentFile.mkdirs
    file.createNewFile
    new FileOutputStream(file)
  }
  def getResourceAsString(name: String): String = {
    val stream = getResource(name)
    if (stream == null) return ""
    val reader = new BufferedReader(new InputStreamReader(stream))
    var line: String = reader.readLine
    var full = line + "\n"
    while (line != null) {
      line = reader.readLine
      full += line + "\n"
    }
    return full
  }
  def getImage(name: String): BufferedImage = {
    if (!images.contains(name)) {
      val resource = getResource(image_path + name)
      images += name -> (if (resource != null) ImageIO.read(resource) else dummy_image)
    }
    return images(name)
  }
  def getFont(name: String, size: Double, attributes: Int = Font.PLAIN): Font = {
    if (!fonts.contains(name)) {
      val resource = getResource(font_path + name)
      fonts += name -> (if (resource != null) Font.createFont(Font.TRUETYPE_FONT, resource) else dummy_font)
    }
    return fonts(name).deriveFont(attributes, size.asInstanceOf[Float])
  }
  def getXML(name: String): Elem = {
    val stream = getResource(xml_path + name)
    if (stream != null) {
      XML.load(stream)
    } else {
      null
    }
  }
}