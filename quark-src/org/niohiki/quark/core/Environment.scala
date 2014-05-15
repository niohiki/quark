package org.niohiki.quark.core

import java.io.File

trait LayerInterface {
  def follow(s: Spatial, margin: Double): Unit
  def xCenter: Double
  def yCenter: Double
  def setCenter(x: Double, y: Double): Unit
}
trait CanvasInterface {
  def settings: CanvasSettings
  def threadSettings: ThreadSettings
  def addLayer: Layer
  def removeLayer(view: Layer): Unit
  def save(file: File, info: Any): Unit
  def load(file: File): Any
  def runInBackground(load: Unit => Unit): Unit
  def setCursor(image_name: String): Unit
}
trait InputInterface {
  def isKeyPressed(keyCode: Int): Boolean
  def isButtonPressed(button: Int): Boolean
  def isMouseInWindow: Boolean
  def getMousePositionInLayer: (Double, Double)
  def getMousePositionInScreen: (Double, Double)
}
trait ConsoleInterface {
  def open: Unit
  def put(name: String, value: Any): Unit
  def eval(script: String): Unit
}
trait WorldInterface {
  def getSpatials: List[Spatial]
  def getCollidablesForPoint(p: (Double, Double)): List[Collidable]
  def getCollidablesForPosition(x: Double, y: Double): List[Collidable]
  def getSpatialsForPosition(x: Double, y: Double, m_x: Double, m_y: Double): List[Spatial]
  def getSpatialsForPoint(p: (Double, Double), mx: Double, my: Double): List[Spatial]
  def add(e: Entity): Unit
  def remove(e: Entity): Unit
}

class Environment(render_canvas_instance: RenderCanvas,
  layer_instance: Layer,
  world_instance: World) {
  val render_canvas: CanvasInterface = render_canvas_instance
  val layer: LayerInterface = layer_instance
  val input: InputInterface = render_canvas_instance.getInputForLayer(layer_instance)
  val world: WorldInterface = world_instance
}