import sbt._
import Keys._
import java.io.StringWriter;
import java.io.File
import java.io.FileWriter
import java.util.Properties
import scala.collection.JavaConversions._
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity

object ScailsCommand extends Plugin {

  val mvcImpl = new LiftApp

  override lazy val settings = Seq(
    commands += scailsMain
  )
  lazy val scailsMain = 
    Command.single("scail") { (state: State, appName : String) =>
      println("About to scale " + appName + "!")
      state
     scailApplication(state, appName)
    }

  def scailApplication(state : State, appName : String) = {
    Velocity.setProperty("resource.loader", "class")
    Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
    Velocity.init()
    val context = new VelocityContext()
    context.put( "appName", appName)

    if(file("build.sbt").exists()) file("build.sbt").delete()

    mvcImpl.prepDirectories()
    writePlugins()
    writeProject(appName)

    val initFolder = "init/lift_2.4-M1/"
    readProps(initFolder+"template.properties").foreach { entry => 
      val (key,value) = (entry.getKey(), entry.getValue()) 
      if(value == null || "" == value.toString)
        IO.transfer(resourceStream(initFolder + key), file(key.toString))
      else {
        val sw = new StringWriter
        val template = Velocity.getTemplate(initFolder + key)
        template.merge(context, sw)
        IO.write(file(value.toString), sw.toString)
      }
    }
    readProps(initFolder+"directory.properties").foreach { entry => 
      file(entry.getKey.toString).mkdirs
    }

    setupCommands(appName).foldLeft(state){(state : State, command : String) =>
      Command.process(command,state)
    }
  }

  def readProps(name:String) = {
    val templates = new Properties()
    templates.load(resourceStream(name))
    templates.entrySet
  }


  def resourceStream(name : String) = this.getClass().getClassLoader.getResourceAsStream(name)

  def writePlugins() {
    file("project/plugins").mkdirs
    file("project/plugins/build.sbt").delete()
    val fw = new FileWriter(file("project/plugins/build.sbt"))
    fw.write(pluginBuildFile)
    fw.close
  }

  def writeProject(appName : String) {
    file("build.sbt").delete()
    val fw = new FileWriter(file("build.sbt"))
    fw.write(projectBuildFile(appName))
    fw.close
  }

  def setupCommands(appName:String)  : List[String] = "reload" :: "update" :: Nil

  def pluginBuildFile = """
    |resolvers += "Web plugin repo" at "http://siasia.github.com/maven2"
    |
    |libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % ("0.1.0-"+v))
    |""".stripMargin

  def projectBuildFile(appName:String) = "name := \""+appName+"\"\n" +
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
  |libraryDependencies +=  "net.liftweb" %% "lift-webkit" % "2.4-M1" % "compile->default"
  |
  |libraryDependencies += "net.liftweb" %% "lift-mapper" % "2.4-M1" % "compile->default"
  |
  |libraryDependencies += "net.liftweb" %% "lift-wizard" % "2.4-M1" % "compile->default"
  |""".stripMargin   
}
