import sbt._
import Keys._
import complete.DefaultParsers._

object ScailsCommand extends Plugin {
  override lazy val settings = Seq(
    commands ++= Seq(scailsMain, scaffoldMain)
  )

  def initCommands(appName: String) = List(
      "set name := \""+ appName +"\"",
      """set libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % "7.3.0.v20110203" % "jetty" """,
      """set libraryDependencies += "net.liftweb" %% "lift-webkit" % "2.4-M1" % "compile->default" """,
      """set libraryDependencies += "net.liftweb" %% "lift-mapper" % "2.4-M1" % "compile->default" """,
      """set libraryDependencies += "net.liftweb" %% "lift-wizard" % "2.4-M1" % "compile->default" """,
      "session save",
      "reload"
    )

  lazy val scailsMain = 
    Command.args("scail", "<appName>") { (state: State, args : Seq[String]) => val appName = args(0)
      val newState = initCommands(appName).foldLeft(state)(processCommands)
      startingDirectories.foreach(file(_).mkdirs)
      execute(newState, Lift.init, Map("appName" -> appName))
    }

  lazy val scaffold = Space ~> token(ID.examples("<scaffoldName>")) ~ typeList
  lazy val typeList = Space ~> token(rep1sep(field, Space))
  lazy val field = ID.examples("<fieldName>") ~ typeDec map { t => FieldDefinition(t._1, t._2)}
  lazy val typeDec = ":" ~> (
    "string" ^^^ StringData | 
    "text" ^^^ TextData |
    "bool" ^^^ BooleanData |
    "date" ^^^ DateData |
    "datetime" ^^^ DateTimeData |
    "double" ^^^ DoubleData |
    "int" ^^^ IntData |
    "long" ^^^ LongData)
  lazy val scaffoldMain = 
    Command("scaffold")(_ => scaffold){ (state, args) => val (scaffoldName, typeList) = (args._1, args._2)
      val arguments = Map(
        "properName" -> toProper(scaffoldName), 
        "lowerName" -> scaffoldName.toLowerCase,
        "fieldList" -> typeList)
      execute(state, Lift.scaffold(toProper(scaffoldName)), arguments)
    }

  def execute(state : State, command : ScailsCommandExecution, templateArguments : Map[String,Any] = Map()) = {
   command match { case ScailsCommandExecution(templates, tasks, commands) => 
      templates.foreach { t =>
        TemplateRunner.runTemplate(t, templateArguments)
      }
      tasks.foreach(_.apply())
      commands.foldLeft(state)(processCommands)
    }
  }

  def toProper(name : String) = name.head.toUpper + name.tail
  val startingDirectories = List("src/main/scala", "src/main/resources", "src/main/webapp")
  val processCommands = (state:State, command:String) => Command.process(command,state)
}

object Lift {
  def init = ScailsCommandExecution(templates = List("init/lift_2.4-M1/"), commands = List("reload", "update"))
  def scaffold(properName : String) = ScailsCommandExecution(templates = List("scaffold/lift_2.4-M1/"), tasks = List(() => {
    val lowerName = properName.toLowerCase
    val menuFile = "src/main/scala/bootstrap/liftweb/ScaffoldList.scala"
    val menu = LiftMenuParser(IO.read(file(menuFile)))
    val newMenu = menu match {
      case Some(v) => v.addScaffold(properName)
      case None => throw new RuntimeException("No ScaffoldList.scala found. Please restore the file.")
    }
    IO.write(file(menuFile), newMenu.toString)
  }))
}

case class FieldDefinition(name : String, typeInfo : DataType) {
  val properName = name.head.toUpper + name.tail
  val lowerName = name.toLowerCase
}

sealed trait DataType
case object StringData extends DataType
case object TextData extends DataType
case object BooleanData extends DataType
case object DateData extends DataType
case object DateTimeData extends DataType
case object DoubleData extends DataType
case object IntData extends DataType
case object LongData extends DataType

case class ScailsCommandExecution(
  templates : List[String] = List[String](), 
  tasks : List[Function0[Unit]] = List(()=>{}), 
  commands : List[String] = List[String]())