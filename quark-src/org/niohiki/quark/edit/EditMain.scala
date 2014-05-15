package org.niohiki.quark.edit

import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream

import scala.Array.canBuildFrom
import scala.collection.mutable.HashMap
import scala.xml.Node

import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextField
import org.niohiki.quark.core.ApplicationSettings
import org.niohiki.quark.core.CanvasSettings
import org.niohiki.quark.core.Environment
import org.niohiki.quark.core.Global
import org.niohiki.quark.core.RenderWindow
import org.niohiki.quark.core.ThreadSettings
import org.niohiki.quark.core.Updateable
import org.niohiki.quark.core.WorldSettings
import org.niohiki.quark.util.Resources
import org.niohiki.quark.util.Tile

class EditWindow(val editor_list: String, val default_file: String) extends RenderWindow(
  ApplicationSettings(title = "Editor"),
  CanvasSettings(width = 700, height = 600, buffers = 2, debug_mode = true),
  ThreadSettings(update_cps = 120, render_cps = 60, background_cps = 30), null, rc => {
  }) {
  private var xml_list = Resources.getXML(editor_list)
  private val world_element = xml_list \ "world"
  private val world_settings = WorldSettings(
    x_tiles = (world_element \ "@x_tiles").toString.toInt,
    y_tiles = (world_element \ "@y_tiles").toString.toInt,
    spacing = (world_element \ "@spacing").toString.toInt,
    margin = (world_element \ "@margin").toString.toInt)
  private var layer = render_canvas.addLayer
  layer.createWorld(world_settings)
  private def getLayer = layer
  private class EntityRepresentation(node: Node, xpos: Double, ypos: Double, pars: String) extends Tile((node \ "@image").toString) {
    x_center = xpos
    y_center = ypos
    def image_name = (node \ "@image").toString
    def class_name = (node \ "@class").toString
    def type_name = (node \ "@type").toString
    def params = pars
  }
  private class EntityItem(val node: Node) {
    override def toString = (node \ "@type").toString
  }
  remove(render_canvas)
  private val entity_list = xml_list \ "types" \ "entity"
  private val tab_panel = new JTabbedPane
  private val editor_panel = new JPanel
  editor_panel.setLayout(new BorderLayout)
  editor_panel.add(render_canvas, BorderLayout.CENTER)
  tab_panel.add("Editor", editor_panel)
  add(tab_panel, BorderLayout.NORTH)
  private val north_panel = new JPanel
  north_panel.setLayout(new GridLayout(3, 1))
  editor_panel.add(north_panel, BorderLayout.NORTH)
  private val north_sub_panel = new JPanel
  north_sub_panel.setLayout(new GridLayout(1, 3))
  north_panel add north_sub_panel
  private val output_file = new JTextField(default_file)
  private val save_button = new JButton("Save")
  private val load_button = new JButton("Load")
  north_sub_panel add output_file
  north_sub_panel add save_button
  north_sub_panel add load_button
  private val entity_types_combo = new JComboBox
  entity_list foreach { entity_types_combo addItem new EntityItem(_) }
  north_panel add entity_types_combo
  private val params_field = new JTextField
  north_panel add params_field
  private val save_listener = new ActionListener {
    def actionPerformed(evt: ActionEvent) {
      val xml = "<xml>\n\t" +
        (xml_list \ "world").toString + "\n\t" +
        (xml_list \ "types").toString + "\n" +
        "\t" + (<view x={ layer.x_center.toString } y={ layer.y_center.toString }/>).toString + "\n\t\t" +
        "\t<load>\n\t\t" +
        getLayer.world.getSpatials.map { _.asInstanceOf[EntityRepresentation] }.map { e =>
          {
            <spatial type={ e.type_name } x={ e.xCenter.toString } y={ e.yCenter.toString } params={ e.params }/>
          }
        }.map { _.toString }.reduceLeft { _ + "\n\t\t" + _ }.toString + "\n\t</load>\n</xml>"
      val file = new File(Resources.xml_path + output_file.getText)
      file.getParentFile.mkdirs
      file.createNewFile
      val printer = new PrintStream(new FileOutputStream(file))
      printer.print(xml)
      printer.close
    }
  }
  private val load_listener = new ActionListener {
    def actionPerformed(evt: ActionEvent) {
      render_canvas.removeLayer(getLayer)
      entity_types_combo.removeAllItems
      val map = new HashMap[String, Node]
      xml_list = Resources.getXML(output_file.getText)
      val world = xml_list \ "world"
      val world_settings = WorldSettings(
        x_tiles = (world \ "@x_tiles").toString.toInt,
        y_tiles = (world \ "@y_tiles").toString.toInt,
        spacing = (world \ "@spacing").toString.toInt,
        margin = (world \ "@margin").toString.toInt)
      layer = render_canvas.addLayer
      layer.createWorld(world_settings)
      layer.x_center = (xml_list \ "view" \ "@x").toString.toDouble
      layer.y_center = (xml_list \ "view" \ "@y").toString.toDouble
      (xml_list \ "types" \ "entity") foreach { x =>
        {
          entity_types_combo.addItem(new EntityItem(x))
          map += (x \ "@type").toString -> x
        }
      }
      (xml_list \ "load" \ "spatial") foreach { x =>
        layer.world.add(new EntityRepresentation(map((x \ "@type").toString),
          (x \ "@x").toString.toDouble,
          (x \ "@y").toString.toDouble,
          (x \ "@params").toString))
      }
      addMasterEntity
      render_canvas.requestFocus
    }
  }
  save_button addActionListener save_listener
  load_button addActionListener load_listener
  private val south_panel = new JPanel
  south_panel.setLayout(new GridLayout(4, 4))
  editor_panel.add(south_panel, BorderLayout.SOUTH)
  private val label_x_grid = new JLabel("Grid X")
  private val text_x_grid = new JTextField("32")
  private val label_y_grid = new JLabel("Grid Y")
  private val text_y_grid = new JTextField("32")
  south_panel add label_x_grid
  south_panel add text_x_grid
  south_panel add label_y_grid
  south_panel add text_y_grid
  private val label_x_mouse = new JLabel
  private val label_y_mouse = new JLabel
  private val label_a = new JLabel
  private val label_b = new JLabel
  south_panel add label_x_mouse
  south_panel add label_y_mouse
  south_panel add label_a
  south_panel add label_b
  private val text_view_center_x = new JTextField(layer.x_center.toString)
  private val label_world_size_x = new JLabel
  private val text_view_center_y = new JTextField(layer.y_center.toString)
  private val label_world_size_y = new JLabel
  south_panel add label_world_size_x
  south_panel add text_view_center_x
  south_panel add label_world_size_y
  south_panel add text_view_center_y
  private val entity_type = new JLabel
  private val entity_image = new JLabel
  private val entity_class = new JLabel
  private val entity_params = new JLabel
  south_panel add entity_type
  south_panel add entity_image
  south_panel add entity_class
  south_panel add entity_params
  private val view_position_listener = new ActionListener {
    def actionPerformed(evt: ActionEvent) {
      try {
        val x_center = text_view_center_x.getText.toDouble
        val y_center = text_view_center_y.getText.toDouble
        layer.x_center = x_center
        layer.y_center = y_center
      } catch {
        case e: NumberFormatException => {}
      }
    }
  }
  text_view_center_x addActionListener view_position_listener
  text_view_center_y addActionListener view_position_listener
  private var x_mouse = 0: Double
  private var y_mouse = 0: Double
  private var x_mouse_snap = 0: Double
  private var y_mouse_snap = 0: Double
  private var x_grid = 1: Double
  private var y_grid = 1: Double
  private def addMasterEntity {
    layer.world.add(new Global with Updateable {
      override def update(env: Environment, delta: Double) {
        label_world_size_x setText ("View center X (0-" + layer.world.settings.x_tiles * layer.world.settings.spacing + ")")
        label_world_size_y setText ("View center Y (0-" + layer.world.settings.y_tiles * layer.world.settings.spacing + ")")
        ((try {
          (text_x_grid.getText.toDouble, text_y_grid.getText.toDouble)
        } catch {
          case e: NumberFormatException => (1, 1)
        }): (Double, Double)) match {
          case (xg, yg) => {
            x_grid = xg
            y_grid = yg
          }
        }
        env.input.getMousePositionInLayer match {
          case (x, y) => {
            x_mouse = x
            y_mouse = y
            x_mouse_snap = Math.round(x_mouse / x_grid) * x_grid
            y_mouse_snap = Math.round(y_mouse / y_grid) * y_grid
            val spatials = layer.world.getSpatialsForPosition(x, y, x_grid / 2, y_grid / 2)
            if (spatials.length > 0) {
              entity_image setText (spatials(0).asInstanceOf[EntityRepresentation].image_name)
              entity_params setText (spatials(0).asInstanceOf[EntityRepresentation].params)
              entity_class setText (spatials(0).asInstanceOf[EntityRepresentation].class_name)
              entity_type setText (spatials(0).asInstanceOf[EntityRepresentation].type_name)
            } else {
              entity_type setText ("<type>")
              entity_params setText ("<params>")
              entity_image setText ("<image>")
              entity_class setText ("<class>")
            }
            label_x_mouse setText ("Mouse X: " + x_mouse_snap)
            label_y_mouse setText ("Mouse Y: " + y_mouse_snap)
          }
        }
      }
    })
  }
  addMasterEntity
  private var x_mouse_screen = 0: Int
  private var y_mouse_screen = 0: Int
  private var x_mouse_screen_prev = 0: Int
  private var y_mouse_screen_prev = 0: Int
  private def addMouse {
    if (layer.world.getSpatialsForPosition(
      x_mouse_snap, y_mouse_snap, x_grid / 2, y_grid / 2).length == 0) {
      layer.world.add(new EntityRepresentation(
        entity_types_combo.getSelectedItem.asInstanceOf[EntityItem].node,
        x_mouse_snap, y_mouse_snap, params_field.getText))
    }
  }
  private def deleteMouse {
    layer.world.getSpatialsForPosition(x_mouse, y_mouse, x_grid / 2, y_grid / 2).
      foreach(layer.world.remove(_))
  }
  private def actionMouse {
    if (render_canvas.getInputForLayer(null).isKeyPressed(KeyEvent.VK_SHIFT)) {
      layer.x_center -= x_mouse_screen - x_mouse_screen_prev
      layer.y_center -= y_mouse_screen - y_mouse_screen_prev
      text_view_center_x.setText(layer.x_center.toString)
      text_view_center_y.setText(layer.y_center.toString)
    } else {
      if (render_canvas.getInputForLayer(null).isKeyPressed(KeyEvent.VK_CONTROL)) {
        deleteMouse
      } else {
        addMouse
      }
    }
  }
  render_canvas.addMouseListener(new MouseAdapter {
    override def mousePressed(evt: MouseEvent) {
      x_mouse_screen = evt.getX
      y_mouse_screen = evt.getY
      x_mouse_screen_prev = x_mouse_screen
      y_mouse_screen_prev = y_mouse_screen
    }
  })
  render_canvas.addMouseMotionListener(new MouseMotionAdapter {
    override def mouseDragged(evt: MouseEvent) {
      x_mouse_screen_prev = x_mouse_screen
      y_mouse_screen_prev = y_mouse_screen
      x_mouse_screen = evt.getX
      y_mouse_screen = evt.getY
      actionMouse
    }
  })
  render_canvas.requestFocus
  repaint()
  validate
  setResizable(true)
}