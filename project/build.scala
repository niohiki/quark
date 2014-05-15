import sbt._
import Keys._
import complete.DefaultParsers._

object Build extends sbt.Build {
  def subProjects(base: File): Seq[Project] = if((base/"quark-projects").isDirectory) { 
    for (subProjectFolder <- (base / "quark-projects").listFiles if subProjectFolder.isDirectory) yield new SubProject(quark, subProjectFolder).project
  }else Seq[Project]()
  override def projectDefinitions(baseDirectory: File) = subProjects(baseDirectory) ++ Seq(quark)

  lazy val quarkTasks = Seq(
    InputKey[Unit]("startup","Creates new project") := { 
      val args = spaceDelimited("<args>").parsed
      if(args.length != 2) println("Usage: startup [project-name] [organization.name]")
      else{ 
	val projectName = args(0)
	val projectOrganization = args(1)
	val projectFolder = baseDirectory.value / "quark-projects" / projectName 
	val projectSource = projectOrganization.toLowerCase.split("\\.").toList
	  .foldLeft(projectFolder / "src")((file,next) => file / next) / projectName.toLowerCase
	IO.write(projectSource / "Main.scala", BuildFiles.mainFile(projectName, projectOrganization))
	IO.write(projectFolder / ".classpath", BuildFiles.classpathFile(projectName, projectOrganization))
	IO.write(projectFolder / ".project", BuildFiles.projectFile(projectName))
      }
    }
  )
  lazy val quarkSettings = Seq(
    name := "Quark", version := "1.0", organization := "org.niohiki",
    scalaSource in Compile := baseDirectory.value / "quark-src",
    unmanagedResourceDirectories in Compile += baseDirectory.value / "quark-src"
  )
  lazy val quark = Project("quark", file("."), settings = Defaults.defaultSettings ++ quarkSettings ++ quarkTasks)
}
class SubProject(main:Project,folder:File){ 
  lazy val subProjectSettings = Seq(
    name := folder.getName,
    scalaSource in Compile := folder / "src",
    unmanagedResourceDirectories in Compile += folder / "src",
    packageOptions in (Compile,packageBin) += { 
      Package.ManifestAttributes(java.util.jar.Attributes.Name.CLASS_PATH -> "lib/quark.jar")
    }
  )
  lazy val subProjectTasks = Seq(
    TaskKey[Unit]("dist","Make distribution folder") := { 
      IO.copy(Seq(
	packageBin.in(Compile).in(main).value -> folder / "dist" / "lib" / "quark.jar",
	packageBin.in(Compile).in(project).value -> folder / "dist" / (folder.getName+".jar")
      ))
      println("Gathered jar files in dist folder")
    }
  )
  lazy val project: Project = Project("quark-"+folder.getName.toLowerCase, folder,
			     settings = Defaults.defaultSettings ++ subProjectSettings ++ subProjectTasks) dependsOn(main) aggregate(main)
}
object BuildFiles{ 
  def classpathFile(name:String, organization:String): String = """
  <classpath>
    <classpathentry kind="src" path="src"/>
    <classpathentry kind="src" path="quark-src"/>
    <classpathentry kind="con" path="org.scala-ide.sdt.launching.SCALA_COMPILER_CONTAINER"/>
    <classpathentry kind="con" path="org.scala-ide.sdt.launching.SCALA_CONTAINER"/>
    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
    <classpathentry kind="output" path=".bin"/>
  </classpath>
  """
 
  def projectFile(name:String): String = """
  <projectDescription>
    <name>"""+name+"""</name>
    <buildSpec>
      <buildCommand>
	<name>org.scala-ide.sdt.core.scalabuilder</name>
      </buildCommand>
    </buildSpec>
    <natures>
      <nature>org.scala-ide.sdt.core.scalanature</nature>
      <nature>org.eclipse.jdt.core.javanature</nature>
    </natures>
    <linkedResources>
      <link>
	<name>quark-src</name>
	<type>2</type>
	<locationURI>PARENT-2-PROJECT_LOC/quark-src</locationURI>
      </link>
    </linkedResources>
  </projectDescription>
  """

  def mainFile(name:String, organization:String): String = 
"""package """+organization.toLowerCase+"""."""+name.toLowerCase+"""
import org.niohiki.quark.core._
object Main {
  def main(args: Array[String]) {
    val window = new RenderWindow(ApplicationSettings(title = """"+name+""""), CanvasSettings(width = 400, height = 400, buffers = 2, debug_mode = false),  ThreadSettings(update_cps = 60, render_cps = 60, background_cps = 30, delay_factor = 4), null, rc => {
    })
  }
}"""
}
