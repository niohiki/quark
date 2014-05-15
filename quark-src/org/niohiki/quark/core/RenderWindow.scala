package org.niohiki.quark.core

import java.awt.BorderLayout
import java.awt.Toolkit

import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import org.niohiki.quark.util.Resources

class RenderWindow(as: ApplicationSettings, cs: CanvasSettings, ts: ThreadSettings,
  load_image_name: String = null, load_orders: (RenderCanvas) => Unit = (x) => Unit) extends JFrame {
  def centerFrame(frame: JFrame) {
    val h: Int = Toolkit.getDefaultToolkit.getScreenSize.getHeight.toInt
    val w: Int = Toolkit.getDefaultToolkit.getScreenSize.getWidth.toInt
    frame.setLocation(w / 2 - frame.getWidth / 2, h / 2 - frame.getHeight / 2)
  }
  val load_frame = new JFrame
  if (load_image_name != null) {
    load_frame.add(new JLabel(new ImageIcon(Resources.getImage(load_image_name))))
    load_frame.pack
    load_frame.setTitle(as.title + "...")
    load_frame.setVisible(true)
    centerFrame(load_frame)
  }
  val render_canvas = new RenderCanvas
  render_canvas.init(cs, ts)
  if (load_orders != null) try {
    load_orders(render_canvas)
  } catch {
    case ex: Exception => ex.printStackTrace
  }
  setResizable(false)
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
  setLayout(new BorderLayout)
  add(render_canvas, BorderLayout.CENTER)
  setSize(cs.width, cs.height)
  setTitle(as.title)
  centerFrame(this)
  setVisible(true)
  render_canvas.start
  load_frame.dispose
}
case class ApplicationSettings(val title: String = "")

