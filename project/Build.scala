import sbt._
import Keys._
import Project.Initialize

import java.util.Properties
import java.io.FileInputStream

import scala.collection.JavaConversions.asScalaSet

object MyBuild extends Build {
  import TemplateProcessor._

  val templateDirectory = "target/templates-prepared"
  val scalateTemplateDir = SettingKey[File]("scalate-template-directory")

  lazy val root = Project("root", file(".")) settings(
    sbtPlugin := true,
    name := "Scails",
    organization := "com.codequirks",
    version := "0.1.0",
    scalateTemplateDir := file("src/main/resources"),
    scalateLoggingConfig in Compile <<= scalateLoggingConfigValue,

    cleanFiles <<= (cleanFiles, baseDirectory) { 
      (files, basedir) => 
        files ++ Seq(new File(basedir, "/" + templateDirectory))
    },

    libraryDependencies ++= Seq(
      "org.fusesource.scalate" % "scalate-core" % "1.4.1" % "compile",
      "ch.qos.logback" %  "logback-classic" % "0.9.28" % "compile"
    ),

    sourceGenerators in Compile <+= scalateTemplateDir map { startDir => 
      file(templateDirectory).mkdirs
      processDirectory(Seq[String](), startDir) 
      Seq[File]()
    },
    sourceGenerators in Compile <+= scalateTemplateProcessor(templateDirectory)
  )

  def processDirectory(basePath : Seq[String], dir : File) {
    file(templateDirectory).mkdirs
    dir.listFiles.filter(_.getName == "template.properties").foreach(copyTemplates(basePath, _))
    dir.listFiles.filter(_.isDirectory).foreach(f => processDirectory(basePath :+ f.getName, f))
  }
  
  def copyTemplates(basePath : Seq[String], propsFile : File) {
    val props = new Properties
    val templatePrefix = basePath.mkString("","_","_")
    props.load(new FileInputStream(propsFile))
    props.keySet.foreach { keyO =>
      val key = keyO.toString 
      if(isTemplate(key)) {
        val template = file(propsFile.getParent+"/"+key)
        val templateTarget = key.replace("/", "_")
        val target = file(templateDirectory+"/"+templatePrefix+templateTarget)
        IO.copyFile(template, target)
      }
    }
  }
  
  def isTemplate(key : String) = {
    key.endsWith(".scaml") || 
    key.endsWith(".jade") || 
    key.endsWith(".ssp") ||
    key.endsWith(".mousetache")
  }

}

// The following code is a copy of the com.zentrope.ScalatePlugin version 1.3
// The plugin, as was, could not properly process a directory of my choosing
// https://github.com/zentrope/xsbt-scalate-precompile-plugin
object TemplateProcessor {
  import org.fusesource.scalate.TemplateSource
  import org.fusesource.scalate.TemplateEngine

  val scalateLoggingConfig = SettingKey[File]("scalate-logging-config",
                                              "Logback config to get rid of that infernal debug output.")
  def scalateLoggingConfigValue: Initialize[File] =
    (resourceDirectory in Compile) { (d) => new File(d, "/logback.xml") }

  private def scala(template: File, outputdir: File) =
    new File(outputdir, "/%s.scala".format(template.getName.replaceAll("[.]", "_")))

  private def recognized(template: File) =
    TemplateEngine.templateTypes.filter(template.getName.endsWith(_)).size > 0

  private def updated(template: File, scala: File) =
    (! scala.exists()) || (template.lastModified > scala.lastModified)

  private def changed(template: File, outputdir: File) =
    recognized(template) && updated(template, scala(template, outputdir))

  private def code(engine: TemplateEngine, template: File) = {
    val source = TemplateSource.fromFile(template, template.getName)
    source.engine = engine
    source
    engine.generateScala(source).source
  }

  private def generate (engine: TemplateEngine, template: File, outputdir: File, log: Logger) = {
    log.info(" compiling template: " + template)
    IO.write(scala(template, outputdir), code(engine, template))
  }
    
  def scalateTemplateProcessor(templateDirectory : String) : Initialize[Task[Seq[File]]] =
    (streams, sourceManaged in Compile, scalateLoggingConfig in Compile) map {

      (out, outputDir, logConfig) => {

        // If we throw an exception here, it'll break the compile. Which is what
        // I want.

        System.setProperty("logback.configurationFile", logConfig.toString)

        val engine = new org.fusesource.scalate.TemplateEngine()
        engine.packagePrefix = ""

        val dir = file(templateDirectory)
        if (dir.listFiles != null)
          for (template <- dir.listFiles)
            generate(engine, template, outputDir, out.log)

        outputDir.listFiles match {
          case null => Seq()
          case (files) => files.toList
        }
      }
    }
}