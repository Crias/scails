import sbt._
import Keys._
import java.io.File
import java.io.FileWriter
object ScailsCommand extends Plugin {

  val mvcImpl = new LiftApp

  val liftVersion = SettingKey[String]("liftVersion")

  override lazy val settings = Seq(commands += scailsMain)
  lazy val scailsMain = 
    Command.single("scail") { (state: State, projectName : String) =>
      println("About to scale " + projectName + "!")
      state
     scailApplication(state, projectName)
    }

  def scailApplication(state : State, projectName : String) = {
    if(file("build.sbt").exists()) file("build.sbt").delete()

    mvcImpl.prepDirectories()
    writePlugins()
    writeProject(projectName)

    IO.unzipStream(resourceStream("lift_2.4-M1.zip"), file("src/main"))

    setupCommands(projectName).foldLeft(state){(state : State, command : String) =>
      Command.process(command,state)
    }
  }

  def resourceStream(name : String) = this.getClass().getClassLoader.getResourceAsStream(name)

  def writePlugins() {
    file("project/plugins").mkdirs
    file("project/plugins/build.sbt").delete()
    val fw = new FileWriter(file("project/plugins/build.sbt"))
    fw.write(pluginBuildFile)
    fw.close
  }

  def writeProject(projectName : String) {
    file("build.sbt").delete()
    val fw = new FileWriter(file("build.sbt"))
    fw.write(projectBuildFile(projectName))
    fw.close
  }

  def setupCommands(projectName:String)  : List[String] = "reload" :: "update" :: Nil

  def pluginBuildFile = """
    |resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"
    |
    |libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % ("0.1.0-"+v))
    |""".stripMargin

  def projectBuildFile(projectName:String) = "name := \""+projectName+"\"\n" +
    "\n" +
    "seq(webSettings :_*)\n" + 
    "\n" +
    "libraryDependencies += \"org.eclipse.jetty\" % \"jetty-webapp\" % \"7.3.0.v20110203\" % \"jetty\"\n" + 
    mvcImpl.deps
}

trait MVC {
	val deps : String
	def prepDirectories() : Unit
}

class LiftApp extends MVC {
  def prepDirectories() {
  	file("src/main/scala").mkdirs
  	file("src/main/resources").mkdirs
  	file("src/main/webapp").mkdirs
  	file("src/test/scala").mkdirs
  }

  val deps : String = """
	|liftVersion := "2.4-M1"
	|
	|libraryDependencies <+= liftVersion { lV => "net.liftweb" %% "lift-webkit" % lV % "compile->default" }
	|
	|libraryDependencies <+= liftVersion { lV => "net.liftweb" %% "lift-mapper" % lV % "compile->default" }
	|
	|libraryDependencies <+= liftVersion { lV => "net.liftweb" %% "lift-wizard" % lV % "compile->default" }
	|""".stripMargin   
}
