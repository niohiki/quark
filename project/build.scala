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
      if(args.length != 2) println("Usage: startup [project-name] [package.name]")
      else{ 
	val projectName = args(0)
	val projectOrganization = args(1)
	val projectFolder = baseDirectory.value / "quark-projects" / projectName 
	val projectSource = projectOrganization.toLowerCase.split("\\.").toList
	  .foldLeft(projectFolder / "src")((file,next) => file / next) / projectName.toLowerCase
	val projectResources = projectOrganization.toLowerCase.split("\\.").toList
	  .foldLeft(projectFolder / "res")((file,next) => file / next) / projectName.toLowerCase / "resources"
	if(!projectFolder.exists()){
	  IO.write(projectFolder / "build.sbt", BuildFiles.buildFile(projectName, projectOrganization))
	  IO.write(projectSource / "Main.scala", BuildFiles.mainFile(projectName, projectOrganization))
	  IO.createDirectory(projectResources)
	  println("Created project "+projectName+" in "+projectFolder.getAbsolutePath)
	  println("Use \"reload\" to load the project into workspace")
	} else println("Project "+projectName+" already exists")
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
    packageOptions.in(Compile,packageBin).in(project) += { 
      Package.ManifestAttributes(java.util.jar.Attributes.Name.CLASS_PATH -> "lib/quark.jar")
    }
  )
  lazy val subProjectTasks = Seq(
    TaskKey[Unit]("dist","Make distribution folder") <<= (clean,packageBin.in(Compile).in(main),
							  packageBin.in(Compile).in(project)) map
    ((Unit,mainJar,thisJar) =>{ 
      IO.copy(Seq(
	mainJar -> folder / "dist" / "lib" / "quark.jar",
	thisJar -> folder / "dist" / (folder.getName+".jar")
      ))
      println("Gathered jar files in dist folder")
    }),
    update.in(project) <<= (update.in(project), classDirectory.in(Compile).in(project), name.in(project)) map ((report,classDir,projectName)=>{ 
      IO.write(folder / ".classpath", BuildFiles.classpathFile(IO.relativize(folder,classDir).getOrElse("bin")))
      IO.write(folder / ".project", BuildFiles.projectFile(projectName))
      report
    })
  )
  lazy val project: Project = Project("quark-"+folder.getName.toLowerCase, folder,
			     settings = Defaults.defaultSettings ++ subProjectSettings ++ subProjectTasks) dependsOn(main) aggregate(main)
}
object BuildFiles{ 
  def classpathFile(output:String): String =
"""<classpath>
  <classpathentry kind="src" path="src"/>
  <classpathentry kind="src" path="res"/>
  <classpathentry kind="src" path="quark-src"/>
  <classpathentry kind="con" path="org.scala-ide.sdt.launching.SCALA_COMPILER_CONTAINER"/>
  <classpathentry kind="con" path="org.scala-ide.sdt.launching.SCALA_CONTAINER"/>
  <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
  <classpathentry kind="output" path=""""+ output +""""/>
</classpath>
"""
 
  def projectFile(name:String): String =
"""<projectDescription>
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
  
  def buildFile(name:String, organization:String): String =
"""name := """" + name + """"

organization := """" + organization + """"

scalaSource in Compile := baseDirectory.value / "src"

unmanagedResourceDirectories in Compile += baseDirectory.value / "res"

mainClass in (Compile,packageBin) := Some("""" + organization.toLowerCase + """.""" + name.toLowerCase + """.Main")

"""
}
