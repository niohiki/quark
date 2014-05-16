package org.niohiki.quark.util

import java.awt.Graphics2D
import java.awt.geom.AffineTransform

import org.niohiki.quark.core.BBox
import org.niohiki.quark.core.Collidable
import org.niohiki.quark.core.Environment
import org.niohiki.quark.core.Renderable
import org.niohiki.quark.core.Spatial

class Image(image_name: String, init_transform: AffineTransform => Unit = null,
  resources: => Resources = DefaultResources)
  extends Spatial with Renderable {

  def image = resources.getImage(image_name)
  val transform = new AffineTransform
  if (init_transform != null) {
    init_transform(transform)
  }
  transform.translate(-image.getWidth / 2, -image.getHeight / 2)
  def render(env: Environment, g: Graphics2D) {
    g.drawImage(image, transform, null)
  }
  def rotate(angle: Double) {
    transform.rotate(angle, image.getWidth / 2, image.getHeight / 2)
  }
}
class Tile(image_name: String, init_transform: AffineTransform => Unit = null,
  resources: => Resources = DefaultResources)
  extends Image(image_name, init_transform, resources) with Collidable {

  private val b_box = new BBox(image.getWidth, image.getHeight)
  def bBox = b_box
}