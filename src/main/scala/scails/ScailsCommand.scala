import sbt._
import Keys._

object ScailsCommand extends Plugin {
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
    file("src/main/scala").mkdirs
    file("src/main/resources").mkdirs
    file("src/main/webapp").mkdirs
    file("src/test/scala").mkdirs

    TemplateRunner.runTemplate("init/lift_2.4-M1/", Map("appName" -> appName))
    setupCommands(appName).foldLeft(state){(state : State, command : String) =>
      Command.process(command,state)
    }
  }

  def setupCommands(appName:String)  : List[String] = 
    "reload" :: 
    "update" :: Nil
}
