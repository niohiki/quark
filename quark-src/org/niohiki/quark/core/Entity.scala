package org.niohiki.quark.core

import java.awt.Graphics2D

import org.niohiki.quark.util.Updater

sealed trait Entity extends Serializable {
  var a = 5
  def onAdd(world: WorldInterface) = {}
  def onRemove(world: WorldInterface) = {}
}
class Global extends Entity
class Spatial extends Entity {
  protected var x_center: Double = 0
  protected var y_center: Double = 0
  def xCenter = x_center
  def yCenter = y_center
  def liesIn(x: Double, y: Double, margin_x: Double, margin_y: Double) =
    Math.abs(x - x_center) <= margin_x && Math.abs(y - y_center) <= margin_y
}
trait Movable extends Spatial {
  def moveTo(x: Double, y: Double) {
    x_center = x
    y_center = y
  }
  def move(x: Double, y: Double) {
    x_center += x
    y_center += y
  }
}
trait Collidable extends Spatial {
  def bBox: BBox
  def contains(x: Double, y: Double) = Math.abs(x - x_center) <= bBox.width / 2 && Math.abs(y - y_center) <= bBox.height / 2
}
case class CollisionInfo(val other: Collidable, val collisionType: CollisionType,
  val moveOut: Boolean => SideCollision)
trait Collider extends Movable with Collidable {
  def collision(env: Environment, info: CollisionInfo): Unit
}
trait Accelerable extends Movable with Updateable {
  var x_speed: Double = 0
  var y_speed: Double = 0
  var x_acceleration: Double = 0
  var y_acceleration: Double = 0
}
class BBox(var width: Double, var height: Double) extends Serializable
trait Renderable {
  def render(env: Environment, graphics: Graphics2D)
  def zIndex = 0
}
trait Updateable extends Updater {
  def update(env: Environment, delta: Double)
}