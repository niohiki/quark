package org.niohiki.quark.core

import java.awt.Color
import java.awt.Graphics2D
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream

import scala.Array.canBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.xml.Elem

import org.niohiki.quark.util.SpatialFactory

class World(val settings: WorldSettings) extends Serializable with WorldInterface {
  private val globals = new ArrayBuffer[Global]
  private val patches = Array.tabulate[Patch](settings.x_tiles, settings.y_tiles) {
    case (i, j) => new Patch(
      settings.spacing * (i - settings.x_tiles / 2.0),
      settings.spacing * (j - settings.y_tiles / 2.0),
      settings.spacing / 2 + settings.margin)
  }
  patches.indices.foreach(i => {
    patches(i).indices.foreach(j => {
      def tryPatch(try_x: Int, try_y: Int): Patch =
        if (try_x >= 0 && try_y >= 0 && try_x < settings.x_tiles && try_y < settings.y_tiles) patches(try_x)(try_y) else null
      patches(i)(j).left_patch = tryPatch(i - 1, j); patches(i)(j).right_patch = tryPatch(i + 1, j)
      patches(i)(j).top_patch = tryPatch(i, j + 1); patches(i)(j).bottom_patch = tryPatch(i, j - 1)
    })
  })
  private def getIndexForPosition(x: Double, y: Double): (Int, Int) = {
    var i = Math.round(x / settings.spacing + settings.x_tiles / 2.0).asInstanceOf[Int]
    var j = Math.round(y / settings.spacing + settings.y_tiles / 2.0).asInstanceOf[Int]
    if (i >= settings.x_tiles) i = settings.x_tiles - 1; if (i < 0) i = 0
    if (j >= settings.y_tiles) j = settings.y_tiles - 1; if (j < 0) j = 0
    return (i, j)
  }
  private def getPatchForPosition(x: Double, y: Double) = getIndexForPosition(x, y) match { case (i, j) => patches(i)(j) }
  private def getPatchesForArea(x_center: Double, y_center: Double, width: Double, height: Double): List[Patch] = {
    val (i_min, j_min) = getIndexForPosition(x_center - width / 2, y_center - height / 2)
    val (i_max, j_max) = getIndexForPosition(x_center + width / 2, y_center + height / 2)
    val result = for (i <- i_min to i_max; j <- j_min to j_max) yield patches(i)(j)
    return result.toList
  }
  private def getPatches = patches flatMap (x => x)
  def updateGlobals(env: Environment) {
    globals filter { _.isInstanceOf[Updateable] } foreach (x => {
      x.asInstanceOf[Updateable].tick(env.render_canvas.threadSettings.delay_factor /
        env.render_canvas.threadSettings.update_cps)
      x.asInstanceOf[Updateable].update(env, x.asInstanceOf[Updateable].getDelta)
    })
  }
  def renderGlobals(env: Environment, g: Graphics2D) {
    globals filter { _.isInstanceOf[Renderable] } foreach (_.asInstanceOf[Renderable] render (env, g))
  }
  def renderArea(env: Environment, g: Graphics2D, x_center: Double, y_center: Double, width: Double, height: Double) {
    getPatchesForArea(x_center, y_center, width, height).flatMap(p => {
      if (env.render_canvas.settings.debug_mode) {
        g.setColor(Color.BLACK); g.drawRect(
          (p.x - p.bound).asInstanceOf[Int],
          (p.y - p.bound).asInstanceOf[Int],
          p.bound.asInstanceOf[Int] * 2,
          p.bound.asInstanceOf[Int] * 2)
      }; p.renderables
    }).sortWith((a, b) => a.zIndex < b.zIndex).foreach(x => {
      val g_new = g.create.asInstanceOf[Graphics2D]; g_new.translate(
        x.xCenter.asInstanceOf[Int],
        x.yCenter.asInstanceOf[Int])
      x.render(env, g_new); g_new.dispose
    })
  }
  def updateArea(env: Environment, x_center: Double, y_center: Double, width: Double, height: Double) {
    val ps = getPatchesForArea(x_center, y_center, width, height)
    ps.foreach { x => x.update(env); x.checkParents }
    ps.foreach { _ collide (env) }
  }
  private class Patch(val x: Double, val y: Double, val bound: Double) extends Serializable {
    var left_patch: Patch = _; var right_patch: Patch = _
    var top_patch: Patch = _; var bottom_patch: Patch = _
    val spatials = new ArrayBuffer[Spatial]
    val renderables = new ArrayBuffer[Renderable with Spatial]
    val updateables = new ArrayBuffer[Updateable]
    val movables = new ArrayBuffer[Movable]
    val collidables = new ArrayBuffer[Collidable]
    val colliders = new ArrayBuffer[Collider]
    def add(s: Spatial) {
      spatials.synchronized {
        if (!(spatials contains s)) {
          spatials += s
          if (s.isInstanceOf[Renderable]) renderables += s.asInstanceOf[Renderable with Spatial]
          if (s.isInstanceOf[Updateable]) updateables += s.asInstanceOf[Updateable]
          if (s.isInstanceOf[Movable]) movables += s.asInstanceOf[Movable]
          if (s.isInstanceOf[Collidable]) collidables += s.asInstanceOf[Collidable]
          if (s.isInstanceOf[Collider]) colliders += s.asInstanceOf[Collider]
        }
      }
    }
    def remove(s: Spatial) {
      spatials.synchronized {
        if (spatials contains s) {
          spatials -= s
          if (s.isInstanceOf[Renderable]) renderables -= s.asInstanceOf[Renderable with Spatial]
          if (s.isInstanceOf[Updateable]) updateables -= s.asInstanceOf[Updateable]
          if (s.isInstanceOf[Movable]) movables -= s.asInstanceOf[Movable]
          if (s.isInstanceOf[Collidable]) collidables -= s.asInstanceOf[Collidable]
          if (s.isInstanceOf[Collider]) colliders -= s.asInstanceOf[Collider]
        }
      }
    }
    def update(env: Environment) {
      spatials.synchronized {
        updateables.foreach(x => {
          if (x == null) return
          x.tick(env.render_canvas.threadSettings.delay_factor
            / env.render_canvas.threadSettings.update_cps)
          x.update(env, x.getDelta); if (x.isInstanceOf[Accelerable]) {
            val delta = x.getDelta
            val acc = x.asInstanceOf[Accelerable]
            acc.move(acc.x_speed * delta, acc.y_speed * delta)
            acc.x_speed += acc.x_acceleration * delta
            acc.y_speed += acc.y_acceleration * delta
          }
        })
      }
    }
    private def checkPos(spatial: Spatial) {
      if (spatial != null) {
        if (spatial.xCenter > x + bound || spatial.xCenter < x - bound ||
          spatial.yCenter > y + bound || spatial.yCenter < y - bound) {
          spatial.synchronized {
            remove(spatial); getPatchForPosition(spatial.xCenter, spatial.yCenter).add(spatial)
          }
        }
      }
    }
    def checkParents {
      spatials.synchronized { spatials.foreach(checkPos(_)) }
    }
    def collide(env: Environment) {
      spatials.synchronized {
        val collidables = surroundingPatches.flatMap(patch => if (patch != null) patch.collidables else List())
        colliders.foreach(collider => {
          collidables.foreach(collidable => {
            if (collidable != collider && collider != null) {
              val delta_x = Math.abs(collider.xCenter - collidable.xCenter)
              val delta_y = Math.abs(collider.yCenter - collidable.yCenter)
              val x_add = (collider.bBox.width + collidable.bBox.width) / 2
              val y_add = (collider.bBox.height + collidable.bBox.height) / 2
              if (delta_x < x_add && delta_y < y_add) {
                val x_diff = Math.abs(collider.bBox.width - collidable.bBox.width) / 2
                val y_diff = Math.abs(collider.bBox.height - collidable.bBox.height) / 2
                val collider_right = collider.xCenter > collidable.xCenter
                val collider_up = collider.yCenter > collidable.yCenter
                collider.collision(env, CollisionInfo(collidable,
                  if (delta_x < x_diff && delta_y < y_diff) {
                    CentralCollision
                  } else if (delta_x < x_diff) {
                    if (collider_up) TopCentralCollision else BottomCentralCollision
                  } else if (delta_y < y_diff) {
                    if (collider_right) RightCentralCollision else LeftCentralCollision
                  } else {
                    if (collider_up) {
                      if (collider_right) TopRightCollision else TopLeftCollision
                    } else {
                      if (collider_right) BottomRightCollision else BottomLeftCollision
                    }
                  },
                  jump => {
                    if (x_add - delta_x < y_add - delta_y) {
                      if (collider_right ^ jump) {
                        collider.moveTo(collidable.xCenter + x_add, collider.yCenter)
                        RightCentralCollision
                      } else {
                        collider.moveTo(collidable.xCenter - x_add, collider.yCenter)
                        LeftCentralCollision
                      }
                    } else {
                      if (collider_up ^ jump) {
                        collider.moveTo(collider.xCenter, collidable.yCenter + y_add)
                        TopCentralCollision
                      } else {
                        collider.moveTo(collider.xCenter, collidable.yCenter - y_add)
                        BottomCentralCollision
                      }
                    }
                  }))
              }
            }
          })
        })
      }
    }
    def surroundingPatches: List[Patch] = {
      return List(this,
        right_patch, if (right_patch != null) right_patch.top_patch else null,
        top_patch, if (top_patch != null) top_patch.left_patch else null,
        left_patch, if (left_patch != null) left_patch.bottom_patch else null,
        bottom_patch, if (bottom_patch != null) bottom_patch.right_patch else null)
    }
    override def toString = "patch at (" + x + "," + y + ")"
  }
  def getSpatials = getPatches flatMap (p => p.spatials) toList
  def getCollidablesForPoint(p: (Double, Double)) = p match {
    case (x, y) =>
      getCollidablesForPosition(x, y)
  }
  def getCollidablesForPosition(x: Double, y: Double) = this.synchronized {
    getPatchForPosition(x, y).surroundingPatches.filter { _ != null }.flatMap {
      _.collidables.filter { _.contains(x, y) }.toList
    }
  }
  def getSpatialsForPosition(x: Double, y: Double, m_x: Double, m_y: Double) = this.synchronized {
    getPatchForPosition(x, y).surroundingPatches.filter { _ != null }.flatMap {
      _.spatials.filter { _.liesIn(x, y, m_x, m_y) }.toList
    }
  }
  def getSpatialsForPoint(p: (Double, Double), mx: Double, my: Double) =
    p match { case (x, y) => getSpatialsForPosition(x, y, mx, my) }
  def add(e: Entity) {
    e.onAdd(this)
    e match {
      case _: Global => globals += e.asInstanceOf[Global]
      case _: Spatial => getPatchForPosition(
        e.asInstanceOf[Spatial].xCenter, e.asInstanceOf[Spatial].yCenter).add(e.asInstanceOf[Spatial])
    }
  }
  def remove(e: Entity) {
    e.onRemove(this)
    e match {
      case _: Global => globals -= e.asInstanceOf[Global]
      case _: Spatial => getPatchForPosition(
        e.asInstanceOf[Spatial].xCenter, e.asInstanceOf[Spatial].yCenter).surroundingPatches.filter { _ != null }.foreach {
          _.remove(e.asInstanceOf[Spatial])
        }
    }
  }
}
class Layer(val size_x: Int, val size_y: Int) extends LayerInterface {
  var x_center: Double = 0
  var y_center: Double = 0
  private var world_var: World = null; def world = world_var
  private val zone_scale = 1.2
  def setWorld(w: World) = world_var = w
  def createWorld(worldSettings: WorldSettings) = world_var = new World(worldSettings)
  def createWorld(xml: Elem) {
    val map = new HashMap[String, SpatialFactory]
    val world_xml = xml \ "world"
    val world_settings = WorldSettings(
      x_tiles = (world_xml \ "@x_tiles").toString.toInt,
      y_tiles = (world_xml \ "@y_tiles").toString.toInt,
      spacing = (world_xml \ "@spacing").toString.toInt,
      margin = (world_xml \ "@margin").toString.toInt)
    world_var = new World(world_settings)
    (xml \ "types" \ "entity") foreach { x =>
      map += (x \ "@type").toString -> Class.forName((x \ "@class").toString).newInstance.asInstanceOf[SpatialFactory]
    }
    (xml \ "load" \ "spatial") foreach { x =>
      world.add(map((x \ "@type").toString).
        create((x \ "@x").toString.toDouble, (x \ "@y").toString.toDouble, (x \ "@params").toString))
    }
    x_center = (xml \ "view" \ "@x").toString.toDouble
    y_center = (xml \ "view" \ "@y").toString.toDouble
  }
  def update(parent: RenderCanvas) {
    if (world == null) return
    val environment = new Environment(parent, this, world)
    world.updateGlobals(environment)
    world.updateArea(environment, x_center, y_center, size_x * zone_scale, size_y * zone_scale)
  }
  def render(parent: RenderCanvas, g: Graphics2D) {
    if (world == null) return
    val environment = new Environment(parent, this, world)
    val graphics_new = g.create.asInstanceOf[Graphics2D]
    graphics_new.translate((-x_center + size_x / 2).asInstanceOf[Int], (-y_center + size_y / 2).asInstanceOf[Int])
    world.renderArea(environment, graphics_new, x_center, y_center, size_x * zone_scale, size_y * zone_scale)
    world.renderGlobals(environment, g)
    graphics_new.dispose
  }
  def saveWorld(os: OutputStream): Unit = {
    val oos = new ObjectOutputStream(os); oos.writeObject(world); oos.writeObject((x_center, y_center))
  }
  def loadWorld(is: InputStream): Unit = {
    val ois = new ObjectInputStream(is); world_var = ois.readObject.asInstanceOf[World]
    ois.readObject match { case (x: Double, y: Double) => { x_center = x; y_center = y } }
  }
  def follow(s: Spatial, margin: Double) {
    if (s.xCenter > x_center + size_x / 2.0 - margin) x_center = s.xCenter + margin - size_x / 2.0
    if (s.xCenter < x_center - size_x / 2.0 + margin) x_center = s.xCenter - margin + size_x / 2.0
    if (s.yCenter > y_center + size_y / 2.0 - margin) y_center = s.yCenter + margin - size_y / 2.0
    if (s.yCenter < y_center - size_y / 2.0 + margin) y_center = s.yCenter - margin + size_y / 2.0
  }
  def xCenter = x_center
  def yCenter = y_center
  def setCenter(x: Double, y: Double) {
    x_center = x
    y_center = y
  }
  def moveCenter(dx: Double, dy: Double){
    x_center += dx
    y_center += dy
  }
}
case class WorldSettings(val x_tiles: Int = 1, val y_tiles: Int = 1, val spacing: Double = 0.0, val margin: Double = 0.0)
