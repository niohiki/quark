package org.niohiki.quark.core

import java.awt.Canvas
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Toolkit
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue

import org.niohiki.quark.util.Resources

class RenderCanvas extends Canvas with CanvasInterface {
  private var settings_var: CanvasSettings = null; private var thread_settings_var: ThreadSettings = null
  private var back_color = Color.WHITE
  def settings = settings_var
  def threadSettings = thread_settings_var
  private val layers = new ArrayBuffer[Layer]
  private val events = new EventHandler
  private val background_orders = new Queue[Unit => Unit]
  addKeyListener(events); addMouseListener(events); addMouseMotionListener(events); addFocusListener(events)
  private def forceClear {
    val g = getBufferStrategy.getDrawGraphics.asInstanceOf[Graphics2D]
    g.setColor(back_color); g.fillRect(0, 0, getWidth, getHeight)
    g.dispose; getBufferStrategy.show; Toolkit.getDefaultToolkit.sync
  }
  def init(cs: CanvasSettings, ts: ThreadSettings) {
    setSize(cs.width, cs.height); settings_var = cs; thread_settings_var = ts
  }
  def start {
    createBufferStrategy(settings.buffers)
    val render_thread = new DaemonLoop(threadSettings.render_cps, delta => {
      val g = getBufferStrategy.getDrawGraphics.asInstanceOf[Graphics2D]
      g.setColor(back_color); g.fillRect(0, 0, getWidth, getHeight)
      layers.foreach { _ render (RenderCanvas.this, g) }
      g.dispose; getBufferStrategy.show; Toolkit.getDefaultToolkit.sync
    })
    val update_thread = new DaemonLoop(threadSettings.update_cps, delta => {
      layers.foreach { _ update RenderCanvas.this }
    })
    val background_thread = new DaemonLoop(threadSettings.background_cps, delta => {
      while (!background_orders.isEmpty) {
        background_orders.dequeue.apply()
      }
    })
    update_thread.start
    background_thread.start
    render_thread.start
    requestFocus
  }
  private class EventHandler extends KeyAdapter with MouseListener with MouseMotionListener
    with FocusListener {
    private val pressedKeys = new ArrayBuffer[Int]
    private val pressedButtons = new ArrayBuffer[Int]
    private var inWindow = true
    private var mouse_x: Double = 0
    private var mouse_y: Double = 0
    override def keyPressed(e: KeyEvent) = if (!pressedKeys.contains(e.getKeyCode)) pressedKeys += e.getKeyCode
    override def keyReleased(e: KeyEvent) = pressedKeys -= e.getKeyCode
    def mouseClicked(e: MouseEvent) {}
    def mousePressed(e: MouseEvent) = if (!pressedButtons.contains(e.getButton)) pressedButtons += e.getButton
    def mouseReleased(e: MouseEvent) = pressedButtons -= e.getButton
    def mouseEntered(e: MouseEvent) = inWindow = true
    def mouseExited(e: MouseEvent) = inWindow = false
    def mouseDragged(e: MouseEvent) { mouse_x = e.getX - getWidth / 2; mouse_y = e.getY - getHeight / 2 }
    def mouseMoved(e: MouseEvent) { mouse_x = e.getX - getWidth / 2; mouse_y = e.getY - getHeight / 2 }
    def isKeyPressed(keyCode: Int) = pressedKeys.contains(keyCode)
    def isButtonPressed(button: Int) = pressedButtons.contains(button)
    def isMouseInWindow = inWindow
    def getMousePosition(view: Layer) = (view.x_center + mouse_x, view.y_center + mouse_y)
    def getMousePositionInScreen = (mouse_x, mouse_y)
    def focusGained(e: FocusEvent) = Unit
    def focusLost(e: FocusEvent) = {
      pressedButtons.clear
      pressedKeys.clear
    }
  }
  private class DaemonLoop(cps: Double, loop: Double => Unit) {
    def start = daemon_thread.start
    private val daemon_thread = new Thread(new Runnable {
      def run {
        var lastTime = System.nanoTime
        while (true) {
          val currentTime = System.nanoTime
          val delta = currentTime - lastTime
          lastTime = currentTime
          try if (hasFocus) loop(delta * 1.0e-9)
          catch {
            case e: Exception => if (settings_var.debug_mode) {
              println("In daemon thread " + DaemonLoop.this); e.printStackTrace
            }
          }
          val sleepTime = 1.0e9 / cps - (System.nanoTime - lastTime)
          if (sleepTime > 0) Thread.sleep((sleepTime * 1e-6).asInstanceOf[Int])
        }
      }
    })
    daemon_thread.setDaemon(true)
  }
  def addLayer: Layer = {
    val view = new Layer(getWidth, getHeight)
    layers += view; return view
  }
  def save(file: File, info: Any): Unit = {
    val fos = new FileOutputStream(file)
    layers.foreach(_.saveWorld(fos))
    new ObjectOutputStream(fos).writeObject(info)
    fos.close
  }
  def load(file: File): Any = {
    val fis = new FileInputStream(file)
    layers.foreach(_.loadWorld(fis))
    val retval = new ObjectInputStream(fis).readObject
    fis.close
    return retval
  }
  def runInBackground(load: Unit => Unit) = background_orders += load
  def setCursor(image_name: String) {
    val image = Resources.getImage(image_name)
    RenderCanvas.this.setCursor(getToolkit().createCustomCursor(image,
      new Point(image.getWidth / 2, image.getHeight / 2), image_name))
  }
  def removeLayer(view: Layer) = layers -= view

  def getInputForLayer(layer: Layer) = new InputInterface {
    def isKeyPressed(keyCode: Int) = events.isKeyPressed(keyCode)
    def isButtonPressed(button: Int) = events.isButtonPressed(button)
    def isMouseInWindow = events.isMouseInWindow
    def getMousePositionInLayer = events.getMousePosition(layer)
    def getMousePositionInScreen = events.getMousePositionInScreen
  }
}
case class CanvasSettings(val width: Int = 500, val height: Int = 400,
  val buffers: Int = 2, val debug_mode: Boolean = false)
case class ThreadSettings(val update_cps: Double = 120, val render_cps: Double = 60, val background_cps: Double = 30,
  val delay_factor: Double = 4)
  