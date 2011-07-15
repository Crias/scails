import sbt._
import Keys._
import sbt.Load.LoadedPlugins
import java.io.FileWriter
import java.util.Properties
import scala.collection.JavaConversions.asScalaSet
import org.fusesource.scalate.util._
import org.fusesource.scalate.TemplateEngine

object ScailsCommand extends Plugin {
  val engine = new TemplateEngine()
//  engine.resourceLoader = new FileResourceLoader {
//    override def resource(uri: String): Option[Resource] =
//      Some(Resource.fromText(uri, IO.readStream(resourceStream(uri))))
//  }

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
    if(file("build.sbt").exists()) file("build.sbt").delete()

      val extr = Project.extract(state)
      val buildStruct = extr.structure
      val plugins: LoadedPlugins = buildStruct.units(buildStruct.root).unit.plugins
      engine.classpath = plugins.classpath.filter(_.getName.contains("scala-compiler")).mkString(":")

    mvcImpl.prepDirectories()
    writePlugins()
    writeProject(appName)

    val initFolder = "init/lift_2.4-M1/"
    readProps(initFolder+"template.properties").foreach { entry => 
      val (key,value) = (entry.getKey().toString, entry.getValue()) 
      val templateResource = initFolder.replace("/","_") + key.replace("/","_")
      if(value == null || "" == value.toString)
        IO.transfer(resourceStream(initFolder + key), file(key))
      else
        IO.write(file(value.toString), engine.layout(templateResource, Map("appName" -> appName)))
    }
    readProps(initFolder+"directory.properties").foreach { entry => 
      file(entry.getKey.toString).mkdirs
    }

    setupCommands(appName).foldLeft(state){(state : State, command : String) =>
      Command.process(command,state)
    }
  }

  // The following method inspired by https://github.com/fhars/sbt-ensime-plugin
  def getClasspath(state : State, cpTaskKey : TaskKey[Classpath]) {
      val extr = Project.extract(state)
      import extr._

      val cpOpt : Option[Seq[Attributed[File]]] = 
  Project.evaluateTask(cpTaskKey in (currentRef, Test), state) flatMap {
    case Inc(_) => None
    case Value(v) => Some(v)
  }
  for (cp <- cpOpt) {
      val jars = cp.map(_.data).filter(f => f.exists && f.isFile)
      def fmt(files: Seq[File]) = files.map("\"" + _ + "\"").reduceRight(_+" "+_)
      if(jars != Nil)
        println(cpTaskKey.key.label + " (" + fmt(jars) + ")")
      else println(cpTaskKey.key.label + " is empty")
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
    "\n" +
    "libraryDependencies += \"org.eclipse.jetty\" % \"jetty-webapp\" % \"7.3.0.v20110203\" % \"jetty\"\n\n" + 
    "libraryDependencies += \"com.codequirks\" %% \"scails\" % \"0.1.0\"\n\n" + 
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
