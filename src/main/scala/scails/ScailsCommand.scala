import sbt._
import Keys._

object ScailsCommand extends Plugin {
  override lazy val settings = Seq(
    commands ++= Seq(scailsMain, scaffoldMain)
  )

  lazy val scailsMain = 
    Command.args("scail", "<appName>") { (state: State, args : Seq[String]) => val appName = args(0)
      println("About to scale " + appName + "!")
      scailApplication(state, appName)
    }

  lazy val scaffoldMain = 
    Command.args("scaffold", "<name>") { (state: State, args : Seq[String]) => val scaffoldName = args(0)
      println("Scaffolding " + scaffoldName)
      scaffold(state, scaffoldName)
    }

  def scailApplication(state : State, appName : String) = {
    file("src/main/scala").mkdirs
    file("src/main/resources").mkdirs
    file("src/main/webapp").mkdirs
    file("src/test/scala").mkdirs

    TemplateRunner.runTemplate("init/lift_2.4-M1/", Map("appName" -> appName))
    setupCommands(appName).foldLeft(state){(state : State, command : String) =>
      Command.process(command,state)
    }
  }

  def scaffold(state : State, scaffoldName : String) = {
    val menuFile = "src/main/scala/bootstrap/liftweb/LiftScaffoldMenu.scala"
    val menu = LiftMenuParser(IO.read(file(menuFile)))
    val newMenu = menu match {
      case Some(v) => v.addMenuItem(scaffoldName, scaffoldName)
      case None => SProgram(
          SPackage("bootstrap.liftweb"),
          List(SImport(List("net","liftweb","sitemap","_"))), 
          SObject("LiftScaffoldMenu", 
            SMenu("menu", 
              List(SMenuItem(scaffoldName, scaffoldName)))))
    }
    IO.write(file(menuFile), newMenu.toString)
    state
  }

  def setupCommands(appName:String)  : List[String] = 
    "reload" :: 
    "update" :: Nil
}
