package org.niohiki.quark.test

import java.awt.Color
import java.awt.Graphics2D
import java.awt.event.MouseEvent
import org.niohiki.quark.core.ApplicationSettings
import org.niohiki.quark.core.CanvasSettings
import org.niohiki.quark.core.Environment
import org.niohiki.quark.core.RenderWindow
import org.niohiki.quark.core.Renderable
import org.niohiki.quark.core.Spatial
import org.niohiki.quark.core.ThreadSettings
import org.niohiki.quark.core.Updateable
import org.niohiki.quark.core.WorldSettings
import org.niohiki.quark.util.Resources
import org.niohiki.quark.util.DefaultResources

object Test {
  def main(args: Array[String]) {
    val window = new RenderWindow(ApplicationSettings(title = "Quark test"),
      CanvasSettings(width = 400, height = 400, buffers = 2, debug_mode = false),
      ThreadSettings(update_cps = 60, render_cps = 60, background_cps = 30, delay_factor = 4), null, rc => {
        val layer = rc.addLayer
        layer.createWorld(new WorldSettings)
        val square = new Square
        layer.world.add(square)
      })
  }
}

class Square extends Spatial with Renderable with Updateable {
  val speed = 30
  def render(env: Environment, g: Graphics2D) = {
    g.setColor(Color.black)
    g.fillRect(-20, -20, 40, 40)
    g.drawImage(DefaultResources.getImage("bret"), -16, -16, null)
  }
  def update(env: Environment, delta: Double) {
    val evs = env.input
    if (evs.isButtonPressed(MouseEvent.BUTTON1)) evs.getMousePositionInLayer match {
      case (x, y) => {
        def moveRel(update: Double => Unit, orig: Double, dest: Double) {
          update(speed * delta * (if (orig > dest) -1 else 1))
        }
        moveRel(x_center += _, x_center, x)
        moveRel(y_center += _, y_center, y)
      }
    }
  }
}