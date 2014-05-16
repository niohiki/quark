package org.niohiki.quark.util

import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage

import scala.collection.mutable.HashMap

import org.niohiki.quark.core.Accelerable
import org.niohiki.quark.core.BBox
import org.niohiki.quark.core.Collidable
import org.niohiki.quark.core.Environment
import org.niohiki.quark.core.Renderable

class Sprite extends Collidable with Accelerable with Renderable {
  private val sets = new HashMap[String, SpriteRenderer]
  private var current = ""
  private var null_b_box = new BBox(0, 0)
  def currentAnimation = if (sets.contains(current)) sets(current) else null
  def addAnimation(name: String, animation: SpriteRenderer) = sets += name -> animation
  def setAnimation(name: String) = current = name
  def render(env: Environment, g: Graphics2D) = if (currentAnimation != null)
    g.drawImage(currentAnimation.image, currentAnimation.transform, null)
  def bBox = if (currentAnimation != null) currentAnimation.bBox else null_b_box
  def update(env: Environment, delta: Double) = if (currentAnimation != null)
    currentAnimation.tick(delta)

}
trait SpriteRenderer extends Serializable {
  def tick(delta: Double): Unit
  def image: BufferedImage
  def bBox: BBox
  def transform: AffineTransform
}
class SpriteAnimation(image_names: List[String], init: Int, fps: Double,
  init_transform: AffineTransform => Unit = null,
  init_b_box: BBox => Unit = null, resources: => Resources = DefaultResources)
  extends SpriteRenderer {

  def images(i: Int) = resources.getImage(image_names(i))
  private var time: Double = init
  private var frame: Int = init
  protected val trans = new AffineTransform
  private val b_box = new BBox(images(0).getWidth, images(0).getHeight)
  if (init_b_box != null) init_b_box(b_box)
  if (init_transform != null) init_transform(trans)
  trans.translate(-images(0).getWidth / 2, -images(0).getHeight / 2)
  def tick(delta: Double) {
    time += delta * fps
    frame = time.asInstanceOf[Int] % image_names.length
  }
  def image = images(frame)
  def bBox = b_box
  def transform = trans
}
class SpriteStatic(image_name: String,
  init_transform: AffineTransform => Unit = null,
  init_b_box: BBox => Unit = null, resources: => Resources = DefaultResources)
  extends SpriteRenderer {

  def image_val = resources.getImage(image_name)
  protected val trans = new AffineTransform
  private val b_box = new BBox(image_val.getWidth, image_val.getHeight)
  if (init_b_box != null) init_b_box(b_box)
  if (init_transform != null) init_transform(trans)
  trans.translate(-image_val.getWidth / 2, -image_val.getHeight / 2)
  def tick(delta: Double) {}
  def image = image_val
  def bBox = b_box
  def transform = trans
}